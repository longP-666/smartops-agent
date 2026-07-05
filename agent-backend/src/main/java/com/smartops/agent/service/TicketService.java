package com.smartops.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartops.agent.common.BusinessException;
import com.smartops.agent.dto.AgentDtos.CategoryDto;
import com.smartops.agent.dto.AgentDtos.ReplyRequest;
import com.smartops.agent.dto.AgentDtos.TicketDto;
import com.smartops.agent.dto.AgentDtos.TicketRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public TicketService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TicketDto createTicket(TicketRequest request, String traceId) {
        long start = System.currentTimeMillis();
        try {
            CategoryDto category = categories().stream()
                    .filter(item -> item.code().equals(request.categoryCode()))
                    .findFirst()
                    .orElse(new CategoryDto(request.categoryCode(), request.categoryCode(), ""));
            String ticketNo = nextTicketNo();
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update("""
                    INSERT INTO ticket
                    (ticket_no, title, description, category_code, category_name, priority, status, source, assignee_name, creator_id, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, 'PENDING_PROCESS', ?, '客服A', ?, ?, ?)
                    """, ticketNo, request.title(), request.description(), request.categoryCode(), category.name(),
                    request.priority(), request.source() == null ? "AGENT" : request.source(), request.creatorId(), now, now);
            TicketDto dto = queryTicketByNo(ticketNo, traceId);
            logTool(traceId, "createTicket", request, dto, "SUCCESS", start, null);
            return dto;
        } catch (RuntimeException ex) {
            logTool(traceId, "createTicket", request, null, "FAILED", start, ex.getMessage());
            throw ex;
        }
    }

    public TicketDto queryTicketByNo(String ticketNo, String traceId) {
        long start = System.currentTimeMillis();
        try {
            List<TicketDto> rows = jdbcTemplate.query("SELECT * FROM ticket WHERE ticket_no = ?", this::mapTicket, ticketNo);
            if (rows.isEmpty()) {
                throw new BusinessException("未找到工单: " + ticketNo);
            }
            TicketDto dto = rows.get(0);
            logTool(traceId, "queryTicketByNo", Map.of("ticketNo", ticketNo), dto, "SUCCESS", start, null);
            return dto;
        } catch (RuntimeException ex) {
            logTool(traceId, "queryTicketByNo", Map.of("ticketNo", ticketNo), null, "FAILED", start, ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public Map<String, Object> replyTicket(String ticketNo, ReplyRequest request, String traceId) {
        long start = System.currentTimeMillis();
        try {
            queryTicketByNo(ticketNo, traceId);
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update("INSERT INTO ticket_reply (ticket_no, user_id, content, create_time) VALUES (?, ?, ?, ?)",
                    ticketNo, request.userId(), request.content(), now);
            jdbcTemplate.update("UPDATE ticket SET status = 'PROCESSING', update_time = ? WHERE ticket_no = ?", now, ticketNo);
            Map<String, Object> result = Map.of("ticketNo", ticketNo, "status", "PROCESSING", "replyTime", now.toString());
            logTool(traceId, "replyTicket", request, result, "SUCCESS", start, null);
            return result;
        } catch (RuntimeException ex) {
            logTool(traceId, "replyTicket", request, null, "FAILED", start, ex.getMessage());
            throw ex;
        }
    }

    public List<TicketDto> listTickets(Long creatorId) {
        if (creatorId == null) {
            return jdbcTemplate.query("SELECT * FROM ticket ORDER BY id DESC LIMIT 100", this::mapTicket);
        }
        return jdbcTemplate.query("SELECT * FROM ticket WHERE creator_id = ? ORDER BY id DESC LIMIT 100", this::mapTicket, creatorId);
    }

    public List<CategoryDto> categories() {
        return jdbcTemplate.query("SELECT code, name, description FROM ticket_category ORDER BY id", (rs, rowNum) ->
                new CategoryDto(rs.getString("code"), rs.getString("name"), rs.getString("description")));
    }

    public List<Map<String, Object>> replies(String ticketNo) {
        return jdbcTemplate.queryForList("SELECT user_id userId, content, create_time createTime FROM ticket_reply WHERE ticket_no = ? ORDER BY id", ticketNo);
    }

    private TicketDto mapTicket(ResultSet rs, int rowNum) throws SQLException {
        return new TicketDto(
                rs.getLong("id"),
                rs.getString("ticket_no"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getInt("priority"),
                rs.getString("status"),
                rs.getString("assignee_name"),
                rs.getLong("creator_id"),
                rs.getTimestamp("create_time").toLocalDateTime(),
                rs.getTimestamp("update_time").toLocalDateTime()
        );
    }

    private String nextTicketNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket WHERE ticket_no LIKE ?", Integer.class, "TK" + date + "%");
        return "TK" + date + String.format("%04d", (count == null ? 0 : count) + 1);
    }

    private void logTool(String traceId, String tool, Object request, Object response, String status, long start, String error) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO tool_call_log (trace_id, tool_name, request_body, response_body, status, cost_ms, error_message, create_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, traceId, tool, objectMapper.writeValueAsString(request),
                    response == null ? null : objectMapper.writeValueAsString(response),
                    status, System.currentTimeMillis() - start, error, LocalDateTime.now());
        } catch (JsonProcessingException ignored) {
            // Tool logs should never break the user path.
        }
    }
}
