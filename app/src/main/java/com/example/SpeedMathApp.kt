package com.example

import android.app.Application

class SpeedMathApp : Application() {
    var paymentListener: ((Boolean, String, String) -> Unit)? = null

    companion object {
        lateinit var instance: SpeedMathApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
