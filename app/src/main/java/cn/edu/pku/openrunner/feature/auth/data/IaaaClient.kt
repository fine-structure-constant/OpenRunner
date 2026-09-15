package cn.edu.pku.openrunner.feature.auth.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.security.MessageDigest

enum class SecondFactorMode {
    NONE,
    SMS,
    OTP
}

data class SmsChallenge(val mobileMask: String?)

/**
 * Small client for the official PKU IAAA service.
 *
 * IAAA uses a sorted form string plus an MD5 message digest. The signing
 * constant is a public client protocol value, not a user credential.
 */
class IaaaClient(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun querySecondFactorMode(
        username: String,
        appId: String = DEFAULT_APP_ID
    ): SecondFactorMode = withContext(Dispatchers.IO) {
        val response = post(
            "iaaa/svc/authen/isMobileAuthen.do",
            mapOf("appId" to appId, "userName" to username)
        )
        requireSuccess(response, "查询二次认证方式失败")
        when {
            response.optString("authenMode") == "SMS" -> SecondFactorMode.SMS
            response.optString("authenMode") == "OTP" &&
                response.optBoolean("isBind", false) -> SecondFactorMode.OTP
            else -> SecondFactorMode.NONE
        }
    }

    suspend fun requestSmsCode(
        username: String,
        appId: String = DEFAULT_APP_ID
    ): SmsChallenge = withContext(Dispatchers.IO) {
        val response = post(
            "iaaa/svc/authen/sendSMSCode.do",
            mapOf("appId" to appId, "userName" to username)
        )
        requireSuccess(response, "发送短信验证码失败")
        SmsChallenge(response.optString("mobileMask").ifBlank { null })
    }

    suspend fun login(
        username: String,
        password: String,
        verificationCode: String = "",
        mode: SecondFactorMode = SecondFactorMode.NONE,
        appId: String = DEFAULT_APP_ID
    ): String = withContext(Dispatchers.IO) {
        val response = post(
            "iaaa/svc/authen/login.do",
            mapOf(
                "appId" to appId,
                "otpCode" to if (mode == SecondFactorMode.OTP) verificationCode else "",
                "password" to password,
                "randCode" to "",
                "smsCode" to if (mode == SecondFactorMode.SMS) verificationCode else "",
                "userName" to username
            )
        )
        requireSuccess(response, "IAAA 登录失败")
        response.optString("token").takeIf { it.isNotBlank() }
            ?: error("IAAA 返回了空 Token")
    }

    private fun post(path: String, params: Map<String, String>): JSONObject {
        val body = params.toSortedMap()
            .entries
            .joinToString("&") { (key, value) -> key + "=" + value }
        val signedBody = body + "&msgAbs=" + md5(body + SIGNING_KEY)
        val request = Request.Builder()
            .url(BASE_URL + path)
            .post(RequestBody.create(FORM_MEDIA_TYPE, signedBody))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val payload = response.body()?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IaaaException("IAAA HTTP " + response.code())
            }
            return JSONObject(payload)
        }
    }

    private fun requireSuccess(response: JSONObject, prefix: String) {
        val success = response.optBoolean("success", false) ||
            response.optString("success") == "true"
        if (!success) {
            throw IaaaException(prefix + "：" + response.optString("errMsg"))
        }
    }

    private fun md5(value: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    companion object {
        const val BASE_URL = "https://iaaa.pku.edu.cn/"
        const val DEFAULT_APP_ID = "PKU_Runner"
        private const val SIGNING_KEY = "7696baa1fa4ed9679441764a271e556e"
        private val FORM_MEDIA_TYPE = MediaType.parse(
            "application/x-www-form-urlencoded"
        )
    }
}

class IaaaException(message: String) : Exception(message)
