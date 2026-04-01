package com.petcare.backend.service.impl;

import com.petcare.backend.client.MediaServiceClient;
import com.petcare.backend.dto.response.MediaResponse;
import com.petcare.backend.entity.*;
import com.petcare.backend.dto.response.ActivityDTO;
import com.petcare.backend.dto.response.ActivityRecordDTO;
import com.petcare.backend.dto.request.CreateActivityDTO;
import com.petcare.backend.dto.request.UpdateActivityDTO;
import com.petcare.backend.dto.response.ActivityKindDTO;
import com.petcare.backend.repository.*;
import com.petcare.backend.service.ActivityService;
import com.petcare.backend.service.HBaseColdStorageService;
import com.petcare.backend.service.ColdStorageEventPublisher;
import com.petcare.backend.service.ActivityColdDataMigrationJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityKindRepository activityKindRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final FixedActivityRepository fixedActivityRepository;
    private final ActivityReminderRepository activityReminderRepository;
    private final MediaServiceClient mediaServiceClient;
    
    // 冷热分离相关服务
    private final HBaseColdStorageService hBaseColdStorageService;
    private final ColdStorageEventPublisher coldStorageEventPublisher;
    private final ActivityColdDataMigrationJob activityColdDataMigrationJob;

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getActivitiesByUserId(Long userId, Long activityKindId) {
        log.info("获取用户ID为 {} 的活动信息，活动种类ID: {}", userId, activityKindId);

        List<Activity> activities;

        if (activityKindId != null) {
            // 如果提供了活动种类ID，则按用户ID和活动种类ID筛选
            activities = activityRepository.findByUserUserIdAndActivityKindActivityKindIdAndState(userId, activityKindId, 1);
            log.debug("按用户ID和活动种类ID查询，结果数量: {}", activities.size());
        } else {
            // 如果未提供活动种类ID，则只按用户ID查询
            activities = activityRepository.findByUserUserIdAndState(userId, 1);
            log.debug("按用户ID查询，结果数量: {}", activities.size());
        }

        return activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getActivitiesByUserId(Long userId) {
        // 调用带两个参数的版本，事务仍然生效
        return getActivitiesByUserId(userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ActivityDTO> getActivityById(Long activityId) {
        log.info("根据ID获取有效活动信息: {}", activityId);

        return activityRepository.findByActivityIdAndState(activityId, 1)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional
    public Activity createActivity(CreateActivityDTO createActivityDTO) {
        log.info("创建新活动: {}", createActivityDTO.getActivityName());

        // 验证用户是否存在
        User user = userRepository.findById(createActivityDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + createActivityDTO.getUserId()));

        // 验证活动种类是否存在
        ActivityKind activityKind = activityKindRepository.findById(createActivityDTO.getActivityKindId())
                .orElseThrow(() -> new RuntimeException("活动种类不存在，ID: " + createActivityDTO.getActivityKindId()));

        // 创建活动实体
        Activity activity = new Activity();
        activity.setActivityName(createActivityDTO.getActivityName());
        activity.setUser(user); // 修改为设置用户
        activity.setActivityKind(activityKind);
        activity.setState(1); // 设置为有效状态

        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public boolean deleteActivity(Long activityId) {
        log.info("软删除活动: {}", activityId);

        // 检查活动是否存在且有效
        Optional<Activity> activityOpt = activityRepository.findByActivityIdAndState(activityId, 1);
        if (activityOpt.isEmpty()) {
            log.warn("活动不存在或已被删除，ID: {}", activityId);
            return false;
        }

        int updated = activityRepository.softDelete(activityId);
        if (updated > 0) {
            log.info("活动软删除成功，ID: {}", activityId);
            return true;
        } else {
            log.error("活动软删除失败，ID: {}", activityId);
            return false;
        }
    }

    @Override
    @Transactional
    public Activity updateActivity(UpdateActivityDTO updateActivityDTO) {
        log.info("更新活动信息: {}", updateActivityDTO.getActivityId());

        // 查找有效的活动
        Activity activity = activityRepository.findByActivityIdAndState(updateActivityDTO.getActivityId(), 1)
                .orElseThrow(() -> new RuntimeException("活动不存在或已被删除，ID: " + updateActivityDTO.getActivityId()));

        // 更新活动名称（如果提供了新名称）
        if (updateActivityDTO.getActivityName() != null && !updateActivityDTO.getActivityName().trim().isEmpty()) {
            activity.setActivityName(updateActivityDTO.getActivityName());
        }

        // 更新活动种类（如果提供了新种类ID）
        if (updateActivityDTO.getActivityKindId() != null) {
            ActivityKind activityKind = activityKindRepository.findById(updateActivityDTO.getActivityKindId())
                    .orElseThrow(() -> new RuntimeException("活动种类不存在，ID: " + updateActivityDTO.getActivityKindId()));
            activity.setActivityKind(activityKind);
        }

        return activityRepository.save(activity);
    }

    // 以下方法保持不变，因为 ActivityRecord 仍然基于宠物
    @Override
    public List<ActivityKindDTO> getAllActivityKinds() {
        log.info("获取所有活动种类");

        List<ActivityKind> activityKinds = activityKindRepository.findAll();

        return activityKinds.stream()
                .map(activityKind -> {
                    ActivityKindDTO dto = new ActivityKindDTO();
                    dto.setActivityKindId(activityKind.getActivityKindId());
                    dto.setActivityKindName(activityKind.getActivityKindName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<ActivityRecordDTO> searchActivityRecords(Long petId,
                                                         LocalDateTime startDate,
                                                         LocalDateTime endDate,
                                                         Long activityKindId,
                                                         Pageable pageable) {

        log.info("【冷热分离分页查询】petId={}, startDate={}, endDate={}, page={}, size={}",
                petId, startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());

        // 1. 先查 MySQL 热数据
        Page<ActivityRecordDTO> mysqlPage = activityRecordRepository.findActivityRecordsWithDetails(
                petId, startDate, endDate, activityKindId, pageable);

        log.debug("【冷热分离】MySQL查询结果: totalElements={}, contentSize={}",
                mysqlPage.getTotalElements(), mysqlPage.getContent().size());

        // 2. 检查是否需要补充冷数据
        //    条件：MySQL返回数量 < pageSize，说明可能还有冷数据
        int pageSize = pageable.getPageSize();
        List<ActivityRecordDTO> hotRecords = mysqlPage.getContent();

        if (hotRecords.size() < pageSize) {
            log.info("【冷热分离】热数据不足({}/{}), 尝试补充冷数据", hotRecords.size(), pageSize);

            // 计算需要从 HBase 补充的数量
            int needCount = pageSize - hotRecords.size();

            try {
                // 查询 HBase 冷数据
                List<ActivityRecordDTO> coldRecords = hBaseColdStorageService
                        .queryByPetIdAndDateRange(petId, startDate, endDate);

                // 如果有活动种类过滤，需要过滤
                if (activityKindId != null) {
                    coldRecords = coldRecords.stream()
                            .filter(r -> {
                                // 需要通过 activityId 关联查询 activityKindId
                                // 这里先不过滤，后面补全时会处理
                                return true;
                            })
                            .collect(Collectors.toList());
                }

                // 补全冷数据的关联信息
                List<ActivityRecordDTO> enrichedColdRecords = new ArrayList<>();
                for (ActivityRecordDTO coldRecord : coldRecords) {
                    try {
                        ActivityRecordDTO enriched = enrichColdRecord(coldRecord);
                        // 如果有种类过滤，只添加匹配的
                        if (activityKindId == null || enriched.getActivityKindId() == null
                                || enriched.getActivityKindId().equals(activityKindId)) {
                            enrichedColdRecords.add(enriched);
                        }
                    } catch (Exception e) {
                        log.warn("【冷热分离】补全冷记录失败: recordId={}, error={}",
                                coldRecord.getActivityRecordId(), e.getMessage());
                    }
                }

                log.debug("【冷热分离】HBase查询结果: totalColdRecords={}", enrichedColdRecords.size());

                // 合并热数据和冷数据
                if (!enrichedColdRecords.isEmpty()) {
                    List<ActivityRecordDTO> mergedRecords = new ArrayList<>(hotRecords);
                    mergedRecords.addAll(enrichedColdRecords);

                    // 按日期降序排序
                    mergedRecords.sort((a, b) -> {
                        if (a.getActivityDate() == null || b.getActivityDate() == null) return 0;
                        return b.getActivityDate().compareTo(a.getActivityDate());
                    });

                    // 重新计算总数（MySQL总数 + HBase总数，注意去重）
                    Set<Long> existingIds = hotRecords.stream()
                            .map(ActivityRecordDTO::getActivityRecordId)
                            .collect(Collectors.toSet());

                    long coldCount = enrichedColdRecords.stream()
                            .filter(r -> !existingIds.contains(r.getActivityRecordId()))
                            .count();

                    long totalElements = mysqlPage.getTotalElements() + coldCount;

                    // 计算分页
                    int fromIndex = pageable.getPageNumber() * pageable.getPageSize();
                    int toIndex = Math.min(fromIndex + pageable.getPageSize(), mergedRecords.size());

                    List<ActivityRecordDTO> pagedRecords = fromIndex < mergedRecords.size()
                            ? mergedRecords.subList(fromIndex, toIndex)
                            : List.of();

                    log.info("【冷热分离】合并完成: 热数据={}, 冷数据={}, 总数={}, 本页={}",
                            hotRecords.size(), enrichedColdRecords.size(), totalElements, pagedRecords.size());

                    return new org.springframework.data.domain.PageImpl<>(
                            pagedRecords, pageable, totalElements);
                }

            } catch (Exception e) {
                log.error("【冷热分离】查询HBase冷数据失败: petId={}, error={}", petId, e.getMessage(), e);
                // 冷数据查询失败，返回热数据
            }
        }

        // 没有冷数据补充或补充失败，直接返回 MySQL 结果
        return mysqlPage;
    }

    @Override
    @Transactional
    public boolean deleteActivityRecord(Long recordId) {
        try {
            ActivityRecord record = activityRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("活动记录不存在 ID=" + recordId));

            // 检查是否正在迁移中，不允许删除
            if ("MIGRATING".equals(record.getMigrationStatus())) {
                throw new RuntimeException("该记录正在迁移中，请稍后重试");
            }

            // 先删除关联的媒体文件（通过MQ异步删除COS）
            try {
                mediaServiceClient.deleteRelatedFiles("ACTIVITY", recordId);
                log.debug("删除活动记录 {} 的关联媒体成功", recordId);
            } catch (Exception e) {
                log.warn("删除活动记录 {} 的关联媒体失败，继续删除记录: {}", recordId, e.getMessage());
            }

            // 再删除记录
            activityRecordRepository.delete(record);
            log.info("已删除活动记录 ID={}", recordId);
            return true;
        } catch (Exception e) {
            log.error("删除活动记录失败 ID={}", recordId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public ActivityRecord createActivityRecord(Long petId,
                                               Long activityId,
                                               String description,
                                               LocalDateTime date) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("宠物不存在 ID=" + petId));

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在 ID=" + activityId));

        ActivityRecord record = new ActivityRecord();
        record.setPet(pet);
        record.setActivity(activity);

        // 修改：允许描述为空
        record.setActivityDescription(description); // 如果description为null，这里会设置为null

        record.setActivityDate(date != null ? date : LocalDateTime.now());

        ActivityRecord savedRecord = activityRecordRepository.save(record);
        log.info("创建活动记录成功，记录ID: {}", savedRecord.getActivityRecordId());

        // 新增：检查并更新定时活动的提醒
        updateFixedActivityReminder(petId, activityId, savedRecord.getActivityDate());

        return savedRecord;
    }

    /**
     * 更新定时活动的提醒日期
     */
    private void updateFixedActivityReminder(Long petId, Long activityId, LocalDateTime activityDate) {
        try {
            // 查找是否存在对应的定时活动
            Optional<FixedActivity> fixedActivityOpt = fixedActivityRepository.findByPetIdAndActivityId(petId, activityId);

            if (fixedActivityOpt.isPresent()) {
                FixedActivity fixedActivity = fixedActivityOpt.get();
                LocalDate currentReminderDate = activityDate.toLocalDate();
                LocalDate newReminderDate = currentReminderDate.plusDays(fixedActivity.getGapTime());

                log.debug("找到定时活动，固定活动ID: {}, 活动ID: {}, 宠物ID: {}, 间隔天数: {}, 新提醒日期: {}",
                        fixedActivity.getFixedActivityId(), activityId, petId, fixedActivity.getGapTime(), newReminderDate);

                // 修改这里：加上 petId 查询条件
                List<ActivityReminder> reminders = activityReminderRepository.findByActivityIdAndTypeAndPetId(activityId, 1, petId);

                if (!reminders.isEmpty()) {
                    // 通常每个活动在每个宠物上应该只有一个type=1的提醒记录
                    ActivityReminder reminder = reminders.getFirst();

                    // 只有当新日期大于原提醒日期时才更新
                    if (newReminderDate.isAfter(reminder.getReminderDate())) {
                        log.info("更新活动提醒，原日期: {}, 新日期: {}, 活动ID: {}, 宠物ID: {}, 固定活动ID: {}",
                                reminder.getReminderDate(), newReminderDate, activityId, petId, fixedActivity.getFixedActivityId());
                        reminder.setReminderDate(newReminderDate);
                        activityReminderRepository.save(reminder);
                    } else {
                        log.debug("新提醒日期 {} 不大于原日期 {}，不进行更新",
                                newReminderDate, reminder.getReminderDate());
                    }
                } else {
                    // 如果没有找到现有的提醒记录，创建一个新的
                    ActivityReminder newReminder = new ActivityReminder();
                    newReminder.setActivityId(activityId);
                    newReminder.setPetId(petId);
                    newReminder.setReminderDate(newReminderDate);
                    newReminder.setType(1); // 定时活动类型

                    activityReminderRepository.save(newReminder);
                    log.info("创建新的定时活动提醒，活动ID: {}, 宠物ID: {}, 固定活动ID: {}, 提醒日期: {}",
                            activityId, petId, fixedActivity.getFixedActivityId(), newReminderDate);
                }
            } else {
                log.debug("活动ID: {} 在宠物ID: {} 上不是定时活动，无需更新提醒", activityId, petId);
            }
        } catch (Exception e) {
            log.error("更新定时活动提醒失败，活动ID: {}, 宠物ID: {}, 错误: {}",
                    activityId, petId, e.getMessage(), e);
            // 这里不抛出异常，因为主要的活动记录创建已经成功
        }
    }

    @Override
    @Transactional
    public ActivityRecord updateActivityRecord(Long recordId,
                                               Long newActivityId,
                                               String description,
                                               LocalDateTime date) {

        ActivityRecord record = activityRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("活动记录不存在 ID=" + recordId));

        // 检查是否正在迁移中，不允许编辑
        if ("MIGRATING".equals(record.getMigrationStatus())) {
            throw new RuntimeException("该记录正在迁移中，请稍后重试");
        }

        if (newActivityId != null) {
            Activity activity = activityRepository.findById(newActivityId)
                    .orElseThrow(() -> new RuntimeException("活动不存在 ID=" + newActivityId));
            record.setActivity(activity);
        }

        // 修改：允许描述为空，包括空字符串
        if (description != null) {
            record.setActivityDescription(description);
        } else {
            // 如果传入的description为null，设置为null
            record.setActivityDescription(null);
        }

        if (date != null) {
            record.setActivityDate(date);
        }

        return activityRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void deleteActivityCompletely(Long activityId) {

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在 ID=" + activityId));

        // 获取所有相关 record
        List<ActivityRecord> records = activityRecordRepository.findByActivityActivityId(activityId);

        // 先删除所有关联的媒体文件
        for (ActivityRecord record : records) {
            try {
                mediaServiceClient.deleteRelatedFiles("ACTIVITY", record.getActivityRecordId());
                log.debug("删除活动记录 {} 的关联媒体成功", record.getActivityRecordId());
            } catch (Exception e) {
                log.warn("删除活动记录 {} 的关联媒体失败，继续删除活动记录: {}",
                        record.getActivityRecordId(), e.getMessage());
            }
        }

        // 删除所有相关 record
        activityRecordRepository.deleteAll(records);

        // 再删除 activity
        activityRepository.delete(activity);

        log.info("彻底删除 activity={}, 以及所有 {} 条记录及其关联媒体", activityId, records.size());
    }

    /**
     * 将Activity实体转换为ActivityDTO
     */
    private ActivityDTO convertToDTO(Activity activity) {
        ActivityDTO dto = new ActivityDTO();
        dto.setActivityId(activity.getActivityId());
        dto.setActivityName(activity.getActivityName());
        dto.setActivityKindId(activity.getActivityKind().getActivityKindId());
        dto.setActivityKindName(activity.getActivityKind().getActivityKindName());
        dto.setUserId(activity.getUser().getUserId()); // 修改为 userId
        dto.setUserName(activity.getUser().getName()); // 使用User实体的name字段
        dto.setState(activity.getState());

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityRecordDTO> getActivityRecordsByPetIds(List<Long> petIds) {
        log.info("批量获取宠物ID: {} 的活动记录", petIds);

        if (petIds == null || petIds.isEmpty()) {
            return List.of();
        }

        // 查询这些宠物的所有活动记录
        List<ActivityRecord> records = activityRecordRepository.findByPetPetIdIn(petIds);

        return records.stream()
                .map(this::convertToRecordDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateBertResult(Long activityRecordId, Integer bertResult) {
        activityRecordRepository.findById(activityRecordId).ifPresent(record -> {
            record.setBertResult(bertResult);
            activityRecordRepository.save(record);
            log.info("✅ 已更新活动记录 {} 的 BERT 结果为: {}", activityRecordId, bertResult);
        });
    }

    @Override
    public List<ActivityRecordDTO> getAbnormalRecordsByPetId(Long petId) {
        // 找出 bert_result 在 1 到 5 之间的所有记录
        List<ActivityRecord> records = activityRecordRepository
                .findByPet_PetIdAndBertResultBetween(petId, 1, 5);

        return records.stream()
                .map(this::convertToRecordDTO) // 使用你已有的转换方法
                .collect(Collectors.toList());
    }

    // ==================== 冷热分离相关方法 ====================

    /**
     * 查询活动记录（自动路由热/冷数据）
     *
     * 流程：
     * 1. 先查 MySQL 热数据
     * 2. 如果日期范围内有冷数据（MySQL 查不到），再查 HBase
     * 3. 从 HBase 读取后，关联查询 Activity、Pet 等补全信息
     * 4. 合并返回（按日期排序）
     */
    @Override
    @Transactional(readOnly = true)
    public List<ActivityRecordDTO> queryActivityRecords(Long petId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("查询活动记录（自动路由热/冷数据）: petId={}, startDate={}, endDate={}",
                petId, startDate, endDate);

        List<ActivityRecordDTO> result = new java.util.ArrayList<>();

        // 1. 先查 MySQL（热数据 + 可能还未迁移的数据）
        List<ActivityRecord> mysqlRecords = activityRecordRepository
                .findByPetPetIdAndActivityDateBetween(petId, startDate, endDate);

        for (ActivityRecord record : mysqlRecords) {
            result.add(convertToRecordDTO(record));
        }
        log.debug("查询MySQL完成: count={}", mysqlRecords.size());

        // 2. 查询 HBase（冷数据）
        //    如果 MySQL 返回数量少，可能部分数据已迁移到 HBase
        try {
            List<ActivityRecordDTO> hbaseRecords = hBaseColdStorageService
                    .queryByPetIdAndDateRange(petId, startDate, endDate);

            // 合并 HBase 结果（注意去重）
            for (ActivityRecordDTO hbaseRecord : hbaseRecords) {
                boolean existsInMysql = mysqlRecords.stream()
                        .anyMatch(r -> r.getActivityRecordId().equals(hbaseRecord.getActivityRecordId()));
                if (!existsInMysql) {
                    // 从 HBase 读取的数据缺少关联信息，需要补全
                    ActivityRecordDTO enrichedRecord = enrichColdRecord(hbaseRecord);
                    result.add(enrichedRecord);
                }
            }
            log.debug("查询HBase完成: count={}", hbaseRecords.size());
        } catch (Exception e) {
            log.error("查询HBase冷数据失败: petId={}, error={}", petId, e.getMessage(), e);
            // 冷数据查询失败不影响热数据返回
        }

        // 3. 按日期排序
        result.sort((a, b) -> {
            if (a.getActivityDate() == null || b.getActivityDate() == null) return 0;
            return a.getActivityDate().compareTo(b.getActivityDate());
        });

        log.info("查询活动记录完成: petId={}, totalCount={}", petId, result.size());
        return result;
    }

    /**
     * 补全冷数据的关联信息
     *
     * HBase 只存储核心字段（record_id, activity_id, pet_id, desc, date）
     * 其他关联信息需要从 MySQL 补全
     */
    private ActivityRecordDTO enrichColdRecord(ActivityRecordDTO coldRecord) {
        log.debug("补全冷数据关联信息: recordId={}", coldRecord.getActivityRecordId());

        // 补全 activityName, activityKindId, activityKindName
        if (coldRecord.getActivityId() != null) {
            activityRepository.findById(coldRecord.getActivityId())
                    .ifPresent(activity -> {
                        coldRecord.setActivityName(activity.getActivityName());
                        if (activity.getActivityKind() != null) {
                            coldRecord.setActivityKindId(activity.getActivityKind().getActivityKindId());
                            coldRecord.setActivityKindName(activity.getActivityKind().getActivityKindName());
                        }
                    });
        }

        // 补全 petName
        if (coldRecord.getPetId() != null) {
            petRepository.findById(coldRecord.getPetId())
                    .ifPresent(pet -> {
                        coldRecord.setPetName(pet.getName());
                    });
        }

        return coldRecord;
    }

    /**
     * 访问冷数据记录（从 HBase 读取并补全关联信息）
     *
     * 由于迁移后 MySQL 记录被删除，此方法用于：
     * 1. 从 HBase 读取冷数据
     * 2. 补全关联信息后返回
     *
     * 注意：不需要创建临时 MySQL 记录，每次访问都从 HBase 读取 + 补全
     */
    @Override
    @Transactional(readOnly = true)
    public ActivityRecordDTO accessColdRecord(Long activityRecordId, Long petId, LocalDateTime activityDate) {
        log.info("访问冷数据记录: activityRecordId={}, petId={}, activityDate={}",
                activityRecordId, petId, activityDate);

        // 1. 生成 RowKey
        String rowKey = hBaseColdStorageService.generateRowKey(petId, activityDate, activityRecordId);

        // 2. 从 HBase 读取并补全关联信息
        return hBaseColdStorageService.getFromColdStorage(rowKey)
                .map(this::enrichColdRecord)
                .orElseThrow(() -> new RuntimeException("冷数据不存在: activityRecordId=" + activityRecordId));
    }

    /**
     * 获取迁移统计信息
     */
    @Override
    public Object getMigrationStats() {
        return activityColdDataMigrationJob.getMigrationStats();
    }

    /**
     * 将ActivityRecord实体转换为ActivityRecordDTO
     */
    private ActivityRecordDTO convertToRecordDTO(ActivityRecord record) {
        ActivityRecordDTO dto = new ActivityRecordDTO();
        dto.setActivityRecordId(record.getActivityRecordId());
        dto.setActivityId(record.getActivity() != null ? record.getActivity().getActivityId() : null);
        dto.setActivityName(record.getActivity() != null ? record.getActivity().getActivityName() : null);
        dto.setActivityKindId(record.getActivity() != null && record.getActivity().getActivityKind() != null 
                ? record.getActivity().getActivityKind().getActivityKindId() : null);
        dto.setActivityKindName(record.getActivity() != null && record.getActivity().getActivityKind() != null 
                ? record.getActivity().getActivityKind().getActivityKindName() : null);
        dto.setPetId(record.getPet() != null ? record.getPet().getPetId() : null);
        dto.setPetName(record.getPet() != null ? record.getPet().getName() : null);
        dto.setActivityDescription(record.getActivityDescription());
        dto.setActivityDate(record.getActivityDate());
        // 设置 BERT 结果（DTO 内部会自动处理中文转换）
        dto.setBertResult(record.getBertResult());
        return dto;
    }
}