package com.smartops.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartops.agent.common.BusinessException;
import com.smartops.agent.config.AgentProperties;
import com.smartops.agent.dto.AgentDtos.KnowledgeDocumentRequest;
import com.smartops.agent.dto.AgentDtos.RetrievedChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class KnowledgeService {
    private final JdbcTemplate jdbcTemplate;
    private final AgentProperties properties;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public KnowledgeService(JdbcTemplate jdbcTemplate, AgentProperties properties, EmbeddingService embeddingService, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> createDocument(KnowledgeDocumentRequest request) {
        Long kbId = request.knowledgeBaseId() == null ? 1L : request.knowledgeBaseId();
        String type = fileType(request.fileName());
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO knowledge_document
                (knowledge_base_id, title, file_name, file_type, category, parse_status, chunk_count, create_user_id, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, 'PARSING', 0, 1, ?, ?)
                """, kbId, request.title(), request.fileName(), type, request.category(), now, now);
        Long docId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        List<String> chunks = split(request.content());
        int index = 0;
        for (String chunk : chunks) {
            String metadata = metadata(request.fileName(), request.category(), request.title(), index);
            jdbcTemplate.update("""
                    INSERT INTO knowledge_chunk
                    (document_id, knowledge_base_id, chunk_index, content, embedding, metadata, create_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, docId, kbId, index++, chunk, embeddingService.serialize(embeddingService.embed(chunk)), metadata, now);
        }
        jdbcTemplate.update("UPDATE knowledge_document SET parse_status = 'READY', chunk_count = ?, update_time = ? WHERE id = ?",
                chunks.size(), now, docId);
        return Map.of("documentId", Objects.requireNonNull(docId), "chunkCount", chunks.size(), "status", "READY");
    }

    public List<RetrievedChunk> search(String query) {
        return search(query, properties.getRag().getTopK());
    }

    public List<RetrievedChunk> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        double[] queryVector = embeddingService.embed(query);
        List<RetrievedChunk> all = jdbcTemplate.query("""
                SELECT c.id chunk_id, c.document_id, d.title, d.file_name, d.category, c.content, c.embedding
                FROM knowledge_chunk c
                JOIN knowledge_document d ON d.id = c.document_id
                WHERE d.parse_status = 'READY'
                """, (rs, rowNum) -> {
            double vectorScore = embeddingService.cosine(queryVector, embeddingService.deserialize(rs.getString("embedding")));
            double keywordScore = keywordScore(query, rs.getString("content"));
            double score = Math.min(1.0, vectorScore + keywordScore);
            return new RetrievedChunk(
                    rs.getLong("chunk_id"),
                    rs.getLong("document_id"),
                    rs.getString("title"),
                    rs.getString("file_name"),
                    rs.getString("category"),
                    rs.getString("content"),
                    score
            );
        });
        return all.stream()
                .filter(item -> item.score() >= properties.getRag().getSimilarityThreshold())
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    public List<Map<String, Object>> documents() {
        return jdbcTemplate.queryForList("""
                SELECT id, knowledge_base_id knowledgeBaseId, title, file_name fileName, file_type fileType,
                       category, parse_status parseStatus, chunk_count chunkCount, create_time createTime
                FROM knowledge_document
                ORDER BY id DESC
                LIMIT 100
                """);
    }

    private List<String> split(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            throw new BusinessException("知识文档内容不能为空");
        }
        int chunkSize = Math.max(300, properties.getRag().getChunkSize());
        int overlap = Math.max(0, Math.min(properties.getRag().getChunkOverlap(), chunkSize / 2));
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + chunkSize);
            chunks.add(normalized.substring(start, end));
            if (end == normalized.length()) {
                break;
            }
            start = Math.max(0, end - overlap);
        }
        return chunks;
    }

    private double keywordScore(String query, String content) {
        String lower = content == null ? "" : content.toLowerCase();
        double score = 0.0;
        for (String token : query.toLowerCase().split("[\\s,，。！？]+")) {
            if (!token.isBlank() && lower.contains(token)) {
                score += 0.08;
            }
        }
        return Math.min(0.24, score);
    }

    private String fileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "TEXT";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
    }

    private String metadata(String source, String category, String title, int index) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "source", source == null ? title : source,
                    "category", category == null ? "general" : category,
                    "section", title,
                    "chunkIndex", index
            ));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    public boolean hasDocuments() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_document", Integer.class);
        return count != null && count > 0;
    }
}
