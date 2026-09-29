package com.voltedge.mitra

data class FeatureVector(
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,

    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,

    val magX: Float,
    val magY: Float,
    val magZ: Float,

    val gravityX: Float,
    val gravityY: Float,
    val gravityZ: Float,

    val dynamicAccelX: Float,
    val dynamicAccelY: Float,
    val dynamicAccelZ: Float
) {
    /**
     * Converts to an un-normalized array of 15 features matching training column order.
     */
    fun toRawArray(): FloatArray {
        return floatArrayOf(
            accelX, accelY, accelZ,
            gyroX, gyroY, gyroZ,
            magX, magY, magZ,
            gravityX, gravityY, gravityZ,
            dynamicAccelX, dynamicAccelY, dynamicAccelZ
        )
    }

    /**
     * Converts and normalizes using X_mean and X_std: (x - mean) / std
     */
    fun toNormalizedArray(config: ModelConfig): FloatArray {
        val raw = floatArrayOf(
            accelX, accelY, accelZ,
            gyroX, gyroY, gyroZ,
            magX, magY, magZ,
            gravityX, gravityY, gravityZ,
            dynamicAccelX, dynamicAccelY, dynamicAccelZ
        )
        val normalized = FloatArray(raw.size)
        for (i in raw.indices) {
            val mean = config.xMeans.getOrElse(i) { 0f }
            val std = config.xStds.getOrElse(i) { 1f }.let { if (it == 0f) 1f else it }
            normalized[i] = (raw[i] - mean) / std
        }
        return normalized
    }
}