### 1. Input (输入部分)

**System Overview (系统概览):** 本系统是一个基于分布式存储（MySQL + HBase）的冷热分离系统。系统旨在通过定时任务将不常访问的“冷动态”及其关联数据（评论、点赞）归档到 HBase 中，以减轻 MySQL 数据库的存储压力和查询负担。系统包含自动迁移逻辑、基于消息队列（MQ）的异步恢复逻辑以及复杂的并发保护机制，为了确保系统功能的完备性，设计了针对帖子的审核机制。

---

**Functional Requirements (功能需求简述):**

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

> **项目时区规范：** 本项目面向地区为中国大陆，所有业务逻辑（如冷迁移 3天/7天 阈值判定、最后访问时间更新等）涉及的时间计算均以 **北京时间 (UTC+8)** 为基准。

---

**Testing Base (测试依据):**

需求文档：`Documents/HBase/Moment冷热分离需求文档.md`（包含状态转移图及用例表）。

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

- **Prompt 5: 测试覆盖项生成 (Test Coverage Items Generation)**

  > "请基于上述 STT、DT 和 EP/BVA 建模结果，提取出原子级的测试覆盖项（TCI）清单。每一项 TCI 应代表一个最小的可验证逻辑点（如：特定状态下的特定输入、决策表中的一条规则、或一个边界取值）。请为每个 TCI 分配唯一 ID 并标注其关联的质量风险 ID，作为后续测试用例设计的直接依据。"

- **Prompt 6: 测试用例生成 (Test Case Generation)**

  > "请综合上述 STT、DT 和 EP/BVA 模型，生成一组详细的黑盒测试用例。用例应涵盖全生命周期路径覆盖、异常注入测试（Fault Injection）以及负向并发保护测试。每条用例需包含：测试 ID、场景描述、前置条件、测试步骤及预期回滚/自愈行为。"

---

### 3. Generated Output & Artifacts (生成输出与工件说明)

#### 3.1 交付文档体系说明
本项目测试产出存放于 `Moment冷热分离测试/` 目录下，文档演进逻辑如下：
- **AI 初始产出 (.md)**：包含 `风险分析_草稿.md`、`测试建模_草稿.md`、 `TCI覆盖项_草稿.md`、 `测试用例_草稿.md`，记录了 LLM 最初对需求的解析及零散用例生成过程。
- **重构版本 (测试用例_重构.md)**：经过 Prompt 迭代、场景整合（Scenario Consolidation）及人工校对后的正式版本，是自动化脚本编写的直接依据。
- **Excel 展示表 (.xlsx)**：人工整理的成果汇总，提供了更清晰的需求-风险-用例追溯视图。其中  `Moment_Testing_Engineering - 重构.xlsx `为最终版本。

#### 3.2 测试建模统计

根据黑盒测试建模，最终产出的测试资产规模如下：

| 类型 | 数量 | 描述 |
|------|------|------|
| **质量风险 (Risks)** | **26 项** | 包含 25 项业务风险及由意外路径导出的稳定性风险 (RSK-1.4.007) |
| **状态转移 (STT)** | **55 条路径** | 包含 13 条标准与 42 条意外路径；并辅以决策表(DT)、等价类(EP)及边界值(BVA)作为建模补充 |
| **TCI（测试覆盖项）** | **29 项** | 涵盖 STT 状态、决策表(DT)、等价类(EP)及边界值(BVA) |
| **Test Cases（黑盒用例）** | **16 条** | 经过 **路径敏感化 (Path Sensitizing)** 整合后的高内聚场景用例 |
| **Automated Scripts** | **15 个** | 物理执行脚本，其中合并了3个边界值测试，增加一个集成测试 |

#### 3.3 自动化测试脚本清单

针对 `CommunityColdDataMigrationJob` 的 15 个物理测试方法完整覆盖了重构后的 16 个场景用例：

- `CommunityColdDataMigrationJobTest.java`: 包含 9 个核心方法，涵盖了 TS-1（一致性）与 TS-5（业务边界）。
- `MomentServiceLifecycleTest.java`: 包含 6 个针对业务生命周期的测试用例（验证了审核流转及内容修改后的状态回滚）。

#### 3.4 测试脚本执行结果 (Latest Run)

```log
[INFO] Running petcare.example.community_backend.service.CommunityColdDataMigrationJobTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running petcare.example.community_backend.service.MomentServiceLifecycleTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

### 4. Experimental Analysis (实验分析)

#### 4.1 AI 的局限性分析 (Limitations of AI)

在实践过程中，我们观察到 AI 在生成测试用例时存在以下四个核心局限：

1. **风险评估准则缺失**：AI 默认无法感知业务的重要性优先级。例如，它最初将“存储空间浪费”与“数据丢失”视为同等级别的风险。为了解决此问题，我们必须在提示词中人为强制引入评价规则（如：**数据丢失 > 数据不完整 > 数据一致性弱 > 存储空间浪费**）。
2. **长上下文导致的覆盖项重复与追溯性差**：由于 LLM 对 Context（上下文）的阅读长度限制及记忆衰减，当需求文档过长时，AI 容易在表格后续部分生成重复的 TCI ID 或产生逻辑断层。这要求我们必须进行人工二次对齐，剔除冗余项，并整理成结构清晰的 Excel/Markdown 清单以保证追溯链的严谨性。
3. **软件测试概念理解偏差**：AI 倾向于生成零散的、扁平化的用例列表，而非结构化的质量风险域或 Test Suite。它默认忽略了质量特性（如可靠性、效率）的分类，导致测试套件组织混乱。为此，我们人为规定了分类框架，将输出强制引导为分类明确的“质量风险表”和“场景化 Test Case”。
4. **缺乏场景建模与整合**意识：AI 在设计用例时倾向于机械地将 1 个 TCI 映射为 1 个 TC，无法自动理解“一条端到端路径可覆盖多个覆盖项”的工程优化逻辑。若无人类干预，AI 会生成大量冗余、低效的原子用例。
5. **意外迁移路径（负向状态转移）的缺失**：AI 通常只关注状态转移图上明确标记的“正向”路径，而自动忽略了图上未定义的、理论上应禁止的路径（例如：审核中的动态被意外触发迁移）。这种“由于图上没画所以认为不存在”的思维定式需要通过专门的负向测试提示词来修正。

#### 4.2 Refining Prompts for Accuracy (提示词迭代改进)

针对上述局限性，我们对初始提示词进行了迭代：

- **针对风险评估 (Refined Prompt A)**：
  > "请重新评估风险等级。涉及数据的风险评估，请严格遵守以下评判风险的准则：数据丢失 > 数据不完整 > 数据一致性弱 > 存储空间浪费"

  改进效果：根据这个提示词，AI 能够更好地进行风险评分和排序，但是仍然需要人为调整。项目文档中的 **风险分析表_草稿.md** 展示了 AI 迭代风险版本的过程。
  
- **针对概念把握与分类 (Refined Prompt B)**：
  
  > "不要直接输出用例。请先按'数据完整性'、‘数据一致性’、‘并发控制’、‘系统可靠性’、'功能可靠性'5个类别组成的质量风险矩阵生成对应的 Test Suite 结构，随后再设计具体的Test Case。"
  
  改进效果：AI 能够在进行高阶测试设计（high level test design）的基础上，根据质量风险类别设计多个Test Suite，随后再进行具体测试用例的设计，更具有针对性和可回溯性。最终版本的excel表格和 **测试用例_重构.md** 中均引用了下面这个基于puml代码绘制的 质量风险类-测试套组映射图，本图大题描绘了二者之间的设计关系，但具体的 风险-测试用例 映射可能有所出入。
  
  ![image-20260419194748250](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20260419194748250.png)
  
- **针对路径风险 (Refined Prompt C)**：
  
  > "分析状态转移图。请识别所有图中未标出的意外转换。例如，若当前动态的迁移状态为 ACTIVE，接收到“恢复冷数据”事件时，预期行为应为‘静默跳过’。请根据这些‘禁止跳转’的场景，补充新的一类新的质量风险，并更新覆盖项、测试建模，最终生成一组负向测试用例。"
  
  改进效果：根据这个提示词，AI 开始对意外转换路径进行识别，最终人工将识别出的意外转换路径整理到了 excel 文件中。项目文档中的 **生成STT.py** 展示了 AI 生成的自动化脚本，用于更方便地识别出意外路径。下面是核心代码。
  
  ```
  # 提取所有唯一的状态和输入事件
  all_states = sorted(original_stt["当前状态"].unique())
  all_events = sorted(original_stt["输入事件"].unique())
  
  # 生成所有组合
  all_combinations = list(product(all_states, all_events))
  
  # 构建查找字典
  stt_dict = {}
  for _, row in original_stt.iterrows():
      key = (row["当前状态"], row["输入事件"])
      stt_dict[key] = {
          "约束条件(Guard)": row["约束条件(Guard)"],
          "目标状态": row["目标状态"],
          "动作/输出": row["动作/输出"]
      }
  
  # 生成完整 STT
  full_stt_rows = []
  for state, event in all_combinations:
      key = (state, event)
      if key in stt_dict:
          guard = stt_dict[key]["约束条件(Guard)"]
          target = stt_dict[key]["目标状态"]
          action = stt_dict[key]["动作/输出"]
      else:
          guard = "Undefined"
          target = "Undefined"
          action = "Undefined"
      full_stt_rows.append([state, event, guard, target, action])
  
  full_stt_df = pd.DataFrame(full_stt_rows, columns=["当前状态", "输入事件", "约束条件(Guard)", "目标状态", "动作/输出"])
  ```
  
-  **针对场景整合 (Refined Prompt D)**：

  > "请不要进行 1:1 的机械映射。请应用**路径敏感化 (Path Sensitizing)** 和**场景整合 (Scenario Consolidation)** 技术，通过分析状态转移图找到覆盖最多节点的**基本路径 (Basis Paths)**。将相关的状态跳转、边界校验和自愈逻辑合并到同一个端到端业务场景中，生成更具测试效率的场景化用例。"
  
  改进效果：对比最终版本的 excel 文档和前面两个版本，可以发现经过路径敏感化后重新设计的测试用例数量从 29个（和 TCI 一一对应）下降到了 15 个，并且与 TCI 的对应关系灵活。

#### 4.3 缺陷报告与验证 (Bug Discovery)

在执行重构后的场景用例 **TC-1-01 (标准迁移全生命周期与自愈)** 时，自动化测试脚本发现并证实了一个高风险逻辑缺陷：

**[BUG-COLD-001] 状态机自愈优先级低于业务规则导致的“数据孤儿”风险**
- **缺陷描述**：在 `migrateSingleMoment` 方法中，系统在事务开始阶段优先检查了业务准入规则（`last_access_time`），而没有优先判定状态机的锁定状态。
- **触发场景**：若迁移任务在删除 MySQL 阶段崩溃，记录将保持 `MIGRATING` 状态。若此时因运维操作、系统级全量扫描或逻辑漏洞导致该记录的访问时间被更新，下次迁移任务会因其“最近被访问过”而跳过自愈。

**测试失败日志（缺陷证据 - 修复前）:**
```log
19:48:25.312 [main] INFO ... - 【冷迁移】查询到动态: momentId=6, userId=600, status=MIGRATING, lastAccessTime=2026-04-13...
19:48:25.312 [main] INFO ... - 【冷迁移】动态最近被访问过，跳过迁移: momentId=6, lastAccessTime=2026-04-13...
...
[ERROR] petcare.example.community_backend.service.CommunityColdDataMigrationJobTest.testTC_1_01_StandardMigrationAndIdempotentRecovery -- Time elapsed: 0.013 s <<< FAILURE!
org.opentest4j.AssertionFailedError: 自愈迁移应当返回 true ==> expected: <true> but was: <false>
```

- **影响评估**：该动态将由于状态为 `MIGRATING` 而对用户不可见，同时由于“变热”而无法被后台自愈逻辑处理，最终沦为永久无法访问且无法归档的“僵尸数据”。
- **修复方案**：调整 `migrateSingleMoment` 方法的逻辑顺序，将 `MIGRATING` 状态的检查（自愈路径）优先级提升至业务规则判定（冷热检查）之前。

**验证结果**：
开发团队应用修复补丁后，重新运行自动化测试脚本。结果显示 `testTC_DT_04_IdempotentRecovery` 成功忽略了“热点时间戳”的干扰，正确触发了幂等清理逻辑。所有 9 个核心单元测试全部通过。

---

### 5. Project Report (项目总结)

#### 5.1 与传统非 AI 测试技术的对比 (Comparison to Traditional Techniques)

---
*   **传统人工编写模式**：高度依赖测试人员对项目需求和源码逻辑的深度理解。在面对逻辑复杂的系统（如冷热分离）时，测试人员需要耗备大量时间进行手工逻辑穷举和质量风险建模，且容易受到思维定势的影响产生遗漏。
*   **AI 赋能测试模式（Gemini Code Assist）**：
    *   **认知提速**：AI 能够在分钟级完成对大篇幅需求文档及项目源码的解析，迅速建立起对系统全貌的清晰认知。
    *   **深度穷举**：利用 LLM 的泛化建模能力，AI 能以远超人工的速度穷举出潜在的逻辑风险点，并自动生成对应的 TCI（测试覆盖项）和黑盒测试用例。
*   **协作新范式**：虽然 AI 极大提升了产出效率，但**人工干预依然不可或缺**。测试人员的角色从“执行者”转变为“监督者与架构师”：
    *   **价值判断**：根据真实的业务场景对 AI 识别的风险点给出“优先级/重要性”的价值判断。
    *   **全流程把控**：负责从质量风险分析到高阶设计（HLD）、低阶设计（LLD）的整体流程导向。
    *   **追溯性整理**：对 AI 的原生产出进行人工校对与二次整理（如维护 Excel 矩阵），确保测试资产的可追溯性与工程化质量。

#### 5.2 总结 (Summary)
本项目通过“人工定义框架 + AI 填充逻辑 + 人工反馈纠偏”的闭环，不仅在极短时间内实现了 85% 以上的业务代码覆盖，更成功发现了状态机自愈逻辑等深层风险，验证了冷热分离系统的稳健性。

---
**Team Members:** 2351887-孟炜程  2352488 丁桢垚 2353579 孙修明 2353596 吴瑞泽 2353726 付煜超
**Date:** 2026-04-18