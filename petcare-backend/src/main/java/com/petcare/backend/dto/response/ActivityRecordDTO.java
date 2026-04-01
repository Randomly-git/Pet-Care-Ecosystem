package com.petcare.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityRecordDTO {
    private Long activityRecordId;
    private Long activityId;
    private String activityName;
    private Long activityKindId;
    private String activityKindName;
    private Long petId;
    private String petName;
    private Long userId;
    private String activityDescription;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activityDate;

    // --- 新增 BERT 相关字段 ---
    private Integer bertResult;       // 原始数字结果
    private String bertResultName;    // 中文描述结果

    // 新增媒体相关字段
    private List<MediaResponse> mediaFiles;
    private Integer mediaCount;
    private String firstMediaUrl;

    // 构造函数 (已更新，加入 bertResult)
    public ActivityRecordDTO(Long activityRecordId, Long activityId, String activityName,
                             Long activityKindId, String activityKindName, Long petId,
                             String activityDescription, LocalDateTime activityDate,
                             Integer bertResult) {
        this.activityRecordId = activityRecordId;
        this.activityId = activityId;
        this.activityName = activityName;
        this.activityKindId = activityKindId;
        this.activityKindName = activityKindName;
        this.petId = petId;
        this.activityDescription = activityDescription;
        this.activityDate = activityDate;
        this.bertResult = bertResult;
        this.bertResultName = convertBertResultToName(bertResult); // 自动转换中文名
    }

    /**
     * 将 BERT 数字结果转换为中文描述
     */
    public static String convertBertResultToName(Integer result) {
        if (result == null) return "未知";
        return switch (result) {
            case -1 -> "忽略";
            case 0  -> "正常";
            case 1  -> "消化问题";
            case 2  -> "寄生虫";
            case 3  -> "皮肤问题";
            case 4  -> "行动不便";
            case 5  -> "耳部感染";
            default -> "未知";
        };
    }

    // 设置媒体文件时同步更新相关字段
    public void setMediaFiles(List<MediaResponse> mediaFiles) {
        this.mediaFiles = mediaFiles;
        this.mediaCount = mediaFiles != null ? mediaFiles.size() : 0;
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            this.firstMediaUrl = mediaFiles.get(0).getFileUrl();
        }
    }

    public ActivityRecordDTO(){}

    // 如果你手动 setBertResult，也希望它自动更新中文名，可以重写 setter
    public void setBertResult(Integer bertResult) {
        this.bertResult = bertResult;
        this.bertResultName = convertBertResultToName(bertResult);
    }
}