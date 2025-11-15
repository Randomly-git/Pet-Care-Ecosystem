package com.petcare.backend.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class CreatePetRequest {
    @NotBlank(message = "宠物名称不能为空")
    @Size(max = 100, message = "宠物名称长度不能超过100个字符")
    private String name;

    @NotBlank(message = "宠物种类不能为空")
    @Size(max = 50, message = "宠物种类长度不能超过50个字符")
    private String species;

    @Size(max = 100, message = "宠物品种长度不能超过100个字符")
    private String breed;

    private LocalDate birthday;
}