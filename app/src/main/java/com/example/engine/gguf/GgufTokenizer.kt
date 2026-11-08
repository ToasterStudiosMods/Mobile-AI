package com.example.engine.gguf

class GgufTokenizer(
    private val vocabulary: List<String>,
    private val bosTokenId: Int = 1,
    private val eosTokenId: Int = 2
) {
    private val tokenToId = HashMap<String, Int>(vocabulary.size)

    init {
        vocabulary.forEachIndexed { index, token ->
            tokenToId[token] = index
        }
    }

    fun encode(text: String, addBos: Boolean = true): IntArray {
        val tokens = mutableListOf<Int>()
        if (addBos && bosTokenId >= 0) {
            tokens.add(bosTokenId)
        }

        // Greedy longest-match tokenization with byte-level fallback
        var i = 0
        while (i < text.length) {
            var matched = false
            for (len in Math.min(32, text.length - i) downTo 1) {
                val sub = text.substring(i, i + len)
                val id = tokenToId[sub] ?: tokenToId[" $sub"] ?: tokenToId["\u2581$sub"]
                if (id != null) {
                    tokens.add(id)
                    i += len
                    matched = true
                    break
                }
            }
            if (!matched) {
                // Byte fallback
                val ch = text[i]
                val hexByte = "<0x%02X>".format(ch.code.toByte())
                val byteId = tokenToId[hexByte] ?: tokenToId[ch.toString()] ?: 0
                tokens.add(byteId)
                i++
            }
        }
        return tokens.toIntArray()
    }

    fun decode(tokenId: Int): String {
        if (tokenId == eosTokenId || tokenId == bosTokenId) return ""
        if (tokenId < 0 || tokenId >= vocabulary.size) return ""
        val raw = vocabulary[tokenId]
        return raw.replace("\u2581", " ").replace(" ", " ")
    }

    fun decode(tokenIds: IntArray): String {
        val sb = StringBuilder()
        for (id in tokenIds) {
            sb.append(decode(id))
        }
        return sb.toString()
    }

    companion object {
        fun fromGguf(reader: GgufReader): GgufTokenizer {
            val rawTokens = reader.metadata["tokenizer.ggml.tokens"] as? List<*>
            val vocab = mutableListOf<String>()
            if (rawTokens != null) {
                for (item in rawTokens) {
                    if (item is String) vocab.add(item)
                }
            }
            if (vocab.isEmpty()) {
                // Default fallback minimal byte vocabulary
                for (i in 0..255) {
                    vocab.add(i.toChar().toString())
                }
            }
            val bos = (reader.metadata["tokenizer.ggml.bos_token_id"] as? Long)?.toInt() ?: 1
            val eos = (reader.metadata["tokenizer.ggml.eos_token_id"] as? Long)?.toInt() ?: 2
            return GgufTokenizer(vocab, bos, eos)
        }
    }
}
