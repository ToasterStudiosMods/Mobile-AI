package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AiModelInfo
import com.example.data.model.DownloadState
import com.example.data.model.ModelType
import com.example.data.network.HuggingFaceApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ModelRepository(
    private val context: Context,
    private val hardwareProfiler: HardwareProfiler
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val hfClient = HuggingFaceApiClient()
    private val activeDownloadJobs = mutableMapOf<String, Job>()

    private val _models = MutableStateFlow<List<AiModelInfo>>(emptyList())
    val models: StateFlow<List<AiModelInfo>> = _models.asStateFlow()

    private val _activeLoadedModel = MutableStateFlow<AiModelInfo?>(null)
    val activeLoadedModel: StateFlow<AiModelInfo?> = _activeLoadedModel.asStateFlow()

    private val _activeTextModel = MutableStateFlow<AiModelInfo?>(null)
    val activeTextModel: StateFlow<AiModelInfo?> = _activeTextModel.asStateFlow()

    private val _activeImageModel = MutableStateFlow<AiModelInfo?>(null)
    val activeImageModel: StateFlow<AiModelInfo?> = _activeImageModel.asStateFlow()

    private val _isSearchingHf = MutableStateFlow(false)
    val isSearchingHf: StateFlow<Boolean> = _isSearchingHf.asStateFlow()

    init {
        initializeCatalog()
        fetchLiveHuggingFaceTrendingModels()
    }

    private fun initializeCatalog() {
        val profile = hardwareProfiler.profileDevice()
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        // Clean out any corrupted GGUF files or stubs under 100MB to ensure zero false-positives
        modelsDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                if (file.name.endsWith(".gguf", ignoreCase = true)) {
                    if (file.length() < 100_000_000L || !isValidGgufHeader(file)) {
                        file.delete()
                    }
                } else if (file.length() < 100_000L && !file.name.contains("diffusion")) {
                    file.delete()
                }
            }
        }

        fun checkStatus(filename: String, minBytes: Long): Pair<DownloadState, Long> {
            val file = File(modelsDir, filename)
            return if (file.exists() && file.length() >= minBytes && isValidGgufHeader(file)) {
                Pair(DownloadState.DOWNLOADED, file.length())
            } else {
                Pair(DownloadState.NOT_DOWNLOADED, 0L)
            }
        }

        val llamaStatus = checkStatus("Llama-3.2-1B-Instruct-Q4_K_M.gguf", 500_000_000L)

        val initialList = listOf(
            AiModelInfo(
                id = "llama-3.2-1b-instruct",
                name = "Meta Llama 3.2 1B",
                repoId = "bartowski/Llama-3.2-1B-Instruct-GGUF",
                filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                type = ModelType.TEXT,
                sizeBytes = 820_000_000L,
                parameters = "1.23 Billion",
                quantization = "Q4_K_M",
                minRamGb = 1.8f,
                description = "Instruction-tuned lightweight model with rotary embeddings and grouped query attention (ungated community weights).",
                architecture = "Llama-3 (GQA, RoPE)",
                contextWindow = 4096,
                downloadState = llamaStatus.first,
                downloadProgress = if (llamaStatus.first == DownloadState.DOWNLOADED) 1.0f else 0.0f,
                downloadedBytes = llamaStatus.second,
                isLoadedInVram = false,
                isRecommended = profile.recommendedModelId == "llama-3.2-1b-instruct",
                recommendationReason = "Optimal balance of low RAM footprint and high instruction following."
            ),
            AiModelInfo(
                id = "gemma-2-2b-it",
                name = "Google Gemma 2 2B IT",
                repoId = "bartowski/gemma-2-2b-it-GGUF",
                filename = "gemma-2-2b-it-Q4_K_M.gguf",
                type = ModelType.TEXT,
                sizeBytes = 1_650_000_000L,
                parameters = "2.61 Billion",
                quantization = "Q4_K_M",
                minRamGb = 3.2f,
                description = "Google DeepMind's small language model with sliding-window attention and logit soft-capping (ungated community weights).",
                architecture = "Gemma-2 (GeGLU, RMSNorm)",
                contextWindow = 8192,
                downloadState = DownloadState.NOT_DOWNLOADED,
                downloadProgress = 0f,
                isLoadedInVram = false,
                isRecommended = profile.recommendedModelId == "gemma-2-2b-it",
                recommendationReason = "High precision, superior reasoning, and STEM performance."
            ),
            AiModelInfo(
                id = "smollm2-1.7b-instruct",
                name = "HuggingFace SmolLM2 1.7B",
                repoId = "HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF",
                filename = "smollm2-1.7b-instruct-q4_k_m.gguf",
                type = ModelType.TEXT,
                sizeBytes = 1_050_000_000L,
                parameters = "1.71 Billion",
                quantization = "Q4_K_M",
                minRamGb = 2.2f,
                description = "Trained by Hugging Face on 11 trillion tokens specifically for edge devices and phones.",
                architecture = "SmolLM (SwiGLU, GQA)",
                contextWindow = 4096,
                downloadState = DownloadState.NOT_DOWNLOADED,
                downloadProgress = 0f,
                isLoadedInVram = false,
                isRecommended = profile.recommendedModelId == "smollm2-1.7b-instruct",
                recommendationReason = "Specially optimized by Hugging Face for on-device mobile execution."
            ),
            AiModelInfo(
                id = "qwen2.5-0.5b-instruct",
                name = "Qwen 2.5 0.5B Instruct",
                repoId = "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
                filename = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                type = ModelType.TEXT,
                sizeBytes = 398_000_000L,
                parameters = "490 Million",
                quantization = "Q4_K_M",
                minRamGb = 0.9f,
                description = "Ultra-compact edge model that executes with zero thermal throttle and minimal memory pressure.",
                architecture = "Qwen-2.5 (Dense)",
                contextWindow = 2048,
                downloadState = DownloadState.NOT_DOWNLOADED,
                downloadProgress = 0f,
                isLoadedInVram = false,
                isRecommended = profile.recommendedModelId == "qwen2.5-0.5b-instruct",
                recommendationReason = "Fits under 400MB RAM, allowing multi-tasking without background kills."
            ),
            AiModelInfo(
                id = "qwen2.5-1.5b-instruct",
                name = "Qwen 2.5 1.5B Instruct",
                repoId = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
                filename = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
                type = ModelType.TEXT,
                sizeBytes = 986_000_000L,
                parameters = "1.54 Billion",
                quantization = "Q4_K_M",
                minRamGb = 2.0f,
                description = "Powerful bilingual and coding edge model with extended 32k context capability.",
                architecture = "Qwen-2.5 (Dense, RoPE)",
                contextWindow = 4096,
                downloadState = DownloadState.NOT_DOWNLOADED,
                downloadProgress = 0f,
                isLoadedInVram = false,
                isRecommended = profile.totalRamGb in 4.0f..7.0f,
                recommendationReason = "Excellent instruction following and coding capability for mid-tier devices."
            ),
            AiModelInfo(
                id = "deepseek-r1-1.5b",
                name = "DeepSeek R1 Distill 1.5B",
                repoId = "bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
                filename = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
                type = ModelType.TEXT,
                sizeBytes = 1_120_000_000L,
                parameters = "1.78 Billion",
                quantization = "Q4_K_M",
                minRamGb = 2.4f,
                description = "Reasoning-specialized distillation trained on long chains of thought (CoT).",
                architecture = "DeepSeek (Reasoning)",
                contextWindow = 4096,
                downloadState = DownloadState.NOT_DOWNLOADED,
                downloadProgress = 0f,
                isLoadedInVram = false,
                isRecommended = profile.totalRamGb >= 6.0f,
                recommendationReason = "Advanced step-by-step reasoning and logical deduction."
            ),
            AiModelInfo(
                id = "tiny-sd-mobile",
                name = "Tiny-SD Mobile Denoise",
                repoId = "IlyasMoutawwakil/tiny-stable-diffusion-onnx",
                filename = "unet/model.onnx",
                type = ModelType.IMAGE,
                sizeBytes = 860_000_000L,
                parameters = "560 Million",
                quantization = "FP16 UNet",
                minRamGb = 2.5f,
                description = "Mobile latent diffusion UNet compiled for edge GPUs and Hexagon NPUs.",
                architecture = "Latent Diffusion",
                contextWindow = 77,
                downloadState = DownloadState.NOT_DOWNLOADED,
                downloadProgress = 0.0f,
                downloadedBytes = 0L,
                isLoadedInVram = false,
                isRecommended = true,
                recommendationReason = "Sub-2s mobile diffusion pipeline with hardware acceleration."
            ),
            AiModelInfo(
                id = "sd-turbo-onnx",
                name = "Stability SD-Turbo ONNX",
                repoId = "softwareweaver/Sdxl-Turbo-Olive-Onnx",
                filename = "unet/model.onnx",
                type = ModelType.IMAGE,
                sizeBytes = 1_750_000_000L,
                parameters = "890 Million",
                quantization = "FP16 / INT8",
                minRamGb = 4.0f,
                description = "Adversarial Diffusion Distillation (ADD) enabling 1-step real-time photorealistic synthesis.",
                architecture = "ADD / Latent Consistency Model",
                contextWindow = 77,
                downloadState = DownloadState.NOT_DOWNLOADED,
                downloadProgress = 0f,
                isLoadedInVram = false,
                isRecommended = profile.totalRamGb >= 6.0f,
                recommendationReason = "Single-step high quality image generation."
            ),
            AiModelInfo(
                id = "edgeloom-latent-diffusion",
                name = "Edgeloom Latent Diffusion",
                repoId = "local/edgeloom-latent-diffusion",
                filename = "edgeloom-latent-diffusion.bin",
                type = ModelType.IMAGE,
                sizeBytes = 48_000_000L,
                parameters = "64x64x4 Latent UNet",
                quantization = "Float32 Latent",
                minRamGb = 1.0f,
                description = "Built-in on-device latent diffusion & Fourier neural synthesis engine. Zero cloud APIs, instant local generation.",
                architecture = "Latent Diffusion (Euler / CFG)",
                contextWindow = 77,
                downloadState = DownloadState.DOWNLOADED,
                downloadProgress = 1.0f,
                downloadedBytes = 48_000_000L,
                isLoadedInVram = true,
                isRecommended = true,
                recommendationReason = "Instant on-device execution with zero external download required."
            )
        )

        _models.value = initialList
        _activeLoadedModel.value = null
        _activeTextModel.value = null
        _activeImageModel.value = initialList.firstOrNull { it.id == "edgeloom-latent-diffusion" }
    }

    private fun isValidGgufHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        return try {
            java.io.FileInputStream(file).use { fis ->
                val magic = ByteArray(4)
                val read = fis.read(magic)
                read == 4 && magic[0] == 'G'.code.toByte() && magic[1] == 'G'.code.toByte() &&
                        magic[2] == 'U'.code.toByte() && magic[3] == 'F'.code.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun fetchLiveHuggingFaceTrendingModels() {
        scope.launch {
            try {
                _isSearchingHf.value = true
                val liveDtos = hfClient.searchModels("gguf")
                if (liveDtos.isNotEmpty()) {
                    val currentIds = _models.value.map { it.id }.toSet()
                    val converted = liveDtos
                        .filterNot { currentIds.contains(it.id) || currentIds.contains(it.name) }
                        .take(8)
                        .map { dto ->
                            AiModelInfo(
                                id = dto.id.replace("/", "--"),
                                name = dto.name,
                                repoId = dto.id,
                                filename = dto.defaultFilename,
                                type = if (dto.pipelineTag.contains("image", ignoreCase = true)) ModelType.IMAGE else ModelType.TEXT,
                                sizeBytes = dto.estimatedSizeBytes,
                                parameters = "${dto.downloads} DLs on HF",
                                quantization = if (dto.tags.any { it.contains("q4", ignoreCase = true) }) "Q4_K_M" else "GGUF",
                                minRamGb = (dto.estimatedSizeBytes / (1024f * 1024f * 1024f) * 1.5f).coerceAtLeast(1.0f),
                                description = "Live model from Hugging Face by @${dto.author} with ${dto.downloads} downloads and ${dto.likes} likes.",
                                architecture = "Hugging Face Hub"
                            )
                        }

                    _models.update { current -> current + converted }
                }
            } catch (e: Exception) {
                Log.e("ModelRepository", "Failed to fetch live HF models", e)
            } finally {
                _isSearchingHf.value = false
            }
        }
    }

    fun startDownload(modelId: String) {
        val model = _models.value.find { it.id == modelId } ?: return
        if (model.downloadState == DownloadState.DOWNLOADING || model.downloadState == DownloadState.DOWNLOADED) return

        _models.update { list ->
            list.map {
                if (it.id == modelId) it.copy(downloadState = DownloadState.DOWNLOADING) else it
            }
        }

        val job = scope.launch {
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) modelsDir.mkdirs()
            val targetFilename = model.filename.substringAfterLast("/")
            val targetFile = File(modelsDir, targetFilename)

            val downloadResult = hfClient.downloadRealFile(
                repoId = model.repoId,
                filename = model.filename,
                destinationFile = targetFile
            ) { downloadedBytes, totalBytes, speedMbps ->
                val progress = (downloadedBytes.toFloat() / totalBytes.coerceAtLeast(1L)).coerceIn(0f, 1f)
                _models.update { list ->
                    list.map {
                        if (it.id == modelId) {
                            it.copy(
                                downloadProgress = progress,
                                downloadedBytes = downloadedBytes,
                                downloadSpeedMbps = speedMbps
                            )
                        } else it
                    }
                }
            }

            if (downloadResult.success && targetFile.exists() && targetFile.length() > 0) {
                val sha = hfClient.calculateSha256(targetFile)
                Log.i("ModelRepository", "Model downloaded successfully: ${targetFile.name}, size=${targetFile.length()} bytes, sha256=$sha")
                _models.update { list ->
                    list.map {
                        if (it.id == modelId) {
                            it.copy(
                                downloadState = DownloadState.DOWNLOADED,
                                downloadProgress = 1.0f,
                                downloadedBytes = targetFile.length(),
                                downloadSpeedMbps = 0f,
                                filename = downloadResult.resolvedFilename,
                                repoId = downloadResult.resolvedRepoId
                            )
                        } else it
                    }
                }
            } else {
                Log.w("ModelRepository", "Download failed for $modelId: ${downloadResult.errorMessage}")
                _models.update { list ->
                    list.map {
                        if (it.id == modelId) it.copy(downloadState = DownloadState.ERROR) else it
                    }
                }
            }
            activeDownloadJobs.remove(modelId)
        }

        activeDownloadJobs[modelId] = job
    }

    fun pauseDownload(modelId: String) {
        activeDownloadJobs[modelId]?.cancel()
        activeDownloadJobs.remove(modelId)
        _models.update { list ->
            list.map {
                if (it.id == modelId) it.copy(downloadState = DownloadState.PAUSED, downloadSpeedMbps = 0f) else it
            }
        }
    }

    fun deleteDownloadedModel(modelId: String) {
        if (modelId == "edgeloom-latent-diffusion") return
        activeDownloadJobs[modelId]?.cancel()
        activeDownloadJobs.remove(modelId)

        val model = _models.value.find { it.id == modelId } ?: return
        val modelsDir = File(context.filesDir, "models")
        val file = File(modelsDir, model.filename)
        if (file.exists()) file.delete()

        _models.update { list ->
            list.map {
                if (it.id == modelId) {
                    it.copy(
                        downloadState = DownloadState.NOT_DOWNLOADED,
                        downloadProgress = 0f,
                        downloadedBytes = 0L,
                        isLoadedInVram = false
                    )
                } else it
            }
        }

        if (_activeLoadedModel.value?.id == modelId) {
            _activeLoadedModel.value = _models.value.firstOrNull { it.downloadState == DownloadState.DOWNLOADED && it.type == ModelType.TEXT }
        }
        if (_activeTextModel.value?.id == modelId) {
            _activeTextModel.value = _models.value.firstOrNull { it.downloadState == DownloadState.DOWNLOADED && it.type == ModelType.TEXT }
        }
        if (_activeImageModel.value?.id == modelId) {
            _activeImageModel.value = _models.value.firstOrNull { it.downloadState == DownloadState.DOWNLOADED && it.type == ModelType.IMAGE }
        }
    }

    fun loadModelInVram(modelId: String) {
        val target = _models.value.find { it.id == modelId } ?: return
        if (target.downloadState != DownloadState.DOWNLOADED) return

        _models.update { list ->
            list.map {
                if (it.type == target.type) {
                    it.copy(isLoadedInVram = (it.id == modelId))
                } else it
            }
        }
        _activeLoadedModel.value = target.copy(isLoadedInVram = true)
        if (target.type == ModelType.TEXT) {
            _activeTextModel.value = target.copy(isLoadedInVram = true)
        } else {
            _activeImageModel.value = target.copy(isLoadedInVram = true)
        }
    }

    fun addCustomHuggingFaceModel(repoId: String, filename: String, type: ModelType) {
        val cleanRepo = repoId.trim()
        val cleanFile = filename.trim().ifEmpty { "${cleanRepo.substringAfterLast("/")}.gguf" }
        val id = cleanRepo.replace("/", "--")

        val newModel = AiModelInfo(
            id = id,
            name = cleanRepo.substringAfterLast("/"),
            repoId = cleanRepo,
            filename = cleanFile,
            type = type,
            sizeBytes = if (type == ModelType.TEXT) 1_400_000_000L else 1_200_000_000L,
            parameters = "Custom HF Model",
            quantization = "Q4_K_M GGUF",
            minRamGb = 2.5f,
            description = "Custom model from Hugging Face ($cleanRepo). Ready to download and run.",
            architecture = if (type == ModelType.TEXT) "Transformer" else "Diffusion",
            contextWindow = 4096,
            downloadState = DownloadState.NOT_DOWNLOADED,
            downloadProgress = 0f
        )

        _models.update { listOf(newModel) + it }
        startDownload(newModel.id)
    }
}
