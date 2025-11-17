// RegisterRequest.java
package com.petcare.backend.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 50, message = "用户名长度必须在1-50个字符之间")
    private String name;

    @NotBlank(message = "密码不能为空")
    @Size(min = 1, max = 100, message = "密码长度必须在1-100个字符之间")
    private String password;
}