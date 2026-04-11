# Status 的 Canal 监听 + HBase 日志

## 1. 目标

- 监听 [pet_system.status](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 表的状态更新
- 将状态变更写入 HBase 时间线表
- 提供按宠物和状态ID分页查询历史变更

## 2. 关键实现位置

- [CanalClientService.java](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- [PetStatusHBaseService.java](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- [StatusController.java](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 配置：[application-dev.yml](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 和 [program-config.yml](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

## 3. Canal 监听规则

- 连接 Canal Server：[canal.host](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html), [canal.port](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html), [canal.destination](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 订阅过滤器：[pet_system.status](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 仅处理 [UPDATE](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 事件
- 仅当 `status_value` 发生变化时写 HBase
- 批次处理：[connector.getWithoutAck(batchSize)](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 成功时 [ack(batchId)](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)，失败时 [rollback()](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

## 4. HBase 表结构

- 表名：`<namespace>:pet_status_history`
- RowKey：[{petId补零10位}_{Long.MAX_VALUE - timestamp}](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 列族：[info](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 列：
  - [info:status_id](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
  - [info:status_name](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
  - [info:status_value](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
  - [info:op_type](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

## 5. 查询接口

- ## [GET /api/status/timeline/{petId}](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 可选参数：
  - [statusId](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
  - [page](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)（从 0 开始）
  - [size](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)（默认 50）

## 6. 读取逻辑

- 使用 HBase 反向扫描（`Scan.setReversed(true)`）
- 根据 RowKey 范围过滤同一 [petId](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)
- 可选 [statusId](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 过滤
- 将 [Long.MAX_VALUE - rowKeyTS](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 转换为时间戳并返回 [TimelineDTO](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html)

## 7. 用例表

| 用例编号 | 描述               | 触发条件                                                     | 预期结果                         |
| -------- | ------------------ | ------------------------------------------------------------ | -------------------------------- |
| S-1      | 状态更新发生       | [pet_system.status](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) UPDATE，`status_value` 改变 | 写入 HBase `pet_status_history`  |
| S-2      | Canal 写入失败     | HBase 写异常                                                 | 事务 rollback，待下次 Canal 重试 |
| S-3      | 查询时间线         | [GET /api/status/timeline/{petId}](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) | 返回按时间倒序的状态记录         |
| S-4      | 按状态ID过滤       | 同上并传 [statusId](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) | 仅返回该状态ID的变化             |
| S-5      | 非 UPDATE 事件忽略 | INSERT/DELETE 或其他表变更                                   | 不写 HBase                       |

## 8. Status 状态转移图

```
stateDiagram-v2
    [*] --> LISTENING
    LISTENING --> PROCESS_EVENT : Canal 接收 UPDATE
    PROCESS_EVENT --> CHECK_CHANGE : 提取 old/new status_value
    CHECK_CHANGE --> WRITE_HBASE : status 有变化
    WRITE_HBASE --> ACK : HBase 写成功
    WRITE_HBASE --> ROLLBACK : 写失败
    ACK --> LISTENING
    ROLLBACK --> LISTENING
```

![PlantUML Diagram](https://uml.planttext.com/plantuml/png/SoWkIImgAStDuUMArefLqDMrK_3n30vnzVFqS-VbWZF14W51_iwkmS7nhc6kVY4ALWhEYNc9EGhF-fO-cx9D8JJ0nJ74bPSBLH58cxE7gxDtlBE7ev-xAr3Ni_u9J_kdAUJdfEZdfPOh0EqlAIsEBqlCAKtbvK9H2z8S7kGPuXhluUGO3D8BKQpmR4xdq_uPJtkcyN8XouFKEpfx0zLvE2KMfoeyRPlvh6F2qwxvcCg2_7ryd21gWQgUBjduOijINYwG05e8m1MYYJkavgK0Gmq0)



## 9. 测试建议

- Canal 监听测试：只处理 [UPDATE](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 事件
- HBase 写入测试：RowKey 反向时间排序
- 查询测试：分页、按 [statusId](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 过滤
- 失败恢复测试：异常时 [rollback](vscode-file://vscode-app/e:/Microsoft VS Code/e7fb5e96c0/resources/app/out/vs/code/electron-browser/workbench/workbench.html) 保持消息未 ack