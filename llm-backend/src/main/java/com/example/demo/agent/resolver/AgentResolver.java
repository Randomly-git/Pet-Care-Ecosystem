package com.example.demo.agent.resolver;

import com.example.demo.agent.dto.AgentResult;
import com.example.demo.agent.service.AgentService;
import graphql.kickstart.tools.GraphQLQueryResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AgentResolver implements GraphQLQueryResolver {

    private final AgentService agentService;

    public AgentResolver(AgentService agentService) {
        this.agentService = agentService;
    }

    public AgentResult aiAgent(String petId, String message) {
        log.info("aiAgent: petId={}, message={}", petId, message);
        return agentService.process(petId, message);
    }
}
