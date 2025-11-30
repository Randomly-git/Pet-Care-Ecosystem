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

    @PutMapping("/{activityReminderId}/confirm")
    public ResponseEntity<Void> confirmReminderWithoutDescription(@PathVariable Long activityReminderId) {
        reminderService.confirmReminderWithoutDescription(activityReminderId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{activityReminderId}/confirm-with-description")
    public ResponseEntity<Void> confirmReminderWithDescription(
            @PathVariable Long activityReminderId,
            @RequestBody(required = false) String description) {
        reminderService.confirmReminderWithDescription(activityReminderId, description);
        return ResponseEntity.ok().build();
    }
}