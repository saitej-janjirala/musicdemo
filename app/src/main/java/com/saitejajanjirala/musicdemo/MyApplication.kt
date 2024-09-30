package com.saitejajanjirala.musicdemo

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
const val channel_id = "demo_id"
const val channel_name = "demo_channel"
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

    }
}