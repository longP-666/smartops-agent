package com.smartops.agent.controller;

import com.smartops.agent.common.ApiResponse;
import com.smartops.agent.dto.AgentDtos.StepDto;
import com.smartops.agent.dto.AgentDtos.TraceDto;
import com.smartops.agent.service.TraceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/traces")
public class TraceController {
    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping
    public ApiResponse<List<TraceDto>> list(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(traceService.traces(limit));
    }

    @GetMapping("/{traceId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String traceId) {
        Map<String, Object> result = new HashMap<>();
        result.put("trace", traceService.trace(traceId));
        result.put("steps", traceService.steps(traceId));
        return ApiResponse.ok(result);
    }

    @GetMapping("/{traceId}/steps")
    public ApiResponse<List<StepDto>> steps(@PathVariable String traceId) {
        return ApiResponse.ok(traceService.steps(traceId));
    }
}
