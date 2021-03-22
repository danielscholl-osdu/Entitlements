package org.opengroup.osdu.entitlements.v2.azure.config;

import com.azure.security.keyvault.secrets.SecretClient;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Autowired
    private SecretClient secretClient;

    @Value("${redis.port:6380}")
    private int redisPort;

    @Value("${redis.database:8}")
    private int redisDatabase;

    @Bean
    public int getRedisTtlSeconds() {
        if(System.getenv("REDIS_TTL_SECONDS") == null) return 1;
        else return Integer.parseInt(System.getenv("REDIS_TTL_SECONDS"));
    }

    @Bean
    public RedisCache<String, ParentReferences> groupCacheRedis() {
        return new RedisCache<>(getRedisHostname(), redisPort, getRedisPassword(), getRedisTtlSeconds(), redisDatabase, String.class,
                ParentReferences.class);
    }

    public String getRedisHostname() {
        return KeyVaultFacade.getSecretWithValidation(secretClient, "redis-hostname");
    }

    public String getRedisPassword() {
        return KeyVaultFacade.getSecretWithValidation(secretClient, "redis-password");
    }
}
