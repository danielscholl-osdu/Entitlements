/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.entitlements.v2.aws.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.aws.cache.CacheFactory;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceAws implements GroupCacheService {

    private final RetrieveGroupRepo retrieveGroupRepo;
    private final ICache<String, Set<ParentReference>> awsGroupCache = new VmCache<>(300, 1000);

    @Value("${app.domain}")
    private String domain;

    private EntityNode createEntityNode(String requesterId, String partitionId) {
        Pattern domainPattern = Pattern.compile(String.format(".+@%s\\.%s", Pattern.quote(partitionId), Pattern.quote(domain)));
        if (domainPattern.matcher(requesterId).matches()) {
            return EntityNode.createNodeFromGroupEmail(requesterId);
        } else {
            return EntityNode.createMemberNodeForNewUser(requesterId, partitionId);
        }
    }

    private String getKey(String requesterId, String partitionId) {
        return String.format("%s-%s", requesterId, partitionId);
    }

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        String key = getKey(requesterId, partitionId);
        Set<ParentReference> result = awsGroupCache.get(key);
        if (result == null) {
            EntityNode entityNode = createEntityNode(requesterId, partitionId);
            result = retrieveGroupRepo.loadAllParents(entityNode).getParentReferences();
            awsGroupCache.put(key, result);
        }
        return result;
    }

    @Override
    public void refreshListGroupCache(Set<String> userIds, String partitionId) {
      // this method is empty implement in future
    }

    @Override
    public void flushListGroupCacheForUser(String userId, String partitionId) {
        awsGroupCache.delete(getKey(userId, partitionId));
    }
}
