package com.example.blesstify

import android.app.Application
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BlesstifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
        Log.d("BlesstifyApp", "Facebook SDK initialized: ${FacebookSdk.isInitialized()}")
    }
}
