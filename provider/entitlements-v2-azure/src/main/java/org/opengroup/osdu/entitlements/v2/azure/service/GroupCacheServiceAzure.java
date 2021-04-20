package org.opengroup.osdu.entitlements.v2.azure.service;

import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses.HitsNMissesMetricService;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceAzure implements GroupCacheService {
    private final JaxRsDpsLog log;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final RedisCache<String, ParentReferences> redisGroupCache;
    private final PartitionCacheTtlService partitionCacheTtlService;
    private final HitsNMissesMetricService metricService;
    private final RedissonClient redissonClient;
    private final Retry retry;
    @Value("${redisson.lock.acquisition.timeout}")
    private int redissonLockAcquisitionTimeOut;
    @Value("${redisson.lock.expiration}")
    private int redissonLockExpiration;

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        String key = String.format("%s-%s", requesterId, partitionId);
        ParentReferences parentReferences = redisGroupCache.get(key);
        if (parentReferences == null) {
            RLock cacheEntryLock = redissonClient.getLock(key);
            return lockCacheEntryAndRebuild(cacheEntryLock, key, requesterId, partitionId);
        } else {
            metricService.sendHitsMetric();
            return parentReferences.getParentReferencesOfUser();
        }
    }

    /**
     The unblock function may throw exception when cache update takes longer than the lock expiration time,
     so when the time it tries to unlock the lock has already expired or re-acquired by another thread. In this case, since the lock is already released, we just
     log the error message without doing anything further. The log is for the tracking purpose to understand the possibility so we can adjust parameters accordingly.
     Refer to: https://github.com/redisson/redisson/issues/581
     */
    private Set<ParentReference> lockCacheEntryAndRebuild(RLock cacheEntryLock, String key, String requesterId, String partitionId) {
        boolean locked = false;
        try {
            locked = cacheEntryLock.tryLock(redissonLockAcquisitionTimeOut, redissonLockExpiration, TimeUnit.MILLISECONDS);
            if (locked) {
                metricService.sendMissesMetric();
                ParentReferences parentReferences = rebuildCache(requesterId, partitionId);
                long ttlOfKey = partitionCacheTtlService.getCacheTtlOfPartition(partitionId);
                redisGroupCache.put(key, ttlOfKey, parentReferences);
                return parentReferences.getParentReferencesOfUser();
            } else {
                ParentReferences parentReferences = Retry.decorateSupplier(retry, () -> redisGroupCache.get(key)).get();
                if (parentReferences == null) {
                    metricService.sendMissesMetric();
                } else {
                    metricService.sendHitsMetric();
                    return parentReferences.getParentReferencesOfUser();
                }
            }
        } catch (InterruptedException ex) {
            log.error(String.format("InterruptedException caught when lock the cache key %s: %s", key, ex));
            Thread.currentThread().interrupt();
        } finally {
            if (locked) {
                try {
                    cacheEntryLock.unlock();
                } catch (Exception ex) {
                    log.warning(String.format("unlock exception: %s", ex));
                }
            }
        }
        throw new AppException(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "Failed to get the groups");
    }

    private ParentReferences rebuildCache(String requesterId, String partitionId) {
        EntityNode entityNode = EntityNode.createMemberNodeForNewUser(requesterId, partitionId);
        Set<ParentReference> allParents = retrieveGroupRepo.loadAllParents(entityNode).getParentReferences();
        ParentReferences parentReferences = new ParentReferences();
        parentReferences.setParentReferencesOfUser(allParents);
        return parentReferences;
    }

}
