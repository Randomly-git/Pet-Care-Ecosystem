package com.petcare.backend.controller;

import com.petcare.backend.dto.response.ReminderDTO;
import com.petcare.backend.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping("/pet/{petId}/overdue")
    public ResponseEntity<List<ReminderDTO>> getOverdueRemindersByPetId(@PathVariable Long petId) {
        List<ReminderDTO> result = reminderService.getOverdueRemindersByPetId(petId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{activityReminderId}/postpone")
    public ResponseEntity<Void> postponeReminder(@PathVariable Long activityReminderId) {
        reminderService.postponeReminder(activityReminderId);
        return ResponseEntity.ok().build();
    }

    /**
     * 合并后的确认接口
     * 如果请求体中包含 description，则调用带描述的确认逻辑；否则执行普通确认。
     */
    @PutMapping("/{activityReminderId}/confirm")
    public ResponseEntity<Void> confirmReminder(
            @PathVariable Long activityReminderId,
            @RequestBody(required = false) String description) {

        if (description != null && !description.trim().isEmpty()) {
            reminderService.confirmReminderWithDescription(activityReminderId, description);
        } else {
            reminderService.confirmReminderWithoutDescription(activityReminderId);
        }

        return ResponseEntity.ok().build();
    }
}