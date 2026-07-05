package com.smartops.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartops.agent.dto.AgentDtos.StepDto;
import com.smartops.agent.dto.AgentDtos.TraceDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TraceService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public TraceService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public String startTrace(String conversationId, Long userId, String question) {
        String traceId = "TR" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO agent_trace
                (trace_id, conversation_id, user_id, question, status, start_time, create_time)
                VALUES (?, ?, ?, ?, 'RUNNING', ?, ?)
                """, traceId, conversationId, userId, question, now, now);
        return traceId;
    }

    public void addStep(String traceId, String name, String type, Object input, Object output, String status, long costMs) {
        jdbcTemplate.update("""
                INSERT INTO agent_step_log (trace_id, step_name, step_type, input, output, status, cost_ms, create_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, traceId, name, type, json(input), json(output), status, costMs, LocalDateTime.now());
    }

    public void finishTrace(String traceId, String answer, String status) {
        jdbcTemplate.update("""
                UPDATE agent_trace
                SET final_answer = ?, status = ?, end_time = ?, total_cost_ms = TIMESTAMPDIFF(MICROSECOND, start_time, ?)/1000
                WHERE trace_id = ?
                """, answer, status, LocalDateTime.now(), LocalDateTime.now(), traceId);
    }

    public List<TraceDto> traces(int limit) {
        return jdbcTemplate.query("""
                SELECT trace_id, conversation_id, user_id, question, final_answer, status, total_cost_ms, create_time
                FROM agent_trace
                ORDER BY id DESC
                LIMIT ?
                """, this::mapTrace, limit);
    }

    public TraceDto trace(String traceId) {
        return jdbcTemplate.query("SELECT trace_id, conversation_id, user_id, question, final_answer, status, total_cost_ms, create_time FROM agent_trace WHERE trace_id = ?",
                this::mapTrace, traceId).stream().findFirst().orElse(null);
    }

    public List<StepDto> steps(String traceId) {
        return jdbcTemplate.query("""
                SELECT step_name, step_type, status, cost_ms, input, output
                FROM agent_step_log
                WHERE trace_id = ?
                ORDER BY id
                """, (rs, rowNum) -> new StepDto(
                rs.getString("step_name"),
                rs.getString("step_type"),
                rs.getString("status"),
                rs.getLong("cost_ms"),
                rs.getString("input"),
                rs.getString("output")
        ), traceId);
    }

    private TraceDto mapTrace(ResultSet rs, int rowNum) throws SQLException {
        return new TraceDto(
                rs.getString("trace_id"),
                rs.getString("conversation_id"),
                rs.getLong("user_id"),
                rs.getString("question"),
                rs.getString("final_answer"),
                rs.getString("status"),
                rs.getObject("total_cost_ms") == null ? null : rs.getLong("total_cost_ms"),
                rs.getTimestamp("create_time").toLocalDateTime()
        );
    }

    private String json(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}
