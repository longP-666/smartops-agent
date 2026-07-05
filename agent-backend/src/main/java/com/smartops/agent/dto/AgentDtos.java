package com.smartops.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AgentDtos {
    private AgentDtos() {
    }

    public record ChatRequest(
            String conversationId,
            @NotNull Long userId,
            @NotBlank String message,
            Boolean confirmCreateTicket
    ) {
    }

    public record ChatResponse(
            String conversationId,
            String traceId,
            String intent,
            String answer,
            boolean needConfirm,
            TicketDraft ticketDraft,
            List<RetrievedChunk> references,
            List<StepDto> steps
    ) {
    }

    public record TicketDraft(
            String title,
            String description,
            String categoryCode,
            Integer priority,
            List<String> missingFields
    ) {
    }

    public record KnowledgeDocumentRequest(
            Long knowledgeBaseId,
            @NotBlank String title,
            String fileName,
            String category,
            @NotBlank String content
    ) {
    }

    public record RetrievedChunk(
            Long chunkId,
            Long documentId,
            String title,
            String source,
            String category,
            String content,
            double score
    ) {
    }

    public record TicketRequest(
            @NotBlank String title,
            @NotBlank String description,
            @NotBlank String categoryCode,
            @NotNull Integer priority,
            @NotNull Long creatorId,
            String source
    ) {
    }

    public record ReplyRequest(
            @NotNull Long userId,
            @NotBlank String content
    ) {
    }

    public record TicketDto(
            Long id,
            String ticketNo,
            String title,
            String description,
            String categoryCode,
            String categoryName,
            Integer priority,
            String status,
            String assigneeName,
            Long creatorId,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
    }

    public record MessageDto(String role, String content, LocalDateTime createTime) {
    }

    public record TraceDto(
            String traceId,
            String conversationId,
            Long userId,
            String question,
            String finalAnswer,
            String status,
            Long totalCostMs,
            LocalDateTime createTime
    ) {
    }

    public record StepDto(String name, String type, String status, Long costMs, String input, String output) {
    }

    public record CategoryDto(String code, String name, String description) {
    }

    public record DashboardSummary(
            long conversations,
            long tickets,
            long documents,
            long traces,
            long handoffs,
            Map<String, Long> ticketStatus,
            List<TraceDto> recentTraces
    ) {
    }
}
