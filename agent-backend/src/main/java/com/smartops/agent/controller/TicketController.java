package com.smartops.agent.controller;

import com.smartops.agent.common.ApiResponse;
import com.smartops.agent.dto.AgentDtos.CategoryDto;
import com.smartops.agent.dto.AgentDtos.ReplyRequest;
import com.smartops.agent.dto.AgentDtos.TicketDto;
import com.smartops.agent.dto.AgentDtos.TicketRequest;
import com.smartops.agent.service.TicketService;
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
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ApiResponse<TicketDto> create(@Valid @RequestBody TicketRequest request) {
        return ApiResponse.ok(ticketService.createTicket(request, null));
    }

    @GetMapping
    public ApiResponse<List<TicketDto>> list(@RequestParam(required = false) Long creatorId) {
        return ApiResponse.ok(ticketService.listTickets(creatorId));
    }

    @GetMapping("/{ticketNo}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String ticketNo) {
        return ApiResponse.ok(Map.of(
                "ticket", ticketService.queryTicketByNo(ticketNo, null),
                "replies", ticketService.replies(ticketNo)
        ));
    }

    @PostMapping("/{ticketNo}/reply")
    public ApiResponse<Map<String, Object>> reply(@PathVariable String ticketNo, @Valid @RequestBody ReplyRequest request) {
        return ApiResponse.ok(ticketService.replyTicket(ticketNo, request, null));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryDto>> categories() {
        return ApiResponse.ok(ticketService.categories());
    }
}
