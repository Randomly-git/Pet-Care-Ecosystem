### 1. Input (输入部分)

**System Overview (系统概览):** 本系统是一个基于分布式存储（MySQL + HBase）的社交动态（Moment）冷热分离系统。系统旨在通过定时任务将不常访问的“冷动态”及其关联数据（评论、点赞）归档到 HBase 中，以减轻 MySQL 数据库的存储压力和查询负担。系统包含自动迁移逻辑、基于消息队列（MQ）的异步恢复逻辑以及复杂的并发保护机制。

**Functional Requirements (功能需求简述):**

- **冷迁移 (M-1, M-2, M-6):** 定时任务扫描 7 天未访问且评论数在阈值内（<2000）的动态，原子性更改状态为 `MIGRATING`，写入 HBase 验证成功后从 MySQL 删除。
- **冷恢复 (M-4, M-3B, M-7):** 用户点击“激活”或列表查询发现热数据不足时，通过 MQ 异步将 HBase 归档写回 MySQL 并清理冷库。
- **并发控制 (M-5):** 动态在 `MIGRATING` 状态下，禁止任何写入（评论、点赞）操作，确保迁移前后数据一致性。
- **状态机自愈 (M-6):** 针对迁移中断导致的 `MIGRATING` 遗留数据，支持幂等重试与自愈。

**Testing Base (测试依据):**

- 需求文档：`Documents/HBase/Moment冷热分离需求文档.md`（包含状态转移图及用例表）。
- 核心代码逻辑：`CommunityColdDataMigrationJob.java`。

------

### 2. Tool Artifact (工具产出过程)

**LLM Used:** Gemini Code Assist (Google)

**Refined Prompts (整理后的核心提示词序列):** 为了符合黑盒测试技术要求（STT, DT, EP/BVA），我使用了以下经过优化的提示词：

- **Prompt 1: 质量风险分析 (Quality Risk Analysis)**

  > "作为软件测试专家，请分析《Moment冷热分离需求文档》及相关逻辑代码，识别潜在的质量风险。请按照 数据丢失 > 数据不一致 > 状态锁定 > 系统稳定性 的优先级，输出一份包含技术风险、业务风险、风险等级（Risk Priority）及测试建议的风险分析表。"

- **Prompt 2: 状态转移建模 (State Transition Modeling)**

  > "根据需求文档中的状态转移图，请生成一份标准的状态转移表（STT）。识别所有合法的状态跳转路径（如 NONE -> MIGRATING -> COLD）以及异常中断路径（如 MIGRATING -> NONE 回滚），并标注每个跳转对应的触发事件、约束条件（Guard）和动作。"

- **Prompt 3: 决策表建模 (Decision Table Modeling)**

  > "针对迁移核心方法 `migrateSingleMoment`，分析其内部多条件组合逻辑（如：是否冷数据、当前状态、评论阈值、HBase写入状态、MySQL删除状态），生成决策表（DT）。重点通过规则组合识别 Rule 5（HBase 故障）和 Rule 6（MySQL 删除失败）等异常边界场景。"

- **Prompt 4: 数据边界分析 (EP & BVA Analysis)**

  > "针对系统中影响决策的关键变量：评论总数（阈值 2000）和最后访问时间（阈值 7天），识别其有效等价类、无效等价类及边界值（BVA）。请提供具体的取值建议以用于后续测试用例设计。"

- **Prompt 5: 测试用例生成 (Test Case Generation)**

  > "请综合上述 STT、DT 和 EP/BVA 模型，生成一组详细的黑盒测试用例。用例应涵盖全生命周期路径覆盖、异常注入测试（Fault Injection）以及负向并发保护测试。每条用例需包含：测试 ID、场景描述、前置条件、测试步骤及预期回滚/自愈行为。"

---

### 3. Generated Output (生成输出)

基于上述提示词，LLM 成功生成了涵盖三个维度的 **21 条黑盒测试用例**（详见 `Moment冷热分离黑盒测试用例.md`）：
- **STT (9条)**：覆盖了从数据变冷到归档、再到 MQ 异步恢复的全生命周期。
- **DT (5条)**：深入覆盖了 HBase 写入失败、MySQL 删除死锁、幂等自愈等异常原子路径。
- **EP/BVA (7条)**：精准捕捉了评论数（2000）与冷却时间（168h）的边界溢出错误。

同时，LLM 生成了基于 **JUnit 5 + Mockito** 的自动化测试脚本，实现了对核心迁移类 `CommunityColdDataMigrationJob` 的逻辑验证。

---

### 4. Experimental Analysis (实验分析)

#### 4.1 Accuracy and Coverage (准确性与覆盖率)
<!-- (SUN) 此部分由负责测试用例设计覆盖率分析的同学填写 -->

#### 4.2 Bug Reporting and Validation by the developers (缺陷报告与验证)

在执行基于决策表建模生成的 **TC-DT-04 (幂等自愈测试)** 时，自动化测试脚本发现并证实了一个高风险逻辑缺陷：

**[BUG-COLD-001] 状态机自愈逻辑被业务规则覆盖导致的“数据孤儿”风险**
- **缺陷描述**：在 `migrateSingleMoment` 方法中，系统在事务开始阶段优先检查了业务准入规则（`last_access_time`），而没有优先判定状态机的锁定状态。
- **触发场景**：若迁移任务在删除 MySQL 阶段崩溃，记录将保持 `MIGRATING` 状态。若此时因运维操作、系统级全量扫描或逻辑漏洞导致该记录的访问时间被更新，下次迁移任务会因其“最近被访问过”而跳过自愈。

**测试失败日志（缺陷证据 - 修复前）:**
```log
19:48:25.312 [main] INFO ... - 【冷迁移】查询到动态: momentId=6, userId=600, status=MIGRATING, lastAccessTime=2026-04-13...
19:48:25.312 [main] INFO ... - 【冷迁移】动态最近被访问过，跳过迁移: momentId=6, lastAccessTime=2026-04-13...
...
[ERROR] petcare.example.community_backend.service.CommunityColdDataMigrationJobTest.testTC_DT_04_IdempotentRecovery -- Time elapsed: 0.013 s <<< FAILURE!
org.opentest4j.AssertionFailedError: 自愈迁移应当返回 true ==> expected: <true> but was: <false>
```

- **影响评估**：该动态将由于状态为 `MIGRATING` 而对用户不可见，同时由于“变热”而无法被后台自愈逻辑处理，最终沦为永久无法访问且无法归档的“僵尸数据”。
- **修复方案**：调整 `migrateSingleMoment` 方法的逻辑顺序，将 `MIGRATING` 状态的检查（自愈路径）优先级提升至业务规则判定（冷热检查）之前。

**验证结果**：
开发团队应用修复补丁后，重新运行自动化测试脚本。结果显示 `testTC_DT_04_IdempotentRecovery` 成功忽略了“热点时间戳”的干扰，正确触发了幂等清理逻辑。所有 9 个核心单元测试全部通过。

**测试执行日志摘要 (2026-04-14 11:14):**
```log
...
11:14:25.579 [main] INFO ... - 【冷迁移】检测到上次迁移失败的遗留数据，继续迁移: momentId=6
11:14:25.579 [main] INFO ... - 【冷迁移】HBase 中已存在数据（幂等跳过）: momentId=6
11:14:25.579 [main] INFO ... - 【冷迁移】开始删除MySQL数据: momentId=6
...
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.094 s
[INFO] BUILD SUCCESS
```

#### 4.3 Refining Prompts for Accuracy (提示词迭代改进)
<!-- (SUN) 此部分由负责 Prompt 优化策略分析的同学填写 -->

---

### 5. Project Report (项目总结)

*(此处预留：比较 AI 测试与传统手工测试在发现复杂分布式 Bug 方面的效率差异)*