package com.example.data.model

import android.graphics.Bitmap

enum class ModelType {
    TEXT,
    IMAGE
}

enum class DownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    DOWNLOADED,
    ERROR
}

data class AiModelInfo(
    val id: String,
    val name: String,
    val repoId: String,
    val filename: String,
    val type: ModelType,
    val sizeBytes: Long,
    val parameters: String,
    val quantization: String,
    val minRamGb: Float,
    val description: String,
    val architecture: String,
    val contextWindow: Int = 4096,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
    val downloadProgress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val downloadSpeedMbps: Float = 0f,
    val isLoadedInVram: Boolean = false,
    val isRecommended: Boolean = false,
    val recommendationReason: String = ""
) {
    val formattedSize: String
        get() = when {
            sizeBytes >= 1_000_000_000L -> String.format("%.2f GB", sizeBytes / 1_000_000_000.0)
            sizeBytes >= 1_000_000L -> String.format("%.1f MB", sizeBytes / 1_000_000.0)
            else -> "$sizeBytes B"
        }
}

data class DeviceHardwareProfile(
    val totalRamGb: Float,
    val availableRamGb: Float,
    val cpuCores: Int,
    val freeStorageGb: Float,
    val totalStorageGb: Float,
    val isVulkanSupported: Boolean,
    val isNnapiAvailable: Boolean,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val thermalState: String,
    val recommendedModelId: String,
    val recommendationSummary: String,
    val performanceTier: String // "Entry", "Balanced", "Performance", "Flagship"
)

enum class ClusterRole {
    LEADER,
    WORKER
}

enum class ClusterNodeStatus {
    ONLINE,
    OFFLINE,
    COMPUTING,
    SYNCING
}

data class ClusterNode(
    val id: String,
    val name: String,
    val address: String, // e.g. "192.168.1.104"
    val port: Int = 8080,
    val role: ClusterRole = ClusterRole.WORKER,
    val status: ClusterNodeStatus = ClusterNodeStatus.ONLINE,
    val assignedLayers: String = "1-14", // e.g. "1-14" out of 28
    val latencyMs: Long = 18L,
    val memoryUsedMb: Int = 1420,
    val totalMemoryMb: Int = 6144,
    val throughputTokensPerSec: Float = 14.8f,
    val isSelf: Boolean = false
)

data class ServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val isRunning: Boolean = false,
    val requireApiKey: Boolean = false,
    val apiKey: String = "sk-localnode-cluster",
    val activeModelId: String = "llama-3.2-1b-instruct",
    val totalRequestsServed: Int = 0,
    val activeConnections: Int = 0,
    val averageLatencyMs: Long = 85L,
    val clusterWorkloadSplitEnabled: Boolean = true
)

data class ServerLog(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val method: String, // "POST", "GET"
    val path: String, // "/v1/chat/completions"
    val statusCode: Int = 200,
    val durationMs: Long,
    val clientIp: String,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val requestPayload: String = "",
    val responsePreview: String = "",
    val nodeProcessing: String = "Local + Cluster Node 2"
)

data class ChatMessage(
    val id: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensGenerated: Int = 0,
    val tokensPerSecond: Float = 0f,
    val timeToFirstTokenMs: Long = 0L,
    val clusterNodesInvolved: List<String> = emptyList(),
    val isStreaming: Boolean = false
)

data class ImageGenConfig(
    val prompt: String = "A cyberpunk city in neon mist, octane render 8k",
    val negativePrompt: String = "blurry, low quality, distorted, watermark",
    val steps: Int = 20,
    val guidanceScale: Float = 7.5f,
    val seed: Long = 42L,
    val width: Int = 512,
    val height: Int = 512,
    val modelId: String = "tiny-sd-mobile"
)

data class ImageGenResult(
    val id: String,
    val config: ImageGenConfig,
    val bitmap: Bitmap? = null,
    val durationMs: Long = 1840L,
    val timestamp: Long = System.currentTimeMillis(),
    val clusterNodeContributions: String = "Node 1 (Steps 1-10) + Local (Steps 11-20)"
)
