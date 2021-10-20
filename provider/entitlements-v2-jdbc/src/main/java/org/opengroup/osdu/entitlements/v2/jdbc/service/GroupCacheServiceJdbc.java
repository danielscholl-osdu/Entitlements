/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.jdbc.JdbcAppProperties;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceJdbc implements GroupCacheService {
    private static final String CACHE_KEY_FORMAT = "%s-%s";

    private final RetrieveGroupRepo retrieveGroupRepo;
    private final VmGroupCache vmGroupCache;
    private final JdbcAppProperties config;

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        String key = String.format(CACHE_KEY_FORMAT, requesterId, partitionId);
        Set<ParentReference> result = vmGroupCache.getGroupCache(key);
        if (result == null) {
            EntityNode entityNode = getNodeByNodeType(requesterId, partitionId);
            result = retrieveGroupRepo.loadAllParents(entityNode).getParentReferences();
            vmGroupCache.addGroupCache(key, result);
        }
        return result;
    }
    @Override
    public void refreshListGroupCache(Set<String> userIds, String partitionId) {
        for (String userId: userIds) {
            String key = String.format(CACHE_KEY_FORMAT, userId, partitionId);
            EntityNode entityNode = EntityNode.createMemberNodeForNewUser(userId, partitionId);
            vmGroupCache.addGroupCache(key, retrieveGroupRepo.loadAllParents(entityNode).getParentReferences());
        }
    }

    @Override
    public void flushListGroupCacheForUser(String userId, String partitionId) {
        String key = String.format(CACHE_KEY_FORMAT, userId, partitionId);
        vmGroupCache.deleteGroupCache(key);
    }

    private EntityNode getNodeByNodeType(String memberId, String partitionId) {
        return memberId.endsWith(String.format("@%s.%s", partitionId, config.getDomain()))
                ? EntityNode.createNodeFromGroupEmail(memberId)
                : EntityNode.createMemberNodeForNewUser(memberId, partitionId);
    }
}
