package com.petcare.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 统一API响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private boolean success;

    // 成功响应
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(20000);
        response.setMessage("操作成功");
        response.setData(data);
        response.setSuccess(true);
        return response;
    }

    // 成功响应（无数据）
    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(20000);
        response.setMessage(message);
        response.setData(null);
        response.setSuccess(true);
        return response;
    }

    // 失败响应
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(null);
        response.setSuccess(false);
        return response;
    }

    public boolean isSuccess() {
        return success && code == 20000;
    }
}