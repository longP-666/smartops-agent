package com.smartops.agent.service;

import com.smartops.agent.dto.AgentDtos.ChatRequest;
import com.smartops.agent.dto.AgentDtos.ChatResponse;
import com.smartops.agent.dto.AgentDtos.MessageDto;
import com.smartops.agent.dto.AgentDtos.RetrievedChunk;
import com.smartops.agent.dto.AgentDtos.StepDto;
import com.smartops.agent.dto.AgentDtos.TicketDraft;
import com.smartops.agent.dto.AgentDtos.TicketRequest;
import com.smartops.agent.dto.AgentDtos.TicketDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentService {
    private static final String SYSTEM_PROMPT = """
            你是企业工单系统的智能客服 Agent。你必须优先基于知识库回答；知识不足时不要编造；
            创建工单前必须获得用户明确确认；涉及退款、账号安全、数据删除等敏感操作只能创建工单或转人工。
            回答要清晰、简洁、可执行，并在必要时说明下一步动作。
            """;

    private final JdbcTemplate jdbcTemplate;
    private final KnowledgeService knowledgeService;
    private final TicketService ticketService;
    private final IntentService intentService;
    private final GuardrailService guardrailService;
    private final TraceService traceService;
    private final LlmClient llmClient;
    private final Map<String, TicketDraft> pendingDrafts = new ConcurrentHashMap<>();

    public AgentService(JdbcTemplate jdbcTemplate, KnowledgeService knowledgeService, TicketService ticketService,
                        IntentService intentService, GuardrailService guardrailService, TraceService traceService, LlmClient llmClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.knowledgeService = knowledgeService;
        this.ticketService = ticketService;
        this.intentService = intentService;
        this.guardrailService = guardrailService;
        this.traceService = traceService;
        this.llmClient = llmClient;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        String conversationId = ensureConversation(request.conversationId(), request.userId(), request.message());
        saveMessage(conversationId, "USER", request.message());
        String traceId = traceService.startTrace(conversationId, request.userId(), request.message());
        List<RetrievedChunk> references = List.of();
        String intent = "FAQ_QUERY";
        String answer;
        boolean needConfirm = false;
        TicketDraft draft = null;
        try {
            long start = System.currentTimeMillis();
            Map<String, Object> intentResult = intentService.detect(request.message());
            intent = String.valueOf(intentResult.get("intent"));
            traceService.addStep(traceId, "intent_detection", "RULE", request.message(), intentResult, "SUCCESS", System.currentTimeMillis() - start);

            if (Boolean.TRUE.equals(request.confirmCreateTicket()) || isConfirm(request.message())) {
                TicketDraft pending = pendingDrafts.get(conversationId);
                if (pending != null) {
                    TicketDto ticket = ticketService.createTicket(new TicketRequest(
                            pending.title(), pending.description(), pending.categoryCode(), pending.priority(), request.userId(), "AGENT"), traceId);
                    answer = "工单已创建，编号 " + ticket.ticketNo() + "，当前状态为待处理，处理人是 " + ticket.assigneeName() + "。我已把本次对话和问题描述写入工单，后续客服会继续跟进。";
                    pendingDrafts.remove(conversationId);
                    traceService.addStep(traceId, "tool_call", "createTicket", pending, ticket, "SUCCESS", 0);
                    return finish(conversationId, traceId, intent, answer, false, null, references);
                }
            }

            if ("QUERY_TICKET".equals(intent)) {
                String ticketNo = String.valueOf(intentResult.get("ticketNo"));
                TicketDto ticket = ticketService.queryTicketByNo(ticketNo, traceId);
                answer = "工单 " + ticket.ticketNo() + " 当前状态为 " + statusName(ticket.status()) + "，分类是 " + ticket.categoryName()
                        + "，处理人 " + ticket.assigneeName() + "。最近更新时间：" + ticket.updateTime() + "。";
                traceService.addStep(traceId, "tool_call", "queryTicketByNo", ticketNo, ticket, "SUCCESS", 0);
                return finish(conversationId, traceId, intent, answer, false, null, references);
            }

            if ("SUPPLEMENT_TICKET".equals(intent)) {
                String ticketNo = String.valueOf(intentResult.get("ticketNo"));
                if (ticketNo.isBlank()) {
                    answer = "请把需要补充的工单编号发给我，例如 TK202607050001，我会把这条信息追加到对应工单。";
                    return finish(conversationId, traceId, intent, answer, false, null, references);
                }
                Map<String, Object> result = ticketService.replyTicket(ticketNo, new com.smartops.agent.dto.AgentDtos.ReplyRequest(request.userId(), request.message()), traceId);
                answer = "已补充到工单 " + result.get("ticketNo") + "，当前状态已更新为处理中。";
                traceService.addStep(traceId, "tool_call", "replyTicket", request.message(), result, "SUCCESS", 0);
                return finish(conversationId, traceId, intent, answer, false, null, references);
            }

            start = System.currentTimeMillis();
            references = knowledgeService.search(request.message());
            traceService.addStep(traceId, "knowledge_retrieval", "RAG", request.message(), references, "SUCCESS", System.currentTimeMillis() - start);

            Map<String, Object> handoff = guardrailService.handoffDecision(request.message(), references);
            traceService.addStep(traceId, "handoff_decision", "GUARDRAIL", request.message(), handoff, "SUCCESS", 0);

            if ("HUMAN_SERVICE".equals(intent) || "CREATE_TICKET".equals(intent) || Boolean.TRUE.equals(handoff.get("needHandoff"))) {
                draft = buildDraft(request.message(), handoff, references);
                pendingDrafts.put(conversationId, draft);
                answer = "这个问题建议进入人工工单处理。已为你整理好工单草稿："
                        + draft.title() + "，分类 " + draft.categoryCode() + "，优先级 P" + draft.priority()
                        + "。确认创建吗？";
                needConfirm = true;
                return finish(conversationId, traceId, intent, answer, true, draft, references);
            }

            start = System.currentTimeMillis();
            answer = generateAnswer(request.message(), references, recentMessages(conversationId));
            traceService.addStep(traceId, "llm_generation", "RAG_PROMPT", request.message(), answer, "SUCCESS", System.currentTimeMillis() - start);
            return finish(conversationId, traceId, intent, answer, needConfirm, draft, references);
        } catch (RuntimeException ex) {
            answer = "我在处理这个请求时遇到异常，建议稍后重试或转人工工单。错误信息：" + ex.getMessage();
            traceService.addStep(traceId, "error", "EXCEPTION", request.message(), answer, "FAILED", 0);
            return finish(conversationId, traceId, intent, answer, false, null, references);
        }
    }

    public List<MessageDto> messages(String conversationId) {
        return jdbcTemplate.query("""
                SELECT role, content, create_time
                FROM agent_message
                WHERE conversation_id = ?
                ORDER BY id
                """, (rs, rowNum) -> new MessageDto(rs.getString("role"), rs.getString("content"), rs.getTimestamp("create_time").toLocalDateTime()), conversationId);
    }

    public List<Map<String, Object>> conversations(Long userId) {
        if (userId == null) {
            return jdbcTemplate.queryForList("SELECT conversation_id conversationId, user_id userId, title, create_time createTime, update_time updateTime FROM agent_conversation ORDER BY id DESC LIMIT 100");
        }
        return jdbcTemplate.queryForList("SELECT conversation_id conversationId, user_id userId, title, create_time createTime, update_time updateTime FROM agent_conversation WHERE user_id = ? ORDER BY id DESC LIMIT 100", userId);
    }

    private ChatResponse finish(String conversationId, String traceId, String intent, String answer,
                                boolean needConfirm, TicketDraft draft, List<RetrievedChunk> references) {
        saveMessage(conversationId, "ASSISTANT", answer);
        jdbcTemplate.update("UPDATE agent_conversation SET update_time = ? WHERE conversation_id = ?", LocalDateTime.now(), conversationId);
        traceService.finishTrace(traceId, answer, "SUCCESS");
        return new ChatResponse(conversationId, traceId, intent, answer, needConfirm, draft, references, traceService.steps(traceId));
    }

    private String ensureConversation(String requestedId, Long userId, String message) {
        if (requestedId != null && !requestedId.isBlank()) {
            Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_conversation WHERE conversation_id = ?", Integer.class, requestedId);
            if (exists != null && exists > 0) {
                return requestedId;
            }
        }
        String conversationId = "CV" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
        String title = message.length() > 24 ? message.substring(0, 24) : message;
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO agent_conversation (conversation_id, user_id, title, create_time, update_time) VALUES (?, ?, ?, ?, ?)",
                conversationId, userId, title, now, now);
        return conversationId;
    }

    private void saveMessage(String conversationId, String role, String content) {
        jdbcTemplate.update("INSERT INTO agent_message (conversation_id, role, content, create_time) VALUES (?, ?, ?, ?)",
                conversationId, role, content, LocalDateTime.now());
    }

    private List<MessageDto> recentMessages(String conversationId) {
        List<MessageDto> rows = jdbcTemplate.query("""
                SELECT role, content, create_time FROM agent_message
                WHERE conversation_id = ?
                ORDER BY id DESC
                LIMIT 6
                """, (rs, rowNum) -> new MessageDto(rs.getString("role"), rs.getString("content"), rs.getTimestamp("create_time").toLocalDateTime()), conversationId);
        List<MessageDto> ordered = new ArrayList<>(rows);
        Collections.reverse(ordered);
        return ordered;
    }

    private String generateAnswer(String question, List<RetrievedChunk> references, List<MessageDto> history) {
        if (references.isEmpty()) {
            return "知识库里没有找到足够可靠的说明，我不能编造答案。你可以补充问题现象、错误码、发生时间；如果影响业务，我可以帮你创建人工工单。";
        }
        StringBuilder context = new StringBuilder();
        for (RetrievedChunk chunk : references) {
            context.append("来源：").append(chunk.title()).append("，相似度：").append(String.format("%.2f", chunk.score())).append("\n")
                    .append(chunk.content()).append("\n\n");
        }
        String prompt = "对话历史：" + history + "\n知识库内容：\n" + context + "\n用户问题：" + question;
        String llm = llmClient.complete(SYSTEM_PROMPT, prompt);
        if (llm != null && !llm.isBlank()) {
            return llm;
        }
        RetrievedChunk best = references.get(0);
        return "根据知识库《" + best.title() + "》，建议先按以下步骤处理：\n"
                + summarize(best.content())
                + "\n\n如果按上述步骤仍未解决，我可以继续帮你整理信息并创建人工工单。";
    }

    private TicketDraft buildDraft(String message, Map<String, Object> handoff, List<RetrievedChunk> references) {
        String category = String.valueOf(handoff.get("suggestedCategoryCode"));
        Integer priority = (Integer) handoff.get("suggestedPriority");
        String title = categoryTitle(category, message);
        String description = message + (references.isEmpty() ? "" : "\n\n知识库参考：" + references.get(0).title());
        return new TicketDraft(title, description, category, priority, List.of());
    }

    private String categoryTitle(String category, String message) {
        if ("REFUND".equals(category)) {
            return "退款售后问题需要人工处理";
        }
        if ("ACCOUNT_LOGIN".equals(category)) {
            return "账号登录异常";
        }
        if ("ORDER".equals(category)) {
            return "订单状态异常";
        }
        return message.length() > 22 ? message.substring(0, 22) : message;
    }

    private boolean isConfirm(String message) {
        return message != null && (message.contains("确认") || message.contains("可以") || message.contains("创建吧") || message.equalsIgnoreCase("yes"));
    }

    private String summarize(String content) {
        String clean = content.replaceAll("\\s+", " ").trim();
        if (clean.length() <= 220) {
            return clean;
        }
        return clean.substring(0, 220) + "...";
    }

    private String statusName(String status) {
        return switch (status) {
            case "PENDING_PROCESS" -> "待处理";
            case "PROCESSING" -> "处理中";
            case "RESOLVED" -> "已解决";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }
}
