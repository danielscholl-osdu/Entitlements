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

    /**
     * Logger is not depending on request data
     */
    private final ILogger logger;
    private final IPartitionFactory partitionFactory;
    private final AzureServicePrincipleTokenService tokenService;

    @Value("${app.redis.ttl.seconds}")
    private int cacheTtl;

    private final ConcurrentMap<String, Long> ttlPerDataPartition = new ConcurrentHashMap<>();

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
     * Refresh the ttl info cache every 5 minutes
     */
    @Scheduled(cron = "0 */5 * ? * *")
    private void refreshTtlInfo() {
        logger.info(LOGGER_NAME, "Starting a scheduled cron job to update cache ttls for data partitions", null);
        DpsHeaders dpsHeaders = new DpsHeaders();
        dpsHeaders.put(DpsHeaders.AUTHORIZATION, "Bearer " + tokenService.getAuthorizationToken());
        dpsHeaders.addCorrelationIdIfMissing();
        IPartitionProvider provider = partitionFactory.create(dpsHeaders);
        getDataPartitionIds(provider).forEach(dataPartitionId -> getPartitionInfo(provider, dataPartitionId)
                .ifPresent(partitionInfo -> {
                    long ttl = 1000L * cacheTtl;
                    if (partitionInfo.getProperties().containsKey(CACHE_TTL_PROPERTY_NAME)) {
                        ttl = new Gson().toJsonTree(partitionInfo.getProperties())
                                .getAsJsonObject()
                                .get(CACHE_TTL_PROPERTY_NAME)
                                .getAsJsonObject()
                                .get("value").getAsLong();
                    }
                    ttlPerDataPartition.put(dataPartitionId, ttl);
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
}
