package com.anitrack.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class AniTrackApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_REMOTE_CONTROL,
                "Remote Control Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for remote control service"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID_REMOTE_CONTROL = "remote_control_service"
        
        lateinit var instance: AniTrackApplication
            private set
    }
}
