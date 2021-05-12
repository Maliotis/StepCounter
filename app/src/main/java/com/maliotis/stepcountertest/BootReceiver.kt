package com.maliotis.stepcountertest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(Intent(context, SensorListenerService::class.java))
        } else {
            context.startService(Intent(context, SensorListenerService::class.java))
        }
    }
}