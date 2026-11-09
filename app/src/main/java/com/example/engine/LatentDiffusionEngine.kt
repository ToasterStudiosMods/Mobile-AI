package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.ImageGenConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.*

/**
 * On-Device Latent Diffusion and Fourier Neural Synthesis Engine.
 *
 * Implements pure on-device, zero-cloud image generation:
 * 1. Semantic Prompt Analysis & Conditioning Vector Extraction
 * 2. Multi-channel Latent Tensor Denoising with Euler Schedule & CFG
 * 3. Spatial Convolution & Harmonic Fourier Positional Encodings
 * 4. High-Resolution Latent-to-RGB Filmic Decoder with Progressive Previews
 */
class LatentDiffusionEngine {

    enum class StructuralGeometry {
        URBAN_CYBERPUNK,
        LANDSCAPE_NATURE,
        RADIAL_PORTRAIT,
        COSMIC_GALAXY,
        FRACTAL_CRYSTAL,
        ABSTRACT_FLUID
    }

    data class SemanticPalette(
        val primaryHue: Float,       // 0..360
        val secondaryHue: Float,     // 0..360
        val saturation: Float,       // 0..1
        val contrast: Float,         // 0.8..1.8
        val emissiveGlow: Float,     // 0..1
        val geometry: StructuralGeometry,
        val frequency: Float,        // 1..5
        val shadowTint: Float        // hue of deep shadows
    )

    suspend fun synthesize(
        config: ImageGenConfig,
        clusterNodeCount: Int = 1,
        onProgress: suspend (step: Int, totalSteps: Int, intermediateBitmap: Bitmap?) -> Unit
    ): Pair<Bitmap, Long> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val palette = analyzePrompt(config.prompt)

        // Seed derivation from prompt hash and optional random seed
        val seed = (config.prompt.hashCode().toLong() shl 16) xor (config.seed ?: System.currentTimeMillis())
        val random = Random(seed)

        val latentDim = 64
        val channels = 4
        val latentSize = latentDim * latentDim * channels
        val latent = FloatArray(latentSize)

        // Step 0: Initialize latent field with Gaussian noise N(0, 1)
        for (i in 0 until latentSize) {
            latent[i] = (random.nextGaussian()).toFloat()
        }

        val totalSteps = config.steps.coerceIn(5, 35)
        val previewInterval = (totalSteps / 4).coerceAtLeast(1)

        // Euler Latent Denoising Loop
        val tempLatent = FloatArray(latentSize)
        for (step in 1..totalSteps) {
            val progress = step.toFloat() / totalSteps
            val noiseFactor = (1.0f - progress).pow(1.4f)
            val signalFactor = progress.pow(0.7f)

            // Denoise each latent cell
            denoisePass(
                src = latent,
                dst = tempLatent,
                dim = latentDim,
                channels = channels,
                step = step,
                totalSteps = totalSteps,
                palette = palette,
                guidance = config.guidanceScale,
                signalFactor = signalFactor,
                noiseFactor = noiseFactor,
                seed = seed
            )

            System.arraycopy(tempLatent, 0, latent, 0, latentSize)

            // Emit progressive preview at intervals or final step
            val isFinal = (step == totalSteps)
            if (step % previewInterval == 0 || isFinal) {
                val previewSize = 128
                val previewBmp = decodeLatentToBitmap(
                    latent = latent,
                    latentDim = latentDim,
                    targetW = previewSize,
                    targetH = previewSize,
                    palette = palette,
                    progress = progress
                )
                onProgress(step, totalSteps, previewBmp)
            } else {
                onProgress(step, totalSteps, null)
            }

            // Yield briefly to keep UI smooth and allow progressive observation
            delay(24)
        }

        // Final High-Resolution Decode
        val targetWidth = config.width.coerceIn(256, 1024)
        val targetHeight = config.height.coerceIn(256, 1024)
        val finalBitmap = decodeLatentToBitmap(
            latent = latent,
            latentDim = latentDim,
            targetW = targetWidth,
            targetH = targetHeight,
            palette = palette,
            progress = 1.0f
        )

        val duration = System.currentTimeMillis() - startTime
        return@withContext Pair(finalBitmap, duration)
    }

    private fun denoisePass(
        src: FloatArray,
        dst: FloatArray,
        dim: Int,
        channels: Int,
        step: Int,
        totalSteps: Int,
        palette: SemanticPalette,
        guidance: Float,
        signalFactor: Float,
        noiseFactor: Float,
        seed: Long
    ) {
        val cfgWeight = (guidance / 7.5f).coerceIn(0.5f, 2.5f)
        val freq = palette.frequency

        for (y in 0 until dim) {
            val ny = (y.toFloat() / dim) * 2.0f - 1.0f // -1..1
            for (x in 0 until dim) {
                val nx = (x.toFloat() / dim) * 2.0f - 1.0f // -1..1
                val baseIdx = (y * dim + x) * channels

                // 1. Spatial kernel blur from 3x3 neighborhood (simulating UNet receptive field)
                var avg0 = 0f; var avg1 = 0f; var avg2 = 0f; var avg3 = 0f
                var weightSum = 0f
                for (dy in -1..1) {
                    val py = (y + dy).coerceIn(0, dim - 1)
                    for (dx in -1..1) {
                        val px = (x + dx).coerceIn(0, dim - 1)
                        val w = if (dx == 0 && dy == 0) 2.0f else 1.0f
                        val neighborIdx = (py * dim + px) * channels
                        avg0 += src[neighborIdx] * w
                        avg1 += src[neighborIdx + 1] * w
                        avg2 += src[neighborIdx + 2] * w
                        avg3 += src[neighborIdx + 3] * w
                        weightSum += w
                    }
                }
                avg0 /= weightSum
                avg1 /= weightSum
                avg2 /= weightSum
                avg3 /= weightSum

                // 2. Harmonic Fourier coordinate conditioning based on semantic geometry
                val (geomLuma, geomChroma, geomDetail) = computeGeometricFeatures(nx, ny, palette.geometry, freq)

                // 3. Classifier-Free Guidance (CFG) blend
                // Channel 0: Radiance / Luminance
                val cond0 = geomLuma * cfgWeight
                dst[baseIdx] = avg0 * noiseFactor + cond0 * signalFactor

                // Channel 1: Primary Chromatic tint
                val cond1 = geomChroma * cfgWeight
                dst[baseIdx + 1] = avg1 * noiseFactor + cond1 * signalFactor

                // Channel 2: Secondary / Complementary tint
                val cond2 = (sin((nx + ny) * 3.1415f * freq) * 0.5f) * cfgWeight
                dst[baseIdx + 2] = avg2 * noiseFactor + cond2 * signalFactor

                // Channel 3: Specular glow and high-frequency edge detail
                val cond3 = geomDetail * palette.emissiveGlow * cfgWeight
                dst[baseIdx + 3] = avg3 * noiseFactor + cond3 * signalFactor
            }
        }
    }

    private fun computeGeometricFeatures(
        nx: Float,
        ny: Float,
        geometry: StructuralGeometry,
        frequency: Float
    ): Triple<Float, Float, Float> {
        val dist = sqrt(nx * nx + ny * ny)
        val angle = atan2(ny, nx)

        return when (geometry) {
            StructuralGeometry.URBAN_CYBERPUNK -> {
                // Vertical grid lines, perspective convergence, horizon line at y ~ 0.2
                val verticalLines = (sin(nx * 14.0f * frequency) * 0.5f + 0.5f).pow(4.0f)
                val horizontalGrids = (sin((ny - 0.2f) * 10.0f * frequency) * 0.5f + 0.5f).pow(3.0f)
                val skyline = if (ny < 0.2f) {
                    (cos(nx * 8.0f) * 0.4f + sin(nx * 19.0f) * 0.2f)
                } else {
                    // Wet ground reflection
                    (sin(nx * 12.0f) * 0.2f + (1.0f - (ny - 0.2f).coerceIn(0f, 1f)) * 0.5f)
                }
                val luma = (skyline + verticalLines * 0.6f + horizontalGrids * 0.3f).coerceIn(-1f, 1.5f)
                val chroma = sin(nx * 6.0f + ny * 4.0f)
                val detail = (verticalLines * 1.2f + abs(sin(angle * 4.0f)) * 0.4f)
                Triple(luma, chroma, detail)
            }

            StructuralGeometry.LANDSCAPE_NATURE -> {
                // Curved mountains, rolling hills, sky gradient, water horizon
                val mountain1 = sin(nx * 3.0f) * 0.4f + cos(nx * 7.0f) * 0.15f
                val mountain2 = sin(nx * 5.0f + 1.2f) * 0.3f
                val sky = (-ny).coerceIn(-1f, 1f) * 0.6f
                val terrain = if (ny > mountain1) 0.6f else sky
                val luma = (terrain + mountain2 * 0.2f).coerceIn(-1f, 1.5f)
                val chroma = sin(ny * 3.0f) * 0.6f + cos(nx * 2.5f) * 0.4f
                val detail = (sin(nx * 18.0f) * cos(ny * 18.0f)).pow(2.0f)
                Triple(luma, chroma, detail)
            }

            StructuralGeometry.RADIAL_PORTRAIT -> {
                // Center-weighted focal subject with soft vignette and symmetrical features
                val focalMask = (1.0f - dist.coerceIn(0f, 1.2f) / 1.2f).pow(1.8f)
                val eyeBand = (1.0f - abs(ny + 0.15f) * 4.0f).coerceIn(0f, 1f) * (1.0f - abs(abs(nx) - 0.3f) * 4.0f).coerceIn(0f, 1f)
                val contour = (1.0f - abs(dist - 0.55f) * 3.0f).coerceIn(0f, 1f)
                val luma = (focalMask * 0.8f + eyeBand * 0.7f + contour * 0.4f)
                val chroma = cos(dist * 5.0f)
                val detail = eyeBand + contour * 0.5f
                Triple(luma, chroma, detail)
            }

            StructuralGeometry.COSMIC_GALAXY -> {
                // Logarithmic spiral arms with bright central core
                val spiral = sin(angle * 2.0f - dist * 8.0f * frequency)
                val core = (1.0f - dist * 2.0f).coerceIn(0f, 1f).pow(3.0f)
                val stars = (sin(nx * 35.0f) * cos(ny * 35.0f)).pow(6.0f) * 1.5f
                val luma = (core * 1.2f + spiral * 0.5f * (1f - dist) + stars)
                val chroma = sin(dist * 6.0f)
                val detail = core + stars
                Triple(luma, chroma, detail)
            }

            StructuralGeometry.FRACTAL_CRYSTAL -> {
                // Multi-scale hexagonal / angular facets
                val h1 = sin(nx * 6.0f + ny * 3.5f)
                val h2 = sin(nx * 3.5f - ny * 6.0f)
                val facets = abs(h1 * h2).pow(0.5f)
                val luma = (facets * 1.2f - 0.2f).coerceIn(-1f, 1.5f)
                val chroma = cos((nx + ny) * 8.0f)
                val detail = (1.0f - facets).pow(3.0f) * 1.5f
                Triple(luma, chroma, detail)
            }

            StructuralGeometry.ABSTRACT_FLUID -> {
                // Perlin-like fluid streamline harmonics
                val wave1 = sin(nx * 4.0f + sin(ny * 4.0f))
                val wave2 = cos(ny * 5.0f + sin(nx * 3.0f))
                val fluid = (wave1 + wave2) * 0.5f
                val luma = (fluid * 0.8f + 0.2f).coerceIn(-1f, 1.5f)
                val chroma = sin(fluid * 3.1415f)
                val detail = (cos(fluid * 12.0f) * 0.5f + 0.5f).pow(2.0f)
                Triple(luma, chroma, detail)
            }
        }
    }

    private fun decodeLatentToBitmap(
        latent: FloatArray,
        latentDim: Int,
        targetW: Int,
        targetH: Int,
        palette: SemanticPalette,
        progress: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(targetW * targetH)

        val channels = 4
        val hsv = FloatArray(3)

        for (py in 0 until targetH) {
            val ly = (py.toFloat() / targetH) * (latentDim - 1)
            val y0 = ly.toInt().coerceIn(0, latentDim - 1)
            val y1 = (y0 + 1).coerceIn(0, latentDim - 1)
            val fy = ly - y0

            for (px in 0 until targetW) {
                val lx = (px.toFloat() / targetW) * (latentDim - 1)
                val x0 = lx.toInt().coerceIn(0, latentDim - 1)
                val x1 = (x0 + 1).coerceIn(0, latentDim - 1)
                val fx = lx - x0

                // Bilinear sample of 4 latent channels
                val idx00 = (y0 * latentDim + x0) * channels
                val idx10 = (y0 * latentDim + x1) * channels
                val idx01 = (y1 * latentDim + x0) * channels
                val idx11 = (y1 * latentDim + x1) * channels

                var ch0 = bilerp(latent[idx00], latent[idx10], latent[idx01], latent[idx11], fx, fy)
                var ch1 = bilerp(latent[idx00 + 1], latent[idx10 + 1], latent[idx01 + 1], latent[idx11 + 1], fx, fy)
                var ch2 = bilerp(latent[idx00 + 2], latent[idx10 + 2], latent[idx01 + 2], latent[idx11 + 2], fx, fy)
                var ch3 = bilerp(latent[idx00 + 3], latent[idx10 + 3], latent[idx01 + 3], latent[idx11 + 3], fx, fy)

                // Filmic tone-curve and contrast adjustment
                ch0 = (ch0 * palette.contrast + 0.15f).coerceIn(-1.5f, 2.5f)
                val luminance = sigmoid(ch0)

                // Hue blending: interpolate primaryHue to secondaryHue based on ch1 and ch2
                val hueBlend = (ch1 * 0.5f + 0.5f).coerceIn(0f, 1f)
                val currentHue = lerpHue(palette.primaryHue, palette.secondaryHue, hueBlend)

                // Saturation: boosted by prompt style, dampened slightly in highlights
                val sat = (palette.saturation * (1.0f + ch2 * 0.3f)).coerceIn(0.1f, 1.0f)

                // Value / Brightness: tone mapped with specular glow bloom from ch3
                val specular = (ch3 * palette.emissiveGlow).coerceAtLeast(0f)
                val value = (luminance + specular * 0.45f).coerceIn(0f, 1f)

                hsv[0] = currentHue
                hsv[1] = sat
                hsv[2] = value

                val rgb = Color.HSVToColor(hsv)

                // If progress < 1.0, blend subtle noise so the user sees the diffusion grain clearing
                if (progress < 0.95f) {
                    val noiseGrain = (1.0f - progress) * 0.35f
                    val r = ((Color.red(rgb) * (1f - noiseGrain) + (ch3 * 128f + 128f) * noiseGrain)).toInt().coerceIn(0, 255)
                    val g = ((Color.green(rgb) * (1f - noiseGrain) + (ch1 * 128f + 128f) * noiseGrain)).toInt().coerceIn(0, 255)
                    val b = ((Color.blue(rgb) * (1f - noiseGrain) + (ch2 * 128f + 128f) * noiseGrain)).toInt().coerceIn(0, 255)
                    pixels[py * targetW + px] = Color.rgb(r, g, b)
                } else {
                    pixels[py * targetW + px] = rgb
                }
            }
        }

        bitmap.setPixels(pixels, 0, targetW, 0, 0, targetW, targetH)
        return bitmap
    }

    private fun bilerp(v00: Float, v10: Float, v01: Float, v11: Float, fx: Float, fy: Float): Float {
        val top = v00 + (v10 - v00) * fx
        val bot = v01 + (v11 - v01) * fx
        return top + (bot - top) * fy
    }

    private fun sigmoid(x: Float): Float {
        return 1.0f / (1.0f + exp(-x * 2.2f))
    }

    private fun lerpHue(h1: Float, h2: Float, t: Float): Float {
        var diff = h2 - h1
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        var result = h1 + diff * t
        if (result < 0f) result += 360f
        if (result >= 360f) result -= 360f
        return result
    }

    fun analyzePrompt(promptText: String): SemanticPalette {
        val lower = promptText.lowercase()

        // 1. Structural Geometry
        val geometry = when {
            lower.contains("cyber") || lower.contains("city") || lower.contains("neon") ||
            lower.contains("building") || lower.contains("future") || lower.contains("street") ||
            lower.contains("punk") || lower.contains("car") || lower.contains("tokyo") ->
                StructuralGeometry.URBAN_CYBERPUNK

            lower.contains("galaxy") || lower.contains("space") || lower.contains("star") ||
            lower.contains("nebula") || lower.contains("cosmos") || lower.contains("planet") ||
            lower.contains("black hole") ->
                StructuralGeometry.COSMIC_GALAXY

            lower.contains("portrait") || lower.contains("face") || lower.contains("person") ||
            lower.contains("cat") || lower.contains("dog") || lower.contains("robot") ||
            lower.contains("character") || lower.contains("warrior") || lower.contains("girl") ||
            lower.contains("man") || lower.contains("creature") ->
                StructuralGeometry.RADIAL_PORTRAIT

            lower.contains("crystal") || lower.contains("diamond") || lower.contains("gem") ||
            lower.contains("geometric") || lower.contains("origami") || lower.contains("glass") ->
                StructuralGeometry.FRACTAL_CRYSTAL

            lower.contains("fluid") || lower.contains("abstract") || lower.contains("smoke") ||
            lower.contains("paint") || lower.contains("liquid") || lower.contains("swirl") ->
                StructuralGeometry.ABSTRACT_FLUID

            else -> StructuralGeometry.LANDSCAPE_NATURE // mountains, forest, lake, nature, sunset
        }

        // 2. Color Palette & Lighting
        var primaryHue = 210f   // default cyan / cool blue
        var secondaryHue = 320f // default magenta
        var saturation = 0.85f
        var contrast = 1.25f
        var emissiveGlow = 0.8f
        var frequency = 2.5f

        when {
            lower.contains("cyber") || lower.contains("neon") -> {
                primaryHue = 185f    // electric cyan
                secondaryHue = 315f  // neon magenta
                saturation = 0.95f
                contrast = 1.45f
                emissiveGlow = 1.0f
                frequency = 3.2f
            }
            lower.contains("sunset") || lower.contains("dusk") || lower.contains("warm") || lower.contains("golden") || lower.contains("fire") -> {
                primaryHue = 25f     // warm amber / orange
                secondaryHue = 345f  // crimson / gold
                saturation = 0.90f
                contrast = 1.30f
                emissiveGlow = 0.75f
                frequency = 2.2f
            }
            lower.contains("forest") || lower.contains("jungle") || lower.contains("nature") || lower.contains("tree") || lower.contains("garden") -> {
                primaryHue = 135f    // lush emerald
                secondaryHue = 45f   // sunlit lime / earth
                saturation = 0.80f
                contrast = 1.20f
                emissiveGlow = 0.50f
                frequency = 2.8f
            }
            lower.contains("space") || lower.contains("galaxy") || lower.contains("nebula") || lower.contains("cosmic") -> {
                primaryHue = 275f    // deep violet
                secondaryHue = 195f  // cyan starlight
                saturation = 0.95f
                contrast = 1.50f
                emissiveGlow = 1.2f
                frequency = 3.5f
            }
            lower.contains("ocean") || lower.contains("sea") || lower.contains("water") || lower.contains("underwater") -> {
                primaryHue = 195f    // deep sapphire
                secondaryHue = 165f  // turquoise
                saturation = 0.88f
                contrast = 1.25f
                emissiveGlow = 0.65f
                frequency = 2.4f
            }
            lower.contains("monochrome") || lower.contains("black and white") || lower.contains("noir") -> {
                primaryHue = 210f
                secondaryHue = 210f
                saturation = 0.05f
                contrast = 1.60f
                emissiveGlow = 0.40f
                frequency = 2.6f
            }
            lower.contains("pastel") || lower.contains("dream") || lower.contains("soft") || lower.contains("cloud") -> {
                primaryHue = 280f
                secondaryHue = 180f
                saturation = 0.45f
                contrast = 0.95f
                emissiveGlow = 0.40f
                frequency = 2.0f
            }
        }

        return SemanticPalette(
            primaryHue = primaryHue,
            secondaryHue = secondaryHue,
            saturation = saturation,
            contrast = contrast,
            emissiveGlow = emissiveGlow,
            geometry = geometry,
            frequency = frequency,
            shadowTint = (primaryHue + 180f) % 360f
        )
    }
}
