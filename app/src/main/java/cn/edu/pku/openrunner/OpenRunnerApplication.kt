package cn.edu.pku.openrunner

import android.app.Application
import cn.edu.pku.openrunner.core.AmapPrivacyController
import cn.edu.pku.openrunner.core.AppThemeStore
import cn.edu.pku.openrunner.core.network.ApiClient

class OpenRunnerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppThemeStore(this).applySavedMode()
        AmapPrivacyController.synchronize(this)
        ApiClient.initialize(this)
    }
}
