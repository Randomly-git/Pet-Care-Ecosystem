package petcare.example.community_backend.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import petcare.example.community_backend.client.dto.ApiResponse;
import petcare.example.community_backend.client.dto.MediaResponse;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 媒体微服务的远程调用客户端（Facade）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MediaServiceFacade {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.media-service.url}")
    private String mediaServiceUrl;

    // ----------------------------------------------------------------------
    // 内部 DTO：用于批量关联请求体，匹配媒体微服务的 API 接口
    // ----------------------------------------------------------------------
    private static class MediaBatchUpdateRequest {
        public List<Long> mediaIds;
        public String relatedType;
        public Long relatedId;

        public MediaBatchUpdateRequest(List<Long> mediaIds, String relatedType, Long relatedId) {
            this.mediaIds = mediaIds;
            this.relatedType = relatedType;
            this.relatedId = relatedId;
        }
    }
    // ----------------------------------------------------------------------

    /**
     * 获取与特定业务实体关联的所有文件URL。
     * API: GET /api/media/related/{relatedType}/{relatedId}
     */
    public List<String> getMediaUrlsByRelated(String relatedType, Long relatedId) {
        // 构建 API 调用的完整 URL
        String url = String.format("%s/api/media/related/%s/%d",
                mediaServiceUrl, relatedType, relatedId);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("❌ 远程调用媒体服务失败。URL: {}, Status: {}", url, response.getStatusCode());
                return Collections.emptyList();
            }

            ApiResponse<List<MediaResponse>> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<List<MediaResponse>>>() {}
            );

            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                return apiResponse.getData().stream()
                        .map(MediaResponse::getFileUrl)
                        .collect(Collectors.toList());
            } else {
                log.warn("⚠️ 媒体服务业务返回失败. Message: {}", apiResponse.getMessage());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("❌ 调用媒体服务获取文件 URL 发生异常. Type: {}, ID: {}", relatedType, relatedId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 批量更新媒体文件的 relatedId，实现文件关联。
     * API: PATCH /api/media/batch/related
     * @param mediaIds 前端预上传后返回的媒体文件ID列表
     * @param relatedType 业务类型 (例如: "MOMENT")
     * @param newRelatedId 新的关联业务 ID (例如: Moment ID)
     * @return 成功关联的文件数量 (业务层面)
     */



    /**
     * 调用媒体微服务 API，删除与特定业务实体关联的所有文件。
     * API: DELETE /api/media/related/{relatedType}/{relatedId}
     *
     * @param relatedType 业务类型 (例如: "MOMENT")
     * @param relatedId 业务 ID (例如: Moment ID)
     * @return 删除是否成功 (业务层面)
     */
    public void deleteRelatedFiles(String relatedType, Long relatedId) {
        String url = String.format("%s/api/media/related/%s/%d", mediaServiceUrl, relatedType, relatedId);
        try {
            restTemplate.delete(url);
        } catch (Exception e) {
            log.error("❌ 删除媒体文件失败: {}", e.getMessage());
        }
    }

    /**
     * @param relatedType 关联类型 (如: MOMENT)
     * @param relatedIds 业务实体 ID 列表 (如: Moment ID 列表)
     * @return Map<RelatedId, List<MediaResponse>> 聚合后的数据
     */
    public Map<Long, List<MediaResponse>> getMediaFilesByRelatedIds(String relatedType, List<Long> relatedIds) {
        if (relatedIds == null || relatedIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 构造 API URL，使用逗号分隔的 ID 列表
        String idsString = relatedIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String url = String.format("%s/api/v1/media/related/batch?relatedType=%s&relatedIds=%s",
                mediaServiceUrl, relatedType, idsString);

        try {
            // 2. 发送 GET 请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // 3. 检查状态码并解析响应体：ApiResponse<List<MediaResponse>>
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse<List<MediaResponse>> apiResponse = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<ApiResponse<List<MediaResponse>>>() {}
                );

                // 4. 将 List<MediaResponse> 转换为 Map<RelatedId, List<MediaResponse>> 方便 MomentService 使用
                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    return apiResponse.getData().stream()
                            .collect(Collectors.groupingBy(MediaResponse::getRelatedId));
                }
            }
        } catch (Exception e) {
            log.error("❌ 调用媒体服务批量查询文件失败. Type: {}, IDs: {}", relatedType, idsString, e);
        }
        return Collections.emptyMap();
    }

    public Map<Long, List<MediaResponse>> batchGetMediaMap(String relatedType, Set<Long> relatedIds) {
        // 使用 Set 作为参数类型更规范
        if (relatedIds == null || relatedIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<MediaResponse>> result = new java.util.HashMap<>();

        // 临时方案：由于媒体服务未提供批量查询API，使用循环调用单个查询API
        // TODO: 等媒体服务提供批量查询API后，改为单次批量调用以提高性能
        for (Long relatedId : relatedIds) {
            try {
                // 构造单个查询的API URL
                String url = String.format("%s/api/media/related/%s/%d",
                        mediaServiceUrl, relatedType, relatedId);

                // 发送 GET 请求
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                // 检查状态码并解析响应体
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    ApiResponse<List<MediaResponse>> apiResponse = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<ApiResponse<List<MediaResponse>>>() {}
                    );

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        result.put(relatedId, apiResponse.getData());
                    }
                }
            } catch (Exception e) {
                // 单个查询失败不影响其他ID，仅记录警告日志
                log.warn("⚠️ 调用媒体服务查询单个文件失败. Type: {}, ID: {}", relatedType, relatedId, e);
            }
        }

        return result;
    }

    /**
     * 批量查询：一次请求获取所有动态的媒体文件
     */
    public Map<Long, List<MediaResponse>> getMediaFilesBatch(String relatedType, List<Long> relatedIds) {
        if (relatedIds == null || relatedIds.isEmpty()) return Collections.emptyMap();

        // 构建 URL: /api/media/batch?relatedType=MOMENT&relatedIds=1,2,3
        String idsParam = relatedIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String url = String.format("%s/api/media/batch?relatedType=%s&relatedIds=%s",
                mediaServiceUrl, relatedType, idsParam);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse<List<MediaResponse>> apiResponse = objectMapper.readValue(
                        response.getBody(), new TypeReference<ApiResponse<List<MediaResponse>>>() {});

                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    // 在客户端按 relatedId 分组
                    return apiResponse.getData().stream()
                            .collect(Collectors.groupingBy(MediaResponse::getRelatedId));
                }
            }
        } catch (Exception e) {
            log.error("❌ 批量调用媒体服务失败: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    public int batchUpdateRelatedId(List<Long> mediaIds, String relatedType, Long newRelatedId) {
        // 1. 将 PATCH 改为 POST (前提是媒体服务的 Controller 也改成了 @PostMapping)
        String url = String.format("%s/api/media/batch/related", mediaServiceUrl);

        Map<String, Object> body = new HashMap<>();
        body.put("mediaIds", mediaIds);
        body.put("relatedType", relatedType);
        body.put("newRelatedId", newRelatedId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 使用 POST 替代 PATCH 解决底层库不支持的问题
            // 使用 ResponseEntity<Map> 方便读取 ApiResponse 里的 data
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Integer code = (Integer) responseBody.get("code");

                // 🚀 修改点 3: 必须判断业务状态码 20000
                if (code != null && code == 20000) {
                    Object data = responseBody.get("data");
                    return data instanceof Integer ? (Integer) data : 0;
                } else {
                    log.error("❌ 媒体服务业务逻辑失败: {}", responseBody.get("message"));
                    throw new RuntimeException("媒体关联业务失败: " + responseBody.get("message"));
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("❌ 关联媒体失败 (网络或参数异常): {}", e.getMessage());
            throw new RuntimeException("媒体关联服务异常", e);
        }
    }

}