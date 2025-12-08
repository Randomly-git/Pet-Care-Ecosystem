package com.petcare.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 用户响应DTO - 用于社区微服务调用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String nickname;
    private String avatarUrl;

    // 可以从User实体转换
    public static UserResponseDTO fromEntity(com.petcare.backend.entity.User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getUserId());
        dto.setNickname(user.getNickname() != null ? user.getNickname() : user.getName());
        dto.setAvatarUrl(user.getAvatarUrl());
        return dto;
    }
}