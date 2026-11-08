package com.example

import com.example.engine.gguf.GgufReader
import com.example.engine.gguf.GgufTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GgufBinaryTest {

    @Test
    fun testTokenizer_encodeAndDecode() {
        val vocab = listOf("<unk>", "<s>", "</s>", "hello", " world", "local", "ai")
        val tokenizer = GgufTokenizer(vocabulary = vocab, bosTokenId = 1, eosTokenId = 2)

        val encoded = tokenizer.encode("hello world", addBos = true)
        assertTrue("Tokens must include BOS token at start", encoded.isNotEmpty() && encoded[0] == 1)

        val decoded = tokenizer.decode(3) // "hello"
        assertEquals("hello", decoded)
    }

    @Test
    fun testGgufReader_rejectsInvalidFile() {
        val temp = File.createTempFile("fake_model", ".gguf")
        temp.writeText("INVALID_HEADER_NOT_GGUF")
        try {
            val reader = GgufReader(temp)
            val parsed = reader.parse()
            assertFalse("GgufReader must reject non-GGUF binary files", parsed)
        } finally {
            temp.delete()
        }
    }
}
