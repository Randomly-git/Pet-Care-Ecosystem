package com.petcare.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 活动记录实体
 * 支持冷热数据分离存储
 * 
 * 简化设计：
 * - 迁移状态只有 NONE/MIGRATING（迁移后直接删除记录）
 * - thaw_expire_time 用于10分钟临时访问窗口
 * - 所有冗余字段已移除（storage_status, storage_location, hbase_row_key 等）
 */
@Entity
@Table(name = "activity_record")
@Data
public class ActivityRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_record_id")
    private Long activityRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", referencedColumnName = "activity_id")
    @JsonIgnore
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", referencedColumnName = "pet_id")
    @JsonIgnore
    private Pet pet;

    @Column(name = "activity_description", length = 1000)
    private String activityDescription;

    @Column(name = "activity_date")
    private LocalDateTime activityDate;

    // ==================== 冷热分离字段（简化后） ====================
    
    /**
     * 迁移状态: NONE(无需迁移) / MIGRATING(迁移中)
     * 迁移成功后此记录会被删除
     */
    @Column(name = "migration_status", length = 20)
    private String migrationStatus = "NONE";

    /**
     * 解冻过期时间（用于临时访问，10分钟后过期）
     * 当用户访问冷数据时设置此时间
     */
    @Column(name = "thaw_expire_time")
    private LocalDateTime thawExpireTime;
}