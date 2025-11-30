package com.petcare.backend.controller;

import com.petcare.backend.dto.request.CreateReservedActivityDTO;
import com.petcare.backend.dto.request.UpdateReservedActivityDTO;
import com.petcare.backend.dto.response.ReservedActivityDTO;
import com.petcare.backend.entity.ActivityReminder;
import com.petcare.backend.service.ReservedActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reserved-activities")
@RequiredArgsConstructor
public class ReservedActivityController {

    private final ReservedActivityService reservedActivityService;

    @PostMapping
    public ResponseEntity<ActivityReminder> createReservedActivity(@RequestBody CreateReservedActivityDTO createDTO) {
        ActivityReminder result = reservedActivityService.createReservedActivity(createDTO);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<ReservedActivityDTO>> getReservedActivitiesByPetId(@PathVariable Long petId) {
        List<ReservedActivityDTO> result = reservedActivityService.getReservedActivitiesByPetId(petId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/date")
    public ResponseEntity<ActivityReminder> updateReservedActivityDate(@RequestBody UpdateReservedActivityDTO updateDTO) {
        ActivityReminder result = reservedActivityService.updateReservedActivityDate(updateDTO);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{activityReminderId}")
    public ResponseEntity<Void> deleteReservedActivity(@PathVariable Long activityReminderId) {
        reservedActivityService.deleteReservedActivity(activityReminderId);
        return ResponseEntity.ok().build();
    }
}