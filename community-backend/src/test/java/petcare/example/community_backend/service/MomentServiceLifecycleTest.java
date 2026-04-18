package petcare.example.community_backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import petcare.example.community_backend.mapper.MomentMapper;
import petcare.example.community_backend.client.MediaServiceFacade;
import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.service.CommentService;
import petcare.example.community_backend.service.CommunityColdStorageEventPublisher;
import petcare.example.community_backend.service.LikeService;
import petcare.example.community_backend.service.CommunityHBaseColdStorageService;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.repository.PetMomentRepository;
import petcare.example.community_backend.dto.MomentResponseDTO;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MomentServiceLifecycleTest {

    @InjectMocks
    private MomentService momentService;

    @Mock
    private PetMomentRepository momentRepository;
    
    @Mock
    private MomentMapper momentMapper;

    @Mock
    private MediaServiceFacade mediaServiceFacade;

    @Mock
    private CommunityHBaseColdStorageService hBaseService;

    @Mock
    private LikeService likeService;

    @Mock
    private UserServiceFacade userServiceFacade;

    @Mock
    private CommentService commentService;

    @Mock
    private CommunityColdStorageEventPublisher eventPublisher;

    @Test
    void testTC_STT_09_ApproveMoment_Success() {
        Long momentId = 101L;
        when(momentRepository.updateAuditStatus(momentId, "PENDING", "APPROVED")).thenReturn(1);

        boolean result = momentService.approveMoment(momentId);

        assertTrue(result);
        verify(momentRepository).updateAuditStatus(momentId, "PENDING", "APPROVED");
    }

    @Test
    void testTC_STT_10_UpdateMoment_ResetsToPending() {
        Long momentId = 102L;
        Long userId = 1L;
        PetMoment existingMoment = new PetMoment();
        existingMoment.setId(momentId);
        existingMoment.setUserId(userId);
        existingMoment.setAuditStatus("APPROVED"); // 原本是通过状态
        existingMoment.setMigrationStatus("NONE");

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(existingMoment));
        when(momentRepository.save(any(PetMoment.class))).thenAnswer(i -> i.getArgument(0));
        when(momentMapper.toResponseDTO(any())).thenReturn(new MomentResponseDTO());

        momentService.updateMoment(momentId, userId, "New Content", Collections.emptyList());

        // 验证状态是否被重置为 PENDING
        assertEquals("PENDING", existingMoment.getAuditStatus(), "修改内容后审核状态必须重置为 PENDING");
        verify(momentRepository).save(existingMoment);
    }

    @Test
    void testTC_STT_14_PendingMoment_InvisibleInAllMoments() {
        // 模拟全站动态查询，数据库中应该只查 APPROVED 状态
        PageRequest pageable = PageRequest.of(0, 10);
        when(momentRepository.findAllByAuditStatusOrderByCreatedAtDesc(eq("APPROVED"), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        momentService.getAllMomentsWithPagination(pageable);

        // 验证 Repository 调用参数
        verify(momentRepository).findAllByAuditStatusOrderByCreatedAtDesc("APPROVED", pageable);
    }

    @Test
    void testTC_STT_04_UpdateMoment_RejectedWhenMigrating() {
        Long momentId = 103L;
        Long userId = 1L;
        PetMoment migratingMoment = new PetMoment();
        migratingMoment.setId(momentId);
        migratingMoment.setUserId(userId);
        migratingMoment.setMigrationStatus("MIGRATING"); // 正在迁移

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(migratingMoment));

        assertThrows(IllegalStateException.class, () -> {
            momentService.updateMoment(momentId, userId, "Edit content", null);
        }, "正在迁移的动态禁止修改");
    }
}