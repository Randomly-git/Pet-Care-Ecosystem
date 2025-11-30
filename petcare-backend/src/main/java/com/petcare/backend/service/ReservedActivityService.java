package com.petcare.backend.service;

import com.petcare.backend.dto.request.CreateReservedActivityDTO;
import com.petcare.backend.dto.request.UpdateReservedActivityDTO;
import com.petcare.backend.dto.response.ReservedActivityDTO;
import com.petcare.backend.entity.ActivityReminder;

import java.util.List;

public interface ReservedActivityService {

    /**
     * 创建一次性提醒
     */
    ActivityReminder createReservedActivity(CreateReservedActivityDTO createReservedActivityDTO);

    /**
     * 根据宠物ID获取所有一次性提醒
     */
    List<ReservedActivityDTO> getReservedActivitiesByPetId(Long petId);

    /**
     * 修改一次性提醒日期
     */
    ActivityReminder updateReservedActivityDate(UpdateReservedActivityDTO updateReservedActivityDTO);

    /**
     * 删除一次性提醒
     */
    void deleteReservedActivity(Long activityReminderId);
}