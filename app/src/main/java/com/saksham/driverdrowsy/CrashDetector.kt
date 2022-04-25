package com.saksham.driverdrowsy

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt


class CrashDetector(context: Context) : SensorEventListener {

    interface CrashListener {
        fun onCrashSuspected()
    }

    private val CRASH_MAGNITUDE = 20
    private val mSensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val mAccelerometer: Sensor =
        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val crashListenerList = mutableListOf<CrashListener>()


    fun onResume() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun onPause() {
        mSensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val values = event.values
            val valueSquaredSum = (values[0] * values[0] +
                    values[1] * values[1] +
                    values[2] * values[2])
            val magnitude = sqrt(
                valueSquaredSum.toDouble()
            ).toFloat()

            if (magnitude > CRASH_MAGNITUDE) {
                for (listener: CrashListener in crashListenerList) {
                    listener.onCrashSuspected()
                }
            }
        }
    }


    fun registerCrashListener(listener: CrashListener) {
        this.crashListenerList.add(listener)
    }

    fun unregisterCrashListener(listener: CrashListener) {
        this.crashListenerList.remove(listener)
    }
}