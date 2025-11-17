// RegisterResponse.java
package com.petcare.backend.dto.response;

import lombok.Data;

@Data
public class RegisterResponse {
    private Long userId;
    private String name;
    private String message;

    public RegisterResponse(Long userId, String name, String message) {
        this.userId = userId;
        this.name = name;
        this.message = message;
    }

    public static RegisterResponse success(Long userId, String name) {
        return new RegisterResponse(userId, name, "注册成功");
    }

    public static RegisterResponse failure(String message) {
        return new RegisterResponse(null, null, message);
    }
}