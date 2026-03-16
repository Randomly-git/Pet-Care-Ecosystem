package com.petcare.backend.service.impl;

import com.petcare.backend.dto.request.CreatePetRequest;
import com.petcare.backend.dto.request.LoginRequest;
import com.petcare.backend.dto.request.RegisterRequest;
import com.petcare.backend.dto.response.LoginResponse;
import com.petcare.backend.dto.response.PetResponse;
import com.petcare.backend.dto.response.RegisterResponse;
import com.petcare.backend.dto.request.CreateActivityDTO;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.User;
import com.petcare.backend.exception.InvalidCredentialsException;
import com.petcare.backend.exception.PetNotFoundException;
import com.petcare.backend.exception.UserAlreadyExistsException;
import com.petcare.backend.exception.UserNotFoundException;
import com.petcare.backend.repository.*;
import com.petcare.backend.service.ActivityService;
import com.petcare.backend.service.StatusService;
import com.petcare.backend.service.UserService;
import com.petcare.backend.util.PasswordUtil;
import com.petcare.backend.util.JwtTokenUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final PasswordUtil passwordUtil;
    private final JwtTokenUtil jwtTokenUtil;
    private final ActivityService activityService;
    private final StatusService statusService;

    public UserServiceImpl(PetRepository petRepository,
                           UserRepository userRepository,
                           ActivityRecordRepository activityRecordRepository,
                           PasswordUtil passwordUtil,
                           JwtTokenUtil jwtTokenUtil,
                           ActivityService activityService,
                           StatusService statusService) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.activityRecordRepository = activityRecordRepository;
        this.passwordUtil = passwordUtil;
        this.jwtTokenUtil = jwtTokenUtil;
        this.activityService = activityService;
        this.statusService = statusService;
    }

    @Override
    public Pet createPet(CreatePetRequest createPetRequest) {
        log.info("创建新宠物: {}", createPetRequest.getName());

        // 验证用户存在
        User user = userRepository.findById(createPetRequest.getUserId())
                .orElseThrow(() -> new UserNotFoundException(createPetRequest.getUserId()));

        Pet pet = new Pet();
        BeanUtils.copyProperties(createPetRequest, pet);
        pet.setUser(user); // 设置用户关联

        // 处理gender属性（如果CreatePetRequest中有）
        // 注意：如果CreatePetRequest中没有gender字段，这里需要单独设置或确保DTO中有该字段

        Pet savedPet = petRepository.save(pet);
        log.info("宠物创建成功, ID: {}, 性别: {}", savedPet.getPetId(),
                savedPet.getGender() != null ? (savedPet.getGender() ? "公" : "母") : "未知");

        return savedPet;
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponse getPetById(Long petId) {
        log.info("查询宠物信息, ID: {}", petId);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException(petId));

        return convertToPetResponse(pet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> getAllPets() {
        log.info("查询所有宠物信息");

        List<Pet> pets = petRepository.findAll();

        return pets.stream()
                .map(this::convertToPetResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> getPetsByUserId(Long userId) {
        log.info("查询用户ID: {} 的所有宠物", userId);

        // 验证用户存在
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        List<Pet> pets = petRepository.findByUserUserId(userId);

        return pets.stream()
                .map(this::convertToPetResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Pet getPetEntityById(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException(petId));
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        log.info("用户注册: {}", registerRequest.getName());

        // 检查用户名是否已存在
        if (userRepository.existsByName(registerRequest.getName())) {
            throw new UserAlreadyExistsException(registerRequest.getName());
        }

        // 创建新用户
        User user = new User();
        user.setName(registerRequest.getName());
        user.setPasswordHash(passwordUtil.encodePassword(registerRequest.getPassword()));

        User savedUser = userRepository.save(user);
        log.info("用户注册成功, ID: {}", savedUser.getUserId());

        // 创建默认的 Activity
        createDefaultActivities(savedUser.getUserId());
        // 注：status 现在与 pet 关联，创建宠物时再创建默认状态

        return RegisterResponse.success(savedUser.getUserId(), savedUser.getName());
    }

    /**
     * 为用户创建默认活动
     */
    private void createDefaultActivities(Long userId) {
        log.info("为用户 ID: {} 创建默认活动", userId);

        // 默认活动列表：ActivityKindId -> 活动名称列表
        List<DefaultActivity> defaultActivities = Arrays.asList(
                new DefaultActivity(1L, Arrays.asList("吃主粮", "吃零食", "喝水")),
                new DefaultActivity(2L, Arrays.asList("与主人互动", "被主人训斥", "与陌生人互动")),
                new DefaultActivity(3L, Arrays.asList("剪指甲", "洗澡", "清理居所")),
                new DefaultActivity(4L, Arrays.asList("散步", "去公园", "接触其他宠物", "接触流浪/野生动物")),
                new DefaultActivity(5L, Arrays.asList("跑跳", "游泳", "玩球", "取物训练", "障碍训练")),
                new DefaultActivity(7L, Arrays.asList("发情", "交配", "生产", "哺育幼崽")),
                new DefaultActivity(8L, Arrays.asList("攻击人类", "攻击宠物", "破坏物品", "逃跑", "拒食", "持续吠叫/嚎叫", "异常叫声", "异常舔毛", "焦虑", "呕吐", "异常排泄")),
                new DefaultActivity(6L, Arrays.asList("打疫苗", "驱虫", "体检", "手术", "绝育", "美容"))
        );

        for (DefaultActivity defaultActivity : defaultActivities) {
            Long activityKindId = defaultActivity.getActivityKindId();
            for (String activityName : defaultActivity.getActivityNames()) {
                CreateActivityDTO createActivityDTO = new CreateActivityDTO();
                createActivityDTO.setActivityName(activityName);
                createActivityDTO.setActivityKindId(activityKindId);
                createActivityDTO.setUserId(userId);

                try {
                    activityService.createActivity(createActivityDTO);
                    log.debug("创建默认活动: {} - {}", activityKindId, activityName);
                } catch (Exception e) {
                    log.error("创建默认活动失败: {} - {}, 错误: {}", activityKindId, activityName, e.getMessage());
                }
            }
        }
        log.info("为用户 ID: {} 创建默认活动完成", userId);
    }

    /**
     * 为用户创建默认状态
     */
    private void createDefaultStatuses(Long userId) {
        log.info("为用户 ID: {} 创建默认状态", userId);

        List<String> defaultStatusNames = Arrays.asList(
                "主粮", "零食", "水源", "地理位置", "居所概况", "家庭成员", "疾病", "受伤", "怀孕"
        );

        for (String statusName : defaultStatusNames) {
            try {
                statusService.createStatus(userId, statusName);
                log.debug("创建默认状态: {}", statusName);
            } catch (Exception e) {
                log.error("创建默认状态失败: {}, 错误: {}", statusName, e.getMessage());
            }
        }
        log.info("为用户 ID: {} 创建默认状态完成", userId);
    }

    /**
     * 内部类，用于存储默认活动信息
     */
    private static class DefaultActivity {
        private Long activityKindId;
        private List<String> activityNames;

        public DefaultActivity(Long activityKindId, List<String> activityNames) {
            this.activityKindId = activityKindId;
            this.activityNames = activityNames;
        }

        public Long getActivityKindId() {
            return activityKindId;
        }

        public List<String> getActivityNames() {
            return activityNames;
        }
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByName(loginRequest.getName())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordUtil.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // 使用JWT生成token
        String jwtToken = jwtTokenUtil.generateToken(user.getUserId(), user.getName());

        String nickname = user.getNickname() != null && !user.getNickname().trim().isEmpty()
                ? user.getNickname() : user.getName();
        return LoginResponse.success(user.getUserId(), user.getName(), nickname, jwtToken);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * 将 Pet 实体转换为 PetResponse DTO
     */
    private PetResponse convertToPetResponse(Pet pet) {
        PetResponse response = new PetResponse();

        // 复制基本属性
        response.setPetId(pet.getPetId());
        response.setName(pet.getName());
        response.setSpecies(pet.getSpecies());
        response.setBreed(pet.getBreed());
        response.setBirthday(pet.getBirthday());
        response.setCreatedAt(pet.getCreatedAt());
        // 添加gender属性
        response.setGender(pet.getGender());
        // 添加gender的文字描述
        response.setGenderText(pet.getGender() != null ? (pet.getGender() ? "公" : "母") : "未知");

        // 设置用户信息
        if (pet.getUser() != null) {
            response.setUserId(pet.getUser().getUserId());
            response.setUserName(pet.getUser().getName());
        }

        // 添加统计信息
        // 状态现在直接存储在 status 表中（每个宠物有多个状态类型，每个状态有当前值）
        Long statusCount = statusService.getStatusCountByPetId(pet.getPetId());
        Long activityRecordCount = activityRecordRepository.countByPetPetId(pet.getPetId());

        response.setStatusRecordCount(statusCount);
        response.setActivityRecordCount(activityRecordCount);

        return response;
    }

    @Override
    public List<User> batchGetUsersByIds(Set<Long> userIds) {
        return userRepository.findAllById(userIds);
    }

    @Override
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }
}