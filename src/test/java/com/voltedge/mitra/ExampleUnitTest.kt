package com.voltedge.mitra

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun testDeadReckoningEngine() {
        val dr = DeadReckoningEngine(37.7749, -122.4194, 0f)
        val pose = dr.step(predictedVelocityKmh = 36f, yawRateDegS = 0f, dynamicAccelNorm = 0.5f, dtSec = 1.0f)
        assertEquals(36f, pose.speedKmh, 0.01f)
        assertTrue(pose.latitude > 37.7749) // moving north
    }

    @Test
    fun testSensorBuffer() {
        val buffer = SensorBuffer(sequenceLength = 30, featureCount = 15)
        assertFalse(buffer.isReady())
        val sample = FloatArray(15)
        for (i in 0 until 30) {
            buffer.push(sample)
        }
        assertTrue(buffer.isReady())
        assertEquals(450, buffer.getFlattenedWindow().size)
    }
}