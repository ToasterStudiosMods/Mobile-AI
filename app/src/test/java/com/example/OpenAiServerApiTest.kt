package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AiModelInfo
import com.example.data.model.ClusterNode
import com.example.data.model.ClusterNodeStatus
import com.example.data.model.ClusterRole
import com.example.data.model.ModelType
import com.example.engine.LocalInferenceEngine
import com.example.server.LocalOpenAiServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenAiServerApiTest {

    private lateinit var server: LocalOpenAiServer
    private lateinit var engine: LocalInferenceEngine
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val testPort = 18080

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        engine = LocalInferenceEngine(context)
        server = LocalOpenAiServer(context, engine)
        server.updateConfig(server.config.value.copy(port = testPort, requireApiKey = false))

        val testModels = listOf(
            AiModelInfo(
                id = "llama-3.2-1b-instruct",
                name = "Meta Llama 3.2 1B",
                repoId = "meta-llama/Llama-3.2-1B-Instruct-GGUF",
                filename = "llama.gguf",
                type = ModelType.TEXT,
                sizeBytes = 820_000_000L,
                parameters = "1.2B",
                quantization = "Q4_K_M",
                minRamGb = 1.8f,
                description = "Test Model",
                architecture = "Llama-3"
            )
        )
        val testNodes = listOf(
            ClusterNode(
                id = "self",
                name = "Host",
                address = "127.0.0.1",
                port = testPort,
                role = ClusterRole.LEADER,
                status = ClusterNodeStatus.ONLINE,
                assignedLayers = "Layers 1 - 28",
                latencyMs = 1L,
                memoryUsedMb = 1000,
                totalMemoryMb = 6144,
                throughputTokensPerSec = 18f,
                isSelf = true
            )
        )
        server.startServer(testModels, testNodes)
        Thread.sleep(150)
    }

    @After
    fun tearDown() {
        server.stopServer()
    }

    @Test
    fun testGetModels_returnsValidOpenAiListSchema() {
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/v1/models")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        assertEquals(200, response.code)

        val body = response.body?.string() ?: ""
        val json = JSONObject(body)
        assertEquals("list", json.optString("object"))

        val data = json.optJSONArray("data")
        assertTrue("Data array must not be null", data != null && data.length() > 0)
        assertEquals("model", data!!.getJSONObject(0).optString("object"))
    }

    @Test
    fun testHealthcheck_returnsOk() {
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/health")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        assertEquals(200, response.code)
        assertTrue(response.body?.string()?.contains("\"status\":\"ok\"") == true)
    }

    @Test
    fun testNonexistentEndpoint_returns404() {
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/v1/unknown_endpoint")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        assertEquals(404, response.code)
    }
}
