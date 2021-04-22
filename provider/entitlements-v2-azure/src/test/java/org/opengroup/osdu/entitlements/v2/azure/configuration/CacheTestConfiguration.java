package org.opengroup.osdu.entitlements.v2.azure.configuration;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class CacheTestConfiguration {

    @Primary
    @Bean
    public RedisCache<String, ParentReferences> groupCache() {
        return new RedisCache<>("localhost", 7000, 0, String.class, ParentReferences.class);
    }
}
