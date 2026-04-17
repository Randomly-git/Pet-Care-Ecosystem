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
import petcare.example.community_backend.dto.ColdArchiveData;
import petcare.example.community_backend.dto.HBaseArchiveRecord;
import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.model.TargetType;
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

    @Test
    void testTC_STT_01_StandardMigrationSuccess() {
        Long momentId = 1L;
        Long userId = 100L;
        LocalDateTime oldAccessTime = LocalDateTime.now().minusDays(8);
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "NONE", oldAccessTime, 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING")).thenReturn(1);
        // 修复：第一次检查不存在(false)，第二次验证已写入(true)
        when(hBaseService.exists(userId, momentId)).thenReturn(false).thenReturn(true);
        when(momentRepository.existsById(momentId)).thenReturn(false); // 模拟删除成功

        boolean result = migrationJob.migrateSingleMoment(momentId);
        assertTrue(result);

        verify(hBaseService).saveArchive(any(), any());
        verify(momentRepository).deleteById(momentId);
    }

    @Test
    void testTC_STT_02_SkipHotMoment() {
        Long momentId = 2L;
        Long userId = 200L;
        LocalDateTime recentAccessTime = LocalDateTime.now().minusDays(1); 
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "NONE", recentAccessTime, 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        // 必须 Mock 锁的获取和释放
        when(momentRepository.updateMigrationStatus(momentId, "MIGRATING", "NONE")).thenReturn(1);

        boolean result = migrationJob.migrateSingleMoment(momentId);
        assertFalse(result, "热动态不应迁移");
        verify(hBaseService, never()).saveArchive(any(), any());
    }

    @Test
    void testTC_STT_03_HBaseExceptionRollback() {
        Long momentId = 3L;
        Long userId = 300L;
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "NONE", LocalDateTime.now().minusDays(8), 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        doThrow(new RuntimeException("HBase Connection Timeout")).when(hBaseService).saveArchive(any(), any());

        assertThrows(RuntimeException.class, () -> migrationJob.migrateSingleMoment(momentId));
        verify(momentRepository, never()).deleteById(any());
    }

    @Test
    void testTC_DT_02_HBaseFakeSuccess() {
        Long momentId = 4L;
        Long userId = 400L;
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "NONE", LocalDateTime.now().minusDays(8), 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(hBaseService.exists(userId, momentId)).thenReturn(false); // 写入前不存在
        // 模拟写入后 exists 检查依然返回 false
        when(hBaseService.exists(userId, momentId)).thenReturn(false); 

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> migrationJob.migrateSingleMoment(momentId));
        assertEquals("HBase写入验证失败", thrown.getMessage());
    }

    @Test
    void testTC_DT_03_MySqlDeleteTimeout() {
        Long momentId = 5L;
        Long userId = 500L;
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "NONE", LocalDateTime.now().minusDays(8), 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        // 修复：让写入验证通过，才能走到后续的删除逻辑
        when(hBaseService.exists(userId, momentId)).thenReturn(false).thenReturn(true);
        // 模拟删除时存在依然检查失败
        when(momentRepository.existsById(momentId)).thenReturn(true); 

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> migrationJob.migrateSingleMoment(momentId));
        assertTrue(thrown.getMessage().contains("动态删除失败"));
    }

    @Test
    void testTC_DT_04_IdempotentRecovery() {
        Long momentId = 6L;
        Long userId = 600L;
        // 模拟已经是 MIGRATING 状态（遗留数据）
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "MIGRATING", LocalDateTime.now().minusDays(8), 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(hBaseService.exists(userId, momentId)).thenReturn(true); // HBase 已有数据
        when(momentRepository.existsById(momentId)).thenReturn(false); // 删除成功

        boolean result = migrationJob.migrateSingleMoment(momentId);
        assertTrue(result, "自愈迁移应当返回 true");
        
        verify(hBaseService, never()).saveArchive(any(), any()); // 不应重复写入
        verify(momentRepository).deleteById(momentId); // 应该执行删除
    }

    @Test
    void testTC_BVA_03_CommentCountExact2000() {
        Long momentId = 7L;
        Long userId = 700L;
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "NONE", LocalDateTime.now().minusDays(8), 2000, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        // 修复：让写入验证通过
        when(hBaseService.exists(userId, momentId)).thenReturn(false).thenReturn(true);
        when(momentRepository.existsById(momentId)).thenReturn(false);

        boolean result = migrationJob.migrateSingleMoment(momentId);
        assertTrue(result, "边界值 2000 评论应当允许迁移");
    }

    @Test
    void testTC_BVA_05_LastAccessTime167Hours() {
        Long momentId = 8L;
        Long userId = 800L;
        LocalDateTime hotTime = LocalDateTime.now().minusHours(167); 
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "NONE", hotTime, 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(momentRepository.updateMigrationStatus(momentId, "MIGRATING", "NONE")).thenReturn(1);

        boolean result = migrationJob.migrateSingleMoment(momentId);
        assertFalse(result, "167小时仍属于热数据");
    }

    @Test
    void testTC_BVA_07_LastAccessTime169Hours() {
        Long momentId = 9L;
        Long userId = 900L;
        LocalDateTime coldTime = LocalDateTime.now().minusHours(169);
        PetMoment moment = createMoment(momentId, userId, "APPROVED", "NONE", coldTime, 0, LocalDateTime.now().minusDays(10));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING")).thenReturn(1);
        // 修复：让写入验证通过
        when(hBaseService.exists(userId, momentId)).thenReturn(false).thenReturn(true);
        when(momentRepository.existsById(momentId)).thenReturn(false);

        boolean result = migrationJob.migrateSingleMoment(momentId);
        assertTrue(result, "169小时应当迁移");
    }

    @Test
    void testTC_STT_10_PendingMomentNotMigrated() {
        Long momentId = 10L;
        Long userId = 1000L;
        LocalDateTime oldAccessTime = LocalDateTime.now().minusDays(10);
        PetMoment moment = createMoment(momentId, userId, "PENDING", "NONE", oldAccessTime, 0, LocalDateTime.now().minusDays(15));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(momentRepository.updateMigrationStatus(momentId, "MIGRATING", "NONE")).thenReturn(1);

        boolean result = migrationJob.migrateSingleMoment(momentId);
        assertFalse(result, "PENDING 状态严禁迁移");
        verify(hBaseService, never()).saveArchive(any(), any());
    }

    @Test
    void testTC_STT_11_RejectedMomentFastMigration() {
        Long momentId = 11L;
        Long userId = 1100L;
        // 已拒绝状态，超过 3 天未访问即可迁移
        LocalDateTime accessTime = LocalDateTime.now().minusDays(4);
        PetMoment moment = createMoment(momentId, userId, "REJECTED", "NONE", accessTime, 0, LocalDateTime.now().minusDays(5));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        // 修复：让写入验证通过
        when(hBaseService.exists(userId, momentId)).thenReturn(false).thenReturn(true);
        when(momentRepository.existsById(momentId)).thenReturn(false);

        boolean result = migrationJob.migrateSingleMoment(momentId);
        assertTrue(result, "REJECTED 状态超过 3 天应被迁移");
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