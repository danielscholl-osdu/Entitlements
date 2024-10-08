// Copyright © Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.entitlements.v2.azure.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses.HitsNMissesMetricService;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReferences;
import org.opengroup.osdu.entitlements.v2.service.MemberCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class MemberCacheServiceAzure implements MemberCacheService {

    private final JaxRsDpsLog log;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final RedisAzureCache<String, ChildrenReferences> redisMemberCache;
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



    public List<ChildrenReference> getFromPartitionCache(String groupId, String partitionId){
        String cacheKey = String.format(REDIS_KEY_FORMAT, groupId, partitionId);
        ChildrenReferences childrenReferences= redisMemberCache.get(cacheKey);
         if (childrenReferences == null || childrenReferences.getChildReferencesOfGroup() == null) {
            String lockKey = String.format("lock-%s", cacheKey);
            RLock cacheEntryLock = redisMemberCache.getLock(lockKey);
            return lockCacheEntryAndRebuild(cacheEntryLock, cacheKey, groupId, partitionId);
        } else {
            metricService.sendHitsMetric();
            return childrenReferences.getChildReferencesOfGroup();
        }

    }

    public void flushListMemberCacheForGroup(String groupId, String partitionId){
        String key = String.format(REDIS_KEY_FORMAT, groupId, partitionId);
        if (redisMemberCache.getTtl(key) > cacheFlushTtlBase + cacheFlushTtlJitter) {
            SecureRandom random = new SecureRandom();
            long ttlOfKey = cacheFlushTtlBase + (long) (random.nextDouble() * cacheFlushTtlJitter);
            redisMemberCache.updateTtl(key, ttlOfKey);
        }
    }


    private List<ChildrenReference> lockCacheEntryAndRebuild(RLock cacheEntryLock, String key, String groupId,
            String partitionId) {
        boolean locked = false;
        if (cacheEntryLock == null) {
            log.info("Redis secrets are not available yet");
            ChildrenReferences childrenReferences = rebuildCache(groupId, partitionId);
            return childrenReferences.getChildReferencesOfGroup();
        } else {
            try {
                locked = cacheEntryLock.tryLock(redissonLockAcquisitionTimeOut, redissonLockExpiration,
                        TimeUnit.MILLISECONDS);
                if (locked) {
                    metricService.sendMissesMetric();
                    ChildrenReferences childrenReferences = rebuildCache(groupId, partitionId);
                    long ttlOfKey = 1000L * cacheTtl;
                    redisMemberCache.put(key, ttlOfKey, childrenReferences);
                    return childrenReferences.getChildReferencesOfGroup();
                } else {
                    ChildrenReferences childrenReferences = Retry.decorateSupplier(retry, () -> redisMemberCache.get(key))
                            .get();
                    if (childrenReferences == null) {
                        metricService.sendMissesMetric();
                    } else {
                        metricService.sendHitsMetric();
                        return childrenReferences.getChildReferencesOfGroup();
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
                "Failed to get the members");
    }

    private ChildrenReferences rebuildCache(String groupId, String partitionId) {
        List<ChildrenReference> directChildren = retrieveGroupRepo.loadDirectChildren(partitionId,groupId);
        ChildrenReferences childrenReferences = new ChildrenReferences();
        childrenReferences.setChildReferencesOfGroup(directChildren);
        return childrenReferences;
    }
}
