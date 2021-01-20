package org.opengroup.osdu.entitlements.v2.gcp.spi.redis;

import io.lettuce.core.api.StatefulRedisConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class RedisConfiguration {

    @Bean
    public GenericObjectPoolConfig<StatefulRedisConnection<String, String>> redisPoolConfig() {
        final GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMinIdle(10);
        poolConfig.setMaxIdle(10);
        poolConfig.setMaxTotal(1000);
        poolConfig.setMaxWaitMillis(15000);
        return poolConfig;
    }
}
