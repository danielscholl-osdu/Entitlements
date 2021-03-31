package org.opengroup.osdu.entitlements.v2.aws.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceAws implements GroupCacheService {
    private final RetrieveGroupRepo retrieveGroupRepo;



    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        //To-be implemented
        return null;
    }
}
