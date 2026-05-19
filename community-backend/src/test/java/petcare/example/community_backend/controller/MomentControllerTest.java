package petcare.example.community_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.mapper.MomentMapper;
import petcare.example.community_backend.service.MomentService;

import java.util.List;

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
}