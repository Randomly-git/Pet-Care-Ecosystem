# Moment冷热分离需求文档

## 1. 目标

- 将“社区动态”热数据保留在 MySQL
- 将符合冷库条件的动态及其评论、点赞以扁平化方式归档到 HBase
- 前端查询对冷热数据透明
- 支持“主动恢复”冷数据到 MySQL

## 2. 迁移规则

- 条件：
  1. `migration_status = 'NONE'`
  2. audit_status = 'APPROVED' 且 last_access_time > 7天未访问
  3. 或：audit_status = 'REJECTED' 且 last_access_time > 3天未访问
  4. audit_status = 'PENDING' (审核中) 的动态严禁移入冷库。
- 过程：
  1. 定时任务扫描符合条件的 MySQL 动态
  2. 尝试获取迁移锁：将 migration_status 原子性改为 MIGRATING
  3. 二次校验：确保 audit_status 仍符合上述 2、3 条规则
  4. 读取动态、评论、点赞、媒体、用户信息
  5. 构建 ColdArchiveData，压缩写入 HBase
  6. 验证 HBase 写入
  7. 删除 MySQL 中动态、评论、点赞

## 2.1 活跃度维护准则
- **审计隔离**：管理员进行的审核（Audit）、巡检查看等后台操作，**不得**触发 `last_access_time` 的更新。
- **用户触发**：该字段仅由普通用户侧的详情查看、点赞、评论等互动行为触发更新。

## 3. 恢复规则

- 触发：
  - MySQL 查询页数据不足补齐
  - 用户点击“激活”冷数据
- 过程：
  1. [MomentService](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 扫描 HBase 冷数据
  2. 发送 [RESTORE_FROM_COLD](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) MQ 事件
  3. 消费者 [CommunityColdStorageEventConsumer](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 从 HBase 读取归档
  4. 写入 MySQL [moments/comments/likes](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
  5. 事务提交后删除 HBase 归档

## 4. 用例

### M-1: 迁移单个老动态到 HBase

| 项目           | 内容                                                         |
| -------------- | ------------------------------------------------------------ |
| **编号**       | M-1                                                          |
| **简述**       | 定时任务扫描冷动态并迁移至 HBase，同时删除 MySQL 数据        |
| **执行者**     | 系统定时任务                                                 |
| **前置条件**   | 1. 动态 `migration_status = 'NONE'`<br>2. 审核状态为 APPROVED 且超过 7 天未访问，或 REJECTED 且超过 3 天未访问<br>3. 动态评论数 ≤ 阈值（默认 2000） |
| **基本事件流** | 1. 定时任务扫描满足条件的动态<br>2. 系统尝试将 `migration_status` 原子改为 `MIGRATING`（CAS 操作）<br>3. 二次校验：再次检查 audit_status 逻辑<br>4. 查询动态、评论、点赞数据<br>5. 构建扁平化冷存档数据（JSON），包含评论+点赞<br>6. 使用 GZIP 压缩存入 HBase<br>7. 验证 HBase 存在性检查<br>8. 删除 MySQL 中的动态、评论、点赞（按外键顺序）<br>9. 执行 flush() 确保 MySQL 删除完成 |
| **扩展事件流** | **E1-HBase 写入失败**：<br> - 验证失败时抛异常<br> - 回滚当前事务<br> - `migration_status` 保持 `MIGRATING`<br> - 下次扫描重新迁移<br><br>**E2-MySQL 删除失败**：<br> - 删除失败时验证失败抛异常<br> - 回滚当前事务<br> - `migration_status` 保持 `MIGRATING`<br> - 下次扫描重试时 HBase 通过 [exists()](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 幂等跳过，直接删除 |
| **后置条件**   | 1. HBase 中存在该动态的冷归档数据<br>2. MySQL 中该动态及所有相关评论、点赞已删除<br>3. 动态记录不再出现在热库中 |

------

### M-2: 迁移失败时跳过大评论量动态

| 项目           | 内容                                                         |
| -------------- | ------------------------------------------------------------ |
| **编号**       | M-2                                                          |
| **简述**       | 在迁移前检查评论数量，超过阈值则暂不迁移                     |
| **执行者**     | 系统定时任务                                                 |
| **前置条件**   | 1. 动态 `migration_status = 'NONE'`<br>2. 动态 `last_access_time` 超过 7 天<br>3. 动态评论数 > 阈值（2000） |
| **基本事件流** | 1. 定时任务选中该动态<br>2. 将 `migration_status` 改为 `MIGRATING`<br>3. 查询评论数量<br>4. 发现超过阈值<br>5. 将 `migration_status` 恢复为 `NONE`<br>6. 记录日志并跳过 |
| **扩展事件流** | 无                                                           |
| **后置条件**   | 1. 动态保留在 MySQL 中<br>2. 动态状态回到 `NONE`<br>3. 下次扫描仍会尝试迁移（仍不符合条件） |

------

### M-3-A: 查询用户动态列表（热数据充足）

| 项目           | 内容                                                         |
| -------------- | ------------------------------------------------------------ |
| **编号**       | M-3-A                                                        |
| **简述**       | 用户查询自己的动态列表，返回 MySQL 热数据                    |
| **执行者**     | 用户 / 前端应用                                              |
| **前置条件**   | 1. 用户已登录<br>2. 该用户的 MySQL 中有 `migration_status = 'NONE'` 的动态 |
| **基本事件流** | 1. 调用 `/api/v1/moments/user/{userId}?pageNum=1&pageSize=10`<br>2. MomentService 查询该用户的所有 `NONE` 状态动态<br>3. 过滤掉 `MIGRATING` 状态的动态<br>4. 更新访问记录（`last_access_time`）<br>5. 返回分页结果 |
| **扩展事件流** | **E1-动态状态为 MIGRATING**：<br> - 该动态被过滤，不返回<br> - 记录日志 |
| **后置条件**   | 1. 返回结果仅包含状态为 `NONE` 的动态<br>2. 动态的 `last_access_time` 被更新为当前时间<br>3. MIGRATING 状态的数据不被返回 |

------

### M-3-B: 查询用户动态列表（热数据不足）

| 项目           | 内容                                                         |
| -------------- | ------------------------------------------------------------ |
| **编号**       | M-3-B                                                        |
| **简述**       | 用户查询动态列表时热数据不足，系统补齐冷数据                 |
| **执行者**     | 用户 / 前端应用                                              |
| **前置条件**   | 1. 用户已登录<br>2. MySQL 中该用户的 `NONE` 状态动态数 < pageSize<br>3. HBase 中存在该用户的冷归档数据 |
| **基本事件流** | 1. 调用 `/api/v1/moments/all?pageNum=1&pageSize=10`<br>2. MomentService 查询 MySQL 热数据<br>3. 发现热数据不足<br>4. 扫描 HBase 冷数据补齐<br>5. 从 HBase 读取冷档案，解压 JSON<br>6. 对每条冷动态发送 [RESTORE_FROM_COLD](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) MQ 事件<br>7. 返回热数据 + 冷数据（混合列表）<br>8. MQ 异步处理恢复 |
| **扩展事件流** | **E1-HBase 读取失败**：<br> - 捕获异常，不影响热数据返回<br> - 记录日志 |
| **后置条件**   | 1. 返回结果包含热数据 + 冷数据<br>2. 冷数据对应的 MQ 恢复事件已发送<br>3. 冷数据在后续异步处理中被恢复到 MySQL |

------

### M-4: 用户主动恢复冷动态

| 项目           | 内容                                                         |
| -------------- | ------------------------------------------------------------ |
| **编号**       | M-4                                                          |
| **简述**       | 用户点击"激活"按钮恢复冷存档动态到热库                       |
| **执行者**     | 用户 / 前端应用                                              |
| **前置条件**   | 1. 动态已在 HBase 中归档且被删除于 MySQL<br>2. 前端展示了冷动态并提供激活选项 |
| **基本事件流** | 1. 用户点击动态的"激活"按钮<br>2. 前端调用恢复接口或发起 MQ 恢复事件<br>3. CommunityColdStorageEventPublisher 构建 [RESTORE_FROM_COLD](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 事件<br>4. 事件发送到 RabbitMQ<br>5. CommunityColdStorageEventConsumer 订阅并处理<br>6. 从 HBase 读取冷档案数据（[getArchiveData](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)）<br>7. 解压解析 JSON，提取动态、评论、点赞<br>8. 批量 INSERT 到 MySQL（动态表、评论表、点赞表）<br>9. 将每条新恢复的评论/点赞的 `migration_status` 置为 `NONE`<br>10. 删除 HBase 中的冷档案<br>11. 返回成功 |
| **扩展事件流** | **E1-HBase 删除失败**：<br> - 捕获异常，MySQL 恢复已成功<br> - 记录日志，下次扫描可重新删除<br><br>**E2-MySQL 写入失败**：<br> - 抛异常，MQ 消息自动重试<br> - HBase 数据保持不变 |
| **后置条件**   | 1. 动态、评论、点赞恢复到 MySQL（状态为 `NONE`）<br>2. HBase 冷档案已删除<br>3. 用户可继续对该动态进行互动 |

------

### M-5: 迁移中动态拒绝评论/点赞

| 项目           | 内容                                                         |
| -------------- | ------------------------------------------------------------ |
| **编号**       | M-5                                                          |
| **简述**       | 动态处于迁移状态时，禁止用户评论或点赞                       |
| **执行者**     | 用户 / 前端应用                                              |
| **前置条件**   | 1. 某动态的 `migration_status = 'MIGRATING'`<br>2. 用户尝试对该动态或该动态下的评论进行评论/点赞 |
| **基本事件流** | 1. 用户尝试评论或点赞<br>2. CommentService / LikeService 调用 [checkTargetNotMigrating()](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)<br>3. 查询目标动态或评论的 `migration_status`<br>4. 检查发现为 `MIGRATING` 状态<br>5. 抛业务异常，返回失败响应 |
| **扩展事件流** | 无                                                           |
| **后置条件**   | 1. 评论/点赞操作被拒绝<br>2. 返回错误信息给用户<br>3. 迁移状态的动态不会被污染新数据 |

------

### M-6: 迁移失败恢复（重试机制）

| 项目           | 内容                                                         |
| -------------- | ------------------------------------------------------------ |
| **编号**       | M-6                                                          |
| **简述**       | 迁移失败后，下次扫描检测 MIGRATING 状态并重试                |
| **执行者**     | 系统定时任务                                                 |
| **前置条件**   | 1. 上次迁移时 HBase 写入失败或 MySQL 删除失败<br>2. 动态 `migration_status = 'MIGRATING'`（被保留） |
| **基本事件流** | 1. 定时任务扫描所有 `MIGRATING` 状态的动态<br>2. 对每条 MIGRATING 动态，调用 [migrateSingleMoment()](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)<br>3. 检查是否为恢复失败的遗留数据（`isResumingFromFailure = true`）<br>4. 跳过状态转换步骤<br>5. 查询 HBase 中是否已存在归档<br>6. 若已存在：跳过 HBase 写入，直接进入删除 MySQL 步骤<br>7. 若不存在：重新写入 HBase<br>8. 删除 MySQL 数据<br>9. 迁移成功或继续失败回退 |
| **扩展事件流** | **E1-HBase 已存在**：<br> - 通过 `exists()` 检测<br> - 继续删除 MySQL 步骤（幂等性）<br><br>**E2-再次失败**：<br> - 回滚事务<br> - `migration_status` 保持 `MIGRATING`<br> - 下次扫描继续重试 |
| **后置条件**   | 1. 若成功：动态迁移完成，MySQL 删除<br>2. 若失败：动态仍保持 `MIGRATING` 状态，等待下次重试 |

------

### M-7: 访问冷动态的查询回源（HBase 直读）

| 项目           | 内容                                                         |
| -------------- | ------------------------------------------------------------ |
| **编号**       | M-7                                                          |
| **简述**       | 用户通过冷动态 ID 直接查询时，系统从 HBase 读取并返回        |
| **执行者**     | 用户 / 前端应用                                              |
| **前置条件**   | 1. 动态已从 MySQL 删除且迁移到 HBase<br>2. 用户持有该动态的 ID 或通过其他方式获知其存在 |
| **基本事件流** | 1. 调用 [/api/v1/moments/{momentId}](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 查询单个动态<br>2. MomentService 在 MySQL 中查询失败<br>3. 调用 HBase 查询逻辑<br>4. 从 HBase 按 RowKey 读取冷档案<br>5. 解压 JSON，解析数据<br>6. 返回动态+评论+点赞信息<br>7. 发送 [RESTORE_FROM_COLD](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) MQ 事件（可选）<br>8. 返回冷数据给用户 |
| **扩展事件流** | **E1-HBase 不存在**：<br> - 返回 404 Not Found<br><br>**E2-HBase 读取异常**：<br> - 记录日志，返回错误 |
| **后置条件**   | 1. 用户看到冷动态数据<br>2. 可选：触发异步恢复流程           |

------

### M-8: 管理员审核动态 (CAS 锁定)

| 项目           | 内容                                                         |
| -------------- | ------------------------------------------------------------ |
| **编号**       | M-8                                                          |
| **简述**       | 管理员对 PENDING 动态进行审核，确保幂等性与数据一致性        |
| **前置条件**   | 动态处于 `NONE` 状态且 `audit_status = 'PENDING'`            |
| **基本事件流** | 1. 管理员调用审核接口（Approve/Reject）<br>2. 系统使用 CAS 乐观锁尝试更新：`WHERE audit_status = 'PENDING'`<br>3. 更新成功后，该动态可被用户查看（若通过）或进入冷迁移队列 |
| **后置条件**   | 1. `audit_status` 更新为目标状态<br>2. 管理员操作不触发 `last_access_time` 的更新 |

------

### M-9: 用户修改已发布动态

| 项目           | 内容                                                         |
| -------------- | ------------------------------------------------------------ |
| **编号**       | M-9                                                          |
| **简述**       | 用户修改内容后，动态必须退回到待审核状态，并保护迁移过程     |
| **执行者**     | 动态作者                                                     |
| **基本事件流** | 1. 用户调用 `PUT /api/v1/moments/{id}`<br>2. 系统检查 `migration_status` 是否为 `MIGRATING`<br>3. 若非迁移中，更新内容并强制将 `audit_status` 重置为 `PENDING`<br>4. 更新 `last_access_time` 确保其变热 |
| **异常流**     | **E1-迁移中修改**: 状态为 `MIGRATING` 时，拒绝修改并提示用户数据整理中 |
| **后置条件**   | 动态进入 `PENDING` 状态，对普通用户不可见，且暂停冷迁移扫描 |

## 5. Moments 状态转移图

```
@startuml
left to right direction
skinparam state {
  BackgroundColor<<active>> #F0F8FF
  BackgroundColor<<archive>> #F5F5F5
  BorderColor Black
  FontSize 11
}

state ACTIVE as "ACTIVE (NONE)" <<active>> {
    state PENDING : do / wait for admin audit
    state APPROVED : entry / set public visibility\ndo / allow user interactions
    state REJECTED : entry / set author-only visibility

    [*] --> PENDING
    PENDING --> APPROVED : adminApprove [migration_status == 'NONE'] / notifyUser
    PENDING --> REJECTED : adminReject [migration_status == 'NONE'] / notifyUser
    
    APPROVED --> PENDING : userUpdate [migration_status == 'NONE'] / hideFromPublic
    REJECTED --> PENDING : userUpdate [migration_status == 'NONE'] / resetAuditStatus
}

state MIGRATING as "MIGRATING" {
    MIGRATING : entry / acquire migration lock (CAS)
    MIGRATING : do / compress and write to HBase\ndo / validate HBase data integrity
    MIGRATING : exit / release lock (on failure)
}

state COLD as "COLD (ARCHIVED)" <<archive>> {
    COLD : do / data exists in HBase only
}

[*] --> ACTIVE : createMoment / set audit_status = 'PENDING'

' --- 迁移逻辑 (Migration Job) ---
APPROVED --> MIGRATING : scheduledScan [last_access > 7d] / startMigration
REJECTED --> MIGRATING : scheduledScan [last_access > 3d] / startMigration

MIGRATING --> ACTIVE : thresholdExceeded [comments > 2000] / rollbackStatus
MIGRATING --> COLD : migrationSuccess [HBase_exists && MySQL_deleted] / commitTransaction

' --- 业务请求拦截 ---
MIGRATING --> MIGRATING : userAction [any_modifying_request] / throw BusinessException

' --- 恢复逻辑 (Restore Consumer) ---
COLD --> ACTIVE : userActivation [trigger restore] / restoreMySQL & deleteHBase

' --- 删除逻辑 ---
PENDING --> [*] : userDelete / deleteMySQL
APPROVED --> [*] : userDelete / deleteMySQL & cleanupHBase (if migrating)
REJECTED --> [*] : userDelete / deleteMySQL
COLD --> [*] : userDelete / publish DELETE_FROM_COLD event
@enduml
```

![PlantUML diagram](https://cdn-0.plantuml.com/plantuml/png/dLL1Rnj55BxlhtWAKcmZHKr128JQg8wzRbF5IR3TNanbJNQUxI6pC-xCh5E3a2Yd2iI18vm0LI1nGPn0gWH_XeJqNp3pFDvkv4XHKALgCddl-xxtlMyszwnZnXMvJ2IE7JWDHaoc3hWmc3cXLM9FX9enmtBmfGxXimHWbsKd4wCBnLjQQdFd3lErSzpPWRSwsvqFEfqhQqmsNHMz5_w58cqu6Yg0NUax_5r7AzSNdoBSlfrydYHBrcPhiFSu1MPX8nxh1uS7QMC3AkH16aIPH-b1U-_WFdm8NCCjE6N2mTYpC9uB1QpWmbMgcqT7lSF7QTkNet9cuJiiEfWLnr9aC1TM70iftE9ZHMXCIdqAXKK3GZaqZAoo5S1U-Z1j3TO0MU6cscnf9HSLr8GQXsy_WQsjdPLokejdkgzef26QivdHSuHXBYQ61G6Zm5rOk7iNQi6TsXDFgxGJuyKZhtKDiIAI47lu2MRkXe3qNwajm9RMaqEFPfovQrLE1SUEqVaHEKwOKTt_npHeqJN3fllqspTfwkxTxpK77ey2LVwq4HDKNbIsnxAdXJ089IL8dPr0lTNiDzQQA2IPpcTUWWMcE9mQuNcTXWUxp6ACqPn9GOFG9VWZepnDVF5YNSapuMWgYRuwqdiTOoPaOR3nPhpMuNwR9gD3lTbhFTZpow77Khx2E2kLHCKamDDOPxsEg2faDK37UCQNw5ioWvwigtDKheotDxlS1TJYrcf9KlEzMt3nz_D_Vtdr-ejN5tzz2_LkQUL3VTm81KarHvU6jzaKUI6HzpEcO2YPTIEMPS7U7NYVXtNJbwp4J69yRWZrxbLGITb-sG4tDMYdMlBqMOR8aSFGBptu4Q3UsTxUfXXgAOzPTX93U1ahsb-cgbyibGp9_55SnkOcT1VzZ_P772Kw98s1IRY1OSekFpyha__vy_kphtwy-EsFyz-_FF_wv_CNluRhAc_LaFYYcWG1GwOMernp_y25cemCFYtGka3dPzMdi5jOeRo-CF2iIdh-_AUpbz-iDjlpJTeWjBIoHOvckLoQjMfWoJndItPdn6I21iooF_368uqFcx0qWCnPKP-z-E7rToyZTU2fVk9yQYDFcphXLeGWm39lrzOIUIQHgM96x50NuzNQrAHHYTlrhD68A-leJuwTGZlTJmVfgDCxx8we7kUeN78F5Izo-Hy0)

------

## 6. 测试要点建议

- 迁移条件测试：7 天未访问可迁移、`MIGRATING` 记录不重复迁移
- HBase 归档完整性：`full_data` 包含 comments/likes
- 恢复流程测试：[RESTORE_FROM_COLD](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 事件后 MySQL 恢复并删除 HBase
- 并发控制：迁移中动态拒绝评论/点赞
