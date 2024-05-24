/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.entitlements.v2.jdbc.JdbcAppProperties;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceJdbc implements GroupCacheService {

    private final JdbcAppProperties config;

    private final ICache<String, ParentReferences> entityGroupsCache;

    private final RetrieveGroupRepo retrieveGroupRepo;

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        return getFromPartitionCache(requesterId, partitionId, Boolean.FALSE);
    }

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId,
        Boolean roleRequired) {

        EntityNode entityNode = getNodeByNodeType(requesterId, partitionId);
        ParentReferences parentReferences =
            getFromCacheOrLoadParentReferences(entityNode, roleRequired);
        return parentReferences.getParentReferencesOfUser();
    }

    @Override
    public void refreshListGroupCache(Set<String> userIds, String partitionId) {
        for (String userId : userIds) {
            flushListGroupCacheForUser(userId, partitionId);
        }
    }

    @Override
    public void flushListGroupCacheForUser(String userId, String partitionId) {
        EntityNode node = getNodeByNodeType(userId, partitionId);
        entityGroupsCache.delete(node.getUniqueIdentifier());
    }

    private EntityNode getNodeByNodeType(String memberId, String partitionId) {
        return memberId.endsWith(String.format("@%s.%s", partitionId, config.getDomain()))
            ? EntityNode.createNodeFromGroupEmail(memberId)
            : EntityNode.createMemberNodeForNewUser(memberId, partitionId);
    }

    private ParentReferences getFromCacheOrLoadParentReferences(EntityNode entityNode,
        boolean roleRequired) {
        String cacheKey = entityNode.getUniqueIdentifier() + "-" + roleRequired;
        ParentReferences parentReferences = this.entityGroupsCache.get(cacheKey);
        if (parentReferences == null) {
            Set<ParentReference> parentReferenceSet = retrieveGroupRepo
                .loadAllParents(entityNode, roleRequired)
                .getParentReferences();
            parentReferences = new ParentReferences();
            parentReferences.setParentReferencesOfUser(parentReferenceSet);
            entityGroupsCache.put(cacheKey, parentReferences);
        }
        return parentReferences;
    }
}
