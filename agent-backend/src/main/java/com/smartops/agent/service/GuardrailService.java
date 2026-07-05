package com.smartops.agent.service;

import com.smartops.agent.dto.AgentDtos.RetrievedChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GuardrailService {
    public Map<String, Object> handoffDecision(String message, List<RetrievedChunk> references) {
        String text = message == null ? "" : message;
        boolean sensitive = text.contains("退款") || text.contains("删除") || text.contains("账号安全") || text.contains("冻结");
        boolean unresolved = text.contains("没解决") || text.contains("还是不行") || text.contains("无法解决") || text.contains("不能用");
        boolean lowConfidence = references == null || references.isEmpty();
        boolean need = sensitive || unresolved || lowConfidence || text.contains("人工");
        String reason = sensitive ? "涉及敏感业务，需要人工确认"
                : unresolved ? "用户反馈自助方案未解决"
                : lowConfidence ? "知识库未命中可靠内容"
                : text.contains("人工") ? "用户主动要求人工服务"
                : "无需转人工";
        int priority = sensitive || unresolved ? 2 : 3;
        String category = text.contains("退款") ? "REFUND"
                : text.contains("订单") ? "ORDER"
                : text.contains("登录") || text.contains("账号") ? "ACCOUNT_LOGIN"
                : "SYSTEM_FAULT";
        return Map.of("needHandoff", need, "reason", reason, "suggestedPriority", priority, "suggestedCategoryCode", category);
    }
}
