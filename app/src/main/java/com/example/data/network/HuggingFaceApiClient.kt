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
    val id: String, // e.g. "bartowski/Llama-3.2-1B-Instruct-GGUF"
    val name: String,
    val author: String,
    val downloads: Int,
    val likes: Int,
    val tags: List<String>,
    val pipelineTag: String, // e.g. "text-generation", "text-to-image"
    val defaultFilename: String,
    val estimatedSizeBytes: Long
)

data class DownloadResult(
    val success: Boolean,
    val resolvedRepoId: String,
    val resolvedFilename: String,
    val downloadedFile: File,
    val errorMessage: String? = null
)

class HuggingFaceApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        // Known gated official models automatically mapped to high-quality, verified ungated community quantizations
        val GATED_REPO_FALLBACKS = mapOf(
            "meta-llama/Llama-3.2-1B-Instruct-GGUF" to ("bartowski/Llama-3.2-1B-Instruct-GGUF" to "Llama-3.2-1B-Instruct-Q4_K_M.gguf"),
            "meta-llama/Llama-3.2-3B-Instruct-GGUF" to ("bartowski/Llama-3.2-3B-Instruct-GGUF" to "Llama-3.2-3B-Instruct-Q4_K_M.gguf"),
            "meta-llama/Meta-Llama-3.1-8B-Instruct-GGUF" to ("bartowski/Meta-Llama-3.1-8B-Instruct-GGUF" to "Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf"),
            "google/gemma-2-2b-it-GGUF" to ("bartowski/gemma-2-2b-it-GGUF" to "gemma-2-2b-it-Q4_K_M.gguf"),
            "google/gemma-2-9b-it-GGUF" to ("bartowski/gemma-2-9b-it-GGUF" to "gemma-2-9b-it-Q4_K_M.gguf"),
            "segmind/tiny-sd-mobile-onnx" to ("IlyasMoutawwakil/tiny-stable-diffusion-onnx" to "unet/model.onnx"),
            "stabilityai/sd-turbo-onnx" to ("softwareweaver/Sdxl-Turbo-Olive-Onnx" to "unet/model.onnx")
        )
    }

    suspend fun searchModels(query: String = "gguf", filter: String = ""): List<HuggingFaceModelDto> = withContext(Dispatchers.IO) {
        val list = mutableListOf<HuggingFaceModelDto>()
        try {
            val filterParam = if (filter.isNotBlank()) "&filter=$filter" else ""
            val url = "https://huggingface.co/api/models?search=${java.net.URLEncoder.encode(query, "UTF-8")}&sort=downloads&direction=-1&limit=25$filterParam"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Edgeloom-Android/4.0 (Local GGUF Orchestrator)")
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
                        id.contains("0.5B", ignoreCase = true) || id.contains("360M", ignoreCase = true) -> 398_000_000L
                        id.contains("1B", ignoreCase = true) || id.contains("1.5B", ignoreCase = true) || id.contains("1.7B", ignoreCase = true) -> 980_000_000L
                        id.contains("2B", ignoreCase = true) -> 1_650_000_000L
                        id.contains("3B", ignoreCase = true) -> 2_200_000_000L
                        else -> 1_200_000_000L
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

    suspend fun fetchRepoFileTree(repoId: String, subfolder: String = ""): List<Pair<String, Long>> = withContext(Dispatchers.IO) {
        val files = mutableListOf<Pair<String, Long>>()
        try {
            val pathSuffix = if (subfolder.isNotBlank()) "/$subfolder" else ""
            val url = "https://huggingface.co/api/models/$repoId/tree/main$pathSuffix"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Edgeloom-Android/4.0 (GGUF Inspector)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: "[]"
                val jsonArr = JSONArray(jsonStr)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val type = obj.optString("type")
                    val path = obj.optString("path")
                    val size = obj.optLong("size", 0L)
                    if (type == "file" && path.isNotBlank()) {
                        files.add(Pair(path, size))
                    } else if (type == "directory" && subfolder.isEmpty() && path == "unet") {
                        // Recursively fetch subfolder if unet
                        val subFiles = fetchRepoFileTree(repoId, "unet")
                        files.addAll(subFiles)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("HuggingFaceApiClient", "Failed to fetch file tree for $repoId: ${e.message}")
        }
        return@withContext files
    }

    suspend fun resolveBestFileForRepo(repoId: String, preferredFilename: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val files = fetchRepoFileTree(repoId)
        if (files.isEmpty()) return@withContext null

        // 1. Exact match
        val exact = files.find { it.first.equals(preferredFilename, ignoreCase = true) }
        if (exact != null) return@withContext Pair(repoId, exact.first)

        // 2. Substring match
        val cleanPreferred = preferredFilename.substringAfterLast("/")
        val substringMatch = files.find { it.first.contains(cleanPreferred, ignoreCase = true) }
        if (substringMatch != null) return@withContext Pair(repoId, substringMatch.first)

        // 3. Preferred quantization: Q4_K_M or q4_k_m
        val q4km = files.find { it.first.endsWith(".gguf", ignoreCase = true) && it.first.contains("q4_k_m", ignoreCase = true) }
        if (q4km != null) return@withContext Pair(repoId, q4km.first)

        // 4. Any Q4 quantization: Q4_0 or q4_0
        val q40 = files.find { it.first.endsWith(".gguf", ignoreCase = true) && it.first.contains("q4", ignoreCase = true) }
        if (q40 != null) return@withContext Pair(repoId, q40.first)

        // 5. Any .gguf file
        val anyGguf = files.find { it.first.endsWith(".gguf", ignoreCase = true) }
        if (anyGguf != null) return@withContext Pair(repoId, anyGguf.first)

        // 6. ONNX model
        val anyOnnx = files.find { it.first.endsWith(".onnx", ignoreCase = true) }
        if (anyOnnx != null) return@withContext Pair(repoId, anyOnnx.first)

        return@withContext null
    }

    suspend fun downloadRealFile(
        repoId: String,
        filename: String,
        destinationFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long, speedMbps: Float) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        // Step 1: Check if the repository has a known ungated public mirror
        var effectiveRepo = repoId
        var effectiveFile = filename

        val gatedMatch = GATED_REPO_FALLBACKS[repoId]
        if (gatedMatch != null) {
            effectiveRepo = gatedMatch.first
            effectiveFile = gatedMatch.second
            Log.i("HuggingFaceApiClient", "Mapped gated repo $repoId to ungated mirror $effectiveRepo ($effectiveFile)")
        } else if (repoId.startsWith("meta-llama/", ignoreCase = true)) {
            val mirror = repoId.replace("meta-llama/", "bartowski/")
            effectiveRepo = mirror
        } else if (repoId.startsWith("google/", ignoreCase = true) && repoId.contains("gguf", ignoreCase = true)) {
            val mirror = repoId.replace("google/", "bartowski/")
            effectiveRepo = mirror
        }

        // Helper to attempt download of a specific URL with range support
        suspend fun attemptStream(repo: String, fileStr: String, allowTruncateOnFail: Boolean): Boolean {
            val downloadUrl = "https://huggingface.co/$repo/resolve/main/$fileStr"
            var existingBytes = if (destinationFile.exists()) destinationFile.length() else 0L

            val requestBuilder = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Edgeloom-Android/4.0 (Resumable GGUF Downloader)")

            if (existingBytes > 0) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            try {
                destinationFile.parentFile?.mkdirs()
                val response = client.newCall(requestBuilder.build()).execute()

                // If Range Not Satisfiable (416) or Bad Request on Range (400), reset file and retry from 0
                if ((response.code == 416 || response.code == 400) && allowTruncateOnFail && existingBytes > 0) {
                    Log.w("HuggingFaceApiClient", "Range $existingBytes not satisfied (${response.code}). Resetting file.")
                    destinationFile.delete()
                    return attemptStream(repo, fileStr, false)
                }

                if (response.code == 416) {
                    onProgress(destinationFile.length(), destinationFile.length(), 0f)
                    return true
                }

                if (!response.isSuccessful) {
                    Log.w("HuggingFaceApiClient", "HTTP ${response.code} downloading $downloadUrl")
                    return false
                }

                val body = response.body ?: return false
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
                                return false
                            }
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead

                            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.1f)
                            val speedMbps = ((downloaded - existingBytes) / (1024f * 1024f)) / elapsedSec

                            onProgress(downloaded, totalBytes, speedMbps)
                        }
                    }
                }

                return destinationFile.exists() && destinationFile.length() > 0
            } catch (e: Exception) {
                Log.e("HuggingFaceApiClient", "Error downloading $downloadUrl: ${e.message}", e)
                return false
            }
        }

        // Try download with initial parameters
        var success = attemptStream(effectiveRepo, effectiveFile, true)

        // If direct file download failed, inspect the repo tree to resolve the actual path
        if (!success) {
            Log.i("HuggingFaceApiClient", "Direct download failed for $effectiveRepo/$effectiveFile. Resolving tree...")
            val resolved = resolveBestFileForRepo(effectiveRepo, effectiveFile)
            if (resolved != null) {
                effectiveRepo = resolved.first
                effectiveFile = resolved.second
                Log.i("HuggingFaceApiClient", "Discovered real file: $effectiveRepo / $effectiveFile")
                success = attemptStream(effectiveRepo, effectiveFile, true)
            } else if (effectiveRepo != repoId) {
                // Also check original repo if different
                val origResolved = resolveBestFileForRepo(repoId, filename)
                if (origResolved != null) {
                    effectiveRepo = origResolved.first
                    effectiveFile = origResolved.second
                    success = attemptStream(effectiveRepo, effectiveFile, true)
                }
            }
        }

        return@withContext DownloadResult(
            success = success && destinationFile.exists() && destinationFile.length() > 0,
            resolvedRepoId = effectiveRepo,
            resolvedFilename = effectiveFile.substringAfterLast("/"),
            downloadedFile = destinationFile,
            errorMessage = if (!success) "Unable to download file from Hugging Face. Check network connection." else null
        )
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
