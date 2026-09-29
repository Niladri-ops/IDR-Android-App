package com.voltedge.mitra

import kotlin.math.cos
import kotlin.math.sin

data class VehiclePose(
    val latitude: Double,
    val longitude: Double,
    val headingDegrees: Float,
    val speedKmh: Float,
    val eastingMeters: Double = 0.0,
    val northingMeters: Double = 0.0
)

class DeadReckoningEngine(
    private var currentLat: Double = 0.0,
    private var currentLon: Double = 0.0,
    private var currentHeadingDeg: Float = 0.0f
) {
    private var easting: Double = 0.0
    private var northing: Double = 0.0
    private var originLat: Double = currentLat
    private var originLon: Double = currentLon
    private var isInitialized: Boolean = false

    fun updateGpsFix(lat: Double, lon: Double, headingDeg: Float? = null) {
        if (!isInitialized) {
            originLat = lat
            originLon = lon
            isInitialized = true
        }

        currentLat = lat
        currentLon = lon
        if (headingDeg != null && headingDeg > 0.0f) {
            currentHeadingDeg = normalizeAngle(headingDeg)
        }

        val latRad = Math.toRadians(originLat)
        northing = (lat - originLat) * 111132.954
        easting = (lon - originLon) * (111132.954 * cos(latRad))
    }

    /**
     * Integrates position using velocity, yaw rate, dynamic accel, and dt.
     */
    fun step(
        predictedVelocityKmh: Float,
        yawRateDegS: Float,
        dynamicAccelNorm: Float = 0.0f,
        dtSec: Float = 1.0f
    ): VehiclePose {
        val nhcEngine = NhcEngine()
        val motion = nhcEngine.applyConstraints(
            predictedVelocityKmh = predictedVelocityKmh,
            predictedYawRateDegS = yawRateDegS,
            rawLinearAccelMag = dynamicAccelNorm
        )
        return stepWithNhc(motion, dtSec)
    }

    /**
     * Integrates position using NHC-constrained motion parameters.
     */
    fun stepWithNhc(motion: ConstrainedMotion, dtSec: Float): VehiclePose {
        // If stationary, return the existing coordinates untouched
        if (motion.isStationary || motion.forwardVelocityMs <= 0.1f) {
            return VehiclePose(
                latitude = currentLat,
                longitude = currentLon,
                speedKmh = 0.0f,
                headingDegrees = currentHeadingDeg
            )
        }

        // Only integrate heading and displacement when physical movement is verified
        currentHeadingDeg = (currentHeadingDeg + motion.filteredYawRateDegS * dtSec) % 360f
        if (currentHeadingDeg < 0) currentHeadingDeg += 360f

        val distanceMeters = motion.forwardVelocityMs * dtSec
        val headingRad = Math.toRadians(currentHeadingDeg.toDouble())

        val deltaLat = (distanceMeters * cos(headingRad)) / 111132.92
        val deltaLon = (distanceMeters * sin(headingRad)) /
                (111132.92 * cos(Math.toRadians(currentLat)))

        currentLat += deltaLat
        currentLon += deltaLon

        return VehiclePose(
            latitude = currentLat,
            longitude = currentLon,
            speedKmh = motion.forwardVelocityMs * 3.6f,
            headingDegrees = currentHeadingDeg
        )
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle % 360f
        if (a < 0) a += 360f
        return a
    }
}