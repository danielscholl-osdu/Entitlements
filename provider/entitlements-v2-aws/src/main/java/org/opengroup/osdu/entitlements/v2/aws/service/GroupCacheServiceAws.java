// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.entitlements.v2.aws.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceAws implements GroupCacheService {

    private final RetrieveGroupRepo retrieveGroupRepo;
    private final AwsGroupCache awsGroupCache;

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        String key = String.format("%s-%s", requesterId, partitionId);
        Set<ParentReference> result = awsGroupCache.getGroupCache(key);
        if (result == null) {
            EntityNode entityNode = EntityNode.createMemberNodeForNewUser(requesterId, partitionId);
            result = retrieveGroupRepo.loadAllParents(entityNode).getParentReferences();
            awsGroupCache.addGroupCache(key, result);
        }
        return result;
    }

    @Override
    public void refreshListGroupCache(Set<String> userIds, String partitionId) {

    }

    @Override
    public void flushListGroupCacheForUser(String userId, String partitionId) {

    }
}
