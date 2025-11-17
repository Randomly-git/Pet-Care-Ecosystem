// LoginResponse.java
package com.petcare.backend.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private Long userId;
    private String name;
    private String token;
    private String message;

    public LoginResponse(Long userId, String name, String token, String message) {
        this.userId = userId;
        this.name = name;
        this.token = token;
        this.message = message;
    }

    public static LoginResponse success(Long userId, String name, String token) {
        return new LoginResponse(userId, name, token, "登录成功");
    }

    public static LoginResponse failure(String message) {
        return new LoginResponse(null, null, null, message);
    }
}