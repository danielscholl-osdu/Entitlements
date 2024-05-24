// Copyright © Schlumberger
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

package org.opengroup.osdu.entitlements.v2.service;

import java.util.ArrayList;
import java.util.List;

import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;

public interface MemberCacheService {

    default List<ChildrenReference> getFromPartitionCache(String groupId, String partitionId){
        return new ArrayList<ChildrenReference>();
    }

    default void flushListMemberCacheForGroup(String groupId, String partitionId){
    }
}