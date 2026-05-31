package com.example.demo.breed.controller;

import com.example.demo.breed.entity.CatBreed;
import com.example.demo.breed.repository.CatBreedRepository;
import com.example.demo.common.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/cat")
public class CatBreedController {

    private final LlmService llmService;
    private final CatBreedRepository catBreedRepository;

    public CatBreedController(LlmService llmService, CatBreedRepository catBreedRepository) {
        this.llmService = llmService;
        this.catBreedRepository = catBreedRepository;
    }

    @PostMapping("/identify")
    public ResponseEntity<Map<String, Object>> identify(@RequestParam("image") MultipartFile image) {
        try {
            byte[] bytes = image.getBytes();
            log.info("猫品种识别: 图片大小={}字节", bytes.length);

            String breedsStr = catBreedRepository.findAll().stream()
                    .map(CatBreed::getBreedName)
                    .collect(Collectors.joining("、"));

            String prompt = "这只猫是什么品种？只能从以下列表中选择一个：" + breedsStr
                    + "。如果都不匹配只回答\"未知\"。只输出品种名称不要其他内容。";

            String result = llmService.callQwenVisionRaw(bytes, prompt);

            return ResponseEntity.ok(Map.of("breed", result));
        } catch (Exception e) {
            log.error("猫品种识别失败", e);
            return ResponseEntity.internalServerError().body(Map.of("breed", "未知", "error", e.getMessage()));
        }
    }
}
