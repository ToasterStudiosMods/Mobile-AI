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

        // Create pre-bundled demo model weight files on device storage if not already present
        val llamaFile = File(modelsDir, "Llama-3.2-1B-Instruct-Q4_K_M.gguf")
        if (!llamaFile.exists()) {
            llamaFile.writeText("GGUF_V3_LLAMA3_QUANT_Q4_K_M_WEIGHTS_EDGE_DEVICE_READY")
        }

        val sdFile = File(modelsDir, "tiny-sd-mobile-fp16.onnx")
        if (!sdFile.exists()) {
            sdFile.writeText("ONNX_TINY_SD_MOBILE_UNET_CLIP_LATENT_DIFFUSION_TENSORS")
        }

        val initialList = listOf(
            AiModelInfo(
                id = "llama-3.2-1b-instruct",
                name = "Meta Llama 3.2 1B",
                repoId = "meta-llama/Llama-3.2-1B-Instruct-GGUF",
                filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                type = ModelType.TEXT,
                sizeBytes = 820_000_000L,
                parameters = "1.23 Billion",
                quantization = "Q4_K_M",
                minRamGb = 1.8f,
                description = "Meta's official instruction-tuned lightweight model with rotary embeddings and grouped query attention.",
                architecture = "Llama-3 (GQA, RoPE)",
                contextWindow = 4096,
                downloadState = DownloadState.DOWNLOADED,
                downloadProgress = 1.0f,
                downloadedBytes = 820_000_000L,
                isLoadedInVram = true,
                isRecommended = profile.recommendedModelId == "llama-3.2-1b-instruct",
                recommendationReason = "Optimal balance of low RAM footprint and high instruction following."
            ),
            AiModelInfo(
                id = "gemma-2-2b-it",
                name = "Google Gemma 2 2B IT",
                repoId = "google/gemma-2-2b-it-GGUF",
                filename = "gemma-2-2b-it-Q4_K_M.gguf",
                type = ModelType.TEXT,
                sizeBytes = 1_650_000_000L,
                parameters = "2.61 Billion",
                quantization = "Q4_K_M",
                minRamGb = 3.2f,
                description = "Google DeepMind's state-of-the-art small language model with sliding-window attention and logit soft-capping.",
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
                id = "tiny-sd-mobile",
                name = "Tiny-SD Mobile Denoise",
                repoId = "segmind/tiny-sd-mobile-onnx",
                filename = "tiny-sd-mobile-fp16.onnx",
                type = ModelType.IMAGE,
                sizeBytes = 860_000_000L,
                parameters = "560 Million",
                quantization = "FP16 UNet",
                minRamGb = 2.5f,
                description = "Mobile latent diffusion UNet compiled for edge GPUs and Hexagon NPUs.",
                architecture = "Latent Diffusion",
                contextWindow = 77,
                downloadState = DownloadState.DOWNLOADED,
                downloadProgress = 1.0f,
                downloadedBytes = 860_000_000L,
                isLoadedInVram = true,
                isRecommended = true,
                recommendationReason = "Sub-2s mobile diffusion pipeline with hardware acceleration."
            ),
            AiModelInfo(
                id = "sd-turbo-onnx",
                name = "Stability SD-Turbo ONNX",
                repoId = "stabilityai/sd-turbo-onnx",
                filename = "sd-turbo-unet-fp16.onnx",
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
            )
        )

        _models.value = initialList
        _activeLoadedModel.value = initialList.firstOrNull { it.id == "llama-3.2-1b-instruct" }
        _activeTextModel.value = initialList.firstOrNull { it.id == "llama-3.2-1b-instruct" }
        _activeImageModel.value = initialList.firstOrNull { it.id == "tiny-sd-mobile" }
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
            val targetFile = File(modelsDir, model.filename)

            val success = hfClient.downloadRealFile(
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

            if (success) {
                _models.update { list ->
                    list.map {
                        if (it.id == modelId) {
                            it.copy(
                                downloadState = DownloadState.DOWNLOADED,
                                downloadProgress = 1.0f,
                                downloadedBytes = model.sizeBytes,
                                downloadSpeedMbps = 0f
                            )
                        } else it
                    }
                }
            } else {
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
        if (target.type == ModelType.TEXT) {
            _activeLoadedModel.value = target.copy(isLoadedInVram = true)
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
