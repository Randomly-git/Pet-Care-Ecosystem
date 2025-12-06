package com.petcare.backend.service.impl;

import com.petcare.backend.dto.request.CreateStatusRecordDTO;
import com.petcare.backend.dto.request.UpdateStatusRecordDTO;
import com.petcare.backend.dto.response.StatusRecordDTO;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.StatusRecord;
import com.petcare.backend.entity.User; // 新增导入
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.StatusRepository;
import com.petcare.backend.repository.StatusRecordRepository;
import com.petcare.backend.repository.UserRepository; // 新增导入
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
    private StatusRecordRepository statusRecordRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired // 新增 UserRepository
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 1️⃣ 根据用户ID获取所有有效状态
     */
    @Override
    public List<Status> getValidStatusesByUserId(Long userId) {
        log.info("查询用户ID={} 的所有有效状态", userId);
        List<Status> all = statusRepository.findByUserUserId(userId); // 修改方法名
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
    public Status createStatus(Long userId, String statusName) {
        log.info("为用户ID={} 创建新状态: {}", userId, statusName);

        User user = userRepository.findById(userId) // 修改为查询用户
                .orElseThrow(() -> new RuntimeException("未找到用户，ID=" + userId));

        // 若已存在相同状态名称，则可避免重复（可选）
        if (statusRepository.existsByUserUserIdAndStatusName(userId, statusName)) { // 修改方法名
            throw new RuntimeException("该用户已存在同名状态: " + statusName);
        }

        Status status = new Status();
        status.setUser(user); // 修改为设置用户
        status.setStatusName(statusName);
        status.setState(1);

        return statusRepository.save(status);
    }

    @Override
    public List<StatusRecordDTO> getActiveStatusRecordsByPetIdAndDate(Long PetId, LocalDate targetDate) { // 修改参数名
        // 如果 targetDate 为 null，返回所有记录；否则返回指定日期的活跃记录
        return statusRecordRepository.findActiveStatusRecordsByPetIdAndDate(PetId, targetDate); // 修改方法名
    }

    @Override
    public List<StatusRecordDTO> getAllStatusRecordsByPetId(Long petId) {
        log.info("获取宠物ID={} 的所有状态记录", petId);
        // 调用现有方法，传入 null 日期表示获取所有记录
        return getActiveStatusRecordsByPetIdAndDate(petId, null);
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
    public List<StatusRecordDTO> getStatusRecordsByStatusId(Long statusId) {
        log.debug("根据状态ID {} 获取状态记录", statusId);

        // 从数据库查询
        List<StatusRecord> records = statusRecordRepository.findByStatusStatusId(statusId);

        // 转换为DTO
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 如果还没有这个方法，需要添加转换方法
    private StatusRecordDTO convertToDTO(StatusRecord record) {
        return new StatusRecordDTO(
                record.getStatusRecordId(),
                record.getStatus().getStatusId(),
                record.getStatus().getStatusName(),
                record.getPet().getPetId(),
                record.getStartDate(),
                record.getEndDate(),
                record.getStatusDescription()
        );
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