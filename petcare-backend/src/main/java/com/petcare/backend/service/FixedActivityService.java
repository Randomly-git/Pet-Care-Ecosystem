package com.petcare.backend.service;

import com.petcare.backend.dto.request.CreateFixedActivityDTO;
import com.petcare.backend.dto.request.UpdateFixedActivityDTO;
import com.petcare.backend.dto.response.FixedActivityDTO;
import com.petcare.backend.entity.FixedActivity;

import java.util.List;

public interface FixedActivityService {

    /**
     * 创建定时活动
     * 同一个pet_id下不能创建两个activity_id相同的
     */
    FixedActivity createFixedActivity(CreateFixedActivityDTO createFixedActivityDTO);

    /**
     * 修改定时活动的间隔时间
     */
    FixedActivity updateFixedActivityGapTime(UpdateFixedActivityDTO updateFixedActivityDTO);

    /**
     * 查看宠物所有定时活动（包含activity_name等信息）
     */
    List<FixedActivityDTO> getFixedActivitiesByPetId(Long petId);

    /**
     * 删除定时活动，同时删除对应的提醒记录
     */
    void deleteFixedActivity(Long fixedActivityId);
}