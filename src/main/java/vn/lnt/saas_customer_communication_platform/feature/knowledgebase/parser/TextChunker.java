package vn.lnt.saas_customer_communication_platform.feature.knowledgebase.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_CHUNK_OVERLAP = 150;

    public List<String> splitIntoChunks(String text) {
        return splitIntoChunks(text, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    public List<String> splitIntoChunks(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String cleaned = text.trim();
        if (cleaned.length() <= chunkSize) {
            chunks.add(cleaned);
            return chunks;
        }

        int start = 0;
        int textLength = cleaned.length();

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);

            if (end < textLength) {
                int naturalBreak = findNaturalBreak(cleaned, start, end);
                if (naturalBreak > start + (chunkSize / 2)) {
                    end = naturalBreak;
                }
            }

            String chunk = cleaned.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= textLength) {
                break;
            }

            start = Math.max(end - chunkOverlap, start + 1);
        }

        return chunks;
    }

    private int findNaturalBreak(String text, int start, int end) {
        int doubleNewline = text.lastIndexOf("\n\n", end);
        if (doubleNewline > start) {
            return doubleNewline + 2;
        }

        for (int i = end - 1; i >= start + (end - start) / 2; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && (i + 1 == text.length() || Character.isWhitespace(text.charAt(i + 1)))) {
                return i + 1;
            }
        }

        int newline = text.lastIndexOf('\n', end);
        if (newline > start) {
            return newline + 1;
        }

        int space = text.lastIndexOf(' ', end);
        if (space > start) {
            return space + 1;
        }

        return end;
    }
}

