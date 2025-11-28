// AuthController.java
package com.petcare.backend.controller;

import com.petcare.backend.dto.request.LoginRequest;
import com.petcare.backend.dto.request.RegisterRequest;
import com.petcare.backend.dto.response.LoginResponse;
import com.petcare.backend.dto.response.RegisterResponse;
import com.petcare.backend.exception.InvalidCredentialsException;
import com.petcare.backend.exception.UserAlreadyExistsException;
import com.petcare.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest) {
        try {
            RegisterResponse response = userService.register(registerRequest);
            return created(response);
        } catch (UserAlreadyExistsException e) {
            return error(e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return error("注册失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = userService.login(loginRequest);
            return success(response, "登录成功");
        } catch (InvalidCredentialsException e) {
            return error(e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return error("登录失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}