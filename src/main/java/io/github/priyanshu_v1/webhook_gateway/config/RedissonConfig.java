package io.github.priyanshu_v1.webhook_gateway.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RedissonConfig {

    @Value("${webhook-gateway.redis.mode:single}")
    private String redisMode;

    @Value("${webhook-gateway.redis.host:localhost}")
    private String host;

    @Value("${webhook-gateway.redis.port:6379}")
    private int port;

    @Value("${webhook-gateway.redis.cluster-nodes:}")
    private List<String> clusterNodes;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        if ("cluster".equalsIgnoreCase(redisMode)) {
            var clusterConfig = config.useClusterServers();
            for (String node : clusterNodes) {
                clusterConfig.addNodeAddress("redis://" + node);
            }
        } else {
            config.useSingleServer()
                  .setAddress("redis://" + host + ":" + port);
        }

        return Redisson.create(config);
    }
}
