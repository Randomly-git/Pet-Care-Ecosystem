package com.petcare.backend.controller;

import com.petcare.backend.dto.request.CreateFixedActivityDTO;
import com.petcare.backend.dto.request.UpdateFixedActivityDTO;
import com.petcare.backend.dto.response.FixedActivityDTO;
import com.petcare.backend.entity.FixedActivity;
import com.petcare.backend.service.FixedActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/fixed-activities")
public class FixedActivityController {

    private final FixedActivityService fixedActivityService;

    public FixedActivityController(FixedActivityService fixedActivityService) {
        this.fixedActivityService = fixedActivityService;
    }

    @PostMapping
    public ResponseEntity<FixedActivity> createFixedActivity(
            @Valid @RequestBody CreateFixedActivityDTO createFixedActivityDTO) {
        FixedActivity fixedActivity = fixedActivityService.createFixedActivity(createFixedActivityDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(fixedActivity);
    }

    @PutMapping("/{fixedActivityId}")
    public ResponseEntity<FixedActivity> updateFixedActivityGapTime(
            @PathVariable Long fixedActivityId,
            @Valid @RequestBody UpdateFixedActivityDTO updateFixedActivityDTO) {
        updateFixedActivityDTO.setFixedActivityId(fixedActivityId);
        FixedActivity fixedActivity = fixedActivityService.updateFixedActivityGapTime(updateFixedActivityDTO);
        return ResponseEntity.ok(fixedActivity);
    }

    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<FixedActivityDTO>> getFixedActivitiesByPetId(@PathVariable Long petId) {
        List<FixedActivityDTO> fixedActivities = fixedActivityService.getFixedActivitiesByPetId(petId);
        return ResponseEntity.ok(fixedActivities);
    }

    @DeleteMapping("/{fixedActivityId}")
    public ResponseEntity<Void> deleteFixedActivity(@PathVariable Long fixedActivityId) {
        fixedActivityService.deleteFixedActivity(fixedActivityId);
        return ResponseEntity.noContent().build();
    }
}