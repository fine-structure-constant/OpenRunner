package cn.edu.pku.openrunner.feature.run.data

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** Compatibility implementation of the legacy record integrity fields. */
object RunRecordSecurity {
    fun generateCheckField(userId: String, dateMillis: Long): String {
        val key = "${userId}_${dateMillis}"
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"))
        return Base64.encodeToString(
            cipher.doFinal(CHECK_TEXT.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
    }

    fun verifyCheckField(userId: String, dateMillis: Long, value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return runCatching {
            val key = "${userId}_${dateMillis}"
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"))
            val decoded = Base64.decode(value, Base64.NO_WRAP)
            String(cipher.doFinal(decoded), Charsets.UTF_8) == CHECK_TEXT
        }.getOrDefault(false)
    }

    fun uploadAbstract(userId: String, dateMillis: Long): String {
        val source = "${userId}_${dateMillis}_YCVNc92y"
        return MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .take(32)
    }

    private const val CHECK_TEXT = "android11"
}
