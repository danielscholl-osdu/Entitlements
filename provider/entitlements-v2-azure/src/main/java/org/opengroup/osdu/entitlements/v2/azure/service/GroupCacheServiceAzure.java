package org.opengroup.osdu.entitlements.v2.azure.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses.HitsNMissesMetricService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceAzure implements GroupCacheService {
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final ICache<String, ParentReferences> redisGroupCache;
    private final HitsNMissesMetricService metricService;

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        String key = String.format("%s-%s", requesterId, partitionId);
        ParentReferences parentReferences = redisGroupCache.get(key);
        if (parentReferences == null) {
            metricService.sendMissesMetric();
            EntityNode entityNode = EntityNode.createMemberNodeForNewUser(requesterId, partitionId);
            Set<ParentReference> allParents = retrieveGroupRepo.loadAllParents(entityNode).getParentReferences();
            parentReferences = new ParentReferences();
            parentReferences.setParentReferencesOfUser(allParents);
            redisGroupCache.put(key, parentReferences);
        } else {
            metricService.sendHitsMetric();
        }
        return parentReferences.getParentReferencesOfUser();
    }
}
