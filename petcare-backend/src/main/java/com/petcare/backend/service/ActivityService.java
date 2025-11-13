// ActivityService.java (更新后)
package com.petcare.backend.service;

import com.petcare.backend.entity.Activity;
import com.petcare.backend.dto.response.ActivityDTO;
import com.petcare.backend.dto.response.CreateActivityDTO;
import com.petcare.backend.dto.response.UpdateActivityDTO;
import com.petcare.backend.dto.response.ActivityKindDTO;

import java.util.List;
import java.util.Optional;

public interface ActivityService {

    // 根据宠物ID和活动种类ID获取
    List<ActivityDTO> getActivitiesByPetId(Long petId, Long activityKindId);

    // 根据宠物ID获取（不带种类筛选）
    List<ActivityDTO> getActivitiesByPetId(Long petId);


    // 根据活动ID获取有效活动信息
    Optional<ActivityDTO> getActivityById(Long activityId);

    // 创建新活动
    Activity createActivity(CreateActivityDTO createActivityDTO);

    // 软删除活动（更新state为0）
    boolean deleteActivity(Long activityId);

    // 更新活动信息
    Activity updateActivity(UpdateActivityDTO updateActivityDTO);

    List<ActivityKindDTO> getAllActivityKinds();
}