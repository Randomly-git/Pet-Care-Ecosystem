package petcare.example.community_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import petcare.example.community_backend.dto.MomentCreateRequestDTO;
import petcare.example.community_backend.dto.LikeRequestDTO;
import petcare.example.community_backend.dto.CommentCreateRequestDTO;
import petcare.example.community_backend.dto.FollowRequestDTO;
import petcare.example.community_backend.model.TargetType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 完整的 Spring Boot 应用上下文
@SpringBootTest
// 自动配置 MockMvc 用于 HTTP 请求
@AutoConfigureMockMvc
// 每次测试后回滚事务，确保测试独立性
@Transactional
// 如果您为测试配置了特定的 profile (如 application-test.properties)
@ActiveProfiles("test")
class CommunityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 定义基本 URL
    private final String MOMENT_URL = "/api/v1/moments";
    private final String LIKE_URL = "/api/v1/likes";
    private final String COMMENT_URL = "/api/v1/comments";
    private final String FOLLOW_URL = "/api/v1/follows";

    /**
     * 测试核心用户流：发布动态 -> 点赞 -> 评论 -> 验证计数
     */
    @Test
    void coreUserFlow_MomentLikeComment_ShouldUpdateCounts() throws Exception {
        Long userIdA = 100L; // 作者
        Long userIdB = 200L; // 互动用户

        // --- 1. 用户 A 发布动态 ---
        MomentCreateRequestDTO createMomentRequest = new MomentCreateRequestDTO();
        createMomentRequest.setUserId(userIdA);
        createMomentRequest.setContent("这是用户A的测试动态。");

        String momentResponseJson = mockMvc.perform(post(MOMENT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createMomentRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // 获取新创建的动态ID
        Long momentId = objectMapper.readTree(momentResponseJson).get("id").asLong();

        // --- 2. 用户 B 点赞动态 ---
        LikeRequestDTO likeRequest = new LikeRequestDTO();
        likeRequest.setUserId(userIdB);
        likeRequest.setTargetType(TargetType.MOMENT);
        likeRequest.setTargetId(momentId);

        mockMvc.perform(post(LIKE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(likeRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("点赞成功"));

        // --- 3. 用户 B 评论动态 ---
        CommentCreateRequestDTO commentRequest = new CommentCreateRequestDTO();
        commentRequest.setUserId(userIdB);
        commentRequest.setMomentId(momentId);
        commentRequest.setContent("这是一条评论。");

        mockMvc.perform(post(COMMENT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated());

        // --- 4. 验证动态的点赞数和评论数 ---
        mockMvc.perform(get(MOMENT_URL + "/user/{userId}", userIdA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(momentId))
                .andExpect(jsonPath("$[0].likeCount").value(1))   // 验证点赞数
                .andExpect(jsonPath("$[0].commentCount").value(1)); // 验证评论数
    }

    /**
     * 测试关注和取消关注功能
     */
    @Test
    void followFlow_ShouldToggleFollowStatus() throws Exception {
        Long authorId = 300L; // 被关注者
        Long followerId = 400L; // 关注者

        FollowRequestDTO followRequest = new FollowRequestDTO();
        followRequest.setFollowerId(followerId);
        followRequest.setFollowedId(authorId);

        // --- 1. 用户关注作者 ---
        mockMvc.perform(post(FOLLOW_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(followRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("关注成功"));

        // --- 2. 验证粉丝数 (作者的粉丝数应为 1) ---
        mockMvc.perform(get(FOLLOW_URL + "/followers/count/{userId}", authorId))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        // --- 3. 用户取消关注作者 ---
        mockMvc.perform(post(FOLLOW_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(followRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("取消关注成功"));

        // --- 4. 再次验证粉丝数 (应为 0) ---
        mockMvc.perform(get(FOLLOW_URL + "/followers/count/{userId}", authorId))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}