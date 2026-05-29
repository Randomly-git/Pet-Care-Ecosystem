package com.example.demo.agent.dto;

import lombok.Data;
import java.util.List;

@Data
public class AgentResult {
    private String petId;
    private String petName;
    private String message;
    private List<ToolCallLog> toolCalls;

    @Data
    public static class ToolCallLog {
        private String toolName;
        private String arguments;
        private String result;
        private boolean success;
    }
}
