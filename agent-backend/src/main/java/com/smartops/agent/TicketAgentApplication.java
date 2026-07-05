package com.smartops.agent;

import com.smartops.agent.config.AgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AgentProperties.class)
public class TicketAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketAgentApplication.class, args);
    }
}
