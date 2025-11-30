package com.petcare.backend.service;

import com.petcare.backend.dto.response.ReminderDTO;

import java.util.List;

public interface ReminderService {

    /**
     * 获取宠物ID的过期提醒（reminder_date早于系统日期）
     */
    List<ReminderDTO> getOverdueRemindersByPetId(Long petId);

    /**
     * 暂时忽略提醒 - 将提醒日期推迟一天
     */
    void postponeReminder(Long activityReminderId);

    /**
     * 确认提醒 - 创建无描述的活动记录，如果是type=2则删除提醒
     */
    void confirmReminderWithoutDescription(Long activityReminderId);

    /**
     * 确认提醒 - 创建有描述的活动记录，如果是type=2则删除提醒
     */
    void confirmReminderWithDescription(Long activityReminderId, String description);
}