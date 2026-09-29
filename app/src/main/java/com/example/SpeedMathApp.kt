package com.example

import android.app.Application
import com.example.data.local.SessionManager

class SpeedMathApp : Application() {
    var paymentListener: ((Boolean, String, String) -> Unit)? = null

    companion object {
        lateinit var instance: SpeedMathApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.example.data.remote.SupabaseClient.init(this)
        SessionManager.init(this)
    }
}

typealias MathBaaziApp = SpeedMathApp
