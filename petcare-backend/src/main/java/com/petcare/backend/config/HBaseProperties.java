package com.petcare.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HBase 配置属性类
 * 从 program-config.yml 中读取 HBase 相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "hbase")
public class HBaseProperties {

    /**
     * HBase ZooKeeper 地址
     */
    private String host;

    /**
     * HBase ZooKeeper 端口
     */
    private int port;

    /**
     * ZooKeeper 客户端连接端口
     */
    private String zookeeperPropertyClientPort;

    /**
     * ZooKeeper ZNode 父节点
     */
    private String zookeeperZnodeParent;

    /**
     * HBase 命名空间
     */
    private String namespace;

    /**
     * 连接池配置
     */
    private Pool pool = new Pool();

    /**
     * 冷数据迁移配置
     */
    private ColdData coldData = new ColdData();

    @Data
    public static class Pool {
        /**
         * 最大连接数
         */
        private int maxTotal = 100;

        /**
         * 最大空闲连接数
         */
        private int maxIdle = 20;

        /**
         * 最小空闲连接数
         */
        private int minIdle = 5;

        /**
         * 借用时测试
         */
        private boolean testOnBorrow = true;

        /**
         * 空闲时测试
         */
        private boolean testWhileIdle = true;
    }

    @Data
    public static class ColdData {
        /**
         * 活动记录冷数据配置
         */
        private ActivityRecord activityRecord = new ActivityRecord();

        @Data
        public static class ActivityRecord {
            /**
             * 超过多少天转为冷数据
             */
            private int daysThreshold = 30;

            /**
             * 批量迁移大小
             */
            private int batchSize = 1000;

            /**
             * 迁移任务 Cron 表达式
             */
            private String migrationCron = "0 0 2 * * ?";
        }
    }
}
