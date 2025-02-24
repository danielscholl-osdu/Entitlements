/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.entitlements.v2.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@RequiredArgsConstructor
public class RequestScopeGroupsProviderImpl implements GroupsProvider {
  private final Map<String, Set<ParentReference>> groupMap = new HashMap<>();
  private final GroupCacheService groupCacheService;

  @Override
  public Set<ParentReference> getGroupsInContext(String requesterId, String partitionId) {
   return getGroupsInContext(requesterId, partitionId, false);
  }

  @Override
  public Set<ParentReference> getGroupsInContext(String requesterId, String partitionId, Boolean roleRequired) {
    String mapKey = Boolean.TRUE==roleRequired ? requesterId + "-" + partitionId + "-role" : requesterId + "-" + partitionId;
    Set<ParentReference> groups = groupMap.get(mapKey);
    if (groups == null || groups.isEmpty()) {
      groups = groupCacheService.getFromPartitionCache(requesterId, partitionId, roleRequired);
      groupMap.put(mapKey, groups);
    }
    return groups;
  }
}
