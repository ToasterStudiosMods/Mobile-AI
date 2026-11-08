package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.HardwareProfiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HardwareProfilerTest {

    private lateinit var profiler: HardwareProfiler

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        profiler = HardwareProfiler(context)
    }

    @Test
    fun testRecommender_ultraLowRamTier() {
        val result = profiler.evaluateRecommendation(1.8f)
        assertEquals("Ultra Lightweight Tier", result.tier)
        assertEquals("qwen2.5-0.5b-instruct", result.modelId)
        assertTrue("Explanation must mention memory bounds", result.explanation.contains("400MB RAM"))
    }

    @Test
    fun testRecommender_balancedRamTier() {
        val result = profiler.evaluateRecommendation(4.0f)
        assertEquals("Balanced Tier", result.tier)
        assertEquals("smollm2-1.7b-instruct", result.modelId)
        assertTrue("Explanation must justify selection", result.explanation.contains("efficiency"))
    }

    @Test
    fun testRecommender_highPerformanceTier() {
        val result = profiler.evaluateRecommendation(6.0f)
        assertEquals("High Performance", result.tier)
        assertEquals("llama-3.2-1b-instruct", result.modelId)
        assertTrue("Explanation must mention Llama 3.2", result.explanation.contains("Llama 3.2 1B"))
    }

    @Test
    fun testRecommender_flagshipRamTier() {
        val result = profiler.evaluateRecommendation(12.0f)
        assertEquals("Flagship Tier", result.tier)
        assertEquals("gemma-2-2b-it", result.modelId)
        assertTrue("Explanation must explain high-end headroom", result.explanation.contains("Gemma 2 2B"))
    }
}
