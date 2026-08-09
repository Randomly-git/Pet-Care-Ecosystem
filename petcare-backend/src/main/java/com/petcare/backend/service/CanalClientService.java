package com.petcare.backend.service;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.petcare.backend.config.CanalProperties;
import com.petcare.backend.config.TestProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Canal 客户端服务
 * 异步监听 MySQL 数据变更，同步宠物状态变更到 HBase
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CanalClientService implements CommandLineRunner {

    private final CanalProperties canalProperties;
    private final TestProperties testProperties;
    private final PetStatusHBaseService petStatusHBaseService;

    // 故障模拟开关
    @Value("${test.simulate-failure:false}")
    private boolean isSimulateFailure;

    private volatile boolean running = true;

    @Override
    public void run(String... args) throws Exception {
        // CommandLineRunner 在 Spring 启动阶段执行；这里另起线程，避免阻塞应用启动流程。
        Thread canalThread = new Thread(this::startCanalListener, "Canal-Listener-Thread");
        // 守护线程不会阻止 JVM 正常退出；主应用关闭后监听线程也会随之结束。
        canalThread.setDaemon(true);
        // 启动后台 binlog 消费循环。
        canalThread.start();
        log.info("Canal 监听线程已启动");
    }

    /**
     * 启动 Canal 监听器
     */
    private void startCanalListener() {
        // 连接器生命周期限定在该方法内，finally 中统一断开，避免连接泄漏。
        CanalConnector connector = null;
        try {
            // 创建单机连接器；host/port/destination 来自 canal 配置，账号由 Canal 服务端管理。
            connector = CanalConnectors.newSingleConnector(
                    new InetSocketAddress(canalProperties.getHost(), canalProperties.getPort()),
                    canalProperties.getDestination(),
                    "",
                    ""
            );

            // 建立到 Canal Server 的 TCP 连接。
            connector.connect();
            // 订阅过滤器；当前配置 pet_system.status 让 Canal 只推送目标表的 binlog。
            connector.subscribe(canalProperties.getFilter());
            // 清理上次未确认批次的消费位置，保证未完成的数据可以重新投递。
            connector.rollback();

            log.info("Canal 连接成功，host={}, port={}, destination={}, filter={}",
                    canalProperties.getHost(), canalProperties.getPort(),
                    canalProperties.getDestination(), canalProperties.getFilter());

            // 循环监听
            while (running) {
                try {
                    // 无确认拉取：先拿到批次但不提交消费位点，等 HBase 写成功后再 ack。
                    Message message = connector.getWithoutAck(canalProperties.getBatchSize());
                    // batchId 是 Canal 的消费位点，ack/rollback 都以它为边界。
                    long batchId = message.getId();

                    if (batchId == -1 || message.getEntries().isEmpty()) {
                        // 没有数据时短暂休眠，避免空轮询持续占用 CPU。
                        Thread.sleep(1000);
                        continue;
                    }

                    log.debug("收到 Canal 消息，batchId={}, 条目数={}", batchId, message.getEntries().size());

                    // 逐条解析 binlog；任意一行失败都会让整个批次返回 false。
                    boolean processSuccess = processMessage(message.getEntries());

                    if (processSuccess) {
                        // 只有全部状态变更已写入 HBase 才确认，避免确认后丢失数据。
                        connector.ack(batchId);
                        log.debug("消息处理成功，已 ack: batchId={}", batchId);
                    } else {
                        // 回滚整个批次，下一轮重新投递未完成的数据。
                        connector.rollback();
                        log.warn("消息处理失败，已 rollback: batchId={}", batchId);
                    }

                } catch (Exception e) {
                    log.error("Canal 监听异常", e);
                    // 网络或解析异常同样不能确认，回滚后按原批次重试。
                    try {
                        connector.rollback();
                    } catch (Exception rollbackEx) {
                        log.error("Rollback 失败", rollbackEx);
                    }
                    // 等待一段时间后重试
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            log.error("Canal 连接失败", e);
        } finally {
            if (connector != null) {
                connector.disconnect();
                log.info("Canal 连接已断开");
            }
        }
    }

    /**
     * 处理 Canal 消息
     */
    private boolean processMessage(List<CanalEntry.Entry> entries) {
        // 一个 Canal Message 是一个批次；返回值直接决定 ack 还是 rollback。
        for (CanalEntry.Entry entry : entries) {
            try {
                // Canal 还会发送事务、心跳等元数据事件，这些事件不包含业务行。
                if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                    continue;
                }

                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                CanalEntry.EventType eventType = rowChange.getEventType();

                // 本服务只分析状态更新；INSERT/DELETE 不应写入状态变化时间线。
                if (eventType != CanalEntry.EventType.UPDATE) {
                    continue;
                }

                log.debug("处理 UPDATE 事件，表名={}", entry.getHeader().getTableName());

                // 处理每一行数据
                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    // executeTime 是数据库执行时间，用作 HBase 时间线的排序依据。
                    boolean success = processRowData(rowData, entry.getHeader().getExecuteTime());
                    if (!success) {
                        return false; // 原子地放弃整个批次，避免部分写入造成分析结果不一致。
                    }
                }

            } catch (Exception e) {
                log.error("处理 Canal 消息异常", e);
                return false;
            }
        }
        return true;
    }

    /**
     * 处理单行数据
     */
    private boolean processRowData(CanalEntry.RowData rowData, long executeTime) {
        try {
            // beforeColumns 保存旧值，afterColumns 保存新值；两者共同用于判断字段是否真正变化。
            List<CanalEntry.Column> beforeColumns = rowData.getBeforeColumnsList();
            List<CanalEntry.Column> afterColumns = rowData.getAfterColumnsList();

            // 提取 pet_id
            Long petId = null;
            String oldStatusValue = null;
            String newStatusValue = null;
            Long statusId = null;
            String statusName = null;

            // 从新值中提取写入 HBase 所需的宠物、状态和状态名称。
            for (CanalEntry.Column column : afterColumns) {
                switch (column.getName()) {
                    case "pet_id":
                        petId = Long.valueOf(column.getValue());
                        break;
                    case "status_value":
                        newStatusValue = column.getValue();
                        break;
                    case "status_id":
                        statusId = Long.valueOf(column.getValue());
                        break;
                    case "status_name":
                        statusName = column.getValue();
                        break;
                }
            }

            // 只取旧 status_value，因为本业务只关心状态值变化。
            for (CanalEntry.Column column : beforeColumns) {
                if ("status_value".equals(column.getName())) {
                    oldStatusValue = column.getValue();
                    break;
                }
            }

            // petId 为空说明不是有效状态行；新旧值相同则忽略无效更新，避免制造重复时间线。
            if (petId != null && !equals(oldStatusValue, newStatusValue)) {
                log.info("检测到宠物状态变更: petId={}, oldStatus={}, newStatus={}, timestamp={}",
                        petId, oldStatusValue, newStatusValue, executeTime);

                // 测试开关在真正写 HBase 前抛错，用于验证 Canal rollback 能否重新投递。
                if (isSimulateFailure) {
                    log.warn("故障模拟开关开启，抛出 RuntimeException");
                    throw new RuntimeException("模拟故障：HBase 写入失败");
                }

                // 先持久化到 HBase，外层 processMessage 成功后才会 ack Canal 批次。
                petStatusHBaseService.saveStatusChange(petId, statusId, statusName, newStatusValue, executeTime, "UPDATE");

                log.info("宠物状态变更同步到 HBase 成功: petId={}", petId);
            }

            return true;

        } catch (Exception e) {
            log.error("处理单行数据异常", e);
            return false;
        }
    }

    /**
     * 安全比较字符串（处理 null 值）
     */
    private boolean equals(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.equals(s2);
    }

    /**
     * 停止监听（用于测试或关闭时）
     */
    public void stop() {
        running = false;
        log.info("Canal 监听服务已停止");
    }
}