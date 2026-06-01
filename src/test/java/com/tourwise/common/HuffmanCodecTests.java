package com.tourwise.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HuffmanCodecTests {

    @Test
    void compressAndDecompressChineseTravelLog() {
        String text = "从北京邮电大学西土城校区出发，沿着校园主路经过图书馆、主楼和体育馆，整体路线清晰。";

        HuffmanCodec.CompressedText compressed = HuffmanCodec.compress(text);
        String decoded = HuffmanCodec.decompress(compressed.getPayload());

        assertNotNull(compressed.getPayload());
        assertEquals(text, decoded);
        assertEquals(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, compressed.getOriginalSize());
    }

    @Test
    void compressAndDecompressEmptyText() {
        HuffmanCodec.CompressedText compressed = HuffmanCodec.compress("");

        assertEquals("", HuffmanCodec.decompress(compressed.getPayload()));
        assertEquals(0, compressed.getOriginalSize());
    }
}
