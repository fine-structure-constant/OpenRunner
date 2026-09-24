package cn.edu.pku.openrunner.feature.run.data

import android.annotation.SuppressLint
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * 旧版记录完整性字段的兼容实现。
 *
 * **格式由旧客户端与服务端协议钉死，不要「改进」它。** 两个字段各自的来历见
 * `docs/PKUNEWYOUTH_API.md` 的「兼容旧 Android 客户端的字段生成规则」：
 *
 *  - [generateCheckField] / [verifyCheckField]：**只存在本机、不上传**。以
 *    `"${userId}_${dateMillis}"` 为 AES 密钥、用 `AES/ECB/PKCS5Padding` 加密固定协议
 *    文本 `"android11"`。上传前用它校验本地记录是否与当前账号、完成时间匹配
 *    （对不上就拒绝上传，提示「本地记录完整性校验失败」）。
 *  - [uploadAbstract]：**要上传**，是服务端要求的摘要字段，算法写死在协议里。
 *
 * ## 为什么这里用 ECB 不是缺陷，lint 的 `GetInstance` 是误报
 *
 * lint 报「Cipher.getInstance with ECB」时的默认假设是「ECB 会泄漏明文结构」。
 * 这条假设在这里不成立，三个理由缺一不可：
 *
 *  1. **明文是公开常量** `"android11"`，不是用户数据 —— ECB 的结构泄漏（相同明文块
 *     产生相同密文）没有可泄漏的东西；
 *  2. **密钥完全由记录自身字段推出**（userId + 完成时间），它本来就不是秘密，
 *     换成任何模式都不会增加保密性；
 *  3. **密文不离开设备**，所以也不存在网络侧被分析的面。
 *
 * 真正要防的是「本地记录被改过」—— 改了 userId 或完成时间，重新推导出的 checkField
 * 就对不上。**ECB 的确定性恰好是这个用途需要的性质**，换成带随机 IV 的模式反而要额外
 * 存储 IV。
 *
 * ## 所以改它是破坏，不是修复
 *
 * 换成 AES/GCM 会改变密文字节，于是旧客户端写下的记录全部校验失败、再也传不上去。
 * `LegacyRecordFieldTest` 把旧版实现（`pkuautorun/autorun/crypto.py` 与
 * `pkuautorun/tools/oracle/CryptoOracle.java`）产出的 golden 值钉成了断言 ——
 * 谁改算法谁就会看到它变红。
 *
 * ## 密钥长度：这里有一个真实缺陷，已修
 *
 * AES 只接受 16/24/32 字节的密钥，而 `"${userId}_${dateMillis}"` 的长度取决于 userId。
 * 10 位学号 + `"_"` + 13 位毫秒正好 24 字节，所以旧版能跑；**但接口文档只说
 * 「学生账号通常是学号」（`docs/PKUNEWYOUTH_API.md:77`），并没有保证 10 位。**
 * 实测（`java CryptoOracle.java <9 位 id> <millis>`）会抛
 * `InvalidKeyException: Invalid AES key length: 23 bytes`；而 [generateCheckField]
 * 原先没有兜底，异常会一路冒到 `RunViewModel`，于是这类账号**每次保存记录都失败**，
 * 界面上还会显示 "Invalid AES key length: 23 bytes" 这种实现细节。
 *
 * 修法见 [keyMaterial]。因为这个字段不上传、且旧客户端在长度不合法时**同样抛异常、
 * 根本存不下记录**，所以「非 10 位账号」不存在需要兼容的历史密文 —— 退化不影响任何人。
 */
@SuppressLint("GetInstance") // ECB 由旧版协议规定，理由见类注释；改掉它会破坏旧记录
object RunRecordSecurity {
    fun generateCheckField(userId: String, dateMillis: Long): String =
        Base64.encodeToString(checkFieldBytes(userId, dateMillis), Base64.NO_WRAP)

    fun verifyCheckField(userId: String, dateMillis: Long, value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return runCatching {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyMaterial(userId, dateMillis), "AES"))
            val decoded = Base64.decode(value, Base64.NO_WRAP)
            String(cipher.doFinal(decoded), Charsets.UTF_8) == CHECK_FIELD_PLAINTEXT
        }.getOrDefault(false)
    }

    fun uploadAbstract(userId: String, dateMillis: Long): String = abstractHex(userId, dateMillis)

    /**
     * 旧版 check 字段的密文字节（不含 Base64 编码）。
     *
     * 公开到 `internal` 是为了可测试性：单元测试跑在没有 Android 运行时的 JVM 上，
     * `android.util.Base64` 在那里是 stub，所以能被断言的边界必须停在**字节**这一层。
     * 生产代码请用 [generateCheckField]。
     */
    internal fun checkFieldBytes(userId: String, dateMillis: Long): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyMaterial(userId, dateMillis), "AES"))
        return cipher.doFinal(CHECK_FIELD_PLAINTEXT.toByteArray(Charsets.UTF_8))
    }

    /** 旧版 abstract 字段。纯 JVM，无 Android 依赖。 */
    internal fun abstractHex(userId: String, dateMillis: Long): String =
        MessageDigest.getInstance("SHA-256")
            .digest("${userId}_${dateMillis}_$ABSTRACT_SALT".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .take(32)

    /**
     * 旧版密钥材料：`"${userId}_${dateMillis}"` 的 UTF-8 字节。
     *
     * 长度是 16/24/32 时**原样返回** —— 这一步保证与旧客户端逐字节一致，旧记录才校验得过
     * （10 位学号 + 13 位毫秒 = 24 字节，是唯一实际出现过的形态）。
     *
     * 长度不合法时（接口文档只说 id「通常是学号」）退化为 SHA-256 派生的 24 字节密钥：
     * 旧客户端在同一个地方就抛 InvalidKeyException、存不下记录，所以没有历史密文要兼容，
     * 这里只需要 generate 与 verify 自洽。
     */
    private fun keyMaterial(userId: String, dateMillis: Long): ByteArray {
        val raw = "${userId}_$dateMillis".toByteArray(Charsets.UTF_8)
        if (raw.size in VALID_AES_KEY_LENGTHS) return raw
        return MessageDigest.getInstance("SHA-256").digest(raw).copyOf(24)
    }

    /** 旧版协议规定的明文，见 `pkuautorun/autorun/config.py` 的 `CHECK_FIELD_PLAINTEXT`。 */
    private const val CHECK_FIELD_PLAINTEXT = "android11"

    /** 旧版协议规定的盐，见 `pkuautorun/autorun/config.py` 的 `ABSTRACT_SALT`。 */
    private const val ABSTRACT_SALT = "YCVNc92y"

    /** AES 只接受这三种密钥长度。 */
    private val VALID_AES_KEY_LENGTHS = setOf(16, 24, 32)
}
