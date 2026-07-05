package com.smartops.agent.controller;

import com.smartops.agent.common.ApiResponse;
import com.smartops.agent.dto.AgentDtos.KnowledgeDocumentRequest;
import com.smartops.agent.dto.AgentDtos.RetrievedChunk;
import com.smartops.agent.service.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/documents")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody KnowledgeDocumentRequest request) {
        return ApiResponse.ok(knowledgeService.createDocument(request));
    }

    @GetMapping("/documents")
    public ApiResponse<List<Map<String, Object>>> documents() {
        return ApiResponse.ok(knowledgeService.documents());
    }

    @GetMapping("/search")
    public ApiResponse<List<RetrievedChunk>> search(@RequestParam String query,
                                                    @RequestParam(defaultValue = "5") int topK) {
        return ApiResponse.ok(knowledgeService.search(query, topK));
    }
}
