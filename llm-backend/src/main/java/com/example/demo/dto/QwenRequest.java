package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class QwenRequest {
    private String model;
    private List<Message> messages;
    private boolean stream = false;

    @Data
    public static class Message {
        private String role;
        private String content;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}