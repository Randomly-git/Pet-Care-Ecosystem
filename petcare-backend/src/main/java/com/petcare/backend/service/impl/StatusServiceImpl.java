package com.petcare.backend.service.impl;

import com.petcare.backend.dto.request.CreateStatusRecordDTO;
import com.petcare.backend.dto.request.UpdateStatusRecordDTO;
import com.petcare.backend.dto.response.StatusRecordDTO;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.StatusRecord;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.StatusRepository;
import com.petcare.backend.repository.StatusRecordRepository;
import com.petcare.backend.service.StatusService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusServiceImpl implements StatusService {

    @Autowired
    private StatusRecordRepository statusRecordRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private PetRepository petRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 1️⃣ 根据宠物ID获取所有有效状态
     */
    @Override
    public List<Status> getValidStatusesByPetId(Long petId) {
        log.info("查询宠物ID={} 的所有有效状态", petId);
        List<Status> all = statusRepository.findByPetPetId(petId);
        return all.stream()
                .filter(s -> s.getState() != null && s.getState() == 1)
                .toList();
    }

    /**
     * 2️⃣ 根据状态ID软删除状态（state = 0）
     */
    @Override
    @Transactional
    public void softDeleteStatus(Long statusId) {
        log.info("软删除状态ID={}", statusId);
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new RuntimeException("未找到状态，ID=" + statusId));
        status.setState(0);
        statusRepository.save(status);
    }

    /**
     * 3️⃣ 根据状态ID修改状态名称
     */
    @Override
    @Transactional
    public Status updateStatusName(Long statusId, String newName) {
        log.info("修改状态ID={} 的名称为 {}", statusId, newName);
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new RuntimeException("未找到状态，ID=" + statusId));
        status.setStatusName(newName);
        return statusRepository.save(status);
    }

    /**
     * 4️⃣ 新增状态
     */
    @Override
    @Transactional
    public Status createStatus(Long petId, String statusName) {
        log.info("为宠物ID={} 创建新状态: {}", petId, statusName);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("未找到宠物，ID=" + petId));

        // 若已存在相同状态名称，则可避免重复（可选）
        if (statusRepository.existsByPetPetIdAndStatusName(petId, statusName)) {
            throw new RuntimeException("该宠物已存在同名状态: " + statusName);
        }

        Status status = new Status();
        status.setPet(pet);
        status.setStatusName(statusName);
        status.setState(1);

        return statusRepository.save(status);
    }

    @Override
    public List<StatusRecordDTO> getActiveStatusRecordsByPetIdAndDate(Long petId, LocalDate targetDate) {
        // 如果 targetDate 为 null，返回所有记录；否则返回指定日期的活跃记录
        return statusRecordRepository.findActiveStatusRecordsByPetIdAndDate(petId, targetDate);
    }

    @Override
    @Transactional
    public StatusRecord createStatusRecord(CreateStatusRecordDTO createStatusRecordDTO) {
        // 验证宠物和状态是否存在
        Pet pet = petRepository.findById(createStatusRecordDTO.getPetId())
                .orElseThrow(() -> new RuntimeException("宠物不存在"));

        Status status = statusRepository.findById(createStatusRecordDTO.getStatusId())
                .orElseThrow(() -> new RuntimeException("状态不存在"));

        // 创建新的状态记录
        StatusRecord statusRecord = new StatusRecord();
        statusRecord.setPet(pet);
        statusRecord.setStatus(status);
        statusRecord.setStartDate(createStatusRecordDTO.getStartDate());
        statusRecord.setStatusDescription(createStatusRecordDTO.getStatusDescription());
        // end_date 保持为 null

        return statusRecordRepository.save(statusRecord);
    }

    @Override
    @Transactional
    public StatusRecord stopStatusRecord(Long statusRecordId, LocalDate endDate) {
        StatusRecord statusRecord = statusRecordRepository.findById(statusRecordId)
                .orElseThrow(() -> new RuntimeException("状态记录不存在"));

        // 设置结束日期
        statusRecord.setEndDate(endDate);

        return statusRecordRepository.save(statusRecord);
    }

    @Override
    @Transactional
    public void deleteStatusRecord(Long statusRecordId) {
        StatusRecord statusRecord = statusRecordRepository.findById(statusRecordId)
                .orElseThrow(() -> new RuntimeException("状态记录不存在"));

        statusRecordRepository.delete(statusRecord);
    }

    @Override
    @Transactional
    public void deleteStatusAndRecords(Long statusId) {
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new RuntimeException("状态不存在"));

        // 先删除相关的状态记录
        List<StatusRecord> relatedRecords = statusRecordRepository.findByStatusStatusId(statusId);
        statusRecordRepository.deleteAll(relatedRecords);

        // 然后删除状态
        statusRepository.delete(status);
    }

    @Override
    @Transactional
    public StatusRecord updateStatusRecord(UpdateStatusRecordDTO updateStatusRecordDTO) {
        StatusRecord statusRecord = statusRecordRepository.findById(updateStatusRecordDTO.getStatusRecordId())
                .orElseThrow(() -> new RuntimeException("状态记录不存在"));

        // 如果提供了新的状态ID，更新状态
        if (updateStatusRecordDTO.getStatusId() != null) {
            Status newStatus = statusRepository.findById(updateStatusRecordDTO.getStatusId())
                    .orElseThrow(() -> new RuntimeException("状态不存在"));
            statusRecord.setStatus(newStatus);
        }

        // 更新其他字段
        if (updateStatusRecordDTO.getStartDate() != null) {
            statusRecord.setStartDate(updateStatusRecordDTO.getStartDate());
        }
        if (updateStatusRecordDTO.getEndDate() != null) {
            statusRecord.setEndDate(updateStatusRecordDTO.getEndDate());
        }
        if (updateStatusRecordDTO.getStatusDescription() != null) {
            statusRecord.setStatusDescription(updateStatusRecordDTO.getStatusDescription());
        }

        return statusRecordRepository.save(statusRecord);
    }
}
