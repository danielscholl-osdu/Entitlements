package org.opengroup.osdu.entitlements.v2.azure.service;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class PartitionCacheTtlService {

    private static final String LOGGER_NAME = "CronJobLogger";
    private static final String CACHE_TTL_PROPERTY_NAME = "ent-cache-ttl";
    private static final String CACHE_FLUSH_TTL_BASE = "ent-cache-flush-ttl-base";
    private static final String CACHE_FLUSH_TTL_JITTER = "ent-cache-flush-ttl-jitter";
    private static final long MAX_TTL_BASE = 10000;
    private static final long MAX_TTL_JITTER = 50000;

    /**
     * Logger is not depending on request data
     */
    private final ILogger logger;
    private final IPartitionFactory partitionFactory;
    private final AzureServicePrincipleTokenService tokenService;

    @Value("${app.redis.ttl.seconds}")
    private int cacheTtl;
    @Value("${cache.flush.ttl.base}")
    private long cacheFlushTtlBase;
    @Value("${cache.flush.ttl.jitter}")
    private long cacheFlushTtlJitter;

    private final ConcurrentMap<String, Long> ttlPerDataPartition = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> baseTtlPerDataPartition = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> jitterTtlPerDataPartition = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        refreshTtlInfo();
    }

    /**
     * Returns ttl in milliseconds of cache of given data partition id
     */
    public long getCacheTtlOfPartition(String dataPartitionId) {
        Long ttl = ttlPerDataPartition.get(dataPartitionId);
        if (ttl == null) {
            return 1000L * cacheTtl;
        }
        return ttl;
    }

    /**
     * Returns the base ttl for cache flush in milliseconds for a given data partition id. Maximum is 10000ms.
     */
    public long getCacheFlushTtlBaseOfPartition(String dataPartitionId) {
        Long baseTtl = baseTtlPerDataPartition.get(dataPartitionId);
        if (baseTtl == null || baseTtl > MAX_TTL_BASE) {
            return cacheFlushTtlBase;
        }
        return baseTtl;
    }

    /**
     * Returns the jitter amount for cache flush in milliseconds for a given data partition id. Maximum is 50000ms.
     */
    public long getCacheFlushTtlJitterOfPartition(String dataPartitionId) {
        Long jitterTtl = jitterTtlPerDataPartition.get(dataPartitionId);
        if (jitterTtl == null || jitterTtl > MAX_TTL_JITTER) {
            return cacheFlushTtlJitter;
        }
        return jitterTtl;
    }

    /**
     * Refresh the ttl info cache every 5 minutes
     */
    @Scheduled(cron = "0 */5 * ? * *")
    protected void refreshTtlInfo() {
        logger.info(LOGGER_NAME, "Starting a scheduled cron job to update cache ttls for data partitions", null);
        DpsHeaders dpsHeaders = new DpsHeaders();
        dpsHeaders.put(DpsHeaders.AUTHORIZATION, "Bearer " + tokenService.getAuthorizationToken());
        dpsHeaders.addCorrelationIdIfMissing();
        IPartitionProvider provider = partitionFactory.create(dpsHeaders);
        getDataPartitionIds(provider).forEach(dataPartitionId -> getPartitionInfo(provider, dataPartitionId)
                .ifPresent(partitionInfo -> {
                    long ttl = 1000L * cacheTtl;
                    long ttlFlushBase = cacheFlushTtlBase;
                    long ttlFlushJitter = cacheFlushTtlJitter;
                    if (partitionInfo.getProperties().containsKey(CACHE_TTL_PROPERTY_NAME)) {
                        ttl = getValueFromKey(partitionInfo, CACHE_TTL_PROPERTY_NAME);
                    }
                    ttlPerDataPartition.put(dataPartitionId, ttl);
                    if (partitionInfo.getProperties().containsKey(CACHE_FLUSH_TTL_BASE)) {
                        ttlFlushBase = getValueFromKey(partitionInfo, CACHE_FLUSH_TTL_BASE);
                    }
                    baseTtlPerDataPartition.put(dataPartitionId, ttlFlushBase);
                    if (partitionInfo.getProperties().containsKey(CACHE_FLUSH_TTL_JITTER)) {
                        ttlFlushJitter = getValueFromKey(partitionInfo, CACHE_FLUSH_TTL_JITTER);
                    }
                    jitterTtlPerDataPartition.put(dataPartitionId, ttlFlushJitter);
                }));
    }

    private Optional<PartitionInfo> getPartitionInfo(IPartitionProvider provider, String dataPartitionId) {
        try {
            return Optional.of(provider.get(dataPartitionId));
        } catch (PartitionException e) {
            logger.warning(LOGGER_NAME, "Couldn't get PartitionInfo from partition service for partition id: " + dataPartitionId, e, null);
            return Optional.empty();
        }
    }

    private List<String> getDataPartitionIds(IPartitionProvider provider) {
        try {
            return provider.list();
        } catch (PartitionException e) {
            logger.warning(LOGGER_NAME, "Couldn't get data partition ids from partition service", e, null);
            return Collections.emptyList();
        }
    }

    private Long getValueFromKey(PartitionInfo partitionInfo, String key) {
        return new Gson().toJsonTree(partitionInfo.getProperties())
                .getAsJsonObject()
                .get(key)
                .getAsJsonObject()
                .get("value").getAsLong();
    }
}
