package com.example.demo.breed.resolver;

import com.example.demo.breed.dto.BreedResult;
import com.example.demo.breed.entity.CatBreed;
import com.example.demo.breed.repository.CatBreedRepository;
import com.example.demo.common.service.LlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.kickstart.tools.GraphQLQueryResolver;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BreedResolver implements GraphQLQueryResolver {

    private final LlmService llmService;
    private final CatBreedRepository catBreedRepository;
    private final ObjectMapper objectMapper;

    public BreedResolver(LlmService llmService, CatBreedRepository catBreedRepository, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.catBreedRepository = catBreedRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (catBreedRepository.count() == 0) {
            log.info("初始化猫品种数据...");
            add("英国短毛猫", "圆脸大眼，毛发短密，性格温和安静", "体型圆润、脸大、短毛、易胖");
            add("美国短毛猫", "体质强健，性格温和，适应力强", "肌肉发达、花纹美丽、性格稳定");
            add("布偶猫", "体型大，性格温顺，毛发长而柔软", "蓝眼睛、长毛、性格温和、粘人");
            add("暹罗猫", "体型修长，性格活泼，毛发短", "蓝眼睛、重点色、叫声大、聪明");
            add("波斯猫", "扁脸长毛，性格安静优雅", "扁脸、长毛、性格安静、需定期梳理");
            add("缅因猫", "体型最大的家猫品种，性格温和", "体型巨大、长毛、聪明、友好");
            add("苏格兰折耳猫", "耳朵向前折叠，性格甜美", "折耳、圆脸、性格温和、有关节遗传病");
            add("中华田园猫", "体质好，适应力强，聪明", "体质强健、独立、聪明、捕鼠能力强");
            log.info("猫品种数据初始化完成，共{}种", catBreedRepository.count());
        }
    }

    private void add(String name, String desc, String chars) {
        CatBreed b = new CatBreed();
        b.setBreedName(name);
        b.setDescription(desc);
        b.setCharacteristics(chars);
        catBreedRepository.save(b);
    }

    public BreedResult identifyCatBreed(String imageData) {
        log.info("猫品种识别: imageData length={}", imageData != null ? imageData.length() : 0);

        List<CatBreed> all = catBreedRepository.findAll();
        String breedList = all.stream()
                .map(b -> "- " + b.getBreedName() + "：" + b.getDescription())
                .collect(Collectors.joining("\n"));

        String prompt = "请仔细观察这张图片中的猫，从以下品种列表中选择最匹配的一个品种。\n\n"
                + "可识别的猫品种：\n" + breedList + "\n\n"
                + "请以JSON格式返回，包含：\n"
                + "{\n"
                + "  \"breedName\": \"品种名称\",\n"
                + "  \"confidence\": 信心指数(0-1),\n"
                + "  \"reason\": \"判断依据\"\n"
                + "}\n"
                + "如果图片中不是猫，breedName返回'非猫类动物'。\n"
                + "只返回JSON。";

        LlmService.QwenResponse resp = llmService.callQwenVision(prompt, imageData);
        String raw = resp.getContent() != null ? resp.getContent() : "";

        BreedResult result = new BreedResult();
        result.setRawAnalysis(raw);

        try {
            String json = extractJson(raw);
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
            String breedName = root.get("breedName").asText("未知");
            result.setIdentifiedBreed(breedName);
            result.setConfidence(root.has("confidence") ? root.get("confidence").asDouble(0) : 0);

            if (!"非猫类动物".equals(breedName) && !"未知".equals(breedName)) {
                catBreedRepository.findByBreedName(breedName).ifPresent(b -> {
                    result.setDescription(b.getDescription());
                    result.setCharacteristics(b.getCharacteristics());
                });
            }
        } catch (Exception e) {
            log.warn("解析VLM响应失败", e);
            result.setIdentifiedBreed("未知");
            result.setConfidence(0);
        }
        return result;
    }

    private String extractJson(String text) {
        int s = text.indexOf('{');
        int e = text.lastIndexOf('}');
        if (s != -1 && e != -1 && e > s) return text.substring(s, e + 1);
        return "{\"breedName\":\"未知\",\"confidence\":0,\"reason\":\"解析失败\"}";
    }
}
