### 一、 TCI 覆盖项清单 

| TCI ID         | 来源模型 | 被测覆盖逻辑描述 (Test Coverage Item Description)            | 优先级 |
| -------------- | -------- | ------------------------------------------------------------ | ------ |
| **TCI-STT-01** | STT      | 验证标准成功路径：`NONE` -> `MIGRATING` -> `COLD`            | P0     |
| **TCI-STT-02** | STT      | 验证阈值拦截路径：`MIGRATING` -> `NONE` (当评论数 > 2000)    | P1     |
| **TCI-STT-03** | STT      | 验证异常回滚路径：`MIGRATING` -> `NONE` (当写入失败时事务回滚) | P0     |
| **TCI-STT-04** | STT      | 验证幂等自愈路径：`MIGRATING` -> `COLD` (重试时发现 HBase 已有数据) | P0     |
| **TCI-DT-01**  | DT       | 验证条件组合：[冷数据=Y, 状态=NONE, 评论<=2000, HBase=成功, MySQL=成功] | P0     |
| **TCI-DT-02**  | DT       | 验证条件组合：[冷数据=Y, 状态=MIGRATING, 评论<=2000, HBase=存在, MySQL=成功] | P0     |
| **TCI-BVA-01** | BVA      | 验证评论数边界值：刚好等于 2000 (允许) 与 2001 (拦截)        | P1     |
| **TCI-BVA-02** | BVA      | 验证时间边界值：刚好满 168h (迁移) 与 167h (跳过)            | P1     |
| **TCI-CON-01** | 并发     | 验证 `MIGRATING` 状态下的点赞/评论写拦截                     | P0     |



### 二、 需求追溯矩阵 (Traceability Matrix)

| 需求 ID    | 质量风险 ID | 覆盖项 ID (TCI) | 测试用例 ID (TC) | 验证重点           |
| ---------- | ----------- | --------------- | ---------------- | ------------------ |
| **REQ-M1** | RSK-1.1.001 | TCI-STT-01      | TC-STT-01        | 基础迁移全流程     |
| **REQ-M2** | RSK-1.2.001 | TCI-STT-02      | TC-STT-02        | 大评论量动态拦截   |
| **REQ-M6** | RSK-1.1.003 | TCI-STT-04      | TC-DT-04         | 幂等自愈与中断恢复 |
| **REQ-M1** | RSK-1.1.001 | TCI-BVA-02      | TC-BVA-06/05     | 7天冷却时间临界点  |
| **REQ-M5** | RSK-1.3.001 | TCI-CON-01      | TC-STT-04        | 迁移中的状态锁保护 |