package cn.edu.pku.openrunner.core

import android.content.Context
import androidx.core.content.edit
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer

class AmapPrivacyStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE
    )

    val hasDecision: Boolean
        get() = preferences.contains(KEY_AGREED)

    val isAgreed: Boolean
        get() = preferences.getBoolean(KEY_AGREED, false)

    fun recordDecision(agreed: Boolean) {
        preferences.edit(commit = true) {
            putBoolean(KEY_AGREED, agreed)
        }
    }

    companion object {
        private const val FILE_NAME = "openrunner_amap_privacy"
        private const val KEY_AGREED = "agreed"
    }
}

object AmapPrivacyController {
    fun synchronize(context: Context) {
        val store = AmapPrivacyStore(context)
        updateSdk(context, noticeShown = store.hasDecision, agreed = store.isAgreed)
    }

    fun recordDecision(context: Context, agreed: Boolean) {
        AmapPrivacyStore(context).recordDecision(agreed)
        updateSdk(context, noticeShown = true, agreed = agreed)
    }

    private fun updateSdk(context: Context, noticeShown: Boolean, agreed: Boolean) {
        val appContext = context.applicationContext
        MapsInitializer.updatePrivacyShow(appContext, true, noticeShown)
        MapsInitializer.updatePrivacyAgree(appContext, agreed)
        AMapLocationClient.updatePrivacyShow(appContext, true, noticeShown)
        AMapLocationClient.updatePrivacyAgree(appContext, agreed)
    }
}
