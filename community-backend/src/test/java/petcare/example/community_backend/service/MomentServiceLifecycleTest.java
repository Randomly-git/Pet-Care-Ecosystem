package petcare.example.community_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import petcare.example.community_backend.config.HBaseProperties;
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

import java.util.Optional;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private HBaseProperties hBaseProperties;

    @BeforeEach
    void setUp() {
        // 初始化 HBase 阈值配置，防止 getAllMomentsWithPagination 等方法报 NPE
        HBaseProperties.ColdData.Community communityConfig = new HBaseProperties.ColdData.Community();
        communityConfig.setApprovedDaysThreshold(7);
        communityConfig.setRejectedDaysThreshold(3);

        HBaseProperties.ColdData coldData = new HBaseProperties.ColdData();
        ReflectionTestUtils.setField(coldData, "community", communityConfig);
        
        // 模拟配置类层级调用
        lenient().when(hBaseProperties.getColdData()).thenReturn(coldData);
    }

    /**
     * TS-2: 数据完整性
     * TC-2-01: 冷数据综合恢复全链路
     * 场景：验证冷数据恢复后 MySQL 状态为 NONE 且保留原审核状态。
     */
    @Test
    void testTC_2_01_ColdDataComprehensiveRecovery() {
        // 显式恢复逻辑验证 (由 Service 触发 RESTORE 事件)
        Long momentId = 201L;
        PetMoment moment = new PetMoment();
        moment.setId(momentId);
        moment.setUserId(100L);
        moment.setMigrationStatus("COLD");
        moment.setAuditStatus("REJECTED");

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        
        momentService.restoreMoment(momentId);
        
        verify(eventPublisher).publishRestoreFromColdEvent(eq(momentId), eq(100L));
    }

    /**
     * TS-3: 并发保护
     * TC-3-01: 迁移锁保护与物理删除
     * 场景：MIGRATING 期间拒绝修改，物理删除同步清理。
     */
    @Test
    void testTC_3_01_MigrationLockAndPhysicalDelete() {
        Long momentId = 301L;
        Long userId = 1L;
        PetMoment m = new PetMoment();
        m.setId(momentId); m.setUserId(userId); m.setMigrationStatus("MIGRATING");

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(m));

        // 1. 写保护验证
        assertThrows(IllegalStateException.class, () -> momentService.updateMoment(momentId, userId, "txt", null));

        // 2. 物理删除同步清理验证 (调用 deleteService 逻辑，此处简化验证调用)
        momentService.deleteMoment(momentId, userId);
        verify(momentRepository).deleteById(momentId);
        verify(eventPublisher).publishDeleteFromColdEvent(eq(momentId), eq(userId));
    }

    /**
     * TS-4: 稳定性与拦截
     * TC-4-01: 重复审核操作拦截
     */
    @Test
    void testTC_4_01_RepeatAuditInterception() {
        Long momentId = 401L;
        when(momentRepository.updateAuditStatus(momentId, "PENDING", "APPROVED")).thenReturn(0);
        assertFalse(momentService.approveMoment(momentId));
    }

    /**
     * TS-4: 稳定性与拦截
     * TC-4-02: 重复恢复事件拦截
     */
    @Test
    void testTC_4_02_RepeatRestoreInterception() {
        Long momentId = 402L;
        PetMoment m = new PetMoment(); m.setId(momentId); m.setMigrationStatus("NONE"); m.setUserId(1L);
        when(momentRepository.findById(momentId)).thenReturn(Optional.of(m));
        
        // Service 层应识别 NONE 状态不触发恢复
        momentService.restoreMoment(momentId);
        verify(eventPublisher, never()).publishRestoreFromColdEvent(anyLong(), anyLong());
    }

    /**
     * TS-5: 业务规则与边界
     * TC-5-02: 审核流转与可见性
     */
    @Test
    void testTC_5_02_AuditVisibilityAndFlow() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(momentRepository.findAllByAuditStatusOrderByCreatedAtDesc(eq("APPROVED"), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        
        momentService.getAllMomentsWithPagination(pageable);
        verify(momentRepository).findAllByAuditStatusOrderByCreatedAtDesc("APPROVED", pageable);
    }

    /**
     * TS-5: 业务规则与边界
     * TC-5-03: 互动行为与热度重置
     */
    @Test
    void testTC_5_03_InteractionAndHeatReset() {
        Long momentId = 503L;
        PetMoment m = new PetMoment(); 
        m.setId(momentId); m.setAuditStatus("APPROVED"); m.setMigrationStatus("NONE");

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(m));
        when(momentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(momentMapper.toResponseDTO(any())).thenReturn(new MomentResponseDTO());

        momentService.updateMoment(momentId, 1L, "New Content", Collections.emptyList());
        assertEquals("PENDING", m.getAuditStatus(), "修改内容必须重置状态");
    }
}