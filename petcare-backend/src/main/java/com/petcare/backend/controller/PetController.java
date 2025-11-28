// PetController.java
package com.petcare.backend.controller;

import com.petcare.backend.dto.request.CreatePetRequest;
import com.petcare.backend.dto.response.PetResponse;
import com.petcare.backend.exception.PetNotFoundException;
import com.petcare.backend.exception.UserNotFoundException;
import com.petcare.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController extends BaseController {

    private final UserService userService;

    @Autowired
    public PetController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 创建宠物
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PetResponse>> createPet(
            @Valid @RequestBody CreatePetRequest createPetRequest) {
        try {
            var pet = userService.createPet(createPetRequest);
            PetResponse response = userService.getPetById(pet.getPetId());
            return created(response);
        } catch (UserNotFoundException e) {
            return error(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("创建宠物失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 根据ID获取宠物信息
     */
    @GetMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetResponse>> getPetById(@PathVariable Long petId) {
        try {
            PetResponse pet = userService.getPetById(petId);
            return success(pet);
        } catch (PetNotFoundException e) {
            return error(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("获取宠物信息失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 根据用户ID获取宠物列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PetResponse>>> getPetsByUserId(@PathVariable Long userId) {
        try {
            List<PetResponse> pets = userService.getPetsByUserId(userId);
            return success(pets);
        } catch (UserNotFoundException e) {
            return error(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("获取用户宠物列表失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}