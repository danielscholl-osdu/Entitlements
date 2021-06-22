/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.service;

import java.util.Set;

import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
	
@Service	
@RequiredArgsConstructor
public class GroupCacheServiceIBM implements GroupCacheService {

    private final RetrieveGroupRepo retrieveGroupRepo;
    private final IBMGroupCache ibmGroupCache;

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        String key = String.format("%s-%s", requesterId, partitionId);
        Set<ParentReference> result = ibmGroupCache.getGroupCache(key);
        if (result == null) {
            EntityNode entityNode = EntityNode.createMemberNodeForNewUser(requesterId, partitionId);
            result = retrieveGroupRepo.loadAllParents(entityNode).getParentReferences();
            ibmGroupCache.addGroupCache(key, result);
        }
        return result;
    }

	@Override
	public void flushListGroupCacheForUser(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void refreshListGroupCache(Set<String> arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
}
