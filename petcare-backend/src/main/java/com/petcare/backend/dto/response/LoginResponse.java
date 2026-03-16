// LoginResponse.java
package com.petcare.backend.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private Long userId;
    private String name;
    /** 用户昵称，用于社区等展示；未设置时与 name 一致 */
    private String nickname;
    private String token;
    private String message;

    public LoginResponse(Long userId, String name, String nickname, String token, String message) {
        this.userId = userId;
        this.name = name;
        this.nickname = nickname != null ? nickname : name;
        this.token = token;
        this.message = message;
    }

    public static LoginResponse success(Long userId, String name, String token) {
        return new LoginResponse(userId, name, name, token, "登录成功");
    }

    public static LoginResponse success(Long userId, String name, String nickname, String token) {
        return new LoginResponse(userId, name, nickname != null ? nickname : name, token, "登录成功");
    }

    public static LoginResponse failure(String message) {
        return new LoginResponse(null, null, null, null, message);
    }
}