package petcare.example.community_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import petcare.example.community_backend.dto.MomentCreateRequestDTO;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.mapper.MomentMapper;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.service.MomentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 使用 @WebMvcTest 只加载 Controller 层及其依赖（Service和Mapper将被Mock）
@WebMvcTest(MomentController.class)
class MomentControllerTest {

    @Autowired
    private MockMvc mockMvc; // 用于模拟HTTP请求

    @Autowired
    private ObjectMapper objectMapper; // 用于JSON序列化

    @MockBean
    private MomentService momentService; // 模拟 Service 层

    @MockBean
    private MomentMapper momentMapper; // 模拟 Mapper 接口

    private final String BASE_URL = "/api/v1/moments";

    // ------------------------- POST /api/v1/moments 测试 -------------------------

    @Test
    void createMoment_ValidRequest_ReturnsCreated() throws Exception {
        // 准备数据
        MomentCreateRequestDTO requestDTO = new MomentCreateRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setContent("测试动态内容");

        // 假设 PetMoment 实体已使用 @AllArgsConstructor，或者您有一个带所有字段的构造函数
        PetMoment entity = new PetMoment(null, 1L, "测试动态内容", null); // Mock Entity
        PetMoment savedEntity = new PetMoment(1L, 1L, "测试动态内容", LocalDateTime.now());
        MomentResponseDTO responseDTO = new MomentResponseDTO();
        responseDTO.setId(1L);

        // 模拟 Mapper 和 Service 的行为
        when(momentMapper.toEntity(any(MomentCreateRequestDTO.class))).thenReturn(entity);
        when(momentService.createMoment(any(PetMoment.class))).thenReturn(savedEntity);
        when(momentMapper.toResponseDTO(any(PetMoment.class))).thenReturn(responseDTO);

        // 执行 POST 请求
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        // 修复：ObjectMapper.writeValueAsString 拼写错误
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated()) // 验证HTTP状态码 201
                .andExpect(jsonPath("$.id").value(1L)); // 验证返回的JSON体

        // 验证业务方法是否被调用
        verify(momentService, times(1)).createMoment(any(PetMoment.class));
    }

    @Test
    void createMoment_InvalidRequest_ReturnsBadRequest() throws Exception {
        // 准备一个无效的请求（内容为空，违反 @NotBlank 约束）
        MomentCreateRequestDTO invalidRequest = new MomentCreateRequestDTO();
        invalidRequest.setUserId(1L);
        invalidRequest.setContent("");

        // 执行 POST 请求
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        // 修复：ObjectMapper.writeValueAsString 拼写错误
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // 验证HTTP状态码 400

        // 验证 Service 层未被调用
        verify(momentService, never()).createMoment(any(PetMoment.class));
    }

    // ------------------------- GET /api/v1/moments/user/{userId} 测试 -------------------------

    @Test
    void getMomentsByUserId_ReturnsMomentList() throws Exception {
        Long userId = 1L;
        // 准备返回的 DTO 列表
        MomentResponseDTO dto1 = new MomentResponseDTO();
        dto1.setId(101L);
        dto1.setUserId(userId);

        when(momentService.getMomentsByUserId(userId)).thenReturn(List.of(dto1));

        // 执行 GET 请求
        mockMvc.perform(get(BASE_URL + "/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 验证HTTP状态码 200
                .andExpect(jsonPath("$[0].id").value(101L)) // 验证列表中的第一个元素的ID
                .andExpect(jsonPath("$.length()").value(1)); // 验证列表长度

        verify(momentService, times(1)).getMomentsByUserId(userId);
    }

    // ------------------------- DELETE /api/v1/moments/{momentId} 测试 -------------------------

    @Test
    void deleteMoment_ExistingId_ReturnsOk() throws Exception {
        Long momentId = 5L;
        when(momentService.deleteMoment(momentId)).thenReturn(true);

        // 执行 DELETE 请求
        mockMvc.perform(delete(BASE_URL + "/{momentId}", momentId))
                .andExpect(status().isOk()) // 验证HTTP状态码 200
                .andExpect(content().string("删除成功"));

        verify(momentService, times(1)).deleteMoment(momentId);
    }

    @Test
    void deleteMoment_NonExistingId_ReturnsNotFound() throws Exception {
        Long momentId = 999L;
        when(momentService.deleteMoment(momentId)).thenReturn(false);

        // 执行 DELETE 请求
        mockMvc.perform(delete(BASE_URL + "/{momentId}", momentId))
                .andExpect(status().isNotFound()) // 验证HTTP状态码 404
                .andExpect(content().string("动态不存在或删除失败"));

        verify(momentService, times(1)).deleteMoment(momentId);
    }
}