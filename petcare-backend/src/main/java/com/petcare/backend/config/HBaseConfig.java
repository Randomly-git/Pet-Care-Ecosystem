package com.petcare.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HBase 连接配置类
 * 负责创建和管理 HBase Connection
 */
@org.springframework.context.annotation.Configuration
@RequiredArgsConstructor
@Slf4j
public class HBaseConfig {

    private final HBaseProperties hBaseProperties;

    /**
     * 创建 HBase Configuration
     */
    @Bean
    public Configuration hbaseConfig() {
        Configuration config = HBaseConfiguration.create();

        // ZooKeeper 连接配置 (必需)
        String quorum = hBaseProperties.getHost();
        String clientPort = hBaseProperties.getZookeeperPropertyClientPort();
        String znodeParent = hBaseProperties.getZookeeperZnodeParent();

        if (StringUtils.hasText(quorum)) {
            config.set("hbase.zookeeper.quorum", quorum);
        }
        if (StringUtils.hasText(clientPort)) {
            config.set("hbase.zookeeper.property.clientPort", clientPort);
        }
        if (StringUtils.hasText(znodeParent)) {
            config.set("zookeeper.znode.parent", znodeParent);
        }

        // 连接参数优化
        config.setInt("hbase.client.ipc.pool.size", 10);
        config.setInt("hbase.client.pause", 100);
        config.setInt("hbase.client.retries.number", 3);
        config.setInt("hbase.client.operation.timeout", 30000);
        config.setInt("hbase.client.scanner.timeout.period", 60000);

        // RPC 工厂类
        config.set("hbase.rpc.factory", "org.apache.hadoop.hbase.ipc.RpcControllerFactoryNetworkTier");

        // 压缩配置
        config.set("hbase.hfile.impl", "org.apache.hadoop.hbase.io.hfile.HFileConfig.AUTOMATIC");

        log.info("【HBase配置】初始化完成 - ZooKeeper: {}:{}", quorum, clientPort);
        log.info("【HBase配置】ZNode Parent: {}", znodeParent);
        log.info("【HBase配置】命名空间: {}", hBaseProperties.getNamespace());

        return config;
    }

    /**
     * 创建 HBase 连接
     * Connection 是重量级对象，整个应用共享一个实例
     */
    @Bean
    public Connection hbaseConnection(Configuration hbaseConfiguration) throws IOException {
        // 创建线程池
        HBaseProperties.Pool poolConfig = hBaseProperties.getPool();
        ExecutorService executorService = Executors.newFixedThreadPool(
                Math.min(poolConfig.getMaxTotal(), 50),
                r -> {
                    Thread t = new Thread(r, "hbase-pool-thread");
                    t.setDaemon(true);
                    return t;
                }
        );

        Connection connection = ConnectionFactory.createConnection(hbaseConfiguration, executorService);

        log.info("【HBase连接】创建成功 - 线程池大小: {}", poolConfig.getMaxTotal());

        return connection;
    }

    /**
     * 获取命名空间
     */
    @Bean
    public String hbaseNamespace() {
        return hBaseProperties.getNamespace();
    }
}
