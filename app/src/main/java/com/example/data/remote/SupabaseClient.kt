package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClient {

    const val BASE_URL = "https://iwvauaueteocwsyuoioz.supabase.co/"
    const val ANON_KEY = "sb_publishable_zh5b1t_MEwLT4gCQj-NV2Q_QCF2lCKi"

    private const val PREF_NAME = "supabase_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"

    private var prefs: SharedPreferences? = null

    val sessionExpired = MutableStateFlow(false)

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        authToken = prefs?.getString(KEY_TOKEN, null)
        currentUserId = prefs?.getString(KEY_USER_ID, null)
        currentUserEmail = prefs?.getString(KEY_USER_EMAIL, null)
    }

    var authToken: String? = null
        set(value) {
            field = value
            prefs?.edit()?.apply {
                if (value != null) putString(KEY_TOKEN, value)
                else remove(KEY_TOKEN)
                apply()
            }
        }

    var currentUserId: String? = null
        set(value) {
            field = value
            prefs?.edit()?.apply {
                if (value != null) putString(KEY_USER_ID, value)
                else remove(KEY_USER_ID)
                apply()
            }
        }

    var currentUserEmail: String? = null
        set(value) {
            field = value
            prefs?.edit()?.apply {
                if (value != null) putString(KEY_USER_EMAIL, value)
                else remove(KEY_USER_EMAIL)
                apply()
            }
        }

    fun clearSession() {
        authToken = null
        currentUserId = null
        currentUserEmail = null
    }

    private val headerInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("apikey", ANON_KEY)
            .header("Authorization", "Bearer ${authToken ?: ANON_KEY}")
            .header("Content-Type", "application/json")
        chain.proceed(builder.build())
    }

    private val tokenLock = Any()

    private val tokenAuthenticator = Authenticator { _: Route?, response: Response ->
        // Give up if request was already retried or if this is the token refresh call itself
        if (response.request.header("X-Retry") != null || response.request.url.encodedPath.contains("auth/v1/token")) {
            return@Authenticator null
        }

        synchronized(tokenLock) {
            val currentRefresh = SessionManager.refreshToken
            if (currentRefresh.isNullOrBlank()) {
                sessionExpired.value = true
                SessionManager.clearSession()
                clearSession()
                return@Authenticator null
            }

            try {
                val refreshClient = OkHttpClient.Builder().build()
                val refreshRetrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(refreshClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                val authService = refreshRetrofit.create(SupabaseAuthApi::class.java)

                val reqBody = mapOf("refresh_token" to currentRefresh)
                val callRes = runBlocking { authService.refresh(reqBody) }

                if (callRes.isSuccessful && callRes.body() != null) {
                    val authResp = callRes.body()!!
                    val newAccess = authResp.accessToken
                    val newRefresh = authResp.refreshToken ?: currentRefresh

                    if (!newAccess.isNullOrBlank()) {
                        SessionManager.updateTokens(newAccess, newRefresh)
                        authToken = newAccess

                        return@Authenticator response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccess")
                            .header("X-Retry", "1")
                            .build()
                    }
                }
            } catch (e: Exception) {
                Log.e("AUTH_REFRESH", "Exception during token refresh", e)
            }

            // Refresh failed
            sessionExpired.value = true
            SessionManager.clearSession()
            clearSession()
            return@Authenticator null
        }
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("apikey")
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val authApi: SupabaseAuthApi by lazy {
        retrofit.create(SupabaseAuthApi::class.java)
    }

    val restApi: SupabaseRestApi by lazy {
        retrofit.create(SupabaseRestApi::class.java)
    }
}
