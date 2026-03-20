package petcare.example.community_backend.config;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * HBase 连接配置
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class HBaseConfig {

    private final HBaseProperties hBaseProperties;

    @Bean
    public org.apache.hadoop.conf.Configuration hbaseConfiguration() {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();

        config.set("hbase.zookeeper.quorum", hBaseProperties.getHost());
        config.set("hbase.zookeeper.property.clientPort",
                String.valueOf(hBaseProperties.getPort()));
        config.set("zookeeper.znode.parent", hBaseProperties.getZookeeperZnodeParent());

        config.setInt("hbase.rpc.timeout", 60000);
        config.setInt("hbase.client.operation.timeout", 60000);
        config.setInt("hbase.client.scanner.timeout.period", 60000);

        log.info("HBase 配置初始化: host={}, port={}, namespace={}",
                hBaseProperties.getHost(), hBaseProperties.getPort(), hBaseProperties.getNamespace());

        return config;
    }

    @Bean
    public Connection hbaseConnection(org.apache.hadoop.conf.Configuration hbaseConfiguration) {
        try {
            Connection connection = ConnectionFactory.createConnection(hbaseConfiguration);
            log.info("HBase 连接创建成功");
            return connection;
        } catch (IOException e) {
            log.error("HBase 连接创建失败: {}", e.getMessage(), e);
            throw new RuntimeException("HBase 连接创建失败", e);
        }
    }
}
