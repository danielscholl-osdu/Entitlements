package org.opengroup.osdu.entitlements.v2.gcp.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceGcp implements GroupCacheService {
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final VmGroupCache vmGroupCache;
    private static final String REDIS_KEY_FORMAT = "%s-%s";

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        String key = String.format(REDIS_KEY_FORMAT, requesterId, partitionId);
        Set<ParentReference> result = vmGroupCache.getGroupCache(key);
        if (result == null) {
            EntityNode entityNode = EntityNode.createMemberNodeForNewUser(requesterId, partitionId);
            result = retrieveGroupRepo.loadAllParents(entityNode).getParentReferences();
            vmGroupCache.addGroupCache(key, result);
        }
        return result;
    }

    @Override
    public void refreshListGroupCache(Set<String> userIds, String partitionId) {
        for (String userId: userIds) {
            String key = String.format(REDIS_KEY_FORMAT, userId, partitionId);
            EntityNode entityNode = EntityNode.createMemberNodeForNewUser(userId, partitionId);
            vmGroupCache.addGroupCache(key, retrieveGroupRepo.loadAllParents(entityNode).getParentReferences());
        }
    }

    @Override
    public void flushListGroupCacheForUser(String userId, String partitionId) {
        String key = String.format(REDIS_KEY_FORMAT, userId, partitionId);
        vmGroupCache.deleteGroupCache(key);
    }
}
