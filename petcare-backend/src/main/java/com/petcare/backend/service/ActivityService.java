// ActivityService.java (更新后)
package com.petcare.backend.service;

import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityRecord;
import com.petcare.backend.dto.response.ActivityDTO;
import com.petcare.backend.dto.response.ActivityRecordDTO;
import com.petcare.backend.dto.response.CreateActivityDTO;
import com.petcare.backend.dto.response.UpdateActivityDTO;
import com.petcare.backend.dto.response.ActivityKindDTO;
import java.time.LocalDateTime;

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

    // 1. 查找宠物的所有活动记录（日期 & kind 可选）
    List<ActivityRecordDTO> searchActivityRecords(Long petId,
                                                  LocalDateTime startDate,  // 可为空
                                                  LocalDateTime endDate,    // 可为空
                                                  Long activityKindId);     // 可为空

    // 2. 删除活动记录（软删除 or 直接删？这里选择硬删除）
    void deleteActivityRecord(Long activityRecordId);

    // 3. 插入活动记录
    ActivityRecord createActivityRecord(Long petId,
                                        Long activityId,
                                        String description,
                                        LocalDateTime date);

    // 4. 修改活动记录
    ActivityRecord updateActivityRecord(Long recordId,
                                        Long newActivityId,
                                        String description,
                                        LocalDateTime date);

    // 5. 彻底删除一个 Activity（先删 record 再删 activity）
    void deleteActivityCompletely(Long activityId);
}