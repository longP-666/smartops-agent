package com.smartops.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

@Service
public class EmbeddingService {
    private static final int DIMENSIONS = 96;
    private final ObjectMapper objectMapper;

    public EmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public double[] embed(String text) {
        double[] vector = new double[DIMENSIONS];
        for (String token : tokenize(text)) {
            int bucket = Math.floorMod(hash(token), DIMENSIONS);
            vector[bucket] += 1.0;
        }
        normalize(vector);
        return vector;
    }

    public String serialize(double[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("向量序列化失败", ex);
        }
    }

    public double[] deserialize(String value) {
        try {
            return objectMapper.readValue(value, double[].class);
        } catch (JsonProcessingException ex) {
            return new double[DIMENSIONS];
        }
    }

    public double cosine(double[] left, double[] right) {
        double sum = 0.0;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            sum += left[i] * right[i];
        }
        return sum;
    }

    private List<String> tokenize(String text) {
        String normalized = text == null ? "" : text.toLowerCase()
                .replaceAll("[^\\p{IsHan}a-z0-9]+", " ")
                .trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> words = Arrays.stream(normalized.split("\\s+"))
                .filter(word -> !word.isBlank())
                .toList();
        if (!words.isEmpty()) {
            return words;
        }
        return normalized.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .toList();
    }

    private int hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return ((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16) | ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff);
        } catch (NoSuchAlgorithmException ex) {
            return token.hashCode();
        }
    }

    private void normalize(double[] vector) {
        double norm = 0.0;
        for (double item : vector) {
            norm += item * item;
        }
        if (norm == 0) {
            return;
        }
        double sqrt = Math.sqrt(norm);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / sqrt;
        }
    }
}
