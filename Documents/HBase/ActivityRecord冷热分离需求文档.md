# `activity_record` 冷热分离与查询

## 1. 目标

- 将 30 天前的活动记录从 MySQL 迁移到 HBase
- 查询时自动合并热数据与冷数据
- 不需要“恢复到 MySQL”流程，冷数据直接读出并补全关联信息

## 2. 关键实现位置

- ## [HBaseColdStorageService.java](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

- [ActivityColdDataMigrationJob.java](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

- [ColdStorageEventConsumer.java](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

- [ActivityServiceImpl.java](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

- [ActivityController.java](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

## 3. HBase 表结构

- 表名：`<namespace>:activity_record`
- RowKey：`{pet_id}_{yyyyMMdd}_{activity_record_id}`
- 列族：`d`
- 列：
  - `d:record_id`
  - `d:activity_id`
  - `d:pet_id`
  - [d:desc](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
  - [d:date](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

## 4. 迁移规则

- 条件：[activityDate](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 提前 30 天
- 状态：`NONE -> MIGRATING`
- 过程：
  1. 定时任务扫描符合条件的 [ActivityRecord](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
  2. 原子性将状态从 `NONE` 更新为 `MIGRATING`
  3. 发布 MQ 事件到 [petcare.activity.cold.migration.exchange](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
  4. MQ consumer [ColdStorageEventConsumer](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 处理：
     - 若 MySQL 记录不存在，直接成功返回
     - 若 HBase 已存在，直接删除 MySQL
     - 否则写入 HBase 再删除 MySQL

## 5. 查询逻辑

- 通过 [GET /api/activities/records/pet/{petId}](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 查询流程：
  1. 先查询 MySQL 热数据
  2. 如果热数据数量 < pageSize，则从 HBase [queryByPetIdAndDateRange](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 补齐
  3. 冷数据补全关联信息（activityName、activityKind、petName、媒体）
  4. 合并结果后按时间降序排序
  5. 总量计算：MySQL total + HBase 补齐数量（去重）

## 6.审核规则与并发控制

- **初始状态**：用户发布动态后，`audit_status` 默认为 `PENDING`，此时对普通用户不可见。 
- **并发控制（乐观锁）**：管理员进行审核操作时，必须使用 CAS (Compare-And-Swap) 机制保障幂等性。  - SQL 示例：`UPDATE moments SET audit_status = 'APPROVED' WHERE id = ? AND audit_status = 'PENDING'` 
- **状态流转**：  - 通过：`PENDING -> APPROVED`，进入 `ACTIVE (NONE)` 状态，参与后续的冷热迁移扫描。  - 拒绝：`PENDING -> REJECTED`，流程终止，不参与冷热迁移。

## 7. 主要接口

- [GET /api/activities/records/pet/{petId}](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 可能带参数：[startDate](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html), [endDate](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html), [activityKindId](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html), [page](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html), [size](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

## 8. 用例表

| 用例编号 | 描述                    | 触发条件                                                     | 预期结果                                               |
| -------- | ----------------------- | ------------------------------------------------------------ | ------------------------------------------------------ |
| A-1      | 30天前记录进入冷库      | 定时任务执行，记录 [activityDate](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 超过 30 天 | MySQL 记录状态置为 `MIGRATING`，发布 MQ                |
| A-2      | MQ 写 HBase 成功        | 接收 [MIGRATE_TO_COLD](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 事件 | HBase 写入，MySQL 记录删除                             |
| A-3      | 已迁移记录重复消费      | MQ 重试或重复消息                                            | HBase 存在则直接删除 MySQL，保证幂等                   |
| A-4      | 冷数据查询补齐          | [GET /api/activities/records/pet/{petId}](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 热数据不足 pageSize | 查询 HBase 冷数据并补全                                |
| A-5      | 查询失败时回退          | HBase 查询异常                                               | 返回 MySQL 热数据，不影响主流程                        |
| A-6      | 迁移中记录拒绝编辑/删除 | 对 `MIGRATING` 记录执行修改                                  | 业务层抛出失败，阻止操作                               |
| A-7      | 卡住记录自愈            | `MIGRATING` 状态遗留                                         | 定时任务检查 HBase，已存在则删除 MySQL，否则重写 HBase |

| **用例编号** | **描述**     | **触发条件**                                   | **预期结果**                                                 |
| ------------ | ------------ | ---------------------------------------------- | ------------------------------------------------------------ |
| B-1          | 发帖默认隔离 | 用户发布新动态                                 | 动态写入 MySQL，`audit_status` 为 `PENDING`，普通用户查询列表时不返回该记录。 |
| B-2          | 审核通过流转 | 管理员调用 `/approve` 接口                     | 数据库原子更新为 `APPROVED`，帖子对外可见，且开始受冷热分离定时任务监控。 |
| B-3          | 审核拒绝流转 | 管理员调用 `/reject` 接口                      | 数据库原子更新为 `REJECTED`，帖子对外不可见，彻底忽略冷热迁移逻辑。 |
| B-4          | 并发审核拦截 | 两名管理员同时对同一 `PENDING` 记录点通过/拒绝 | 仅第一个请求更新成功，第二个请求因 CAS 乐观锁未匹配到 `PENDING` 状态，抛出 400 业务异常（幂等性保障）。 |

![PlantUML diagram](https://cdn-0.plantuml.com/plantuml/png/dLLRJnj757xVNp7AGmoje3Hvg1G9531kGaM04XIfSY9hiZis6_RYpEw6qgeIb43CXK3Ib2PS6j92iAg2cYW14dFvCzwBd_WBFRDdrxOiDIhumTgPS-RxldCvCrSjctBRqJN96b6DFELK9x1bC_AZHE3NHUMH73STGqcQcib9hveRjhzZccQEejta2kDewzB0ETpjvDmShT_yIP8IfFr__y3vorRYxgwKFyoucmlkKi6VtVF69qXdSgZdLeeqbKj_1g-AH5TpdDggQMI4PiSYxIJHrz-NIZIVZGuZlX2Z2uZc2uHQCH_c8a5GXJizLPai-YiFYM8ITxPOFbh3gD7vYjZ-WjmpLOF8fgupmxO-rzGHPeLUuLz4E93gw-xfktRRQ79tNtaR1-M37UID3FM-dGC3W_stKjrLvyhuId3o2FrZO_s1mTGtgUGG7jX_wyqzzKlhw1rRf2Wi3CWjhBlRSnYYztuYEFexDAS_kqDQMZfYIH2UOp6UoPkMJT9t852BsOGwYcf7HMXFHAu9Z1SvK7CTRemdi6Fv04uLNJKoD9_dvWD6qlN0K55InHRCJbw1I1l8Own48qiSTes5itjCjiz34aCbpj7PbwgTFRtlJ-wuXMd_TGcwoPtQAvUMenPRdCN6EbSVt-YvDjWv1064rQwki8rhwxXJ5MQC4IhBpB9K8qUeeH3TLDJi6AmWFiC4J8rbRM9cQuS1wcR8fbDvM3N0mvH7hi3k06SFHCj3xnkompbyH-1WL1mEeBMvRPKOi0a6AHQVxE_j9asTWydhFL15R5YnX-hn4uHJcnBslMe14SpPzIvgCM8QsbWi-AvfswOk98iJSFHhZTesCvW2ys5occD2R3AQpp3eS4O9vGmCqE8grTGVc2BWiuwcPGHbJU-PotwvbKIpjVRHdIgwRyPn62hZfU3uoLdH425ul-___24uMNMdjhntfM1_4YcaU6ZnIWAji6eXzGs7v72J7cnv6uUL5qjkOU_qSALScgfEOwntPY6OF0gsNxkBJzodyzxZBU_PNhC0bAgUWgk-hooP6YIjKSlEOEr91_bA4UDqsuW6AXetswmT53f1OEMtOkNvMszssIii1oSJ_dR9tNmJlDiwFPn7HUxgxrvfvVImK3vUzn_kbK_MlVc9HjcdXpDIJLB3BICFCta4xh2yer6kscCaBSUTqD5EBhMrjQ5MRchQNNZ0wjIAn-6_WMilKfQg6bD8-jD04366w1MMtDcNM4gea_lnbmQIgF7r-naOBClH6S-CWdH6qjZykB0SJFV5Y-J6sCrlUud2D5RR5s849p1ul_xZBUnw4t_00mkfn0ztwN7mw2_8gRlu7BBScDFoyQh_hEWLzltIjb_QyLvk-Mjp8j52N4Cgy9cuxp0EsRMvciinJZWJmyT8U4LWPk2Xnm0uemgiuNf4vGRa4Pu0rG0S6moNOqSCgObcRSR35rQrc-CMnxbm2nkL5viuMTYe-3GBXJ0QiJW4YaiKhRBSr551U2bO3FvaZL7eMvGDjKCX6FKdCSFJk9KHm50xJK4OIReAHaVN_WK0)

## 9. ActivityRecord 状态转移图

```
stateDiagram-v2
    [*] --> NONE
    NONE --> MIGRATING : 定时任务更新状态
    MIGRATING --> HBASE_WRITTEN : MQ consumer 写 HBase
    HBASE_WRITTEN --> MYSQL_DELETED : MySQL 记录删除
    MYSQL_DELETED --> [*]
```

![PlantUML Diagram](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuUMArefLqDMrK_3pztFbvGAHOAwlftkGOuYddxk2bSBJTREUJT_sPFVkfrqBdytUycpQXkTTsvurDF9o8LI1z7WuEGQxneS7UOQ4kFe1zVa6AYJdvnMNvgOMAJnjcmcKJonEvUL2LGQsB38uq2VUnTN7DSJL1QIn4iXNUB5kmzEzKvzsB7WvSmdGAXGr85r0b_DoEQJcfG3z0G00)

## 10. 测试建议

- 迁移流程测试：`NONE->MIGRATING->HBASE写入->MySQL删除`
- MQ 幂等性测试：重复消费不重复写入
- 查询补齐测试：热数据不足时补齐冷数据
- 卡住记录处理测试：`MIGRATING` 遗留记录自动修复
