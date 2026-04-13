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