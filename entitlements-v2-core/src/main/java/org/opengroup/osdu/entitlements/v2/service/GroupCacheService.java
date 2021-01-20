package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupCacheService implements IGroupCacheService {
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final GroupCache vmGroupCache;

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        String key = String.format("%s-%s", requesterId, partitionId);
        Set<ParentReference> result = vmGroupCache.getGroupCache(key);
        if (result == null) {
            EntityNode entityNode = EntityNode.createMemberNodeForNewUser(requesterId, partitionId);
            result = retrieveGroupRepo.loadAllParents(entityNode).getParentReferences();
            vmGroupCache.addGroupCache(key, result);
        }
        return result;
    }
}
