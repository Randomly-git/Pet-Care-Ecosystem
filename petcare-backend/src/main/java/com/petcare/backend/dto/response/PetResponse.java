package com.petcare.backend.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PetResponse {
    private Long petId;
    private String name;
    private String species;
    private String breed;
    private LocalDate birthday;
    private LocalDateTime createdAt;

    // 可以添加统计信息
    private Long statusCount;
    private Long activityCount;
    private Long statusRecordCount;
    private Long activityRecordCount;
}