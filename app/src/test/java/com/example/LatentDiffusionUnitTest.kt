package com.example

import com.example.data.model.ImageGenConfig
import com.example.engine.LatentDiffusionEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LatentDiffusionUnitTest {

    @Test
    fun testSemanticPromptAnalysis() {
        val engine = LatentDiffusionEngine()

        // 1. Cyberpunk prompt
        val cyberPalette = engine.analyzePrompt("A futuristic neon cyberpunk street in Neo-Tokyo")
        assertEquals(LatentDiffusionEngine.StructuralGeometry.URBAN_CYBERPUNK, cyberPalette.geometry)
        assertTrue("Cyberpunk should have high emissive glow", cyberPalette.emissiveGlow >= 0.8f)

        // 2. Cosmic prompt
        val spacePalette = engine.analyzePrompt("Spiral galaxy with cosmic nebula and bright stars")
        assertEquals(LatentDiffusionEngine.StructuralGeometry.COSMIC_GALAXY, spacePalette.geometry)
        assertTrue("Space should have high contrast", spacePalette.contrast >= 1.3f)

        // 3. Nature prompt
        val naturePalette = engine.analyzePrompt("Sunset over mountain valley with emerald pine trees")
        assertEquals(LatentDiffusionEngine.StructuralGeometry.LANDSCAPE_NATURE, naturePalette.geometry)
    }

    @Test
    fun testOnDeviceSynthesisGeneratesBitmapAndProgress() = runBlocking {
        val engine = LatentDiffusionEngine()
        val config = ImageGenConfig(
            prompt = "Cyberpunk neon city",
            steps = 6,
            guidanceScale = 7.5f,
            width = 256,
            height = 256,
            seed = 42L
        )

        val progressSteps = mutableListOf<Int>()
        var previewCount = 0

        val (bitmap, duration) = engine.synthesize(config) { step, totalSteps, previewBmp ->
            progressSteps.add(step)
            if (previewBmp != null) {
                previewCount++
                assertEquals(128, previewBmp.width)
                assertEquals(128, previewBmp.height)
            }
        }

        assertNotNull("Generated bitmap should not be null", bitmap)
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
        assertTrue("Should have recorded progress steps", progressSteps.isNotEmpty())
        assertEquals(6, progressSteps.last())
        assertTrue("Should have emitted intermediate previews", previewCount >= 1)
        assertTrue("Duration should be recorded", duration >= 0L)
    }
}
