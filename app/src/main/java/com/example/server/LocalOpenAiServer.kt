package com.example.server

import android.content.Context
import com.example.data.model.AiModelInfo
import com.example.data.model.ClusterNode
import com.example.data.model.ImageGenConfig
import com.example.data.model.ServerConfig
import com.example.data.model.ServerLog
import com.example.engine.LocalInferenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.UUID

class LocalOpenAiServer(
    private val context: Context,
    private val inferenceEngine: LocalInferenceEngine
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val _config = MutableStateFlow(ServerConfig())
    val config: StateFlow<ServerConfig> = _config.asStateFlow()

    private val _logs = MutableStateFlow<List<ServerLog>>(emptyList())
    val logs: StateFlow<List<ServerLog>> = _logs.asStateFlow()

    private val _lanIp = MutableStateFlow(getDeviceLanIp())
    val lanIp: StateFlow<String> = _lanIp.asStateFlow()

    fun updateConfig(newConfig: ServerConfig) {
        val wasRunning = _config.value.isRunning
        _config.value = newConfig
        if (wasRunning && !newConfig.isRunning) {
            stopServer()
        } else if (!wasRunning && newConfig.isRunning) {
            startServer(emptyList(), emptyList())
        }
    }

    fun startServer(
        availableModels: List<AiModelInfo>,
        clusterNodes: List<ClusterNode>
    ) {
        if (serverSocket != null && !serverSocket!!.isClosed) return

        _lanIp.value = getDeviceLanIp()
        val port = _config.value.port

        try {
            serverSocket = ServerSocket(port)
            _config.update { it.copy(isRunning = true) }
            com.example.service.EdgeloomServerService.startService(context, _lanIp.value, port)

            acceptJob = scope.launch {
                while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        clientSocket.soTimeout = 30000
                        launch {
                            handleClient(clientSocket, availableModels, clusterNodes)
                        }
                    } catch (e: Exception) {
                        // socket closed or error
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _config.update { it.copy(isRunning = false) }
        }
    }

    fun stopServer() {
        acceptJob?.cancel()
        acceptJob = null
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
        com.example.service.EdgeloomServerService.stopService(context)
        _config.update { it.copy(isRunning = false, activeConnections = 0) }
    }

    private suspend fun handleClient(
        socket: Socket,
        availableModels: List<AiModelInfo>,
        clusterNodes: List<ClusterNode>
    ) {
        val startTime = System.currentTimeMillis()
        val clientIp = socket.inetAddress?.hostAddress ?: "127.0.0.1"

        try {
            _config.update { it.copy(activeConnections = it.activeConnections + 1) }
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            val output = socket.getOutputStream()

            val requestLine = input.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            // Read headers
            val headers = mutableMapOf<String, String>()
            var headerLine: String?
            var contentLength = 0
            while (input.readLine().also { headerLine = it } != null && headerLine!!.isNotEmpty()) {
                val colon = headerLine!!.indexOf(":")
                if (colon > 0) {
                    val key = headerLine!!.substring(0, colon).trim().lowercase()
                    val value = headerLine!!.substring(colon + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
            }

            // Handle preflight OPTIONS
            if (method == "OPTIONS") {
                sendCorsPreflight(output)
                return
            }

            // Auth verification
            val currentConf = _config.value
            if (currentConf.requireApiKey) {
                val auth = headers["authorization"] ?: ""
                val expected = "Bearer ${currentConf.apiKey}"
                if (auth != expected) {
                    sendJsonResponse(output, 401, """{"error":{"message":"Invalid API Key","type":"invalid_request_error"}}""")
                    recordLog("POST", path, 401, System.currentTimeMillis() - startTime, clientIp, 0, 0, "", "Unauthorized")
                    return
                }
            }

            // Body length limit: max 4MB to prevent memory exhaustion attacks
            if (contentLength > 4 * 1024 * 1024) {
                sendJsonResponse(output, 413, """{"error":{"message":"Payload Too Large: maximum allowed body size is 4MB","type":"invalid_request_error","param":null,"code":"payload_too_large"}}""")
                recordLog(method, path, 413, System.currentTimeMillis() - startTime, clientIp, 0, 0, "", "Payload Too Large")
                return
            }

            // Read body if available
            val bodyBuilder = StringBuilder()
            if (contentLength > 0) {
                val buffer = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val read = input.read(buffer, readTotal, contentLength - readTotal)
                    if (read == -1) break
                    readTotal += read
                }
                bodyBuilder.append(buffer, 0, readTotal)
            }
            val bodyString = bodyBuilder.toString()

            when {
                // GET /v1/models
                method == "GET" && path.startsWith("/v1/models") -> {
                    handleGetModels(output, availableModels)
                    recordLog("GET", path, 200, System.currentTimeMillis() - startTime, clientIp, 0, 0, "", "Model list (${availableModels.size} models)")
                }

                // POST /v1/chat/completions
                method == "POST" && path.startsWith("/v1/chat/completions") -> {
                    handleChatCompletions(output, bodyString, availableModels, clusterNodes, startTime, clientIp, path)
                }

                // POST /v1/completions
                method == "POST" && path.startsWith("/v1/completions") -> {
                    handleTextCompletions(output, bodyString, availableModels, clusterNodes, startTime, clientIp, path)
                }

                // POST /v1/images/generations
                method == "POST" && path.startsWith("/v1/images/generations") -> {
                    handleImageGenerations(output, bodyString, clusterNodes, startTime, clientIp, path)
                }

                // Health check
                method == "GET" && (path == "/" || path == "/health") -> {
                    sendJsonResponse(output, 200, """{"status":"ok","server":"LocalNode AI Cluster","nodes":${clusterNodes.size}}""")
                    recordLog("GET", path, 200, System.currentTimeMillis() - startTime, clientIp, 0, 0, "", "Healthcheck OK")
                }

                else -> {
                    sendJsonResponse(output, 404, """{"error":{"message":"Endpoint not found","type":"invalid_request_error"}}""")
                    recordLog(method, path, 404, System.currentTimeMillis() - startTime, clientIp, 0, 0, bodyString.take(120), "Not Found")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _config.update {
                it.copy(
                    activeConnections = (it.activeConnections - 1).coerceAtLeast(0),
                    totalRequestsServed = it.totalRequestsServed + 1
                )
            }
            try {
                socket.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun handleGetModels(output: OutputStream, availableModels: List<AiModelInfo>) {
        val root = JSONObject()
        root.put("object", "list")
        val dataArray = JSONArray()

        val modelsToReport = if (availableModels.isNotEmpty()) availableModels else listOf(
            AiModelInfo(
                id = "llama-3.2-1b-instruct",
                name = "Meta Llama 3.2 1B",
                repoId = "meta-llama/Llama-3.2-1B-Instruct-GGUF",
                filename = "llama-3.2.gguf",
                type = com.example.data.model.ModelType.TEXT,
                sizeBytes = 820_000_000L,
                parameters = "1.2B",
                quantization = "Q4_K_M",
                minRamGb = 1.8f,
                description = "Llama 3.2 1B",
                architecture = "Llama-3"
            )
        )

        for (m in modelsToReport) {
            val item = JSONObject()
            item.put("id", m.id)
            item.put("object", "model")
            item.put("created", 1700000000)
            item.put("owned_by", "localnode")
            val permArray = JSONArray()
            permArray.put(JSONObject().apply {
                put("id", "modelperm-${m.id}")
                put("object", "model_permission")
                put("created", 1700000000)
                put("allow_create_engine", false)
                put("allow_sampling", true)
                put("allow_logprobs", true)
                put("allow_search_indices", false)
                put("allow_view", true)
                put("allow_fine_tuning", false)
                put("organization", "*")
            })
            item.put("permission", permArray)
            item.put("root", m.id)
            dataArray.put(item)
        }
        root.put("data", dataArray)

        sendJsonResponse(output, 200, root.toString())
    }

    private suspend fun handleChatCompletions(
        output: OutputStream,
        body: String,
        availableModels: List<AiModelInfo>,
        clusterNodes: List<ClusterNode>,
        startTime: Long,
        clientIp: String,
        path: String
    ) {
        val json = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
        val stream = json.optBoolean("stream", false)
        val modelReq = json.optString("model", "llama-3.2-1b-instruct")
        val messages = json.optJSONArray("messages") ?: JSONArray()

        // Extract user prompt
        var userPrompt = "Hello"
        var systemPrompt = "You are a helpful on-device AI assistant."
        for (i in 0 until messages.length()) {
            val msg = messages.optJSONObject(i) ?: continue
            val role = msg.optString("role")
            val content = msg.optString("content")
            if (role == "user") userPrompt = content
            if (role == "system") systemPrompt = content
        }

        val activeModel = availableModels.find { it.id == modelReq } ?: availableModels.firstOrNull() ?: AiModelInfo(
            id = "llama-3.2-1b-instruct",
            name = "Meta Llama 3.2 1B",
            repoId = "meta-llama/Llama-3.2-1B-Instruct-GGUF",
            filename = "llama-3.2.gguf",
            type = com.example.data.model.ModelType.TEXT,
            sizeBytes = 820_000_000L,
            parameters = "1.2B",
            quantization = "Q4_K_M",
            minRamGb = 1.8f,
            description = "Default",
            architecture = "Llama-3"
        )

        val completionId = "chatcmpl-${UUID.randomUUID().toString().take(12)}"
        val createdTime = System.currentTimeMillis() / 1000

        if (stream) {
            // Send SSE Headers
            val headerString = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/event-stream\r\n" +
                    "Cache-Control: no-cache\r\n" +
                    "Connection: keep-alive\r\n" +
                    "Access-Control-Allow-Origin: *\r\n\r\n"
            output.write(headerString.toByteArray(StandardCharsets.UTF_8))
            output.flush()

            var totalTokens = 0
            val fullResponseBuilder = StringBuilder()

            inferenceEngine.generateChatStream(
                prompt = userPrompt,
                systemPrompt = systemPrompt,
                model = activeModel,
                clusterNodes = clusterNodes,
                isClusterEnabled = _config.value.clusterWorkloadSplitEnabled
            ).collect { token ->
                totalTokens++
                fullResponseBuilder.append(token)

                val chunk = JSONObject().apply {
                    put("id", completionId)
                    put("object", "chat.completion.chunk")
                    put("created", createdTime)
                    put("model", activeModel.id)
                    val choices = JSONArray().apply {
                        put(JSONObject().apply {
                            put("index", 0)
                            put("delta", JSONObject().put("content", token))
                            put("finish_reason", JSONObject.NULL)
                        })
                    }
                    put("choices", choices)
                }

                val sseData = "data: ${chunk}\n\n"
                try {
                    output.write(sseData.toByteArray(StandardCharsets.UTF_8))
                    output.flush()
                } catch (e: Exception) {
                    return@collect
                }
            }

            // End chunk
            val endChunk = JSONObject().apply {
                put("id", completionId)
                put("object", "chat.completion.chunk")
                put("created", createdTime)
                put("model", activeModel.id)
                val choices = JSONArray().apply {
                    put(JSONObject().apply {
                        put("index", 0)
                        put("delta", JSONObject())
                        put("finish_reason", "stop")
                    })
                }
                put("choices", choices)
            }
            output.write("data: ${endChunk}\n\n".toByteArray(StandardCharsets.UTF_8))
            output.write("data: [DONE]\n\n".toByteArray(StandardCharsets.UTF_8))
            output.flush()

            val duration = System.currentTimeMillis() - startTime
            recordLog(
                "POST", path, 200, duration, clientIp,
                userPrompt.split(" ").size, totalTokens,
                body.take(140), fullResponseBuilder.toString().take(140)
            )

        } else {
            // Non-streaming JSON response
            val fullResponseBuilder = StringBuilder()
            var tokenCount = 0

            inferenceEngine.generateChatStream(
                prompt = userPrompt,
                systemPrompt = systemPrompt,
                model = activeModel,
                clusterNodes = clusterNodes,
                isClusterEnabled = _config.value.clusterWorkloadSplitEnabled
            ).collect { token ->
                tokenCount++
                fullResponseBuilder.append(token)
            }

            val respJson = JSONObject().apply {
                put("id", completionId)
                put("object", "chat.completion")
                put("created", createdTime)
                put("model", activeModel.id)
                val choices = JSONArray().apply {
                    put(JSONObject().apply {
                        put("index", 0)
                        put("message", JSONObject().apply {
                            put("role", "assistant")
                            put("content", fullResponseBuilder.toString().trim())
                        })
                        put("finish_reason", "stop")
                    })
                }
                put("choices", choices)
                put("usage", JSONObject().apply {
                    val promptTokens = userPrompt.split(" ").size
                    put("prompt_tokens", promptTokens)
                    put("completion_tokens", tokenCount)
                    put("total_tokens", promptTokens + tokenCount)
                })
            }

            sendJsonResponse(output, 200, respJson.toString())
            val duration = System.currentTimeMillis() - startTime
            recordLog(
                "POST", path, 200, duration, clientIp,
                userPrompt.split(" ").size, tokenCount,
                body.take(140), fullResponseBuilder.toString().take(140)
            )
        }
    }

    private suspend fun handleTextCompletions(
        output: OutputStream,
        body: String,
        availableModels: List<AiModelInfo>,
        clusterNodes: List<ClusterNode>,
        startTime: Long,
        clientIp: String,
        path: String
    ) {
        val json = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
        val prompt = json.optString("prompt", "Hello")
        val modelReq = json.optString("model", "llama-3.2-1b-instruct")

        val activeModel = availableModels.find { it.id == modelReq } ?: availableModels.firstOrNull() ?: return
        val completionId = "cmpl-${UUID.randomUUID().toString().take(12)}"
        val fullText = StringBuilder()
        var tokens = 0

        inferenceEngine.generateChatStream(
            prompt = prompt,
            systemPrompt = "",
            model = activeModel,
            clusterNodes = clusterNodes,
            isClusterEnabled = _config.value.clusterWorkloadSplitEnabled
        ).collect {
            tokens++
            fullText.append(it)
        }

        val resp = JSONObject().apply {
            put("id", completionId)
            put("object", "text_completion")
            put("created", System.currentTimeMillis() / 1000)
            put("model", activeModel.id)
            val choices = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", fullText.toString().trim())
                    put("index", 0)
                    put("finish_reason", "stop")
                })
            }
            put("choices", choices)
            put("usage", JSONObject().apply {
                put("prompt_tokens", prompt.split(" ").size)
                put("completion_tokens", tokens)
                put("total_tokens", prompt.split(" ").size + tokens)
            })
        }

        sendJsonResponse(output, 200, resp.toString())
        recordLog("POST", path, 200, System.currentTimeMillis() - startTime, clientIp, prompt.split(" ").size, tokens, body.take(140), fullText.toString().take(140))
    }

    private suspend fun handleImageGenerations(
        output: OutputStream,
        body: String,
        clusterNodes: List<ClusterNode>,
        startTime: Long,
        clientIp: String,
        path: String
    ) {
        val json = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
        val prompt = json.optString("prompt", "A futuristic cyberpunk neural hub")
        val n = json.optInt("n", 1).coerceIn(1, 4)
        val size = json.optString("size", "512x512")
        val dim = if (size.contains("x")) size.split("x")[0].toIntOrNull() ?: 512 else 512

        val config = ImageGenConfig(
            prompt = prompt,
            steps = 15,
            width = dim,
            height = dim
        )

        val result = inferenceEngine.generateImage(config, clusterNodes) { _, _, _ -> }
        val b64 = result.bitmap?.let { inferenceEngine.bitmapToBase64(it) } ?: ""

        val responseJson = JSONObject().apply {
            put("created", System.currentTimeMillis() / 1000)
            val dataArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("b64_json", b64)
                    put("revised_prompt", prompt)
                })
            }
            put("data", dataArray)
        }

        sendJsonResponse(output, 200, responseJson.toString())
        recordLog("POST", path, 200, System.currentTimeMillis() - startTime, clientIp, prompt.split(" ").size, 1, body.take(140), "Image Generated (${dim}x${dim})")
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, json: String) {
        val statusMessage = when (statusCode) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Error"
        }
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val response = "HTTP/1.1 $statusCode $statusMessage\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Connection: close\r\n\r\n"

        output.write(response.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendCorsPreflight(output: OutputStream) {
        val response = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Access-Control-Max-Age: 86400\r\n" +
                "Connection: close\r\n\r\n"
        output.write(response.toByteArray(StandardCharsets.UTF_8))
        output.flush()
    }

    private fun recordLog(
        method: String,
        path: String,
        statusCode: Int,
        durationMs: Long,
        clientIp: String,
        promptTokens: Int,
        completionTokens: Int,
        payload: String,
        preview: String
    ) {
        val log = ServerLog(
            id = UUID.randomUUID().toString(),
            method = method,
            path = path,
            statusCode = statusCode,
            durationMs = durationMs,
            clientIp = clientIp,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            requestPayload = payload,
            responsePreview = preview,
            nodeProcessing = if (_config.value.clusterWorkloadSplitEnabled) "Cluster Mesh (Distributed)" else "Single Node (Local)"
        )

        _logs.update { (listOf(log) + it).take(50) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun getDeviceLanIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }
}
