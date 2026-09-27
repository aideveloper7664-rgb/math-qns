package com.example

import android.app.Application
import com.example.data.remote.SupabaseClient

class SpeedMathApp : Application() {
    var paymentListener: ((Boolean, String, String) -> Unit)? = null

    companion object {
        lateinit var instance: SpeedMathApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize SupabaseClient with context to enable session persistence
        SupabaseClient.init(this)
    }
}
