package cn.edu.pku.openrunner.feature.auth.ui

import android.os.Bundle
import android.app.Activity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.core.session.SessionStore
import cn.edu.pku.openrunner.feature.auth.data.AuthRepository
import cn.edu.pku.openrunner.feature.auth.data.IaaaClient
import cn.edu.pku.openrunner.feature.auth.data.SecondFactorMode
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity(R.layout.activity_auth) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usernameInput = findViewById<TextInputEditText>(R.id.auth_username)
        val passwordInput = findViewById<TextInputEditText>(R.id.auth_password)
        val codeInput = findViewById<TextInputEditText>(R.id.auth_code)
        val codeContainer = findViewById<LinearLayout>(R.id.auth_code_container)
        val sendCode = findViewById<Button>(R.id.auth_send_code)
        val submit = findViewById<Button>(R.id.auth_submit)
        val status = findViewById<TextView>(R.id.auth_status)
        val iaaaClient = IaaaClient()
        val sessionRepository = AuthRepository(SessionStore(this))
        var factorMode = SecondFactorMode.NONE
        var factorChecked = false

        sendCode.setOnClickListener {
            val username = usernameInput.text?.toString()?.trim().orEmpty()
            if (username.isBlank() || factorMode != SecondFactorMode.SMS) return@setOnClickListener
            sendCode.isEnabled = false
            lifecycleScope.launch {
                runCatching { iaaaClient.requestSmsCode(username) }
                    .onSuccess { challenge ->
                        val suffix = challenge.mobileMask?.let { "（" + it + "）" }.orEmpty()
                        status.text = getString(R.string.login_code_sent, suffix)
                    }
                    .onFailure { error ->
                        status.text = error.message ?: "发送验证码失败"
                    }
                sendCode.isEnabled = true
            }
        }

        submit.setOnClickListener {
            val username = usernameInput.text?.toString()?.trim().orEmpty()
            val password = passwordInput.text?.toString().orEmpty()
            val code = codeInput.text?.toString()?.trim().orEmpty()
            if (username.isBlank() || password.isBlank()) {
                status.setText(R.string.login_empty_credentials)
                return@setOnClickListener
            }
            submit.isEnabled = false
            lifecycleScope.launch {
                try {
                    if (!factorChecked) {
                        status.setText(R.string.login_checking_factor)
                        factorMode = iaaaClient.querySecondFactorMode(username)
                        factorChecked = true
                        codeContainer.visibility =
                            if (factorMode == SecondFactorMode.NONE) {
                                android.view.View.GONE
                            } else {
                                android.view.View.VISIBLE
                            }
                        status.text = when (factorMode) {
                            SecondFactorMode.SMS -> "需要短信验证码，请点击发送验证码"
                            SecondFactorMode.OTP -> "需要 OTP 验证码"
                            SecondFactorMode.NONE -> "正在登录…"
                        }
                        if (factorMode != SecondFactorMode.NONE) return@launch
                    }
                    if (factorMode != SecondFactorMode.NONE && code.isBlank()) {
                        status.setText(R.string.login_empty_code)
                        return@launch
                    }
                    val iaaaToken = iaaaClient.login(
                        username = username,
                        password = password,
                        verificationCode = code,
                        mode = factorMode
                    )
                    val user = sessionRepository.exchangeIaaaToken(iaaaToken)
                    status.text = getString(R.string.login_success, user.id)
                    passwordInput.text?.clear()
                    codeInput.text?.clear()
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (error: Exception) {
                    status.text = getString(
                        R.string.login_error,
                        error.message ?: "未知错误"
                    )
                    factorChecked = false
                    factorMode = SecondFactorMode.NONE
                    codeContainer.visibility = android.view.View.GONE
                } finally {
                    submit.isEnabled = true
                }
            }
        }
    }
}
