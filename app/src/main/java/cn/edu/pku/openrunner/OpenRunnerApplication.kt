package cn.edu.pku.openrunner

import android.app.Application
import cn.edu.pku.openrunner.core.network.ApiClient

class OpenRunnerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }
}
