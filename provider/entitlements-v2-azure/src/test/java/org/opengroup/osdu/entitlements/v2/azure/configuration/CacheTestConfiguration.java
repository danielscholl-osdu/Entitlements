package org.opengroup.osdu.entitlements.v2.azure.configuration;

import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class CacheTestConfiguration {

    @Primary
    @Bean
    public RedisAzureCache<String, ParentReferences> groupCache() {
        return new RedisAzureCache<>(String.class, ParentReferences.class, new RedisAzureConfiguration(0, 3600, 7000, 3600));
    }
}
