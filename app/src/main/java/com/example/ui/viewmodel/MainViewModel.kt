package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiModelInfo
import com.example.data.model.ChatMessage
import com.example.data.model.ClusterNode
import com.example.data.model.DeviceHardwareProfile
import com.example.data.model.ImageGenConfig
import com.example.data.model.ImageGenResult
import com.example.data.model.ModelType
import com.example.data.model.ServerConfig
import com.example.data.model.ServerLog
import com.example.data.repository.ClusterMeshRepository
import com.example.data.repository.HardwareProfiler
import com.example.data.repository.ModelRepository
import com.example.engine.LocalInferenceEngine
import com.example.server.LocalOpenAiServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

import kotlinx.coroutines.flow.map

enum class AppScreen {
    HOME,
    MODELS,
    RUN,
    CLUSTER,
    SERVER
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val hardwareProfiler = HardwareProfiler(application)
    val modelRepository = ModelRepository(application, hardwareProfiler)
    val clusterRepository = ClusterMeshRepository()
    val inferenceEngine = LocalInferenceEngine(application)
    val server = LocalOpenAiServer(application, inferenceEngine)

    // Current Screen
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Run Sub-Tab ("CHAT" vs "IMAGE")
    private val _runSubTab = MutableStateFlow("CHAT")
    val runSubTab: StateFlow<String> = _runSubTab.asStateFlow()

    // Hardware Profile
    private val _hardwareProfile = MutableStateFlow(hardwareProfiler.profileDevice())
    val hardwareProfile: StateFlow<DeviceHardwareProfile> = _hardwareProfile.asStateFlow()

    // Models
    val models: StateFlow<List<AiModelInfo>> = modelRepository.models
    val activeLoadedModel: StateFlow<AiModelInfo?> = modelRepository.activeLoadedModel
    val activeTextModel: StateFlow<AiModelInfo?> = modelRepository.activeTextModel
    val activeImageModel: StateFlow<AiModelInfo?> = modelRepository.activeImageModel

    // Model Filter Tab
    private val _modelFilter = MutableStateFlow("ALL") // "ALL", "TEXT", "IMAGE", "DOWNLOADED"
    val modelFilter: StateFlow<String> = _modelFilter.asStateFlow()

    // Cluster
    val clusterNodes: StateFlow<List<ClusterNode>> = clusterRepository.nodes
    val isClusterEnabled: StateFlow<Boolean> = clusterRepository.isClusterEnabled
    val layerDistributionMode: StateFlow<String> = clusterRepository.layerDistributionMode
    val isScanningSubnet: StateFlow<Boolean> = clusterRepository.isScanningSubnet
    val scanStatus: StateFlow<String> = clusterRepository.scanStatus
    val clusterEfficiencyWarning: StateFlow<String?> = clusterRepository.clusterEfficiencyWarning

    // Edgeloom Compute Pool usable budget (in GB) = sum(online RAM) * 0.7
    val computePoolUsableGb: StateFlow<Float> = combine(clusterNodes, isClusterEnabled) { nodes, enabled ->
        val active = if (enabled) nodes.filter { it.status == com.example.data.model.ClusterNodeStatus.ONLINE } else nodes.filter { it.isSelf }
        val totalMb = active.sumOf { it.totalMemoryMb }
        val usableGb = (totalMb / 1024f) * 0.7f
        (usableGb * 10f).toInt() / 10f
    }.stateIn(viewModelScope, SharingStarted.Lazily, 4.2f)

    val totalCoresInPool: StateFlow<Int> = combine(clusterNodes, isClusterEnabled) { nodes, enabled ->
        val active = if (enabled) nodes.filter { it.status == com.example.data.model.ClusterNodeStatus.ONLINE } else nodes.filter { it.isSelf }
        active.size * 8
    }.stateIn(viewModelScope, SharingStarted.Lazily, 8)

    val installedCount: StateFlow<Int> = models.map { list ->
        list.count { it.downloadState == com.example.data.model.DownloadState.DOWNLOADED }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 2)

    val totalOnDiskGb: StateFlow<Float> = models.map { list ->
        val bytes = list.filter { it.downloadState == com.example.data.model.DownloadState.DOWNLOADED }.sumOf { it.sizeBytes }
        (bytes / (1024f * 1024f * 1024f) * 10f).toInt() / 10f
    }.stateIn(viewModelScope, SharingStarted.Lazily, 1.7f)

    val downloadingCount: StateFlow<Int> = models.map { list ->
        list.count { it.downloadState == com.example.data.model.DownloadState.DOWNLOADING }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Filtered models
    val filteredModels: StateFlow<List<AiModelInfo>> = combine(models, _modelFilter) { list, filter ->
        when (filter) {
            "TEXT" -> list.filter { it.type == ModelType.TEXT }
            "IMAGE" -> list.filter { it.type == ModelType.IMAGE }
            "DOWNLOADED" -> list.filter { it.downloadState.name == "DOWNLOADED" }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // OpenAI Server
    val serverConfig: StateFlow<ServerConfig> = server.config
    val serverLogs: StateFlow<List<ServerLog>> = server.logs
    val lanIp: StateFlow<String> = server.lanIp

    // Playground Chat
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                role = "assistant",
                content = "👋 Welcome to Edgeloom! I am running on-device on your local hardware.\n\nYou can prompt me directly, configure multi-device pipeline parallel layer splitting under **Cluster**, or connect your IDE/Cursor/LangChain to this phone using the built-in **OpenAI API Server**!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isGeneratingText = MutableStateFlow(false)
    val isGeneratingText: StateFlow<Boolean> = _isGeneratingText.asStateFlow()

    private val _currentStreamingContent = MutableStateFlow("")
    val currentStreamingContent: StateFlow<String> = _currentStreamingContent.asStateFlow()

    private val _currentTps = MutableStateFlow(0f)
    val currentTps: StateFlow<Float> = _currentTps.asStateFlow()

    private val _ttftMs = MutableStateFlow(0L)
    val ttftMs: StateFlow<Long> = _ttftMs.asStateFlow()

    var chatJob: Job? = null

    // Image Gen State
    private val _imageGenConfig = MutableStateFlow(ImageGenConfig())
    val imageGenConfig: StateFlow<ImageGenConfig> = _imageGenConfig.asStateFlow()

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _imageGenStep = MutableStateFlow(0)
    val imageGenStep: StateFlow<Int> = _imageGenStep.asStateFlow()

    private val _intermediateBitmap = MutableStateFlow<Bitmap?>(null)
    val intermediateBitmap: StateFlow<Bitmap?> = _intermediateBitmap.asStateFlow()

    private val _imageResults = MutableStateFlow<List<ImageGenResult>>(emptyList())
    val imageResults: StateFlow<List<ImageGenResult>> = _imageResults.asStateFlow()

    // In-App Server Test State
    private val _serverTestResult = MutableStateFlow<String?>(null)
    val serverTestResult: StateFlow<String?> = _serverTestResult.asStateFlow()

    private val _isTestingServer = MutableStateFlow(false)
    val isTestingServer: StateFlow<Boolean> = _isTestingServer.asStateFlow()

    private val httpClient = OkHttpClient()

    init {
        // Start OpenAI server automatically so user can immediately call it
        server.startServer(modelRepository.models.value, clusterRepository.nodes.value)
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setRunSubTab(tab: String) {
        _runSubTab.value = tab
    }

    fun setModelFilter(filter: String) {
        _modelFilter.value = filter
    }

    fun refreshHardwareProfile() {
        _hardwareProfile.value = hardwareProfiler.profileDevice()
    }

    fun startDownload(modelId: String) {
        modelRepository.startDownload(modelId)
    }

    fun pauseDownload(modelId: String) {
        modelRepository.pauseDownload(modelId)
    }

    fun deleteModel(modelId: String) {
        modelRepository.deleteDownloadedModel(modelId)
    }

    fun loadModel(modelId: String) {
        modelRepository.loadModelInVram(modelId)
        val loaded = modelRepository.models.value.find { it.id == modelId } ?: return
        val app = getApplication<Application>()
        val modelsDir = File(app.filesDir, "models")
        val file = File(modelsDir, loaded.filename.substringAfterLast("/"))
        if (loaded.type == ModelType.TEXT) {
            inferenceEngine.loadGgufModel(file, loaded.id)
        } else {
            inferenceEngine.loadImageModel(file, loaded.id)
        }
    }

    fun addCustomHfModel(repoId: String, filename: String, type: ModelType) {
        modelRepository.addCustomHuggingFaceModel(repoId, filename, type)
    }

    // Cluster actions
    fun toggleCluster(enabled: Boolean) {
        clusterRepository.toggleClusterEnabled(enabled)
        server.updateConfig(serverConfig.value.copy(clusterWorkloadSplitEnabled = enabled))
    }

    fun scanSubnet() {
        clusterRepository.scanLocalSubnet()
    }

    fun addClusterNode(name: String, ip: String, port: Int) {
        clusterRepository.addWorkerNode(name, ip, port)
    }

    fun removeClusterNode(id: String) {
        clusterRepository.removeNode(id)
    }

    fun toggleNodeStatus(id: String) {
        clusterRepository.toggleNodeStatus(id)
    }

    fun setDistributionMode(mode: String) {
        clusterRepository.setLayerDistributionMode(mode)
    }

    // Server actions
    fun toggleServer(running: Boolean) {
        if (running) {
            server.startServer(modelRepository.models.value, clusterRepository.nodes.value)
        } else {
            server.stopServer()
        }
    }

    fun updateServerConfig(port: Int, requireApiKey: Boolean, apiKey: String) {
        val updated = serverConfig.value.copy(
            port = port,
            requireApiKey = requireApiKey,
            apiKey = apiKey
        )
        server.updateConfig(updated)
    }

    fun updateHostingPort(newPort: Int) {
        clusterRepository.updateHostPort(newPort)
        val current = serverConfig.value
        updateServerConfig(newPort, current.requireApiKey, current.apiKey)
    }

    fun clearServerLogs() {
        server.clearLogs()
    }

    // In-app Server Integration Tester (Tests the local HTTP endpoint directly)
    fun testServerEndpoint(prompt: String = "Explain cluster pipeline parallelism") {
        if (!serverConfig.value.isRunning) {
            _serverTestResult.value = "⚠️ Local OpenAI server is currently stopped. Turn on the server first!"
            return
        }

        viewModelScope.launch {
            _isTestingServer.value = true
            _serverTestResult.value = "Sending request to http://127.0.0.1:${serverConfig.value.port}/v1/chat/completions..."

            try {
                val port = serverConfig.value.port
                val jsonBody = JSONObject().apply {
                    put("model", activeLoadedModel.value?.id ?: "llama-3.2-1b-instruct")
                    val msgArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    }
                    put("messages", msgArray)
                    put("stream", false)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBuilder = Request.Builder()
                    .url("http://127.0.0.1:$port/v1/chat/completions")
                    .post(jsonBody.toString().toRequestBody(mediaType))

                if (serverConfig.value.requireApiKey) {
                    requestBuilder.addHeader("Authorization", "Bearer ${serverConfig.value.apiKey}")
                }

                val response = withContext(Dispatchers.IO) {
                    httpClient.newCall(requestBuilder.build()).execute()
                }

                val responseBody = withContext(Dispatchers.IO) {
                    response.body?.string() ?: ""
                }

                if (response.isSuccessful) {
                    val parsed = JSONObject(responseBody)
                    val reply = parsed.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: responseBody
                    _serverTestResult.value = "✅ HTTP 200 OK Response:\n$reply"
                } else {
                    _serverTestResult.value = "❌ HTTP ${response.code}: $responseBody"
                }
            } catch (e: Exception) {
                _serverTestResult.value = "❌ Connection error: ${e.message}"
            } finally {
                _isTestingServer.value = false
            }
        }
    }

    // Playground Chat
    fun sendChatMessage(prompt: String, systemPrompt: String = "You are an intelligent on-device AI assistant.") {
        if (prompt.isBlank() || _isGeneratingText.value) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = prompt.trim()
        )
        _chatMessages.update { it + userMsg }

        val activeModel = activeLoadedModel.value ?: return

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            _isGeneratingText.value = true
            _currentStreamingContent.value = ""

            val startTime = System.currentTimeMillis()
            var firstTokenTime = 0L
            var tokenCount = 0
            val fullContent = StringBuilder()

            val involvedNodes = if (isClusterEnabled.value) {
                clusterNodes.value.filter { it.status.name == "ONLINE" }.map { it.name }
            } else {
                listOf("Local Device")
            }

            inferenceEngine.generateChatStream(
                prompt = prompt,
                systemPrompt = systemPrompt,
                model = activeModel,
                clusterNodes = clusterNodes.value,
                isClusterEnabled = isClusterEnabled.value
            ).collect { token ->
                if (firstTokenTime == 0L) {
                    firstTokenTime = System.currentTimeMillis() - startTime
                    _ttftMs.value = firstTokenTime
                }
                tokenCount++
                fullContent.append(token)
                _currentStreamingContent.value = fullContent.toString()

                val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.01f)
                val tps = tokenCount / elapsedSec
                _currentTps.value = tps
            }

            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "assistant",
                content = fullContent.toString().trim(),
                tokensGenerated = tokenCount,
                tokensPerSecond = _currentTps.value,
                timeToFirstTokenMs = _ttftMs.value,
                clusterNodesInvolved = involvedNodes
            )

            _chatMessages.update { it + assistantMsg }
            _currentStreamingContent.value = ""
            _isGeneratingText.value = false
        }
    }

    fun stopGeneration() {
        chatJob?.cancel()
        chatJob = null
        if (_currentStreamingContent.value.isNotBlank()) {
            val partial = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "assistant",
                content = _currentStreamingContent.value + " [Stopped]"
            )
            _chatMessages.update { it + partial }
        }
        _currentStreamingContent.value = ""
        _isGeneratingText.value = false
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    // Image Generation
    fun updateImagePrompt(prompt: String) {
        _imageGenConfig.update { it.copy(prompt = prompt) }
    }

    fun updateImageSettings(steps: Int, guidance: Float, width: Int, height: Int) {
        _imageGenConfig.update {
            it.copy(steps = steps, guidanceScale = guidance, width = width, height = height)
        }
    }

    fun generateImage() {
        if (_isGeneratingImage.value) return

        viewModelScope.launch {
            _isGeneratingImage.value = true
            _imageGenStep.value = 0
            _intermediateBitmap.value = null

            val result = inferenceEngine.generateImage(
                config = _imageGenConfig.value,
                clusterNodes = clusterNodes.value
            ) { step, total, bitmap ->
                _imageGenStep.value = step
                _intermediateBitmap.value = bitmap
            }

            _imageResults.update { listOf(result) + it }
            _isGeneratingImage.value = false
        }
    }
}
