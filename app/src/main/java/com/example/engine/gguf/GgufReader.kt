package com.example.engine.gguf

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class GgufHeader(
    val magic: String,
    val version: Int,
    val tensorCount: Long,
    val metadataKvCount: Long
)

data class GgufTensorInfo(
    val name: String,
    val dimensions: LongArray,
    val type: Int,
    val offset: Long
)

class GgufReader(val file: File) {

    var header: GgufHeader? = null
        private set
    val metadata = mutableMapOf<String, Any>()
    val tensors = mutableListOf<GgufTensorInfo>()
    var dataOffset: Long = 0
        private set

    private fun logError(tag: String, msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.e(tag, msg, tr)
        } catch (_: Throwable) {
            System.err.println("[$tag] $msg")
        }
    }

    fun parse(): Boolean {
        if (!file.exists() || file.length() < 16) {
            logError("GgufReader", "File ${file.name} does not exist or is too small.")
            return false
        }

        try {
            RandomAccessFile(file, "r").use { raf ->
                val channel = raf.channel
                val buffer = ByteBuffer.allocate(32 * 1024).order(ByteOrder.LITTLE_ENDIAN)
                channel.read(buffer)
                buffer.flip()

                // 1. Magic bytes: 0x46554747 (GGUF)
                val magicBytes = ByteArray(4)
                buffer.get(magicBytes)
                val magic = String(magicBytes)
                if (magic != "GGUF") {
                    logError("GgufReader", "Invalid GGUF magic bytes: $magic")
                    return false
                }

                val version = buffer.int
                val tensorCount = buffer.long
                val metadataKvCount = buffer.long

                header = GgufHeader(magic, version, tensorCount, metadataKvCount)

                // 2. Read metadata entries
                for (i in 0 until metadataKvCount) {
                    if (buffer.remaining() < 64) {
                        refillBuffer(channel, buffer)
                    }
                    val key = readString(channel, buffer)
                    val valueType = buffer.int
                    val value = readValue(channel, buffer, valueType)
                    if (value != null) {
                        metadata[key] = value
                    }
                }

                // 3. Read tensor info table
                for (i in 0 until tensorCount) {
                    if (buffer.remaining() < 64) {
                        refillBuffer(channel, buffer)
                    }
                    val name = readString(channel, buffer)
                    val nDimensions = buffer.int
                    val dims = LongArray(nDimensions)
                    for (d in 0 until nDimensions) {
                        dims[d] = buffer.long
                    }
                    val type = buffer.int
                    val offset = buffer.long
                    tensors.add(GgufTensorInfo(name, dims, type, offset))
                }

                // 4. Align to 32 bytes for tensor binary payload
                val currentPos = channel.position() - buffer.remaining()
                val alignment = (metadata["general.alignment"] as? Long) ?: 32L
                dataOffset = ((currentPos + alignment - 1) / alignment) * alignment
                return true
            }
        } catch (e: Exception) {
            logError("GgufReader", "GGUF parse failed: ${e.message}", e)
            return false
        }
    }

    private fun refillBuffer(channel: FileChannel, buffer: ByteBuffer) {
        val remaining = buffer.remaining()
        val temp = ByteArray(remaining)
        buffer.get(temp)
        buffer.clear()
        buffer.put(temp)
        channel.read(buffer)
        buffer.flip()
    }

    private fun readString(channel: FileChannel, buffer: ByteBuffer): String {
        if (buffer.remaining() < 8) refillBuffer(channel, buffer)
        val length = buffer.long.toInt()
        if (length <= 0) return ""
        val bytes = ByteArray(length)
        if (buffer.remaining() < length) {
            refillBuffer(channel, buffer)
        }
        val readFromBuffer = Math.min(buffer.remaining(), length)
        buffer.get(bytes, 0, readFromBuffer)
        if (readFromBuffer < length) {
            val rafBuffer = ByteBuffer.wrap(bytes, readFromBuffer, length - readFromBuffer)
            channel.read(rafBuffer)
        }
        return String(bytes, Charsets.UTF_8)
    }

    private fun readValue(channel: FileChannel, buffer: ByteBuffer, valueType: Int): Any? {
        if (buffer.remaining() < 8) refillBuffer(channel, buffer)
        return when (valueType) {
            0 -> buffer.get().toInt() and 0xFF // UINT8
            1 -> buffer.get() // INT8
            2 -> buffer.short.toInt() and 0xFFFF // UINT16
            3 -> buffer.short // INT16
            4 -> buffer.int.toLong() and 0xFFFFFFFFL // UINT32
            5 -> buffer.int // INT32
            6 -> buffer.float // FLOAT32
            7 -> buffer.get() != 0.toByte() // BOOL
            8 -> readString(channel, buffer) // STRING
            9 -> {
                // ARRAY: type (int), count (uint64)
                val itemType = buffer.int
                val count = buffer.long.toInt()
                val list = ArrayList<Any?>(count.coerceAtMost(1000))
                for (i in 0 until count) {
                    if (buffer.remaining() < 8) refillBuffer(channel, buffer)
                    val item = readValue(channel, buffer, itemType)
                    if (list.size < 1000) {
                        list.add(item)
                    }
                }
                list
            }
            10 -> buffer.long // UINT64
            11 -> buffer.long // INT64
            12 -> buffer.double // FLOAT64
            else -> null
        }
    }

    fun getArchitecture(): String {
        return (metadata["general.architecture"] as? String) ?: "llama"
    }

    fun getContextLength(): Int {
        val arch = getArchitecture()
        return ((metadata["$arch.context_length"] as? Long) ?: 4096L).toInt()
    }

    fun getEmbeddingLength(): Int {
        val arch = getArchitecture()
        return ((metadata["$arch.embedding_length"] as? Long) ?: 2048L).toInt()
    }
}
