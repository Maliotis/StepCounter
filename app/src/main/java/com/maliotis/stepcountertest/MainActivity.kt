package com.maliotis.stepcountertest

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.util.*

/**
 * What we display from MainActivity is probably wrong
 * Check [SensorListenerService] for step logic
 */
class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var running = false
    private var totalSteps = 0f

    private val logStepsFunction: (Int) -> Unit = { value: Int ->
        Log.d("TAG", "loggingSteps: value = $value")
        val oldSteps = getYesterdaysSteps(applicationContext)
        if (oldSteps > 0) {
            findViewById<TextView>(R.id.steps).text = "Steps: ${oldSteps - value}"
        }
    }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d(TAG, "requestPermission: isGranted = $isGranted")
            if (isGranted) {
                // proceed as usual
            } else {
                // explain to the user the feature is unavailable because the feature
                // requires permission that the user has denied
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ask for permission Activity Recognition
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            requestPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)
        Log.d("TAG", "onCreate: deviceSensors = $deviceSensors")
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            val supportStepCounterTextView = findViewById<TextView>(R.id.supportsStepCounter)
            val txt = supportStepCounterTextView.text.toString()
            supportStepCounterTextView.text = "$txt true"

            val yesterdaysSteps = findViewById<TextView>(R.id.yesterdaySteps)

            val yesterdaysDate = Date().getYesterdaysDate().toCalendar()
            val yesterdaysDay = yesterdaysDate.get(Calendar.DAY_OF_MONTH)

            if (getDayForYesterdaysSteps(applicationContext) == yesterdaysDay.toString())
            yesterdaysSteps.text = "Yesterdays Steps: ${getYesterdaysSteps(applicationContext)}"

        } else {
            val supportStepCounterTextView = findViewById<TextView>(R.id.supportsStepCounter)
            val txt = supportStepCounterTextView.text.toString()
            supportStepCounterTextView.text = "$txt false"
        }

        //startService()

        // restart service every hour to save the current step count
        val nextUpdate: Long = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
        }.timeInMillis
        val am = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent
            .getService(
                applicationContext, 2, Intent(this, SensorListenerService::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        if (Build.VERSION.SDK_INT >= 23) {
            //am.setAndAllowWhileIdle(AlarmManager.RTC, nextUpdate, pi)
            am.setRepeating(AlarmManager.RTC_WAKEUP, nextUpdate, 1000 * 60 * 60 * 24, pi)
        } else {
            am[AlarmManager.RTC_WAKEUP, nextUpdate] = pi
        }

    }

    private fun startService() {
        Intent(this, SensorListenerService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        stepSensor?.also { sensor ->
            sensorManager.registerListener(
                stepSensorListener(logStepsFunction),
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }
}