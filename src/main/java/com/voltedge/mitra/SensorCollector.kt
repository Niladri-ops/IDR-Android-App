package com.voltedge.mitra

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class SensorCollector(
    context: Context,
    private val onFeatureVectorReady: (FeatureVector) -> Unit
) : SensorEventListener {

    var latestRawLinearAccelMag: Float = 0f
        private set

    val latestLinearAccelNorm: Float
        get() = latestRawLinearAccelMag

    var latestGyroNorm: Float = 0f
        private set

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Hardware sensor references
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    // Current cached values
    private val accel = FloatArray(3)
    private val gyro = FloatArray(3)
    private val mag = FloatArray(3)
    private val gravity = FloatArray(3)

    private var hasAccel = false
    private var hasGyro = gyroscope == null
    private var hasMag = magnetometer == null
    private var hasGravity = gravitySensor == null

    init {
        if (gravitySensor == null) {
            gravity[2] = 9.81f
        }
    }

    fun start() {
        val delay = SensorManager.SENSOR_DELAY_GAME
        accelerometer?.let { sensorManager.registerListener(this, it, delay) }
        gyroscope?.let { sensorManager.registerListener(this, it, delay) }
        magnetometer?.let { sensorManager.registerListener(this, it, delay) }
        gravitySensor?.let { sensorManager.registerListener(this, it, delay) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accel, 0, 3)
                hasAccel = true
            }
            Sensor.TYPE_GYROSCOPE -> {
                System.arraycopy(event.values, 0, gyro, 0, 3)
                hasGyro = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, mag, 0, 3)
                hasMag = true
            }
            Sensor.TYPE_GRAVITY -> {
                System.arraycopy(event.values, 0, gravity, 0, 3)
                hasGravity = true
            }
        }

        // Only emit if all sensors have delivered at least one reading
        if (hasAccel && hasGyro && hasMag && hasGravity) {
            val dynX = accel[0] - gravity[0]
            val dynY = accel[1] - gravity[1]
            val dynZ = accel[2] - gravity[2]
            latestRawLinearAccelMag = sqrt(dynX * dynX + dynY * dynY + dynZ * dynZ)
            latestGyroNorm = sqrt(gyro[0] * gyro[0] + gyro[1] * gyro[1] + gyro[2] * gyro[2])

            val vector = FeatureVector(
                accelX = accel[0],
                accelY = accel[1],
                accelZ = accel[2],
                gyroX = gyro[0],
                gyroY = gyro[1],
                gyroZ = gyro[2],
                magX = mag[0],
                magY = mag[1],
                magZ = mag[2],
                gravityX = gravity[0],
                gravityY = gravity[1],
                gravityZ = gravity[2],
                dynamicAccelX = dynX,
                dynamicAccelY = dynY,
                dynamicAccelZ = dynZ
            )
            onFeatureVectorReady(vector)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}