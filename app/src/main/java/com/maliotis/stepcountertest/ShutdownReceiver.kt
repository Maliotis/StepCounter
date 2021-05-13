package com.maliotis.stepcountertest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class ShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Add custom logic
        // context.startService(Intent(context, SensorListenerService::class.java))
    }
}