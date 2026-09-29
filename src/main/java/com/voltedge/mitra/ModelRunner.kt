package com.voltedge.mitra

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.FloatBuffer

data class PredictionResult(
    val velocityKmh: Float,
    val yawRateDegS: Float
)

class ModelRunner(private val context: Context, private val config: ModelConfig) {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open("gru_navigation.onnx").readBytes()
        session = env.createSession(modelBytes)
    }

    fun predict(flattenedWindow: FloatArray): PredictionResult {
        val shape = longArrayOf(1, config.sequenceLength.toLong(), config.inputSize.toLong())
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(flattenedWindow), shape)

        val inputName = session.inputNames.iterator().next()
        val results = session.run(mapOf(inputName to tensor))

        // Output tensor shape is [1, 4]
        @Suppress("UNCHECKED_CAST")
        val rawOutput = (results[0].value as Array<FloatArray>)[0]

        // 1. Velocity is Target Index 0
        val rawNormVel = rawOutput[0]
        val denormVelocityKmh = (rawNormVel * config.yStds[0]) + config.yMeans[0]

        // 2. Yaw Rate is Target Index 3 (NOT index 1!)
        val rawNormYaw = rawOutput[3]
        val denormYawRateDegS = (rawNormYaw * config.yStds[3]) + config.yMeans[3]

        tensor.close()
        results.close()

        return PredictionResult(
            velocityKmh = Math.max(0.0f, denormVelocityKmh),
            yawRateDegS = denormYawRateDegS
        )
    }

    fun close() {
        session.close()
        env.close()
    }
}