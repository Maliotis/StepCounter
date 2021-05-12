package com.maliotis.stepcountertest

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log


private const val APPNAME = "com.maliotis.stepcountertest"
private const val YESTERDAYSTEPS = "YesterdaysSteps"
private const val YESTERDAYSTEPSDAY = "YesterdaysStepsDay"
private const val TODAYSTEPS = "TodaysSteps"
private const val OLDTOTALSTEPS = "oldTotalSteps"
private const val TAG = "SharedPreferences"


fun writeYesterdaysSteps(context: Context, steps: Int):Boolean {
    val sharedPreference = context.getSharedPreferences(APPNAME, MODE_PRIVATE)
    val editor = sharedPreference.edit()
    editor.putInt(YESTERDAYSTEPS, steps)
    return editor.commit()
}

fun getYesterdaysSteps(context: Context): Int {
    val sharedPreference = context.getSharedPreferences(APPNAME, MODE_PRIVATE)
    val yestSteps = sharedPreference.getInt(YESTERDAYSTEPS, 0)
    Log.d(TAG, "getYesterdaysSteps: yestSteps = $yestSteps")
    return yestSteps
}

fun writeOldTotalSteps(context: Context, steps: Int): Boolean {
    val sharedPreference = context.getSharedPreferences(APPNAME, MODE_PRIVATE)
    val editor = sharedPreference.edit()
    editor.putInt(OLDTOTALSTEPS, steps)
    return editor.commit()
}

fun getOldTotalSteps(context: Context): Int {
    val sharedPreference = context.getSharedPreferences(APPNAME, MODE_PRIVATE)
    val oldSteps = sharedPreference.getInt(OLDTOTALSTEPS, 0)
    Log.d(TAG, "getOldTotalSteps: oldSteps = $oldSteps")
    return oldSteps
}


fun writeDayForYesterdaysSteps(context: Context, day: String): Boolean {
    val sharedPreference = context.getSharedPreferences(APPNAME, MODE_PRIVATE)
    val editor = sharedPreference.edit()
    editor.putString(YESTERDAYSTEPSDAY, day)
    return editor.commit()
}

fun getDayForYesterdaysSteps(context: Context): String {
    val sharedPreference = context.getSharedPreferences(APPNAME, MODE_PRIVATE)
    val yestStepsDay = sharedPreference.getString(YESTERDAYSTEPSDAY, "") ?: ""
    Log.d(TAG, "getDayForYesterdaysSteps: yestStepsDay = $yestStepsDay")
    return yestStepsDay
}

fun writeTodaysSteps(context: Context, steps: Int): Boolean {
    val sharedPreference = context.getSharedPreferences(APPNAME, MODE_PRIVATE)
    val editor = sharedPreference.edit()
    editor.putInt(TODAYSTEPS, steps)
    return editor.commit()
}

fun getTodaysSteps(context: Context): Int {
    val sharedPreference = context.getSharedPreferences(APPNAME, MODE_PRIVATE)
    val todaysSteps = sharedPreference.getInt(TODAYSTEPS, 0)
    Log.d(TAG, "getTodaysSteps: todaysSteps = $todaysSteps")
    return todaysSteps
}