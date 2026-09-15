package cn.edu.pku.openrunner.core.network

import android.content.Context
import android.os.Build
import cn.edu.pku.openrunner.BuildConfig
import cn.edu.pku.openrunner.core.session.SessionStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private lateinit var sessionStore: SessionStore
    private lateinit var retrofit: Retrofit

    val api: PkuNewYouthApi
        get() = retrofit.create(PkuNewYouthApi::class.java)

    fun initialize(context: Context) {
        sessionStore = SessionStore(context.applicationContext)
        val headers = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Platform", ApiConfig.PLATFORM)
                .header("Manufacturer", Build.MANUFACTURER)
                .header("ClientVersion", BuildConfig.VERSION_NAME)
            sessionStore.token
                ?.takeIf { it.isNotBlank() }
                ?.let { request.header("Authorization", it) }
            chain.proceed(request.build())
        }

        retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor(headers).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
