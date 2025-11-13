package com.petcare.backend.service.impl;

import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.Status;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.StatusRepository;
import com.petcare.backend.service.StatusService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusServiceImpl implements StatusService {

    private final StatusRepository statusRepository;
    private final PetRepository petRepository;

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
}
