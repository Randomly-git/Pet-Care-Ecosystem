package com.petcare.backend.service.impl;

import com.petcare.backend.dto.request.CreatePetRequest;
import com.petcare.backend.dto.request.LoginRequest;
import com.petcare.backend.dto.request.RegisterRequest;
import com.petcare.backend.dto.response.LoginResponse;
import com.petcare.backend.dto.response.PetResponse;
import com.petcare.backend.dto.response.RegisterResponse;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.User;
import com.petcare.backend.exception.InvalidCredentialsException;
import com.petcare.backend.exception.PetNotFoundException;
import com.petcare.backend.exception.UserAlreadyExistsException;
import com.petcare.backend.exception.UserNotFoundException;
import com.petcare.backend.repository.*;
import com.petcare.backend.service.UserService;
import com.petcare.backend.util.PasswordUtil;
import com.petcare.backend.util.JwtTokenUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final StatusRepository statusRepository;
    private final ActivityRepository activityRepository;
    private final StatusRecordRepository statusRecordRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final PasswordUtil passwordUtil;
    private final JwtTokenUtil jwtTokenUtil;

    public UserServiceImpl(PetRepository petRepository,
                           UserRepository userRepository,
                           StatusRepository statusRepository,
                           ActivityRepository activityRepository,
                           StatusRecordRepository statusRecordRepository,
                           ActivityRecordRepository activityRecordRepository,
                           PasswordUtil passwordUtil,
                           JwtTokenUtil jwtTokenUtil) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
        this.activityRepository = activityRepository;
        this.statusRecordRepository = statusRecordRepository;
        this.activityRecordRepository = activityRecordRepository;
        this.passwordUtil = passwordUtil;
        this.jwtTokenUtil = jwtTokenUtil;
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

        Pet savedPet = petRepository.save(pet);
        log.info("宠物创建成功, ID: {}", savedPet.getPetId());

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

        return RegisterResponse.success(savedUser.getUserId(), savedUser.getName());
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

        return LoginResponse.success(user.getUserId(), user.getName(), jwtToken);
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

        // 设置用户信息
        if (pet.getUser() != null) {
            response.setUserId(pet.getUser().getUserId());
            response.setUserName(pet.getUser().getName());
        }

        // 添加统计信息
        Long statusRecordCount = statusRecordRepository.countByPetPetId(pet.getPetId());
        Long activityRecordCount = activityRecordRepository.countByPetPetId(pet.getPetId());

        response.setStatusRecordCount(statusRecordCount);
        response.setActivityRecordCount(activityRecordCount);

        return response;
    }
}