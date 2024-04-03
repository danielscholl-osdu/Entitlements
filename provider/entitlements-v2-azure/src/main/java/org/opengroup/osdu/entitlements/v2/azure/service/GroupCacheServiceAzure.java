package org.opengroup.osdu.entitlements.v2.azure.service;

import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses.HitsNMissesMetricService;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceAzure implements GroupCacheService {
    private final JaxRsDpsLog log;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final RedisAzureCache<String, ParentReferences> redisGroupCache;
    private final HitsNMissesMetricService metricService;
    private final Retry retry;
    private static final String REDIS_KEY_FORMAT = "%s-%s";

    @Value("${redisson.lock.acquisition.timeout}")
    private int redissonLockAcquisitionTimeOut;

    @Value("${redisson.lock.expiration}")
    private int redissonLockExpiration;

    @Value("${app.redis.ttl.seconds}")
    private int cacheTtl;

    @Value("${cache.flush.ttl.base}")
    private long cacheFlushTtlBase;

    @Value("${cache.flush.ttl.jitter}")
    private long cacheFlushTtlJitter;

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        return getFromPartitionCache(requesterId, partitionId, false);
    }

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId, Boolean roleRequired) {
        String cacheKey = String.format(REDIS_KEY_FORMAT, requesterId, partitionId);

        if(Boolean.TRUE == roleRequired)
            cacheKey += "-role";

        ParentReferences parentReferences = redisGroupCache.get(cacheKey);
        if (parentReferences == null) {
            String lockKey = String.format("lock-%s", cacheKey);
            RLock cacheEntryLock = redisGroupCache.getLock(lockKey);
            return lockCacheEntryAndRebuild(cacheEntryLock, cacheKey, requesterId, partitionId, roleRequired);
        } else {
            metricService.sendHitsMetric();
            return parentReferences.getParentReferencesOfUser();
        }
    }

    @Override
    public void refreshListGroupCache(final Set<String> userIds, String partitionId) {
        userIds.forEach(userId -> flushListGroupCacheForUser(userId, partitionId));
    }

    /**
     * Flush the list group cache for user by setting the ttl for that user's cache
     * entry to a small random value between a lower and upper bound range.
     */
    @Override
    public void flushListGroupCacheForUser(String userId, String partitionId) {
        String key = String.format(REDIS_KEY_FORMAT, userId, partitionId);
        if (redisGroupCache.getTtl(key) > cacheFlushTtlBase + cacheFlushTtlJitter) {
            SecureRandom random = new SecureRandom();
            long ttlOfKey = cacheFlushTtlBase + (long) (random.nextDouble() * cacheFlushTtlJitter);
            redisGroupCache.updateTtl(key, ttlOfKey);
        }
    }

    /**
     * The unblock function may throw exception when cache update takes longer than
     * the lock expiration time,
     * so when the time it tries to unlock the lock has already expired or
     * re-acquired by another thread. In this case, since the lock is already
     * released, we just
     * log the error message without doing anything further. The log is for the
     * tracking purpose to understand the possibility so we can adjust parameters
     * accordingly.
     * Refer to: https://github.com/redisson/redisson/issues/581
     */
    private Set<ParentReference> lockCacheEntryAndRebuild(RLock cacheEntryLock, String key, String requesterId,
            String partitionId, Boolean roleRequired) {
        boolean locked = false;
        if (cacheEntryLock == null) {
            log.info("Redis secrets are not available yet");
            ParentReferences parentReferences = rebuildCache(requesterId, partitionId, roleRequired);
            return parentReferences.getParentReferencesOfUser();
        } else {
            try {
                locked = cacheEntryLock.tryLock(redissonLockAcquisitionTimeOut, redissonLockExpiration,
                        TimeUnit.MILLISECONDS);
                if (locked) {
                    metricService.sendMissesMetric();
                    ParentReferences parentReferences = rebuildCache(requesterId, partitionId, roleRequired);
                    long ttlOfKey = 1000L * cacheTtl;
                    redisGroupCache.put(key, ttlOfKey, parentReferences);
                    return parentReferences.getParentReferencesOfUser();
                } else {
                    ParentReferences parentReferences = Retry.decorateSupplier(retry, () -> redisGroupCache.get(key))
                            .get();
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
        }
        throw new AppException(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "Failed to get the groups");
    }

    private ParentReferences rebuildCache(String requesterId, String partitionId, Boolean roleRequired) {
        EntityNode entityNode = EntityNode.createMemberNodeForNewUser(requesterId, partitionId);
        Set<ParentReference> allParents = retrieveGroupRepo.loadAllParents(entityNode, roleRequired).getParentReferences();
        ParentReferences parentReferences = new ParentReferences();
        parentReferences.setParentReferencesOfUser(allParents);
        return parentReferences;
    }
}
