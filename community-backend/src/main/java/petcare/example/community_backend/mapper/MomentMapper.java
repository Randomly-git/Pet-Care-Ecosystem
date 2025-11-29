package petcare.example.community_backend.mapper;

import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.dto.MomentResponseDTO;
import petcare.example.community_backend.dto.MomentCreateRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;

/**
 * MapStruct Mapper 接口，用于 PetMoment 实体和 DTO 之间的转换。
 * componentModel = "spring" 确保 Spring Boot 可以自动注入该 Mapper。
 */
@Mapper(componentModel = "spring")
public interface MomentMapper {

    // 获取 MapStruct 的默认实例，虽然 Spring 模式下不是必须的，但可用于非 Spring 环境
    MomentMapper INSTANCE = Mappers.getMapper(MomentMapper.class);

    /**
     * 将 PetMoment 实体转换为 MomentResponseDTO
     * 目标字段名与源字段名相同时，MapStruct 自动处理。
     * 额外的 mediaUrls/commentCount/likeCount 会保持默认值（null/0），需要后续 Service 填充。
     */
    MomentResponseDTO toResponseDTO(PetMoment moment);

    /**
     * 将 MomentCreateRequestDTO 转换为 PetMoment 实体
     * 忽略 id 和 createdAt，由 JPA 自动生成。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PetMoment toEntity(MomentCreateRequestDTO requestDTO);

    /**
     * 将 PetMoment 实体列表转换为 MomentResponseDTO 列表
     */
    List<MomentResponseDTO> toResponseDTOList(List<PetMoment> moments);
}