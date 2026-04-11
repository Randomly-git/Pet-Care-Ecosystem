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

## 6. 主要接口

- [GET /api/activities/records/pet/{petId}](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 可能带参数：[startDate](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html), [endDate](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html), [activityKindId](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html), [page](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html), [size](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

## 7. 用例表

| 用例编号 | 描述                    | 触发条件                                                     | 预期结果                                               |
| -------- | ----------------------- | ------------------------------------------------------------ | ------------------------------------------------------ |
| A-1      | 30天前记录进入冷库      | 定时任务执行，记录 [activityDate](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 超过 30 天 | MySQL 记录状态置为 `MIGRATING`，发布 MQ                |
| A-2      | MQ 写 HBase 成功        | 接收 [MIGRATE_TO_COLD](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 事件 | HBase 写入，MySQL 记录删除                             |
| A-3      | 已迁移记录重复消费      | MQ 重试或重复消息                                            | HBase 存在则直接删除 MySQL，保证幂等                   |
| A-4      | 冷数据查询补齐          | [GET /api/activities/records/pet/{petId}](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 热数据不足 pageSize | 查询 HBase 冷数据并补全                                |
| A-5      | 查询失败时回退          | HBase 查询异常                                               | 返回 MySQL 热数据，不影响主流程                        |
| A-6      | 迁移中记录拒绝编辑/删除 | 对 `MIGRATING` 记录执行修改                                  | 业务层抛出失败，阻止操作                               |
| A-7      | 卡住记录自愈            | `MIGRATING` 状态遗留                                         | 定时任务检查 HBase，已存在则删除 MySQL，否则重写 HBase |

## 8. ActivityRecord 状态转移图

```
stateDiagram-v2
    [*] --> NONE
    NONE --> MIGRATING : 定时任务更新状态
    MIGRATING --> HBASE_WRITTEN : MQ consumer 写 HBase
    HBASE_WRITTEN --> MYSQL_DELETED : MySQL 记录删除
    MYSQL_DELETED --> [*]
```

![PlantUML Diagram](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuUMArefLqDMrK_3pztFbvGAHOAwlftkGOuYddxk2bSBJTREUJT_sPFVkfrqBdytUycpQXkTTsvurDF9o8LI1z7WuEGQxneS7UOQ4kFe1zVa6AYJdvnMNvgOMAJnjcmcKJonEvUL2LGQsB38uq2VUnTN7DSJL1QIn4iXNUB5kmzEzKvzsB7WvSmdGAXGr85r0b_DoEQJcfG3z0G00)

## 9. 测试建议

- 迁移流程测试：`NONE->MIGRATING->HBASE写入->MySQL删除`
- MQ 幂等性测试：重复消费不重复写入
- 查询补齐测试：热数据不足时补齐冷数据
- 卡住记录处理测试：`MIGRATING` 遗留记录自动修复
