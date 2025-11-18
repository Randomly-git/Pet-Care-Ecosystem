// ActivityDTO.java (更新后)
package com.petcare.backend.dto.response;

import lombok.Data;

@Data
public class ActivityDTO {
    private Long activityId;
    private String activityName;
    private Long activityKindId;
    private String activityKindName;
    private Long userId; // 改为 userId
    private String userName; // 改为 userName
    private Integer state; // 添加状态字段
}