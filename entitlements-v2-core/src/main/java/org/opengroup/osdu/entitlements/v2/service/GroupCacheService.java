package org.opengroup.osdu.entitlements.v2.service;

import org.opengroup.osdu.entitlements.v2.model.ParentReference;

import java.util.Set;

public interface GroupCacheService {

    Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId);

    default Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId, Boolean roleRequired){
        return getFromPartitionCache(requesterId, partitionId);
    }

    void refreshListGroupCache(final Set<String> userIds, String partitionId);

    void flushListGroupCacheForUser(String userId, String partitionId);
}
