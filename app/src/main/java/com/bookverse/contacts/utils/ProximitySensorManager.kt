package com.bookverse.contacts.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog

class ProximitySensorManager(
    context: Context,
    private val onProximityChanged: (Boolean) -> Unit = {}
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val powerManager: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var isNear = false
    private var isEnabled = false

    fun enable() {
        if (proximitySensor == null) {
            DiagnosticLog.warning(DiagnosticEvent.PROXIMITY_SENSOR_UNAVAILABLE)
            return
        }

        if (isEnabled) {
            DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_SENSOR_ALREADY_ENABLED)
            return
        }

        try {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "ContactsApp:ProximityLock"
            )

            sensorManager.registerListener(
                this,
                proximitySensor,
                SensorManager.SENSOR_DELAY_UI
            )
            isEnabled = true
            DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_SENSOR_ENABLED)
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.PROXIMITY_SENSOR_ENABLE_FAILED)
            cleanup()
        }
    }

    fun disable() {
        if (!isEnabled) {
            DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_SENSOR_ALREADY_DISABLED)
            forceRelease()
            return
        }

        DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_SENSOR_DISABLED)
        cleanup()
    }

    fun forceDisable() {
        DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_SENSOR_FORCE_DISABLED)
        cleanup()
    }

    private fun cleanup() {
        try {
            sensorManager.unregisterListener(this)
            DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_LISTENER_UNREGISTERED)
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.PROXIMITY_LISTENER_UNREGISTER_FAILED)
        }

        releaseWakeLock()

        wakeLock = null
        isEnabled = false
        isNear = false
        DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_CLEANUP_COMPLETED)
    }

    private fun forceRelease() {
        releaseWakeLock()
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_WAKE_LOCK_RELEASED)
                }
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.PROXIMITY_WAKE_LOCK_FAILED)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_PROXIMITY) return
        if (!isEnabled) {
            DiagnosticLog.warning(DiagnosticEvent.PROXIMITY_EVENT_IGNORED)
            return
        }

        val distance = event.values[0]
        val maxRange = event.sensor.maximumRange
        val wasNear = isNear

        isNear = distance < maxRange && distance < 5.0f

        if (wasNear != isNear) {
            DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_EVENT_RECEIVED)

            wakeLock?.let { lock ->
                try {
                    if (isNear && !lock.isHeld) {
                        lock.acquire(10 * 60 * 1000L)
                        DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_WAKE_LOCK_ACQUIRED)
                    } else if (!isNear && lock.isHeld) {
                        lock.release()
                        DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_WAKE_LOCK_RELEASED)
                    }
                } catch (_: Exception) {
                    DiagnosticLog.error(DiagnosticEvent.PROXIMITY_WAKE_LOCK_FAILED)
                }
            }

            onProximityChanged(isNear)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        DiagnosticLog.debug(DiagnosticEvent.PROXIMITY_ACCURACY_CHANGED)
    }
}
