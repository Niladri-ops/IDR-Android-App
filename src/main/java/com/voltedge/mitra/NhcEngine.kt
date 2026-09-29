package com.voltedge.mitra

import kotlin.math.abs

data class ConstrainedMotion(
    val forwardVelocityMs: Float,
    val filteredYawRateDegS: Float,
    val isStationary: Boolean
)

class NhcEngine(
    private val minMoveSpeedKmh: Float = 3.5f,  // Minimum vehicle speed to recognize motion
    private val yawRateDeadbandDegS: Float = 2.0f // Ignore gyro desk drift under 2 deg/sec
) {
    fun applyConstraints(
        predictedVelocityKmh: Float,
        predictedYawRateDegS: Float,
        rawLinearAccelMag: Float
    ): ConstrainedMotion {

        // If physical dynamic acceleration is negligible (< 0.25 m/s^2)
        // OR the predicted speed is low, lock to stationary zero.
        val isStationary = rawLinearAccelMag < 0.35f || predictedVelocityKmh < minMoveSpeedKmh

        if (isStationary) {
            return ConstrainedMotion(
                forwardVelocityMs = 0.0f,
                filteredYawRateDegS = 0.0f,
                isStationary = true
            )
        }

        val cleanYaw = if (abs(predictedYawRateDegS) < yawRateDeadbandDegS) 0.0f else predictedYawRateDegS

        return ConstrainedMotion(
            forwardVelocityMs = predictedVelocityKmh / 3.6f,
            filteredYawRateDegS = cleanYaw,
            isStationary = false
        )
    }
}