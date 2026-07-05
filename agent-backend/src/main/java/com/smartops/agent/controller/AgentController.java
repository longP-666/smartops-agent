package com.smartops.agent.controller;

import com.smartops.agent.common.ApiResponse;
import com.smartops.agent.dto.AgentDtos.ChatRequest;
import com.smartops.agent.dto.AgentDtos.ChatResponse;
import com.smartops.agent.dto.AgentDtos.MessageDto;
import com.smartops.agent.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(agentService.chat(request));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<Map<String, Object>>> conversations(@RequestParam(required = false) Long userId) {
        return ApiResponse.ok(agentService.conversations(userId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<MessageDto>> messages(@PathVariable String conversationId) {
        return ApiResponse.ok(agentService.messages(conversationId));
    }
}
