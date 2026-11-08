package com.example.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import com.example.data.model.DeviceHardwareProfile

class HardwareProfiler(private val context: Context) {

    fun profileDevice(): DeviceHardwareProfile {
        // RAM profiling
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalRamGb = memInfo.totalMem / (1024f * 1024f * 1024f)
        val availableRamGb = memInfo.availMem / (1024f * 1024f * 1024f)

        // CPU cores
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // Storage profiling
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalStorageGb = (totalBlocks * blockSize) / (1024f * 1024f * 1024f)
        val freeStorageGb = (availableBlocks * blockSize) / (1024f * 1024f * 1024f)

        // Vulkan / GPU acceleration
        val hasVulkan = context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION) ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)

        // Battery
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 80
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (scale > 0) ((level / scale.toFloat()) * 100).toInt() else 80
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // Thermal state
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermalState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Nominal (Cool)"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Throttle"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe Thermal Throttle"
                else -> "Normal"
            }
        } else {
            "Normal"
        }

        // Tier evaluation & recommendation
        val performanceTier: String
        val recommendedModelId: String
        val recommendationSummary: String

        when {
            totalRamGb >= 8.0f -> {
                performanceTier = "Flagship Tier"
                recommendedModelId = "gemma-2-2b-it"
                recommendationSummary = "Your device has ample high-speed RAM (${String.format("%.1f", totalRamGb)} GB). Recommending Google Gemma 2 2B (Q4_K_M) with full 4K context and SD-Turbo image generation."
            }
            totalRamGb >= 5.0f -> {
                performanceTier = "High Performance"
                recommendedModelId = "llama-3.2-1b-instruct"
                recommendationSummary = "Balanced memory envelope (${String.format("%.1f", totalRamGb)} GB). Meta Llama 3.2 1B (Q4_K_M) offers great reasoning throughput with ~820MB VRAM footprint."
            }
            totalRamGb >= 3.0f -> {
                performanceTier = "Balanced Tier"
                recommendedModelId = "smollm2-1.7b-instruct"
                recommendationSummary = "Great efficiency. SmolLM2 1.7B or Qwen 2.5 0.5B runs smoothly with fast token generation without heating the device."
            }
            else -> {
                performanceTier = "Ultra Lightweight Tier"
                recommendedModelId = "qwen2.5-0.5b-instruct"
                recommendationSummary = "Low memory overhead mode. Qwen 2.5 0.5B fits under 400MB RAM, leaving full headroom for background multitasking."
            }
        }

        return DeviceHardwareProfile(
            totalRamGb = totalRamGb,
            availableRamGb = availableRamGb,
            cpuCores = cpuCores,
            freeStorageGb = freeStorageGb,
            totalStorageGb = totalStorageGb,
            isVulkanSupported = hasVulkan,
            isNnapiAvailable = true,
            batteryPercent = batteryPct,
            isCharging = isCharging,
            thermalState = thermalState,
            recommendedModelId = recommendedModelId,
            recommendationSummary = recommendationSummary,
            performanceTier = performanceTier
        )
    }
}
