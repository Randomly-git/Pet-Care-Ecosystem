package com.petcare.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 宠物状态变更时间线 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimelineDTO {

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 开始时间，格式：yyyy-MM-dd HH:mm:ss
     */
    private String startTime;

    /**
     * 状态值
     */
    private String statusValue;
}