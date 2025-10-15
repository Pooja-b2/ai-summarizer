package com.pooja.util;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    private static final int CHUNK_SIZE = 1000; // characters

    public static List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();

        for (int start = 0; start < length; start += CHUNK_SIZE) {
            int end = Math.min(length, start + CHUNK_SIZE);
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }
}