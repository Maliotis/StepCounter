package com.maliotis.stepcountertest

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

fun stepSensorListener(value:(step: Int) -> Unit, accuracyChange: ((sensor: Sensor?, accuracy: Int) -> Unit)? = null): SensorEventListener {
    return object: SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val step = event?.values?.get(0)?.toInt() ?: 0
            // return the step back to caller
            value(step)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // do something if sensor accuracy changes
            accuracyChange?.also { callback ->
                callback(sensor, accuracy)
            }

        }

    }
}