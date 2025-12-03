// src/main/java/com/petcare/media/exception/MediaServiceException.java
package com.petcare.media.exception;

import org.springframework.http.HttpStatus;

/**
 * 媒体服务自定义业务异常
 */
public class MediaServiceException extends RuntimeException {

    private final int businessCode;

    // 默认构造函数，用于参数校验失败等
    public MediaServiceException(String message) {
        // 40001: 业务参数或操作错误
        this(40001, message);
    }

    public MediaServiceException(int businessCode, String message) {
        super(message);
        this.businessCode = businessCode;
    }

    public int getBusinessCode() {
        return businessCode;
    }
}