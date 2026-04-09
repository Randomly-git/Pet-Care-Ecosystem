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
        // 异步启动 Canal 监听
        Thread canalThread = new Thread(this::startCanalListener, "Canal-Listener-Thread");
        canalThread.setDaemon(true);
        canalThread.start();
        log.info("Canal 监听线程已启动");
    }

    /**
     * 启动 Canal 监听器
     */
    private void startCanalListener() {
        CanalConnector connector = null;
        try {
            // 创建 Canal 连接器
            connector = CanalConnectors.newSingleConnector(
                    new InetSocketAddress(canalProperties.getHost(), canalProperties.getPort()),
                    canalProperties.getDestination(),
                    "",
                    ""
            );

            // 连接
            connector.connect();
            // 订阅过滤器
            connector.subscribe(canalProperties.getFilter());
            // 回滚到未进行 ack 的地方，下次 fetch 时会从最后一个没有 ack 的地方开始
            connector.rollback();

            log.info("Canal 连接成功，host={}, port={}, destination={}, filter={}",
                    canalProperties.getHost(), canalProperties.getPort(),
                    canalProperties.getDestination(), canalProperties.getFilter());

            // 循环监听
            while (running) {
                try {
                    // 获取指定数量的数据
                    Message message = connector.getWithoutAck(canalProperties.getBatchSize());
                    long batchId = message.getId();

                    if (batchId == -1 || message.getEntries().isEmpty()) {
                        // 没有数据，休眠1秒
                        Thread.sleep(1000);
                        continue;
                    }

                    log.debug("收到 Canal 消息，batchId={}, 条目数={}", batchId, message.getEntries().size());

                    // 处理消息
                    boolean processSuccess = processMessage(message.getEntries());

                    if (processSuccess) {
                        // 只有在 HBase 写入成功后才执行 ack
                        connector.ack(batchId);
                        log.debug("消息处理成功，已 ack: batchId={}", batchId);
                    } else {
                        // 处理失败，执行 rollback
                        connector.rollback();
                        log.warn("消息处理失败，已 rollback: batchId={}", batchId);
                    }

                } catch (Exception e) {
                    log.error("Canal 监听异常", e);
                    // 异常情况下也执行 rollback
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
        for (CanalEntry.Entry entry : entries) {
            try {
                // 只处理数据变更事件
                if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                    continue;
                }

                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                CanalEntry.EventType eventType = rowChange.getEventType();

                // 只处理 UPDATE 事件
                if (eventType != CanalEntry.EventType.UPDATE) {
                    continue;
                }

                log.debug("处理 UPDATE 事件，表名={}", entry.getHeader().getTableName());

                // 处理每一行数据
                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    boolean success = processRowData(rowData, entry.getHeader().getExecuteTime());
                    if (!success) {
                        return false; // 如果某行处理失败，整个批次失败
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
            // 获取变更前后的数据
            List<CanalEntry.Column> beforeColumns = rowData.getBeforeColumnsList();
            List<CanalEntry.Column> afterColumns = rowData.getAfterColumnsList();

            // 提取 pet_id
            Long petId = null;
            String oldStatusValue = null;
            String newStatusValue = null;
            Long statusId = null;
            String statusName = null;

            // 从 afterColumns 中提取当前状态信息
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

            // 从 beforeColumns 中提取旧状态值
            for (CanalEntry.Column column : beforeColumns) {
                if ("status_value".equals(column.getName())) {
                    oldStatusValue = column.getValue();
                    break;
                }
            }

            // 检查 status 字段是否发生变更
            if (petId != null && !equals(oldStatusValue, newStatusValue)) {
                log.info("检测到宠物状态变更: petId={}, oldStatus={}, newStatus={}, timestamp={}",
                        petId, oldStatusValue, newStatusValue, executeTime);

                // 故障模拟
                if (isSimulateFailure) {
                    log.warn("故障模拟开关开启，抛出 RuntimeException");
                    throw new RuntimeException("模拟故障：HBase 写入失败");
                }

                // 保存到 HBase
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