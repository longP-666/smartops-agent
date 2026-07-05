package com.smartops.agent.service;

import com.smartops.agent.dto.AgentDtos.DashboardSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {
    private final JdbcTemplate jdbcTemplate;
    private final TraceService traceService;

    public DashboardService(JdbcTemplate jdbcTemplate, TraceService traceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.traceService = traceService;
    }

    public DashboardSummary summary() {
        Map<String, Long> status = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT status, COUNT(*) count FROM ticket GROUP BY status", rs -> {
            status.put(rs.getString("status"), rs.getLong("count"));
        });
        return new DashboardSummary(
                count("agent_conversation"),
                count("ticket"),
                count("knowledge_document"),
                count("agent_trace"),
                countWhere("tool_call_log", "tool_name = 'createTicket'"),
                status,
                traceService.traces(6)
        );
    }

    private long count(String table) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return value == null ? 0 : value;
    }

    private long countWhere(String table, String where) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
        return value == null ? 0 : value;
    }
}
