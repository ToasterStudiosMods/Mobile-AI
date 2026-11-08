package com.example.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class HuggingFaceModelDto(
    val id: String, // e.g. "meta-llama/Llama-3.2-1B-Instruct-GGUF"
    val name: String,
    val author: String,
    val downloads: Int,
    val likes: Int,
    val tags: List<String>,
    val pipelineTag: String, // e.g. "text-generation", "text-to-image"
    val defaultFilename: String,
    val estimatedSizeBytes: Long
)

class HuggingFaceApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun searchModels(query: String = "gguf", filter: String = ""): List<HuggingFaceModelDto> = withContext(Dispatchers.IO) {
        val list = mutableListOf<HuggingFaceModelDto>()
        try {
            val filterParam = if (filter.isNotBlank()) "&filter=$filter" else ""
            val url = "https://huggingface.co/api/models?search=${java.net.URLEncoder.encode(query, "UTF-8")}&sort=downloads&direction=-1&limit=25$filterParam"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Edgeloom-Android/2.0 (Local GGUF Orchestrator)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: "[]"
                val jsonArr = JSONArray(jsonStr)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val id = obj.optString("id")
                    if (id.isBlank()) continue

                    val author = id.substringBefore("/", "huggingface")
                    val name = id.substringAfter("/")
                    val downloads = obj.optInt("downloads", 0)
                    val likes = obj.optInt("likes", 0)
                    val pipeline = obj.optString("pipeline_tag", "text-generation")

                    val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                    val tags = mutableListOf<String>()
                    for (t in 0 until tagsArr.length()) {
                        tags.add(tagsArr.getString(t))
                    }

                    val isGguf = tags.any { it.contains("gguf", ignoreCase = true) } || id.contains("gguf", ignoreCase = true)
                    val defaultFile = if (isGguf) "$name-Q4_K_M.gguf" else "$name.onnx"
                    val estimatedBytes = when {
                        id.contains("0.5B", ignoreCase = true) || id.contains("360M", ignoreCase = true) -> 380_000_000L
                        id.contains("1B", ignoreCase = true) || id.contains("1.7B", ignoreCase = true) -> 880_000_000L
                        id.contains("2B", ignoreCase = true) -> 1_500_000_000L
                        id.contains("3B", ignoreCase = true) -> 2_200_000_000L
                        else -> 1_100_000_000L
                    }

                    list.add(
                        HuggingFaceModelDto(
                            id = id,
                            name = name,
                            author = author,
                            downloads = downloads,
                            likes = likes,
                            tags = tags,
                            pipelineTag = pipeline,
                            defaultFilename = defaultFile,
                            estimatedSizeBytes = estimatedBytes
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("HuggingFaceApiClient", "Search failed: ${e.message}")
        }
        return@withContext list
    }

    suspend fun downloadRealFile(
        repoId: String,
        filename: String,
        destinationFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long, speedMbps: Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val downloadUrl = "https://huggingface.co/$repoId/resolve/main/$filename"
        val existingBytes = if (destinationFile.exists()) destinationFile.length() else 0L

        val requestBuilder = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Edgeloom-Android/2.0 (Resumable GGUF Downloader)")

        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        try {
            destinationFile.parentFile?.mkdirs()
            val response = client.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful && response.code != 416) {
                // If specific subfile not directly at resolve URL, attempt to query the repository tree
                Log.e("HuggingFaceApiClient", "Download failed with HTTP ${response.code}: $downloadUrl")
                return@withContext false
            }

            if (response.code == 416) {
                // Range already satisfied (entire file already downloaded)
                onProgress(destinationFile.length(), destinationFile.length(), 0f)
                return@withContext true
            }

            val body = response.body ?: return@withContext false
            val isPartial = response.code == 206
            val incomingLength = body.contentLength()
            val totalBytes = if (isPartial) existingBytes + incomingLength else if (incomingLength > 0) incomingLength else 500_000_000L

            var downloaded = if (isPartial) existingBytes else 0L
            val buffer = ByteArray(64 * 1024)
            val startTime = System.currentTimeMillis()

            body.byteStream().use { input ->
                FileOutputStream(destinationFile, isPartial).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                            return@withContext false
                        }
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead

                        val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.1f)
                        val speedMbps = ((downloaded - existingBytes) / (1024f * 1024f)) / elapsedSec

                        onProgress(downloaded, totalBytes, speedMbps)
                    }
                }
            }

            // Verify file actually has nonzero binary content
            return@withContext destinationFile.exists() && destinationFile.length() > 0
        } catch (e: Exception) {
            Log.e("HuggingFaceApiClient", "Download stream failed: ${e.message}", e)
            return@withContext false
        }
    }

    fun calculateSha256(file: File): String {
        if (!file.exists()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(32 * 1024)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
