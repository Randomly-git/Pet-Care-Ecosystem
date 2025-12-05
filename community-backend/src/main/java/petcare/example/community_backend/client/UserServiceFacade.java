package petcare.example.community_backend.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import petcare.example.community_backend.client.dto.ApiResponse;
import petcare.example.community_backend.client.dto.UserResponseDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户微服务的远程调用客户端（Facade）。
 * 负责批量获取用户昵称和头像等信息，用于在社区动态和评论中进行数据聚合。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceFacade {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // 用于解析 JSON 响应体

    @Value("${app.user-service.url}") // 从配置文件中读取用户服务地址
    private String userServiceUrl;

    /**
     * 批量查询用户信息。
     * 假设用户微服务提供一个 GET /api/v1/users/batch?ids=1,2,3 的接口。
     *
     * @param userIds 需要查询的用户ID集合。
     * @return Map<UserId, UserResponseDTO>，如果查询失败返回空Map。
     */
    public Map<Long, UserResponseDTO> batchGetUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 构造 API URL
        String idsString = userIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String url = String.format("%s/api/v1/users/batch?ids=%s", userServiceUrl, idsString);

        try {
            // 2. 发送 GET 请求 (假设响应结构为 ApiResponse<List<UserResponseDTO>>)
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // 3. 检查状态码并解析响应体
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse<List<UserResponseDTO>> apiResponse = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<ApiResponse<List<UserResponseDTO>>>() {} // TypeReference 用于处理泛型
                );

                // 4. 将 List<UserResponseDTO> 转换为 Map<UserId, UserResponseDTO>
                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    return apiResponse.getData().stream()
                            // Function.identity() 表示使用 DTO 本身作为值
                            .collect(Collectors.toMap(UserResponseDTO::getId, Function.identity()));
                }
            }
        } catch (Exception e) {
            // 记录错误，但不抛出，进行降级处理
            log.error("❌ 调用用户服务批量查询用户失败. IDs: {}", idsString, e);
            // 降级：返回空 Map，以便业务逻辑可以使用默认值（如“未知用户”）继续运行
            return Collections.emptyMap();
        }

        return Collections.emptyMap();
    }
}