package cn.edu.pku.openrunner.feature.run.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * 钉住旧版记录完整性字段的**字节级格式**。
 *
 * 为什么值得一个测试：这两个字段的格式不是我们能选的，是旧客户端协议定的
 * （`docs/PKUNEWYOUTH_API.md` 的「兼容旧 Android 客户端的字段生成规则」）。
 * 而 `RunRecordSecurity` 里那个 `AES/ECB/PKCS5Padding` 会被 lint 报成
 * 「Cipher.getInstance with ECB」—— **那条警告在这里是误报，但看起来很像真问题**。
 * 谁照着它去「修」成 GCM，密文字节就变了，于是旧客户端写下的记录全部校验失败。
 *
 * 注释只能提醒愿意读的人；断言拦得住没读的人。所以这里把旧版实现产出的值钉死。
 *
 * ## golden 值从哪来（三个独立来源互相印证）
 *  1. `pkuautorun/tests/test_crypto.py` 里旧版自己记录的 `JAVA_ORACLE_*` 常量；
 *  2. 本次用 `pkuautorun/tools/oracle/CryptoOracle.java`（JCE）复跑，与上面逐个字符一致；
 *  3. `abstract` 另用标准库 `hashlib.sha256` 独立算过，与前两者一致。
 *
 * ## 一处刻意的背离
 * 旧版 `tests/test_crypto.py::test_invalid_key_length_raises` 断言「密钥长度非法就抛错」。
 * 那个行为在这里被改掉了 —— 理由与代价见
 * [idLengthsTheLegacyClientCouldNotHandleNowProduceAField]。
 *
 * 与 NavGeometryTest / ShapeTokenTest 同一路数：纯 JUnit，不需要 Android 运行时。
 * 注意这里断言的是**密文字节**而不是 `generateCheckField` 的字符串结果 —— 后者要过
 * `android.util.Base64`，而单元测试跑在没有 Android 运行时的 JVM 上，那个类是 stub。
 * `java.util.Base64.getEncoder()` 与 `android.util.Base64.encodeToString(bytes, NO_WRAP)`
 * 同为标准字母表 + `=` 补位、不换行，所以两者等价。
 */
class LegacyRecordFieldTest {

    @Test
    fun checkFieldMatchesTheLegacyOracle() {
        assertEquals(
            "checkField 的字节格式由旧客户端协议钉死：AES/ECB/PKCS5Padding 加密固定明文 " +
                "\"android11\"，密钥是 userId + \"_\" + dateMillis 的 UTF-8 字节。" +
                "这个值来自旧版仓库自己记录的 oracle 常量（pkuautorun/tests/test_crypto.py），" +
                "本次已用 CryptoOracle.java 复跑核对。**改了算法，旧客户端写下的本地记录就再也" +
                "校验不过（上传时提示「本地记录完整性校验失败」）。**",
            LEGACY_ORACLE_CHECK_FIELD,
            encode(RunRecordSecurity.checkFieldBytes(LEGACY_USER_ID, LEGACY_DATE_MILLIS))
        )
    }

    @Test
    fun abstractMatchesTheLegacyOracle() {
        val actual = RunRecordSecurity.abstractHex(LEGACY_USER_ID, LEGACY_DATE_MILLIS)
        assertEquals(
            "abstract 是**要上传**的字段，服务端按同样规则校验，改它等于改协议。" +
                "（这个值另用标准库 hashlib.sha256 独立算过。）",
            LEGACY_ORACLE_ABSTRACT,
            actual
        )
        assertEquals("abstract 固定取 32 个十六进制字符 —— 协议里写死的长度。", 32, actual.length)
    }

    @Test
    fun checkFieldUsesTheRawKeyForTheThirtyTwoByteKeyLength() {
        // 密钥长度 = id 长度 + 1 + 13。AES 只接受 16/24/32，所以旧版实际上只在
        // id 长 2 / 10 / 18 位时能跑（10 位=学号 → 24 字节；18 位 → 32 字节）。
        // 上面那条钉住 24 字节档，这条钉住 32 字节档，确认「原样用密钥材料」在两种合法长度下都成立。
        assertEquals(
            "id 长 18 位时密钥是 32 字节（AES-256），同样走「原样使用密钥材料」这条路。" +
                "值由 pkuautorun/tools/oracle/CryptoOracle.java 算出。",
            "v72o34dtoDAx49SlhL8AVg==",
            encode(RunRecordSecurity.checkFieldBytes("210001234567890123", 1758700000000L))
        )
    }

    @Test
    fun checkFieldDiffersByAccountAndByDate() {
        // 这个字段存在的全部意义：改了本地记录里的 userId 或完成时间，重新推导出的
        // checkField 就对不上。所以「同输入必同输出、异输入必异输出」是它的功能本身，
        // 不是附带性质。
        val base = encode(RunRecordSecurity.checkFieldBytes(LEGACY_USER_ID, LEGACY_DATE_MILLIS))

        assertNotEquals(
            "换了账号（或改了记录里的 userId）必须推导出不同的值，否则完整性校验形同虚设。",
            base,
            encode(RunRecordSecurity.checkFieldBytes("2400011462", LEGACY_DATE_MILLIS))
        )
        assertNotEquals(
            "改了完成时间必须推导出不同的值。旧版 tests/test_crypto.py 里也有这条对拍。",
            base,
            encode(RunRecordSecurity.checkFieldBytes(LEGACY_USER_ID, LEGACY_DATE_MILLIS + 1000))
        )
        assertTrue(
            "同一输入必须稳定 —— 保存时算一次、上传前再算一次，两次不一致就永远传不上去。",
            base == encode(RunRecordSecurity.checkFieldBytes(LEGACY_USER_ID, LEGACY_DATE_MILLIS))
        )
    }

    @Test
    fun idLengthsTheLegacyClientCouldNotHandleNowProduceAField() {
        // 这里是与旧版实现**刻意背离**的一处，写清楚理由与代价：
        //
        // 旧版（pkuautorun/tests/test_crypto.py::test_invalid_key_length_raises）断言
        // 「密钥长度非法就抛错」。但接口文档只说 id「学生账号通常是学号」
        // （docs/PKUNEWYOUTH_API.md:77），**没有保证 10 位**；实测 9 位 id 会抛
        // InvalidKeyException: Invalid AES key length: 23 bytes。
        // 旧版抛错只是「存不下记录」，而这个 app 原先没有兜底，异常会冒到 RunViewModel，
        // 于是这类账号**每次保存记录都失败**，界面还显示 "Invalid AES key length: 23 bytes"。
        //
        // 改法：长度合法时原样用（旧记录逐字节兼容，见上面两条 golden 断言），
        // 否则退化为 SHA-256 派生的 24 字节密钥。这样不破坏任何人：
        //   - checkField 只存在本机、不上传，服务器只要 abstract（纯 SHA-256，与密钥长度无关）；
        //   - 四个共存版本 applicationId 不同、数据目录互不可见，旧版读不到这里的记录；
        //   - 旧客户端在长度不合法时根本存不下记录，所以不存在需要兼容的历史密文。
        val nineDigit = RunRecordSecurity.checkFieldBytes("240001146", LEGACY_DATE_MILLIS)
        val elevenDigit = RunRecordSecurity.checkFieldBytes("24000114611", LEGACY_DATE_MILLIS)

        assertEquals(
            "明文 \"android11\" 是 9 字节，PKCS5 补齐到一个 AES 块 → 密文 16 字节。" +
                "这个长度也顺带确认了兜底路径产出的确实是合法 AES 密文（而不是被静默截断）。",
            16,
            nineDigit.size
        )
        assertEquals(16, elevenDigit.size)
        assertNotEquals(
            "不同账号仍要推导出不同的值 —— 兜底不能退化成一个与账号无关的常量。",
            encode(nineDigit),
            encode(elevenDigit)
        )
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private companion object {
        /** 旧版 `pkuautorun/tests/test_crypto.py` 里的输入。 */
        const val LEGACY_USER_ID = "2400011461"
        const val LEGACY_DATE_MILLIS = 1789414987183L

        /** 旧版记录下来的 Java oracle 输出，本次已复跑核对。 */
        const val LEGACY_ORACLE_CHECK_FIELD = "r8P+TlyKHk/dTJQz3nZA1g=="
        const val LEGACY_ORACLE_ABSTRACT = "f4f14b461b5163db3ae6f79a0a90bc21"
    }
}
