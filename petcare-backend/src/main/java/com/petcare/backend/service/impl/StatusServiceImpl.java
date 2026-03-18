package com.petcare.backend.service.impl;

import com.petcare.backend.dto.request.CreateStatusRecordDTO;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.Status;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.StatusRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusServiceImpl implements StatusService {

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
        return statusRepository.findByPetPetId(petId);
    }

    /**
     * 1.5️⃣ 根据宠物ID获取状态数量
     */
    @Override
    public Long getStatusCountByPetId(Long petId) {
        log.info("统计宠物ID={} 的状态数量", petId);
        return statusRepository.countByPetPetId(petId);
    }

    /**
     * 2️⃣ 根据状态ID删除状态（直接从数据库删除）
     */
    @Override
    @Transactional
    public void softDeleteStatus(Long statusId) {
        log.info("删除状态ID={}", statusId);
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new RuntimeException("未找到状态，ID=" + statusId));
        statusRepository.delete(status);
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
     * 3.5️⃣ 根据状态ID修改状态当前值
     */
    @Override
    @Transactional
    public Status updateStatusValue(Long statusId, String statusValue) {
        log.info("修改状态ID={} 的状态值为 {}", statusId, statusValue);
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new RuntimeException("未找到状态，ID=" + statusId));
        status.setStatusValue(statusValue);
        return statusRepository.save(status);
    }

    /**
     * 4️⃣ 新增状态（为宠物创建状态）
     */
    @Override
    @Transactional
    public Status createStatus(Long petId, String statusName) {
        log.info("为宠物ID={} 创建新状态: {}", petId, statusName);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("未找到宠物，ID=" + petId));

        // 若已存在相同状态名称，则避免重复
        if (statusRepository.existsByPetPetIdAndStatusName(petId, statusName)) {
            throw new RuntimeException("该宠物已存在同名状态: " + statusName);
        }

        Status status = new Status();
        status.setPet(pet);
        status.setStatusName(statusName);

        return statusRepository.save(status);
    }

}
