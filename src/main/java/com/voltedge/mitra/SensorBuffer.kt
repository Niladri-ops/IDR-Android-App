package com.voltedge.mitra

class SensorBuffer(
    private val sequenceLength: Int = 30,
    private val featureCount: Int = 15
) {
    // Flattened buffer: sequenceLength * featureCount floats
    private val buffer = FloatArray(sequenceLength * featureCount)
    private var sampleCount = 0

    @Synchronized
    fun push(features: FloatArray) {
        require(features.size == featureCount) { "Expected $featureCount features, got ${features.size}" }

        // Shift existing samples left by one frame (15 floats)
        System.arraycopy(
            buffer, featureCount,
            buffer, 0,
            (sequenceLength - 1) * featureCount
        )

        // Insert new frame at the end
        System.arraycopy(
            features, 0,
            buffer, (sequenceLength - 1) * featureCount,
            featureCount
        )

        sampleCount++
    }

    @Synchronized
    fun isReady(): Boolean = sampleCount >= sequenceLength

    @Synchronized
    fun getFlattenedWindow(): FloatArray {
        return buffer.clone()
    }

    @Synchronized
    fun reset() {
        buffer.fill(0f)
        sampleCount = 0
    }
}