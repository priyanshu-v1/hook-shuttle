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
	
    @Value("${hook-shuttle.redis.mode}")
    private String redisMode;

    @Value("${hook-shuttle.redis.url}")
    private String redisUrl;

    @Value("${hook-shuttle.redis.cluster-nodes:}")
    private List<String> clusterNodes;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        if ("cluster".equalsIgnoreCase(redisMode)) {
        	
        	// Fail fast if cluster mode is on but no nodes are provided!
            if (clusterNodes == null || clusterNodes.isEmpty()) {
                throw new IllegalStateException("Redis mode is set to 'cluster', but 'hook-shuttle.redis.cluster-nodes' is missing or empty!");
            }
        	
            var clusterConfig = config.useClusterServers();
            for (String node : clusterNodes) {
                clusterConfig.addNodeAddress(node);
            }
        } else {
            config.useSingleServer()
                  .setAddress(redisUrl);
        }

        return Redisson.create(config);
    }
}
