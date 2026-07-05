package com.smartops.agent.controller;

import com.smartops.agent.common.ApiResponse;
import com.smartops.agent.dto.AgentDtos.DashboardSummary;
import com.smartops.agent.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> summary() {
        return ApiResponse.ok(dashboardService.summary());
    }
}
