package petcare.example.community_backend.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.config.HBaseProperties;
import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.repository.CommentRepository;
import petcare.example.community_backend.repository.LikeRepository;
import petcare.example.community_backend.repository.PetMomentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CommunityColdDataMigrationJobTest {

    @InjectMocks
    private CommunityColdDataMigrationJob migrationJob;

    @Mock
    private PetMomentRepository momentRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private CommunityHBaseColdStorageService hBaseService;
    @Mock
    private UserServiceFacade userServiceFacade;
    @Mock
    private HBaseProperties hBaseProperties;
    @Mock
    private TransactionTemplate newTransactionTemplate;
    @Mock
    private EntityManager entityManager;

    private HBaseProperties.ColdData.Community communityConfig;

    @BeforeEach
    void setUp() {
        communityConfig = new HBaseProperties.ColdData.Community();
        communityConfig.setApprovedDaysThreshold(7);
        communityConfig.setRejectedDaysThreshold(3);
        
        HBaseProperties.ColdData coldData = new HBaseProperties.ColdData();
        ReflectionTestUtils.setField(coldData, "community", communityConfig);
        when(hBaseProperties.getColdData()).thenReturn(coldData);

        // 模拟 TransactionTemplate 执行
        when(newTransactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // 注入 EntityManager
        ReflectionTestUtils.setField(migrationJob, "entityManager", entityManager);
    }

    private PetMoment createMoment(Long id, Long userId, String auditStatus, String migrationStatus, LocalDateTime lastAccessTime, int commentCount, LocalDateTime createdAt) {
        PetMoment moment = new PetMoment();
        moment.setId(id);
        moment.setUserId(userId);
        moment.setContent("Test Content for " + id);
        moment.setAuditStatus(auditStatus);
        moment.setMigrationStatus(migrationStatus);
        moment.setLastAccessTime(lastAccessTime);
        moment.setCreatedAt(createdAt);

        List<Comment> comments = new ArrayList<>();
        for (int i = 0; i < commentCount; i++) {
            comments.add(new Comment());
        }
        when(commentRepository.findByMomentIdOrderByCreatedAtAsc(id)).thenReturn(comments);
        return moment;
    }

    /**
     * TS-1: 一致性与原子性
     * TC-1-01: 标准全生命周期成功路径与自愈
     * 场景：验证从 APPROVED 到 COLD 的完整迁移，以及 MIGRATING 状态遗留时的幂等自愈。
     */
    @Test
    void testTC_1_01_StandardMigrationAndIdempotentRecovery() {
        Long momentId = 1L;
        Long userId = 100L;
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "NONE", LocalDateTime.now().minusDays(8), 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING")).thenReturn(1);
        when(hBaseService.exists(userId, momentId)).thenReturn(false).thenReturn(true);
        when(momentRepository.existsById(momentId)).thenReturn(false);

        // 1. 正常迁移验证
        boolean result = migrationJob.migrateSingleMoment(momentId);
        assertTrue(result);

        // 2. 自愈路径验证：模拟残留 MIGRATING 记录
        PetMoment staleMoment = createMoment(momentId, userId, "APPROVED", "MIGRATING", LocalDateTime.now().minusDays(8), 0, LocalDateTime.now().minusDays(10));
        when(momentRepository.findById(momentId)).thenReturn(Optional.of(staleMoment));
        when(hBaseService.exists(userId, momentId)).thenReturn(true); 

        boolean recoveryResult = migrationJob.migrateSingleMoment(momentId);
        assertTrue(recoveryResult, "自愈逻辑应通过 exists 校验并删除 MySQL 数据");
        verify(hBaseService, times(1)).saveArchive(any(), any()); // 标准迁移保存一次，自愈不重复保存
    }

    /**
     * TS-1: 一致性与原子性
     * TC-1-02: MySQL 删除阶段超时回滚
     * 场景：HBase 写入成功但 MySQL 删除时发生死锁/超时，验证状态回滚。
     */
    @Test
    void testTC_1_02_MySqlDeleteTimeout() {
        Long momentId = 5L;
        Long userId = 500L;
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "NONE", LocalDateTime.now().minusDays(8), 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(hBaseService.exists(userId, momentId)).thenReturn(false).thenReturn(true);
        // 模拟删除后记录依然存在 (超时或死锁导致删除未生效)
        when(momentRepository.existsById(momentId)).thenReturn(true); 

        assertThrows(RuntimeException.class, () -> migrationJob.migrateSingleMoment(momentId));
    }

    /**
     * TS-1: 一致性与原子性
     * TC-1-03: 存储基础设施故障回滚
     * 场景：模拟 HBase 连接断开，验证事务全局回滚。
     */
    @Test
    void testTC_1_03_HBaseInfrastructureFailure() {
        Long momentId = 3L;
        PetMoment moment = createMoment(momentId, 300L, "APPROVED", "NONE", LocalDateTime.now().minusDays(8), 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        doThrow(new RuntimeException("HBase IO Exception")).when(hBaseService).saveArchive(any(), any());

        assertThrows(RuntimeException.class, () -> migrationJob.migrateSingleMoment(momentId));
        verify(momentRepository, never()).deleteById(any());
    }

    /**
     * TS-1: 一致性与原子性
     * TC-1-04: 虚假写入成功校验拦截
     * 场景：HBase 写入返回成功但后续 exists 验证失败。
     */
    @Test
    void testTC_1_04_FakeSuccessCheck() {
        Long momentId = 4L;
        PetMoment moment = createMoment(momentId, 400L, "APPROVED", "NONE", LocalDateTime.now().minusDays(8), 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(hBaseService.exists(anyLong(), anyLong())).thenReturn(false); // 写入后检查仍不存在

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> migrationJob.migrateSingleMoment(momentId));
        assertTrue(thrown.getMessage().contains("验证失败"));
    }

    /**
     * TS-5: 业务规则与边界
     * TC-5-01: 迁移准入决策过滤器
     * 场景：验证评论超限(2001)或状态为 PENDING 时静默跳过迁移。
     */
    @Test
    void testTC_5_01_MigrationAdmissionFilter() {
        // 场景 A: 评论数 2001
        PetMoment m1 = createMoment(101L, 1L, "APPROVED", "NONE", LocalDateTime.now().minusDays(10), 2001, LocalDateTime.now());
        when(momentRepository.findById(101L)).thenReturn(Optional.of(m1));
        assertFalse(migrationJob.migrateSingleMoment(101L), "2001条评论应被拦截");

        // 场景 B: 审核中 PENDING
        PetMoment m2 = createMoment(102L, 2L, "PENDING", "NONE", LocalDateTime.now().minusDays(10), 0, LocalDateTime.now());
        when(momentRepository.findById(102L)).thenReturn(Optional.of(m2));
        assertFalse(migrationJob.migrateSingleMoment(102L), "PENDING状态应被拦截");
    }

    /**
     * TS-5: 业务规则与边界
     * TC-5-04: 评论数有效边界测试
     * 场景：验证 0, 1999, 2000 条评论均可正常迁移。
     */
    @Test
    void testTC_5_04_CommentCountBoundaries() {
        int[] boundaries = {0, 1999, 2000};
        for (int count : boundaries) {
            PetMoment m = createMoment(100L + count, 1L, "APPROVED", "NONE", LocalDateTime.now().minusDays(8), count, LocalDateTime.now());
            when(momentRepository.findById(100L + count)).thenReturn(Optional.of(m));
            when(hBaseService.exists(anyLong(), anyLong())).thenReturn(false).thenReturn(true);
            when(momentRepository.existsById(anyLong())).thenReturn(false);
            
            assertTrue(migrationJob.migrateSingleMoment(100L + count), "评论数 " + count + " 应允许迁移");
        }
    }

    /**
     * TS-5: 业务规则与边界
     * TC-5-05A/B: REJECTED 状态有效/无效边界
     */
    @Test
    void testTC_5_05_RejectedStatusBoundaries() {
        // A: 有效 (72h)
        PetMoment m1 = createMoment(201L, 1L, "REJECTED", "NONE", LocalDateTime.now().minusHours(72), 0, LocalDateTime.now());
        when(momentRepository.findById(201L)).thenReturn(Optional.of(m1));
        when(hBaseService.exists(anyLong(), anyLong())).thenReturn(false).thenReturn(true);
        assertTrue(migrationJob.migrateSingleMoment(201L));

        // B: 无效 (71h)
        PetMoment m2 = createMoment(202L, 1L, "REJECTED", "NONE", LocalDateTime.now().minusHours(71), 0, LocalDateTime.now());
        when(momentRepository.findById(202L)).thenReturn(Optional.of(m2));
        assertFalse(migrationJob.migrateSingleMoment(202L), "未满72h不应迁移");
    }

    /**
     * TS-5: 业务规则与边界
     * TC-5-06A/B: APPROVED 状态有效/无效边界
     */
    @Test
    void testTC_5_06_ApprovedStatusBoundaries() {
        // A: 有效 (168h)
        PetMoment m1 = createMoment(301L, 1L, "APPROVED", "NONE", LocalDateTime.now().minusHours(168), 0, LocalDateTime.now());
        when(momentRepository.findById(301L)).thenReturn(Optional.of(m1));
        when(hBaseService.exists(anyLong(), anyLong())).thenReturn(false).thenReturn(true);
        assertTrue(migrationJob.migrateSingleMoment(301L));

        // B: 无效 (167h)
        PetMoment m2 = createMoment(302L, 1L, "APPROVED", "NONE", LocalDateTime.now().minusHours(167), 0, LocalDateTime.now());
        when(momentRepository.findById(302L)).thenReturn(Optional.of(m2));
        assertFalse(migrationJob.migrateSingleMoment(302L), "未满168h不应迁移");
    }

    @Test
    void testExecuteMigration_WithStats() {
        // 测试批量执行逻辑
        PetMoment m1 = new PetMoment(); m1.setId(101L); m1.setMigrationStatus("NONE");
        PetMoment m2 = new PetMoment(); m2.setId(102L); m2.setMigrationStatus("MIGRATING");
        
        when(momentRepository.findEligibleRecordsForMigration(any(), any())).thenReturn(List.of(m1));
        when(momentRepository.findByMigrationStatus("MIGRATING")).thenReturn(List.of(m2));
        
        // Mock 循环中的 findById
        when(momentRepository.findById(101L)).thenReturn(Optional.of(m1));
        when(momentRepository.findById(102L)).thenReturn(Optional.of(m2));
        
        // Mock 锁定操作
        when(momentRepository.updateMigrationStatus(101L, "NONE", "MIGRATING")).thenReturn(1);
        
        // 模拟两个都迁移成功
        // 注意：由于 migrateSingleMoment 内部有复杂的逻辑，我们这里可以通过 spy 或直接让其执行
        // 为了测试执行统计，我们简单设置返回值
        CommunityColdDataMigrationJob spyJob = spy(migrationJob);
        doReturn(true).when(spyJob).migrateSingleMoment(anyLong());

        CommunityColdDataMigrationJob.MigrationStats stats = spyJob.executeMigration();
        
        assertEquals(2, stats.getSuccess());
        assertEquals(0, stats.getFailed());
    }
}