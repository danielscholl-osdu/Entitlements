package org.opengroup.osdu.entitlements.v2.azure.config;

import com.azure.security.keyvault.secrets.SecretClient;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class CacheConfig {

    @Autowired
    private SecretClient secretClient;

    @Value("${redis.port}")
    private int redisPort;

    @Value("${redis.database}")
    private int redisDatabase;

    @Value("${app.redis.ttl.seconds}")
    private int redisTtlSeconds;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * To make sure a connection to redis is created beforehand,
     * we need to create this spring bean on application startup
     */
    @Bean
    @Lazy(false)
    public RedisCache<String, ParentReferences> groupCacheRedis() {
        return new RedisCache<>(getRedisHostname(), redisPort, getRedisPassword(), redisTtlSeconds, redisDatabase, String.class,
                ParentReferences.class);
    }

    @Bean
    public RedissonClient getRedissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress(String.format("rediss://%s:%d",getRedisHostname(), redisPort))
                .setPassword(getRedisPassword())
                .setDatabase(redisDatabase)
                .setKeepAlive(true)
                .setClientName(applicationName);
        return Redisson.create(config);
    }

    public String getRedisHostname() {
        return KeyVaultFacade.getSecretWithValidation(secretClient, "redis-hostname");
    }

    public String getRedisPassword() {
        return KeyVaultFacade.getSecretWithValidation(secretClient, "redis-password");
    }
}
