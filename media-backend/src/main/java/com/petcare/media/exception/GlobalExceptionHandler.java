// src/main/java/com/petcare/media/exception/GlobalExceptionHandler.java
package com.petcare.media.exception;

import com.petcare.media.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. 捕获自定义业务异常
    @ExceptionHandler(MediaServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // HTTP 400
    public ApiResponse<?> handleMediaServiceException(MediaServiceException e) {
        log.warn("业务异常: code={}, message={}", e.getBusinessCode(), e.getMessage());
        return ApiResponse.error(e.getBusinessCode(), e.getMessage());
    }

    // 2. 捕获 Spring 校验异常 (如 @RequestParam 缺失)
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST) // HTTP 400
    public ApiResponse<?> handleValidationException(Exception e) {
        log.warn("参数校验失败: {}", e.getMessage());
        // 40003: 参数格式或缺失错误
        String message = e.getMessage().split(";")[0];
        return ApiResponse.error(40003, "请求参数错误: " + message);
    }

    // 3. 捕获所有未被处理的 Exception (系统错误)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // HTTP 500
    public ApiResponse<?> handleUncaughtException(Exception e) {
        // 50000: 通用系统错误码
        log.error("系统未知错误: {}", e.getMessage(), e);
        return ApiResponse.error(50000, "系统繁忙，请稍后再试");
    }
}