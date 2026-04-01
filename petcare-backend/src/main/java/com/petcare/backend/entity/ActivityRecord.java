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
 * - 冷数据直接从 HBase 读取，不需要解冻机制
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

    /**
     * BERT AI 分析结果
     * 映射关系：
     * -1: 忽略
     * 0: 正常 (Normal)
     * 1: 消化问题 (Digestive Issues)
     * 2: 寄生虫 (Parasites)
     * 3: 皮肤问题 (Skin Irritations)
     * 4: 行动不便 (Mobility Problems)
     * 5: 耳部感染 (Ear Infections)
     */
    @Column(name = "bert_result")
    private Integer bertResult;

    // ==================== 冷热分离字段（简化后） ====================
    
    /**
     * 迁移状态: NONE(无需迁移) / MIGRATING(迁移中)
     * 迁移成功后此记录会被删除
     */
    @Column(name = "migration_status", length = 20)
    private String migrationStatus = "NONE";
}