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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * API: PUT /api/media/associate/batch
     * * @param mediaIds 前端预上传后返回的媒体文件ID列表
     * @param relatedType 业务类型 (例如: "MOMENT")
     * @param newRelatedId 新的关联业务 ID (例如: Moment ID)
     * @return 成功关联的文件数量 (业务层面)
     */
    public int batchUpdateRelatedId(List<Long> mediaIds, String relatedType, Long newRelatedId) {
        String url = String.format("%s/api/media/associate/batch", mediaServiceUrl);

        // 创建请求体 DTO
        MediaBatchUpdateRequest requestBody = new MediaBatchUpdateRequest(mediaIds, relatedType, newRelatedId);

        try {
            // 1. 设置请求头为 application/json
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MediaBatchUpdateRequest> requestEntity = new HttpEntity<>(requestBody, headers);

            // 2. 发送 PUT 请求 (更新操作)
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class
            );

            // 3. 检查 HTTP 状态码
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("❌ 远程调用媒体服务批量关联失败。URL: {}, Status: {}, Body: {}", url, response.getStatusCode(), response.getBody());
                // 抛出异常，触发 MomentService 事务回滚
                throw new RuntimeException("媒体文件批量关联失败，HTTP 状态码: " + response.getStatusCode());
            }

            // 4. 解析业务响应。假设返回 ApiResponse<Integer>，其中 Integer 为成功更新数量
            ApiResponse<Integer> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<Integer>>() {}
            );

            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                log.info("✅ 成功关联 {} 个媒体文件. Type: {}, ID: {}", apiResponse.getData(), relatedType, newRelatedId);
                return apiResponse.getData();
            } else {
                log.error("❌ 媒体服务业务返回批量关联失败. Message: {}", apiResponse.getMessage());
                throw new RuntimeException("媒体文件批量关联业务失败: " + apiResponse.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ 调用媒体服务批量关联文件发生异常. Type: {}, ID: {}", relatedType, newRelatedId, e);
            // 抛出异常，触发 MomentService 事务回滚
            throw new RuntimeException("媒体文件批量关联远程调用异常: " + e.getMessage(), e);
        }
    }


    /**
     * 调用媒体微服务 API，删除与特定业务实体关联的所有文件。
     * API: DELETE /api/media/related/{relatedType}/{relatedId}
     *
     * @param relatedType 业务类型 (例如: "MOMENT")
     * @param relatedId 业务 ID (例如: Moment ID)
     * @return 删除是否成功 (业务层面)
     */
    public boolean deleteRelatedFiles(String relatedType, Long relatedId) {
        String url = String.format("%s/api/media/related/%s/%d",
                mediaServiceUrl, relatedType, relatedId);

        try {
            // 使用 exchange 方法发送 DELETE 请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, null, String.class);

            // 1. 检查 HTTP 状态码
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("❌ 远程调用媒体服务删除失败。URL: {}, Status: {}", url, response.getStatusCode());
                return false;
            }

            // 2. 解析业务响应
            ApiResponse<Void> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<Void>>() {}
            );

            // 3. 检查业务响应是否成功
            if (apiResponse.isSuccess()) {
                log.info("✅ 成功删除关联媒体文件. Type: {}, ID: {}", relatedType, relatedId);
                return true;
            } else {
                log.warn("⚠️ 媒体服务业务返回删除失败. Message: {}", apiResponse.getMessage());
                return false;
            }

        } catch (Exception e) {
            log.error("❌ 调用媒体服务删除文件发生异常. Type: {}, ID: {}", relatedType, relatedId, e);
            // 远程服务宕机、网络问题或 JSON 解析异常
            return false;
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
            // 降级：返回空 Map
            return Collections.emptyMap();
        }
        return Collections.emptyMap();
    }
}