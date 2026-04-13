package petcare.example.community_backend.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.repository.CommentRepository;
import petcare.example.community_backend.repository.LikeRepository;
import petcare.example.community_backend.repository.PetMomentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityColdDataMigrationJobTest {

    @InjectMocks
    private CommunityColdDataMigrationJob migrationJob;

    // 注入所有的依赖 Mock
    @Mock private EntityManager entityManager;
    @Mock private TransactionTemplate newTransactionTemplate;
    @Mock private PetMomentRepository momentRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private CommunityHBaseColdStorageService hBaseService;
    @Mock private UserServiceFacade userServiceFacade;

    @BeforeEach
    void setUp() {
        // 关键修复：强行将 mock 出来的 entityManager 注入到 migrationJob 实例中
        // 解决 @PersistenceContext 无法被 @InjectMocks 自动注入导致的 NullPointerException
        ReflectionTestUtils.setField(migrationJob, "entityManager", entityManager);
    }

    @Test
    @DisplayName("TC-STT-01: 标准冷迁移成功路径")
    void testTC_STT_01_StandardMigrationSuccess() {
        // 1. 前置条件 (Arrange)
        Long momentId = 1L;
        Long userId = 100L;
        PetMoment moment = new PetMoment();
        moment.setId(momentId);
        moment.setUserId(userId);
        moment.setMigrationStatus("NONE");
        moment.setLastAccessTime(LocalDateTime.now().minusDays(8)); // 超过7天

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        // 模拟成功获取锁
        when(momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING")).thenReturn(1);
        // 模拟评论数 < 2000
        when(commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId)).thenReturn(new ArrayList<>());
        when(likeRepository.findByTargetTypeAndTargetId(any(), any())).thenReturn(new ArrayList<>());
        when(userServiceFacade.batchGetUsers(any())).thenReturn(new HashMap<>());
        
        // 模拟 HBase 第一次检查不存在，保存后第二次检查存在 (验证通过)
        when(hBaseService.exists(userId, momentId)).thenReturn(false, true);
        // 模拟 MySQL 删除验证成功
        when(momentRepository.existsById(momentId)).thenReturn(false);

        // 2. 测试执行 (Act)
        boolean result = migrationJob.migrateSingleMoment(momentId);

        // 3. 预期结果 (Assert)
        assertTrue(result, "标准迁移应当返回 true");
        
        // 验证确实调用了 HBase 写入
        verify(hBaseService, times(1)).saveArchive(any(), any());
        
        // 验证确实执行了 MySQL 删除逻辑
        verify(commentRepository, times(1)).deleteByMomentId(momentId);
        verify(momentRepository, times(1)).deleteById(momentId);
        
        // 验证执行了 entityManager.flush() 确保物理删除
        verify(entityManager, times(1)).flush();
    }

    @Test
    @DisplayName("TC-STT-02: 热点动态跳过迁移 (评论数超过阈值)")
    void testTC_STT_02_SkipHotMoment() {
        // 1. 前置条件 (Arrange)
        Long momentId = 2L;
        Long userId = 200L;
        PetMoment moment = new PetMoment();
        moment.setId(momentId);
        moment.setUserId(userId);
        moment.setMigrationStatus("NONE");
        moment.setLastAccessTime(LocalDateTime.now().minusDays(8));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        // 模拟获取锁成功
        when(momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING")).thenReturn(1);
        
        // 模拟评论数等于 2001 (超过 2000 的阈值)
        List<Comment> comments = new ArrayList<>();
        for (int i = 0; i < 2001; i++) {
            comments.add(new Comment());
        }
        when(commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId)).thenReturn(comments);

        // 2. 测试执行 (Act)
        boolean result = migrationJob.migrateSingleMoment(momentId);

        // 3. 预期结果 (Assert)
        assertFalse(result, "评论数超限，应放弃迁移并返回 false");
        
        // 验证状态是否被正确回滚回 NONE
        verify(momentRepository, times(1)).updateMigrationStatus(momentId, "MIGRATING", "NONE");
        
        // 验证绝对没有向 HBase 写入，也没有删除 MySQL
        verify(hBaseService, never()).saveArchive(any(), any());
        verify(momentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("TC-STT-03: 迁移执行异常中断回滚 (HBase 写入异常)")
    void testTC_STT_03_HBaseExceptionRollback() {
        // 1. 前置条件 (Arrange)
        Long momentId = 3L;
        Long userId = 300L;
        PetMoment moment = new PetMoment();
        moment.setId(momentId);
        moment.setUserId(userId);
        moment.setMigrationStatus("NONE");
        moment.setLastAccessTime(LocalDateTime.now().minusDays(8));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING")).thenReturn(1);
        when(commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId)).thenReturn(new ArrayList<>());
        when(likeRepository.findByTargetTypeAndTargetId(any(), any())).thenReturn(new ArrayList<>());
        when(userServiceFacade.batchGetUsers(any())).thenReturn(new HashMap<>());
        
        // 模拟 HBase 不存在
        when(hBaseService.exists(userId, momentId)).thenReturn(false);
        // 人为注入 HBase 连接异常
        doThrow(new RuntimeException("HBase Connection Timeout")).when(hBaseService).saveArchive(any(), any());

        // 2. 测试执行并验证异常抛出 (Act & Assert)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            migrationJob.migrateSingleMoment(momentId);
        }, "期望捕获到 HBase 抛出的异常以触发 Spring @Transactional 回滚");

        // 3. 结果断言
        assertEquals("HBase Connection Timeout", exception.getMessage());
        
        // 验证 MySQL 绝对不能被删除
        verify(commentRepository, never()).deleteByMomentId(any());
        verify(momentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("TC-DT-02: HBase 虚假成功校验拦截 (写入没报错但查不到)")
    void testTC_DT_02_HBaseFakeSuccess() {
        // 1. 前置条件 (Arrange)
        Long momentId = 4L;
        Long userId = 400L;
        PetMoment moment = new PetMoment();
        moment.setId(momentId);
        moment.setUserId(userId);
        moment.setMigrationStatus("NONE");
        moment.setLastAccessTime(LocalDateTime.now().minusDays(8));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING")).thenReturn(1);
        when(commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId)).thenReturn(new ArrayList<>());
        when(likeRepository.findByTargetTypeAndTargetId(any(), any())).thenReturn(new ArrayList<>());
        when(userServiceFacade.batchGetUsers(any())).thenReturn(new HashMap<>());

        // 核心 Mock：模拟 HBase 第一次检查不存在，执行 saveArchive，但第二次检查依然返回 false！
        when(hBaseService.exists(userId, momentId)).thenReturn(false, false);

        // 2. 测试执行并验证异常抛出 (Act & Assert)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            migrationJob.migrateSingleMoment(momentId);
        }, "期望捕获到 HBase 虚假成功抛出的异常");

        // 3. 结果断言
        assertEquals("HBase写入验证失败", exception.getMessage());
        
        // 验证状态是否被手动回滚回 NONE (根据源码第 375 行)
        verify(momentRepository, times(1)).updateMigrationStatus(momentId, "MIGRATING", "NONE");
        // 验证 MySQL 绝对没有被删除
        verify(momentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("TC-DT-03: MySQL 删除阶段死锁/超时")
    void testTC_DT_03_MySqlDeleteTimeout() {
        // 1. 前置条件 (Arrange)
        Long momentId = 5L;
        Long userId = 500L;
        PetMoment moment = new PetMoment();
        moment.setId(momentId);
        moment.setUserId(userId);
        moment.setMigrationStatus("NONE");
        moment.setLastAccessTime(LocalDateTime.now().minusDays(8));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING")).thenReturn(1);
        when(commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId)).thenReturn(new ArrayList<>());
        when(likeRepository.findByTargetTypeAndTargetId(any(), any())).thenReturn(new ArrayList<>());
        when(userServiceFacade.batchGetUsers(any())).thenReturn(new HashMap<>());

        // 模拟 HBase 验证全部通过
        when(hBaseService.exists(userId, momentId)).thenReturn(false, true);
        
        // 核心 Mock：人为注入 MySQL 删除主表时的锁超时异常
        doThrow(new RuntimeException("Lock wait timeout")).when(momentRepository).deleteById(momentId);

        // 2. 测试执行并验证异常抛出 (Act & Assert)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            migrationJob.migrateSingleMoment(momentId);
        });

        assertEquals("Lock wait timeout", exception.getMessage());
        
        // 验证 HBase 确实写入了
        verify(hBaseService, times(1)).saveArchive(any(), any());
        // 注意：因为事务回滚，我们不用手动验证恢复 NONE，状态将保持在 MIGRATING 等待自愈
    }

    @Test
    @DisplayName("TC-DT-04: 迁移中断后的幂等自愈 (HBase 已存在，跳写直接删库)")
    void testTC_DT_04_IdempotentRecovery() {
        // 1. 前置条件 (Arrange)
        Long momentId = 6L;
        Long userId = 600L;
        PetMoment moment = new PetMoment();
        moment.setId(momentId);
        moment.setUserId(userId);
        // 核心差异：状态已经是 MIGRATING (模拟上次失败遗留)
        moment.setMigrationStatus("MIGRATING"); 
        // 即使是刚刚访问过也没关系，因为 MIGRATING 状态会跳过日期检查
        moment.setLastAccessTime(LocalDateTime.now()); 

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        // 不需要获取锁，因为本身就是 MIGRATING
        
        when(commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId)).thenReturn(new ArrayList<>());
        when(likeRepository.findByTargetTypeAndTargetId(any(), any())).thenReturn(new ArrayList<>());
        when(userServiceFacade.batchGetUsers(any())).thenReturn(new HashMap<>());

        // 核心 Mock：第一次检查 HBase 就发现已经存在了！
        when(hBaseService.exists(userId, momentId)).thenReturn(true);
        when(momentRepository.existsById(momentId)).thenReturn(false);

        // 2. 测试执行 (Act)
        boolean result = migrationJob.migrateSingleMoment(momentId);

        // 3. 结果断言
        assertTrue(result, "自愈迁移应当返回 true");
        
        // 验证：绝对不能再调用 HBase 写入，节约 I/O
        verify(hBaseService, never()).saveArchive(any(), any());
        
        // 验证：直接走到了 MySQL 删除逻辑
        verify(momentRepository, times(1)).deleteById(momentId);
        verify(entityManager, times(1)).flush();
    }

    @Test
    @DisplayName("TC-BVA-03: 评论数恰好为边界最大值 (2000条)")
    void testTC_BVA_03_CommentCountExact2000() {
        // 1. 前置条件 (Arrange)
        Long momentId = 7L;
        Long userId = 700L;
        PetMoment moment = new PetMoment();
        moment.setId(momentId);
        moment.setUserId(userId);
        moment.setMigrationStatus("NONE");
        moment.setLastAccessTime(LocalDateTime.now().minusDays(8));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING")).thenReturn(1);
        
        // 核心 Mock：精准构造 2000 条评论（恰好压在线上）
        List<Comment> comments = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            comments.add(new Comment());
        }
        when(commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId)).thenReturn(comments);
        when(likeRepository.findByTargetTypeAndTargetId(any(), any())).thenReturn(new ArrayList<>());
        when(userServiceFacade.batchGetUsers(any())).thenReturn(new HashMap<>());
        when(hBaseService.exists(userId, momentId)).thenReturn(false, true);
        when(momentRepository.existsById(momentId)).thenReturn(false);

        // 2. 测试执行 (Act)
        boolean result = migrationJob.migrateSingleMoment(momentId);

        // 3. 结果断言
        assertTrue(result, "恰好 2000 条评论，属于有效边界，应该允许迁移");
        verify(hBaseService, times(1)).saveArchive(any(), any());
    }

    @Test
    @DisplayName("TC-BVA-05: 访问时间边界 (167小时，差1小时满7天)")
    void testTC_BVA_05_LastAccessTime167Hours() {
        // 1. 前置条件 (Arrange)
        Long momentId = 8L;
        Long userId = 800L;
        PetMoment moment = new PetMoment();
        moment.setId(momentId);
        moment.setUserId(userId);
        moment.setMigrationStatus("NONE");
        // 核心 Mock：167小时前访问（热数据边界）
        moment.setLastAccessTime(LocalDateTime.now().minusHours(167));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));

        // 2. 测试执行 (Act)
        boolean result = migrationJob.migrateSingleMoment(momentId);

        // 3. 结果断言
        assertFalse(result, "不足 168 小时，应判定为热数据跳过迁移");
        
        // 验证：绝对不应该去尝试获取状态机锁
        verify(momentRepository, never()).updateMigrationStatus(any(), any(), any());
    }

    @Test
    @DisplayName("TC-BVA-07: 访问时间边界 (169小时，刚超7天1小时)")
    void testTC_BVA_07_LastAccessTime169Hours() {
        // 1. 前置条件 (Arrange)
        Long momentId = 9L;
        Long userId = 900L;
        PetMoment moment = new PetMoment();
        moment.setId(momentId);
        moment.setUserId(userId);
        moment.setMigrationStatus("NONE");
        // 核心 Mock：169小时前访问（冷数据边界）
        moment.setLastAccessTime(LocalDateTime.now().minusHours(169));

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));
        when(momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING")).thenReturn(1);
        when(commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId)).thenReturn(new ArrayList<>());
        when(likeRepository.findByTargetTypeAndTargetId(any(), any())).thenReturn(new ArrayList<>());
        when(userServiceFacade.batchGetUsers(any())).thenReturn(new HashMap<>());
        when(hBaseService.exists(userId, momentId)).thenReturn(false, true);
        when(momentRepository.existsById(momentId)).thenReturn(false);

        // 2. 测试执行 (Act)
        boolean result = migrationJob.migrateSingleMoment(momentId);

        // 3. 结果断言
        assertTrue(result, "超过 168 小时，应判定为冷数据并执行迁移");
        verify(hBaseService, times(1)).saveArchive(any(), any());
    }
}

