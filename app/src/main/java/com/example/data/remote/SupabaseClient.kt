package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Restore persisted session on startup
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

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
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
