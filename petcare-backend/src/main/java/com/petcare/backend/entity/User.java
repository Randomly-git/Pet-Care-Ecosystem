package com.petcare.backend.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    // 可以添加创建时间等审计字段
    // 添加这个字段！
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // 如果没有设置nickname，默认使用name
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = name;
        }
    }
}