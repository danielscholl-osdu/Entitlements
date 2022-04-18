package org.opengroup.osdu.entitlements.v2.azure.config;

import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class CacheConfig {

    @Value("${redis.port}")
    private int redisPort;

    @Value("${redis.database}")
    private int redisDatabase;

    @Value("${app.redis.ttl.seconds}")
    private int redisTtlSeconds;

    @Value("${redis.expiration:3600}")
    private int redisExpiration;


    /**
     * To make sure a connection to redis is created beforehand,
     * we need to create this spring bean on application startup
     */
    @Bean
    @Lazy(false)
    public RedisAzureCache<String, ParentReferences> groupCacheRedis() {
        return new RedisAzureCache<String, ParentReferences>(String.class,ParentReferences.class, new RedisAzureConfiguration(redisDatabase, redisExpiration, redisPort, redisTtlSeconds));
    }
}
