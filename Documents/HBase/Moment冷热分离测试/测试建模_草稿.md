## 1. 状态转移表（STT）

| 当前状态            | 输入事件        | 约束条件 (Guard)                 | 目标状态            | 动作 / 输出 (Action)                   | 对应 ID |
| ------------------- | --------------- | -------------------------------- | ------------------- | -------------------------------------- | ------- |
| **ACTIVE (NONE)**   | 定时任务扫描    | `(APPROVED & >7d) \| (REJECTED & >3d)` | **MIGRATING**       | 原子更新 `migration_status`            | M-1     |
| **ACTIVE (NONE)**   | 用户删除        | 鉴权通过                         | **[TERMINATED]**    | 从 MySQL 物理删除                      | -       |
| **ACTIVE (NONE)**   | 正常查询/互动   | 状态为 NONE                      | **ACTIVE (NONE)**   | 更新 `last_access_time`                | M-3A    |
| **MIGRATING**       | 评论数检查      | `comments > 2000`                | **ACTIVE (NONE)**   | 回滚状态至 NONE                        | M-2     |
| **MIGRATING**       | 迁移执行 (异常) | HBase 写入失败或校验失败         | **ACTIVE (NONE)**   | 记录日志，回滚状态至 NONE              | M-1/M-6 |
| **MIGRATING**       | 迁移执行 (成功) | HBase 验证成功 && MySQL 删除成功 | **COLD (ARCHIVED)** | 提交事务，MySQL 记录消失               | M-1     |
| **MIGRATING**       | 业务请求        | 评论/点赞/编辑操作               | **MIGRATING**       | **拒绝请求** (抛出 Business Exception) | M-5     |
| **MIGRATING**       | 重试任务 (M-6)  | 发现 HBase 已有完整数据          | **COLD (ARCHIVED)** | 幂等清理，删除 MySQL 冗余数据          | M-6     |
| **COLD (ARCHIVED)** | 详情查询/激活   | 用户触发 RESTORE 事件            | **ACTIVE (NONE)**   | 发送 MQ，异步写回 MySQL 后删 HBase     | M-4     |
| **COLD (ARCHIVED)** | 列表查询        | 热数据页不足                     | **ACTIVE (NONE)**   | 补齐冷数据并触发异步恢复               | M-3B    |
| **COLD (ARCHIVED)** | 用户删除        | 鉴权通过                         | **[TERMINATED]**    | 发送事件清理 HBase 存储                | -       |

## 2. 迁移逻辑决策表 (Decision Table)

针对 `migrateSingleMoment` 方法内部的原子迁移逻辑进行建模。

### 2.1. 条件定义 (Conditions)

- **C1**: 符合准入条件 (`APP`&>7d 或 `REJ`&>3d)
- **C2**: 当前迁移状态 (`NONE` / `MIGRATING` / 其他)
- **C3**: 评论数是否在阈值内 (`comments <= 2000`)
- **C4**: HBase 存储结果 (成功或已存在 / 失败)
- **C5**: MySQL 删除结果 (成功 / 失败)

### 2.2. 动作定义 (Actions)

- **A1**: 获取/保持锁定 (`status = MIGRATING`)
- **A2**: 跳过迁移 (返回 `false`)
- **A3**: 释放锁/恢复状态 (`status = NONE`)
- **A4**: 执行物理删除 (MySQL)
- **A5**: 标记迁移完成

### 2.3. 决策表

| 条件 (Conditions)      | R1    | R2    | R3       | R4        | R5       | R6            | R7            |
| ---------------------- | ----- | ----- | -------- | --------- | -------- | ------------- | ------------- |
| **C1**: 是否为冷数据   | **N** | **Y** | **Y**    | **Y**     | **Y**    | **Y**         | **Y**         |
| **C2**: 当前状态       | -     | 其他  | **NONE** | **NONE**  | **NONE** | **MIGRATING** | **MIGRATING** |
| **C3**: 评论数 <= 2000 | -     | -     | **N**    | **Y**     | **Y**    | **Y**         | **Y**         |
| **C4**: HBase 存储结果 | -     | -     | -        | 成功/存在 | 失败     | 成功/存在     | 成功/存在     |
| **C5**: MySQL 删除结果 | -     | -     | -        | 成功      | -        | 失败          | 成功          |
| **动作 (Actions)**     |       |       |          |           |          |               |               |
| **A1**: 获取锁定       |       |       | Yes      | Yes       | Yes      | Yes           | Yes           |
| **A2**: 跳过迁移       | Yes   | Yes   | Yes      |           |          |               |               |
| **A3**: 恢复状态       |       |       | Yes      |           | (回滚)   | (保持)        |               |
| **A4**: 删除 MySQL     |       |       |          | Yes       |          |               | Yes           |
| **A5**: 迁移完成       |       |       |          | Yes       |          |               | Yes           |

------

### 2.4. 关键规则解析 (Rule Analysis)

1. **Rule 1 & 2 (基础准入)**：非冷数据或状态已在 `COLD` 等非法状态的动态，直接跳过。
2. **Rule 3 (阈值保护)**：如果评论数超过 2000，为了防止归档包过大撑爆 HBase Region，系统会获取锁后立即发现不满足条件，并执行 **解锁 (A3)**。
3. **Rule 4 (标准迁移路径)**：这是最理想的路径。状态 `NONE -> MIGRATING`，HBase 写入验证通过，MySQL 删除成功。
4. **Rule 5 (HBase 故障回滚)**：HBase 写入失败时，代码抛出 `RuntimeException`（见代码第 315 行）。由于整个过程在 `newTransactionTemplate` 中，数据库事务会回滚，自动将状态恢复为迁移前的状态（如果是 `NONE` 则回滚为 `NONE`）。
5. **Rule 6 (MySQL 删除故障)**：HBase 已经写入成功，但 MySQL 物理删除失败（如外键约束冲突或锁超时）。由于事务回滚，HBase 数据已成“孤儿数据”（冗余），但 MySQL 中的状态会保持。如果是任务重试（`MIGRATING`），则保持锁定，等待下一次调度自愈。
6. **Rule 7 (断点续传/自愈)**：针对状态为 `MIGRATING` 的遗留数据。如果 HBase 已有数据，则跳过写入直接尝试删除 MySQL，实现幂等迁移。

---

## 3. 数据建模：等价类划分 (EP) 与边界值分析 (BVA)

为了确保逻辑覆盖的有效性，针对决策表和状态转移表中的关键变量进行数据取值分析。

### 3.1. 输入变量：评论总数 (Comment Count)
阈值：2000条

| 有效等价类 | 无效等价类 | 边界值 (BVA) |
| :--- | :--- | :--- |
| [0, 2000] (允许迁移) | > 2000 (禁止迁移) | 0, 1, 1999, 2000, 2001 |

### 3.2. 输入变量：冷数据判定 (Last Access Age)
阈值：7天 (168小时)

| 有效等价类 | 无效等价类 | 边界值 (BVA) |
| :--- | :--- | :--- |
| > 7天 (符合迁移条件) | <= 7天 (热数据，跳过) | 6.9天, 7天, 7.1天 |

### 3.3. 输入变量：HBase 存储状态 (HBase Storage State)

| 等价类 | 模拟手段 / 说明 |
| :--- | :--- |
| **不存在 (Not Exist)** | HBase 中无该 momentId 记录，正常写入路径。 |
| **已存在 (Exists)** | 模拟 M-6 幂等场景，跳过写入，直接触发 MySQL 删除。 |
| **写入超时/异常** | 模拟网络抖动或 HBase 宕机，触发 Rule 5 回滚逻辑。 |
| **数据损坏 (Corrupted)** | 模拟 `exists()` 返回 true 但数据无法解析，验证 1.1.001 风险点。 |