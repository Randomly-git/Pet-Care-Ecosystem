// dto/ApiResponse.java
package com.petcare.backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
}
