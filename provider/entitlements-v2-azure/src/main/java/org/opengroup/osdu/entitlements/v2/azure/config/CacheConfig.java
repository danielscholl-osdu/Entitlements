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

@Configuration
public class CacheConfig {

    @Autowired
    private SecretClient secretClient;

    @Value("${redis.port}")
    private int redisPort;

    @Value("${redis.database}")
    private int redisDatabase;

    @Value("${redisson.connection.timeout}")
    private int redissonConnectionTimeout;

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public int getRedisTtlSeconds() {
        if (System.getenv("REDIS_TTL_SECONDS") == null) return 1;
        else return Integer.parseInt(System.getenv("REDIS_TTL_SECONDS"));
    }

    @Bean
    public RedisCache<String, ParentReferences> groupCacheRedis() {
        return new RedisCache<>(getRedisHostname(), redisPort, getRedisPassword(), getRedisTtlSeconds(), redisDatabase, String.class,
                ParentReferences.class);
    }

    @Bean
    public RedissonClient getRedissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress(String.format("rediss://%s:%d",getRedisHostname(), redisPort))
                .setPassword(getRedisPassword())
                .setDatabase(redisDatabase)
                .setTimeout(redissonConnectionTimeout)
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
