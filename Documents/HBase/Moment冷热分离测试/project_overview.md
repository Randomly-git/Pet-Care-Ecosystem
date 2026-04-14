# 软测作业 & 项目内容梳理

## 一、作业要求摘要

**题目类型**：选择 Static / Black-box / White-box 测试技术之一，用 AI（LLM）作为核心工具实现。

**你们小组选择**：**黑盒动态测试**（Black-box Testing）——等价类划分（EP）+ 边界值分析（BVA）+ 状态转移测试（STT）+ 决策表测试（DT）。

**要交付的内容**：

| 交付物 | 说明 |
|---|---|
| ① 输入 | 系统需求 / 项目代码库 |
| ② 工具伪影（Tool Artifact） | 使用的 Prompt、使用的模型、AI 生成的代码 |
| ③ 生成输出 | 测试用例（黑盒）|
| ④ 实验分析 | 准确率、覆盖率、可推广性分析；迭代改进 Prompt |
| ⑤ 项目报告 | 与传统非 AI 方法的比较；AI 局限性分析；摘要 |

**截止日期**：第 8 周 周一 17:00 前（提交报告+PPT+测试脚本压缩包）

---

## 二、组长（你以外的人）做了什么

组长完成了**测试建模与用例设计**的全部文档，位于 `Documents/HBase/Moment冷热分离测试/`：

### 2.1 测试对象
`community-backend` 模块的 **Moment 冷热分离功能**，即：

- `CommunityColdDataMigrationJob.java` — 核心迁移逻辑（定时扫描、状态机、写 HBase、删 MySQL）
- `CommunityHBaseColdStorageService.java` — HBase 存取
- `CommunityColdStorageEventConsumer.java` — MQ 消费者（恢复冷数据）
- `MomentService.java` — 查询时热数据不足补冷数据

### 2.2 已完成的文档

| 文件 | 内容 |
|---|---|
| `Moment冷热分离需求文档.md` | 7 个用例（M-1 到 M-7）：迁移/恢复/并发保护/列表查询/HBase直读 |
| `Moment冷热分离测试建模.md` | 状态转移表（STT）、迁移逻辑决策表（DT，7条规则）、等价类/边界值分析 |
| `Moment冷热分离黑盒测试用例.md` | **黑盒测试用例表**（21 条，分3组）|
| `Moment冷热分离风险分析表.md` | 风险点分析 |

### 2.3 黑盒测试用例表结构（3组共21条）

| 分组 | 方法 | 用例数 | 重点 |
|---|---|---|---|
| **一、状态转移测试（STT）** | 状态机路径覆盖 | TC-STT-01~09，共 9 条 | 迁移成功路径、热点拦截、HBase超时回滚、并发保护、断点续传、恢复、隐式恢复、访问刷新、物理删除 |
| **二、决策表测试（DT）** | 迁移异常处理 | TC-DT-01~05，共 5 条 | HBase写入异常回滚、虚假成功校验、MySQL死锁、幂等自愈、评论数超限 |
| **三、等价类/边界值（EP/BVA）** | 数值边界覆盖 | TC-BVA-01~07，共 7 条 | 评论数 0/1999/2000/2001，访问时间 167h/168h/169h |

---

## 三、你的工作：基于黑盒测试用例表生成测试代码并运行

### 3.1 工作目标
> 让 AI 根据已有的 21 条黑盒测试用例，生成对应的 **JUnit 5 测试代码**，并在项目中跑通。

### 3.2 现有测试代码现状

项目已有少量测试代码（`community-backend/src/test/`）：

| 文件 | 类型 | 内容 |
|---|---|---|
| `CommunityBackendApplicationTests.java` | Spring Boot 启动测试 | 只有 `contextLoads()` 空测试 |
| `CommunityIntegrationTest.java` | 集成测试（MockMvc） | 已有"发帖→点赞→评论"流程 + 关注流程 |
| `MomentControllerTest.java` | 控制器测试 | 待查看 |
| `NotificationControllerTest.java` | 控制器测试 | 待查看 |
| `NotificationServiceTest.java` | Service 单元测试 | 待查看 |

**目前完全没有针对冷热分离的测试代码**。

### 3.3 需要生成的测试代码

针对 `CommunityColdDataMigrationJob.migrateSingleMoment()` 方法，需用 **Mockito + JUnit 5** 编写 Unit Tests（Mock HBase、MySQL Repository），覆盖以下场景：

**STT 组**（状态迁移路径）：
- TC-STT-01: 标准冷迁移成功（NONE→MIGRATING→COLD/deleted）
- TC-STT-02: 热点动态跳过（评论数=2001，状态回滚NONE）
- TC-STT-03: HBase连接异常，状态回滚
- TC-STT-04: MIGRATING状态时拒绝评论/点赞
- TC-STT-05: 断点续传（HBase已有数据，跳过写入直接删MySQL）
- TC-STT-06/07/08/09: 恢复、列表补冷、访问刷新、物理删除

**DT 组**（决策表）：
- TC-DT-01: HBase IOException → 回滚为NONE
- TC-DT-02: HBase exists() 验证=false → 回滚
- TC-DT-03: MySQL删除死锁/超时 → 保持MIGRATING
- TC-DT-04: 幂等自愈（已有HBase数据跳写，直接清MySQL）
- TC-DT-05: 评论数=2001 → 释放锁回NONE

**BVA 组**（边界值）：
- TC-BVA-01~07: 评论数边界（0/1999/2000/2001）+ 访问时间边界（167h/168h/169h）

### 3.4 技术关键点

- **测试框架**：JUnit 5 + Mockito（项目已引入）
- **Mock 对象**：`PetMomentRepository`、`CommentRepository`、`LikeRepository`、`CommunityHBaseColdStorageService`、`UserServiceFacade`
- **核心被测类**：`CommunityColdDataMigrationJob`（`migrateSingleMoment` 方法，438行）
- **事务**：测试时用 `@Transactional` 或 Mock `TransactionTemplate`
- **测试类位置**：`src/test/java/petcare/example/community_backend/service/ColdDataMigrationJobTest.java`

---

## 四、工作计划建议

```
1. [你的任务] 生成测试代码
   ├── 用 AI (LLM) 生成测试代码（这就是"Tool Artifact"）
   │   ├── 输入：需求文档 + 测试用例表 + migrateSingleMoment 源码
   │   └── 输出：JUnit 5 测试类（带 Mockito）
   ├── 审查并修正 AI 生成的代码
   └── 运行 mvn test，确认测试通过

2. [报告材料] 记录 AI 使用的 Prompt 和模型
3. [报告材料] 分析测试覆盖率和 AI 的局限性
```

---

## 五、关键代码位置速查

| 文件 | 路径 |
|---|---|
| 核心迁移逻辑（被测代码） | `community-backend/src/main/java/petcare/example/community_backend/service/CommunityColdDataMigrationJob.java` |
| HBase服务 | `...service/CommunityHBaseColdStorageService.java` |
| MQ消费者（恢复流程） | `...service/CommunityColdStorageEventConsumer.java` |
| 黑盒测试用例表 | `Documents/HBase/Moment冷热分离测试/Moment冷热分离黑盒测试用例.md` |
| 需求文档 | `Documents/HBase/Moment冷热分离测试/Moment冷热分离需求文档.md` |
| 测试建模 | `Documents/HBase/Moment冷热分离测试/Moment冷热分离测试建模.md` |
| 已有测试代码 | `community-backend/src/test/...` |
