package com.petcare.media.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@Data
public class MediaFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long mediaId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false, unique = true)
    private String fileUrl;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_type", nullable = false)
    private RelatedType relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;

    @Column(name = "description")
    private String description;

    // 冷热状态: Hot(热数据) / Cold(冷数据)
    @Column(name = "status")
    private String status = "Hot";

    // 最后访问时间（用于社区内容的7天倒计时）
    @Column(name = "last_access_time")
    private LocalDateTime lastAccessTime;

    // 头像不参与冷热分离
    @JsonIgnore
    public boolean isExemptFromColdStorage() {
        return relatedType == RelatedType.USER_AVATAR;
    }

    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
        lastAccessTime = LocalDateTime.now();
    }
}