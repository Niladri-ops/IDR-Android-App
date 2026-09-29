package com.voltedge.mitra

import android.content.Context
import org.json.JSONObject

class ModelConfig(context: Context) {
    val sequenceLength: Int
    val inputSize: Int
    val outputSize: Int
    val xMeans: FloatArray
    val xStds: FloatArray
    val yMeans: FloatArray
    val yStds: FloatArray

    init {
        val jsonStr = context.assets.open("model_metadata.json").bufferedReader().use { it.readText() }
        val json = JSONObject(jsonStr)

        sequenceLength = json.getInt("sequence_length")
        inputSize = json.getInt("input_size")
        outputSize = json.getInt("output_size")

        val xMeanArr = json.getJSONArray("X_mean")
        val xStdArr = json.getJSONArray("X_std")
        val yMeanArr = json.getJSONArray("y_mean")
        val yStdArr = json.getJSONArray("y_std")

        xMeans = FloatArray(xMeanArr.length()) { xMeanArr.getDouble(it).toFloat() }
        xStds = FloatArray(xStdArr.length()) { xStdArr.getDouble(it).toFloat() }
        yMeans = FloatArray(yMeanArr.length()) { yMeanArr.getDouble(it).toFloat() }
        yStds = FloatArray(yStdArr.length()) { yStdArr.getDouble(it).toFloat() }
    }
}

    private fun org.json.JSONArray.toFloatArray(): FloatArray {
        return FloatArray(length()) { getDouble(it).toFloat() }
    }
