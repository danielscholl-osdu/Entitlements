/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
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

import com.google.common.cache.CacheLoader;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcCacheLoader extends CacheLoader<EntityNode, Set<ParentReference>> {

    private final RetrieveGroupRepo retrieveGroupRepo;

    @Override
    public Set<ParentReference> load(EntityNode key) {
        return retrieveGroupRepo.loadAllParents(key).getParentReferences();
    }

}
