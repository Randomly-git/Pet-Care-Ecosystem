### 1. Input (输入部分)

**System Overview (系统概览):** 本系统是一个基于分布式存储（MySQL + HBase）的社交动态（Moment）冷热分离系统。系统旨在通过定时任务将不常访问的“冷动态”及其关联数据（评论、点赞）归档到 HBase 中，以减轻 MySQL 数据库的存储压力和查询负担。系统包含自动迁移逻辑、基于消息队列（MQ）的异步恢复逻辑以及复杂的并发保护机制。

**Functional Requirements (功能需求简述):**

---

### 功能需求简述

- **冷迁移（M-1, M-1S, M-2, M-6）**：  
  定时任务根据动态的审核状态（APPROVED / REJECTED）及最后访问时间阈值（7天 / 3天），将符合条件的动态迁移至 HBase。迁移过程中状态变为 `MIGRATING`，并在 HBase 写入成功后从 MySQL 删除。若评论数超过 2000，则跳过迁移并回滚状态。支持迁移失败后的幂等重试与自愈机制。

- **冷恢复（M-4, M-3B, M-7）**：  
  用户主动访问冷动态或列表查询热数据不足时，系统通过 MQ 异步将 HBase 中的归档数据写回 MySQL，并删除 HBase 记录。恢复后迁移状态重置为 `NONE`，保留原审核状态。

- **并发控制与状态保护（M-5, M-8）**：  
  动态处于 `MIGRATING` 状态时，拒绝任何评论、点赞、修改内容或修改审核状态的操作，确保迁移期间数据一致性与状态机完整性。

- **审核流转与内容修改（M-10）**：  
  用户修改已审核通过的动态内容后，系统自动将审核状态回滚至 `PENDING`，并隐藏动态，确保违规内容无法绕过审核公开。

- **动态删除（M-9）**：  
  支持在任意状态（ACTIVE、MIGRATING、COLD）下物理删除动态，同时清理 MySQL 与 HBase 中的对应记录，避免存储浪费。

- **可见性控制（M-3C, M-3A）**：  
  仅审核状态为 `APPROVED` 的动态对他人可见，`PENDING` 或 `REJECTED` 状态的动态仅作者本人可见，防止未通过审核的内容被非法获取。

---


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

  > "针对迁移核心方法 `migrateSingleMoment`，分析其内部多条件组合逻辑（如：是否冷数据、当前状态、评论阈值、HBase写入状态、MySQL删除状态），生成决策表（DT）。重点通过规则组合识别 Rule 6（HBase 故障）和 Rule 7（MySQL 删除失败）等异常边界场景。"

- **Prompt 4: 数据边界分析 (EP & BVA Analysis)**

  > "针对系统中影响决策的关键变量：评论总数（阈值 2000）和最后访问时间（阈值 7天），识别其有效等价类、无效等价类及边界值（BVA）。请提供具体的取值建议以用于后续测试用例设计。"

- **Prompt 5: 测试用例生成 (Test Case Generation)**

  > "请综合上述 STT、DT 和 EP/BVA 模型，生成一组详细的黑盒测试用例。用例应涵盖全生命周期路径覆盖、异常注入测试（Fault Injection）以及负向并发保护测试。每条用例需包含：测试 ID、场景描述、前置条件、测试步骤及预期回滚/自愈行为。"

---

### 3. Generated Output (生成输出)

基于上述提示词，LLM 成功生成了覆盖 **状态转移（STT）、决策表（DT）、边界值分析（BVA）** 三个维度的完整测试资产

#### 3.1 测试覆盖项（TCI）清单

共生成 **21 项测试覆盖项（TCI）**，按测试方法分类：

| 类型 | 数量 |
|------|------|
| **TCI-STT（状态转移表）** | 14 项 | 
| **TCI-DT（决策表）** | 3 项 | 
| **TCI-BVA（边界值分析）** | 7 项 |

#### 3.2 黑盒测试用例

共生成 **28 条详细黑盒测试用例**，每条用例均包含：用例ID、关联TCI、测试场景、前置条件、测试步骤、预期结果。



#### 3.3 需求追溯矩阵（RTM）

构建了从 **需求 → 风险 → TCI → 测试用例 → 自动化脚本** 的完整追溯链，确保每一行代码测试都有据可查。

---


### 4. Experimental Analysis (实验分析)

#### 4.1 Accuracy and Coverage (准确性与覆盖率)
<!-- (SUN) 此部分由负责测试用例设计覆盖率分析（如 TCI 达成率、方法/分支覆盖率统计）的同学填写 -->

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
#### 