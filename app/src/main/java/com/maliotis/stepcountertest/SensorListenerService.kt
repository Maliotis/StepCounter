package com.maliotis.stepcountertest

import android.app.*
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import java.text.DateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.absoluteValue


/**
 * Background service which keeps the step-sensor listener alive to always get
 * the number of steps since boot.
 *
 *
 * This service won't be needed any more if there is a way to read the
 * step-value without waiting for a sensor event
 */
class SensorListenerService : Service() {

    private val TAG = SensorListenerService::class.java.simpleName

    private val shutdownReceiver: BroadcastReceiver = ShutdownReceiver()

    private val logStepsFunction: (Int) -> Unit = { value: Int ->
        Log.d("SensorListenerService", "loggingSteps: value = $value")
        steps = value
        updateIfNecessary()
    }

    private val stepSensorImpl = stepSensorListener(logStepsFunction)


    /**
     * @return true, if notification was updated
     */
    private fun updateIfNecessary(): Boolean {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)

        Log.d(TAG, "updateIfNecessary: day = $day && hours = $hours && minutes = $minutes")


        // should be executed once every time the alarm manager wakes up the service
        return if (hours == 0 && minutes < 10 && getDayDifference(calendar.time, applicationContext, 2L)) {

            // logic
            //0: currentTotal = 0, todays = 0, yesterdays = 0, oldTotalSteps = 0
            //1: currentTotal = 1000, todays = 1000, yesterdays = 0, oldTotalSteps = 0
            //2: currentTotal = 2300, todays = 1300, yesterdays = 1000, oldTotalSteps = 1000
            //3: currentTotal = 3000, todays = 700, yesterdays = 1300, oldTotalSteps = 2300
            //4: currentTotal = 4000, todays = 1000, yesterdays = 700, oldTotalSteps = 3000


            //yesterdays = todays
            //todays = currentTotal - oldTotalSteps
            //oldTotalSteps = currentTotal

            val todaysSteps = getTodaysSteps(applicationContext)
            writeYesterdaysSteps(applicationContext, todaysSteps)
            val newTodaysSteps = steps - getOldTotalSteps(applicationContext)
            writeTodaysSteps(applicationContext, newTodaysSteps)
            writeOldTotalSteps(applicationContext, steps)


            val yesterdaysDate = DateFormat.getDateInstance(DateFormat.LONG).format(Date().getYesterdaysDate())
            // debug
//            val yesterdaysDay = yesterdaysDate.get(Calendar.DAY_OF_MONTH)

            writeDayForYesterdaysSteps(applicationContext, yesterdaysDate.toString())
            showNotification() // update notification
            Log.d(TAG, "updateIfNecessary: set new steps")

            true
        } else {
            false
        }
    }

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground(NOTIFICATION_ID, getNotification(this))
        } else  {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, getNotification(this))
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        reRegisterSensor()
        registerBroadcastReceiver()
        if (!updateIfNecessary()) {
            showNotification()
        }

        Log.d(TAG, "onStartCommand: called")


        // For debug
//        // restart service every hour to save the current step count
//        val nextUpdate: Long = Calendar.getInstance().apply {
//            timeInMillis = System.currentTimeMillis()
//            set(Calendar.HOUR_OF_DAY, 14)
//            set(Calendar.MINUTE, 41)
//        }.timeInMillis
//        val am = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
//        val pi = PendingIntent
//            .getService(
//                applicationContext, 2, Intent(this, SensorListenerService::class.java),
//                PendingIntent.FLAG_UPDATE_CURRENT
//            )
//
//        if (Build.VERSION.SDK_INT >= 23) {
//            am.setAndAllowWhileIdle(AlarmManager.RTC, nextUpdate, pi)
//        } else {
//            am[AlarmManager.RTC, nextUpdate] = pi
//        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        // Restart service in 500 ms
        Log.d(TAG, "onTaskRemoved: called")
        (getSystemService(ALARM_SERVICE) as AlarmManager)[AlarmManager.RTC, System.currentTimeMillis() + 500] =
            PendingIntent
                .getService(this, 3, Intent(this, SensorListenerService::class.java), 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            val sm = getSystemService(SENSOR_SERVICE) as SensorManager
            sm.unregisterListener(stepSensorImpl)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerBroadcastReceiver() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SHUTDOWN)
        registerReceiver(shutdownReceiver, filter)
    }

    private fun reRegisterSensor() {
        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        try {
            sm.unregisterListener(stepSensorImpl)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // enable batching with delay of max 5 min
        val stepSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sm.registerListener(stepSensorImpl, stepSensor, SENSOR_DELAY_NORMAL, fiveMinInMicroS)
    }

    companion object {
        const val fiveMinInMicroS = 60000000 * 5

        const val NOTIFICATION_ID = 1
        private var steps = 0


        const val NOTIFICATION_CHANNEL_ID = "Notification"

        fun getNotification(context: Context): Notification {
            val notificationBuilder =
                if (Build.VERSION.SDK_INT >= 26) getNotificationBuilder(context) else Notification.Builder(
                    context
                )
            if (steps > 0 || getYesterdaysSteps(context) == 0) {

                notificationBuilder
                ?.setContentTitle("Step Counter")
                    //?.setContentText("Yesterdays steps: ${getYesterdaysSteps(context)}\n Todays Steps: $steps")
                    ?.setStyle(Notification.BigTextStyle().bigText("Yesterdays steps: ${
                        getYesterdaysSteps(
                            context
                        )
                    }\n" +
                            " Todays Steps: ${(getYesterdaysSteps(context) - steps).absoluteValue}"))
            } else { // still no step value?
                notificationBuilder?.setContentText("No steps :(")
                    ?.setContentTitle("Step Counter")
            }

            notificationBuilder
                ?.setPriority(Notification.PRIORITY_LOW)
                ?.setShowWhen(false)
                ?.setSmallIcon(R.drawable.ic_launcher_foreground)
                ?.setContentIntent(
                    PendingIntent
                        .getActivity(
                            context, 0, Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                )

                ?.setOngoing(true)
            val notification = notificationBuilder!!.build()
            notification.priority
            return notification
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getNotificationBuilder(context: Context): Notification.Builder? {
            val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_NONE
            )
            channel.importance = NotificationManager.IMPORTANCE_LOW // ignored by Android O ...
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setBypassDnd(false)
            channel.setSound(null, null)
            manager.createNotificationChannel(channel)
            return Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
        }

    }
}