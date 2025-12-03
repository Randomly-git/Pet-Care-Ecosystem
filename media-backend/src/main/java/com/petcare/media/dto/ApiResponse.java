// dto/ApiResponse.java
package com.petcare.media.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
// 引入 AllArgsConstructor 方便内部调用
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    private int code; // 业务状态码 (20000 成功, 4xxxx 业务错误, 5xxxx 系统错误)
    private String message;
    private T data;
    // 移除 boolean success，通过 code 字段判断即可

    // 成功响应 (code: 20000)
    public static <T> ApiResponse<T> success(T data) {
        return success("操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(20000, message, data);
    }

    // 错误响应 (由全局异常处理器调用)
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}