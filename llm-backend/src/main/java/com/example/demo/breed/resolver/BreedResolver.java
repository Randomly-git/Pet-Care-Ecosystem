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
        if (catBreedRepository.count() > 0) return;
        log.info("初始化猫品种数据...");
        String[][] breeds = {
            {"暹罗猫","体型修长性格活泼","蓝眼睛、重点色、聪明、叫声大"},
            {"布偶猫","体型大性格温顺毛发长","蓝眼睛、长毛、粘人、温顺"},
            {"狮子猫","中国本土长毛猫","长毛、体型大、性格温和"},
            {"孟加拉豹猫","野性外观花纹独特","豹纹、活泼、聪明、好动"},
            {"缅因猫","体型最大的家猫","体型巨大、长毛、友好、聪明"},
            {"孟买猫","黑豹般的短毛猫","纯黑、铜色眼睛、肌肉发达"},
            {"加菲猫","扁脸短毛猫","扁脸、短鼻、性格安静"},
            {"英短蓝白猫","经典蓝白配色短毛猫","圆脸、蓝白色、性格温和、易胖"},
            {"安哥拉猫","优雅长毛猫","长毛、白色常见、优雅、聪明"},
            {"挪威森林猫","北欧大型长毛猫","长毛、体型大、适应力强、友好"},
            {"波斯猫","扁脸长毛传统品种","扁脸、长毛、安静、需勤梳理"},
            {"无毛猫","皮肤无毛独特外观","无毛、体温高、性格友善、粘人"},
            {"苏格兰折耳猫","耳朵向前折叠的猫","折耳、圆脸、温和、有关节遗传病"},
            {"德文猫","卷毛大耳精灵猫","卷毛、大耳朵、活泼、粘人"},
            {"金吉拉","银色长毛猫","长毛、银白色、安静、优雅"},
            {"阿比西尼亚猫","短毛优雅活跃猫","短毛、红褐色、优雅、活泼"},
            {"橘猫","常见的橙色猫","橘色、易胖、性格温顺、贪吃"},
            {"狸花猫","中国经典斑纹猫","虎斑纹、聪明、独立、捕鼠强"},
            {"喜马拉雅猫","重点色长毛猫","重点色、蓝眼睛、长毛、安静"},
            {"日本短尾猫","短尾猫品种","短尾、活泼、聪明、好运象征"},
            {"柯尼斯卷毛猫","极短卷毛猫","卷毛、大耳朵、修长、活泼"},
            {"东方短毛猫","纤细修长猫","修长、大耳朵、活泼、话多"},
            {"英短金渐层","金色渐变英短","金色渐变、圆脸、短毛、温和"},
            {"沙特尔猫","法国古老蓝猫","蓝灰色、铜色眼、安静、温和"},
            {"楼猫","韩国自然猫","中型、性格温和、适应力强"},
            {"埃及猫","古埃及斑点猫","斑点、优雅、聪明、活泼"},
            {"哈瓦那褐猫","棕色短毛猫","棕色、绿眼睛、修长、温和"},
            {"科拉特猫","泰国幸运猫","银蓝色、绿眼睛、聪明、温和"},
            {"三花猫","三色花猫","黑白橘三色、多为母猫"},
            {"拉邦猫","卷毛猫品种","卷毛、活泼、友好、聪明"},
            {"马恩岛猫","无尾猫品种","无尾或短尾、圆润、聪明"},
            {"欧西猫","斑点家猫","斑点花纹、短毛、体型中等"},
            {"美国短尾猫","短尾猫","短尾、健壮、性格温和"},
            {"美国卷耳猫","卷耳猫","耳朵向后卷、活泼、友好"},
            {"巴厘猫","长毛暹罗猫","长毛、重点色、蓝眼睛、优雅"},
            {"曼基康矮脚猫","短腿猫","短腿、体型小、活泼、可爱"},
            {"米努特猫","矮脚长毛猫","短腿、长毛、圆脸、温和"},
            {"英国长毛猫","英短的长毛版","长毛、圆脸、温和、安静"},
            {"英短银渐层","银色渐变英短","银色渐变、圆脸、短毛、温和"},
            {"缅甸猫","短毛猫","短毛、金色眼睛、温和、亲近人"},
            {"美短起司猫","经典美短花纹","美短纹、活泼、健康、适应力强"},
            {"虎斑猫","经典条纹猫","条纹、活泼、聪明、独立"},
            {"英短乳白","乳白色英短","乳白色、圆脸、短毛、温和"},
            {"高地立耳猫","长毛立耳猫","长毛、立耳、温和、安静"},
            {"英短三花","三花色英短","三花色、圆脸、短毛"},
            {"英短虎斑","虎斑色英短","虎斑色、圆脸、短毛"},
            {"英短纯黑","纯黑色英短","纯黑、圆脸、短毛、铜色眼"},
            {"伯曼猫","重点色长毛猫","重点色、白手套、蓝眼睛、温和"},
            {"俄罗斯蓝猫","银蓝色短毛猫","银蓝色、绿眼睛、优雅、安静"},
            {"索马里猫","长毛阿比西尼亚","长毛、红褐色、活泼、聪明"},
            {"西伯利亚猫","俄罗斯大型长毛猫","长毛、体型大、低致敏性、友好"},
            {"新加坡猫","体型最小的猫","体型极小、大眼睛、活泼"},
            {"东奇尼猫","暹罗和缅甸杂交","短毛、水蓝色眼睛、活泼"},
            {"土耳其梵猫","喜欢水的猫","半长毛、白色为主、喜欢水"},
            {"黑足猫","非洲小型野猫","体型小、黑足、夜行性"},
            {"曼赤肯猫","短腿猫","短腿、活泼、体型小"},
            {"简州猫","中国四耳猫","中国本土、聪明、捕鼠能力强"},
            {"澳大利亚雾猫","斑点短毛猫","短毛、斑点、性格温和"},
            {"呵叻猫","泰国银蓝猫","银蓝色、绿眼睛、聪明"},
            {"奶牛猫","黑白双色猫","黑白双色、活泼、聪明"},
            {"金猫","亚洲金猫","稀有、金色皮毛"},
            {"沙猫","沙漠小型猫","适应沙漠、体型小"},
            {"渔猫","擅长捕鱼猫","半水生、擅长捕鱼"},
            {"云猫","云纹猫","云状花纹、稀有"},
            {"中华田园猫","中国本土家猫","体质强健、独立、聪明、捕鼠强"}
        };
        for (String[] b : breeds) {
            add(b[0], b[1], b[2]);
        }
        log.info("猫品种数据初始化完成，共{}种", catBreedRepository.count());
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
