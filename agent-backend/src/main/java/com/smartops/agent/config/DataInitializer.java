package com.smartops.agent.config;

import com.smartops.agent.dto.AgentDtos.KnowledgeDocumentRequest;
import com.smartops.agent.service.KnowledgeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final KnowledgeService knowledgeService;

    public DataInitializer(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public void run(String... args) {
        if (knowledgeService.hasDocuments()) {
            return;
        }
        knowledgeService.createDocument(new KnowledgeDocumentRequest(1L, "Account login failure handbook", "account-login.md", "ACCOUNT_LOGIN", """
                Account login failures are usually caused by a wrong password, expired verification code,
                risk-control lock, browser cache issue, or permission changes. Self-service checks:
                verify the login entrance and account case, reset the password, clear browser cache, or try another browser.
                If the account is locked or repeated reset attempts fail, create an account login ticket for manual identity verification.
                """));
        knowledgeService.createDocument(new KnowledgeDocumentRequest(1L, "Refund failure SOP", "refund-policy.md", "REFUND", """
                Refund failures usually come from abnormal bank or payment account information, payment channel processing,
                or an order state that does not meet refund conditions. Collect order number, payment channel,
                refund request time, and failure message. If the refund is not received after 24 hours or there is a money dispute,
                create a refund ticket. The Agent must not promise refund success or change order amounts.
                """));
        knowledgeService.createDocument(new KnowledgeDocumentRequest(1L, "System incident severity guide", "system-fault.md", "SYSTEM_FAULT", """
                System incidents are classified from P1 to P4. P1 means a core service is unavailable or many users are affected,
                and the on-call engineer must be escalated immediately. P2 means a partial function is unavailable or APIs keep failing.
                A fault ticket should include error code, occurrence time, impact scope, reproduction steps, and screenshot or log summary.
                """));
    }
}
