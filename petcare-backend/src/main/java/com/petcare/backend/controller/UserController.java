package com.petcare.backend.controller;

import com.petcare.backend.dto.ApiResponse;
import com.petcare.backend.dto.UserResponseDTO;
import com.petcare.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户控制器 - 提供用户信息API
 * 特别为社区微服务提供批量查询接口
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8082", "http://localhost:8083", "http://localhost:8080", "http://localhost:5173"}, allowCredentials = "false")
public class UserController {

    private final UserService userService;

    /**
     * 批量获取用户信息
     * 为社区微服务提供的专用接口
     *
     * @param ids 用户ID列表，逗号分隔
     * @return 用户信息列表
     */
    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> batchGetUsers(
            @RequestParam("ids") String ids) {
        try {
            // 解析ID字符串
            Set<Long> userIds = Set.of(ids.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());

            if (userIds.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("用户ID列表为空"));
            }

            // 批量查询用户信息
            List<UserResponseDTO> users = userService.batchGetUsersByIds(userIds)
                    .stream()
                    .map(UserResponseDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(users));

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "用户ID格式错误: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "批量查询用户失败: " + e.getMessage()));
        }
    }

    /**
     * 获取单个用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable Long id) {
        try {
            return userService.getUserById(id)
                    .map(user -> ResponseEntity.ok(ApiResponse.success(UserResponseDTO.fromEntity(user))))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "查询用户失败: " + e.getMessage()));
        }
    }
}