// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
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

package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.updateappids.UpdateAppIdsRepo;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class UpdateAppIdsRepoMongoDB extends BasicEntitlementsHelper implements UpdateAppIdsRepo {

    @Override
    public Set<String> updateAppIds(EntityNode groupNode, Set<String> allowedAppIds) {

        //TODO: check is need to replace IDS
        groupHelper.updateAppIds(groupNode, allowedAppIds);

        //TODO: return IDS then cash will work
        return new HashSet<>();
    }
}
