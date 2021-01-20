package org.opengroup.osdu.entitlements.v2.service;

import org.opengroup.osdu.entitlements.v2.model.ParentReference;

import java.util.Set;

public interface IGroupCacheService {

    Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId);
}
