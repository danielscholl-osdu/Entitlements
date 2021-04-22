package org.opengroup.osdu.entitlements.v2.azure.service;

import lombok.AllArgsConstructor;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.service.HealthService;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HealthServiceAzure implements HealthService {

    private final ICache<String, ParentReferences> redisGroupCache;

    /**
     * Checks if connection to redis is in healthy state
     */
    @Override
    public void performHealthCheck() {
        redisGroupCache.get("some-key");
    }
}
