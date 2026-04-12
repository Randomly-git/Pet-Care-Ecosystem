# Moment冷热分离需求文档

## 1. 目标

- 将“社区动态”热数据保留在 MySQL
- 将符合冷库条件的动态及其评论、点赞以扁平化方式归档到 HBase
- 前端查询对冷热数据透明
- 支持“主动恢复”冷数据到 MySQL

## 2. 迁移规则

- 条件：`last_access_time` 7天未访问 / `migration_status = NONE`
- 过程：
  1. 定时任务扫描符合条件的 MySQL 动态
  2. 将状态标记为 `MIGRATING`
  3. 读取动态、评论、点赞、媒体、用户信息
  4. 构建 [ColdArchiveData](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)，压缩写入 HBase
  5. 验证 HBase 写入
  6. 删除 MySQL 中动态、评论、点赞

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
| **前置条件**   | 1. 动态 `migration_status = 'NONE'`<br>2. 动态 `last_access_time` 超过 7 天<br>3. 动态评论数 ≤ 阈值（默认 2000） |
| **基本事件流** | 1. 定时任务扫描满足条件的动态<br>2. 系统尝试将 `migration_status` 原子改为 `MIGRATING`（CAS 操作）<br>3. 查询动态、评论、点赞数据<br>4. 构建扁平化冷存档数据（JSON），包含评论+点赞<br>5. 使用 GZIP 压缩存入 HBase<br>6. 验证 HBase 存在性检查<br>7. 删除 MySQL 中的动态、评论、点赞（按外键顺序）<br>8. 执行 flush() 确保 MySQL 删除完成 |
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

## 5. Moments 状态转移图

```
@startuml
left to right direction
skinparam state {
  BackgroundColor White
  BorderColor Black
  FontSize 11
}

state ACTIVE as "ACTIVE (NONE)" {
    ACTIVE: do / join comments+likes
}

state MIGRATING as "MIGRATING" {
    MIGRATING: do / deny accessing and modifying 
}

state COLD as "COLD (ARCHIVED)" {
    COLD: data exists in HBase only
}

[*] --> ACTIVE

' --- 迁移逻辑 (Migration Job) ---
ACTIVE --> MIGRATING : scan[last_access > 7d] / \n update status to MIGRATING

MIGRATING --> ACTIVE : check_popularity[comments ≥ 2000] / \n rollback status to NONE

MIGRATING --> ACTIVE : migration_failed / \n rollback status to NONE

MIGRATING --> COLD : HBase write success && \n MySQL delete success

' --- 恢复逻辑 (Restore Consumer) ---
COLD --> ACTIVE : user query[trigger restore event] / \n 1. HBase read\n 2. MySQL native insert\n 3. HBase delete(after commit)

' --- 删除逻辑 ---
ACTIVE --> [*] : user delete / delete from MySQL \n and send cleanup event to HBase

COLD --> [*] : user delete / send delete_from_cold event

note top of MIGRATING
  State machine lock:
  Prevents concurrent access
  during migration.
end note

note bottom of COLD
  Flattened storage:
  Comments and likes are
  serialized in full_data.
end note
@enduml
```

![PlantUML diagram](https://cdn-0.plantuml.com/plantuml/png/bPJDJbGn5CVtVOeJ5ZXe-00N9hCWm820uKE1w68aat9xvawTtdPiJz71cCXE5ovSkZC6uqEGwBkOeF0MjlVsUj64XRkUUzhV-Pz_J--I8sx95uefx1EG0Ilp0O6G5ZEIHZCtb7h4BIyWR2M4LmnWXMV3t1glHSSeO-79G1B6xyOAjDMt5HKsCO3xHjE-F46OdsUl6QiWovs3pSThm1rCf6LhPtTdRMgYn4FAjq4Oc8LdHch8J56W9dT7oI6wXhIzkRwtVB2virx2_aG9rAGJIw0U0yyoT4xg7BWMK1WX--CODTJExjPg2ImNhUMzpaRGirhhA_E1o8a3lfIE74WD6olS8HYjnX7KlNq8qzEBgHN6ReLe6Yw_d_xwUdxrvlposmTeRSlSykWoF317Kt43ItR4aqrhRN0Prrt57VKgyR08zyHXQEYf1ZyIKNDKxXsGQGuorZ0QBG6N3J0RzaPcv1MtaiRTsbxuyUuBBCpDpIMsDKeTXOkyHeyNTIEug3lgzRbKAFw7KhdUJZw-i38svQjs9oSZPtky_sWB12fiKhMpFqy_NvozhvtTGqV68dICThv0MvaR2_ojrpkqyDoZ7NV9oZmFaKr7yHWr9HlcPv8gYro4U64cQT6Sv362r8538N4tRKmYMxnF0HdTbJHLIxru--dgurcI-i-bnx59ibAVi_MYRqsHoePASNGTQW6PGgxzgD8BP2e1B3Lx0pAUJ54lSdkPKQ92CAODOG2DmFIlpHB0Vlas2fuDf4PG9Xks6S13Mnvpa1cTUMl3Ehsma1JUIfqtOp73Ok5O89KvCaIcY9MYtlY_K9m8DGg8ry1pZ3KwzNZ6hii_072B8U7GIgxa2GgG6lfUgLvybTVAB8MLBzHl)

------

## 6. 测试要点建议

- 迁移条件测试：7 天未访问可迁移、`MIGRATING` 记录不重复迁移
- HBase 归档完整性：`full_data` 包含 comments/likes
- 恢复流程测试：[RESTORE_FROM_COLD](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 事件后 MySQL 恢复并删除 HBase
- 并发控制：迁移中动态拒绝评论/点赞
