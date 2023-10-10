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

package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.converter;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class EntityNodeToGroupDocConverter implements Converter<EntityNode, GroupDoc> {
    @Override
    public GroupDoc convert(@NonNull EntityNode entityNodeMDB) {
        GroupDoc result = new GroupDoc();
        IdDoc id = new IdDoc(entityNodeMDB.getNodeId(), entityNodeMDB.getDataPartitionId());

        if (entityNodeMDB.getType() == NodeType.USER) {
            throw new AppException(401, "Incorrect converter was used", "user cannot be converted to GroupDoc");
        }

        result.setId(id);
        result.setAppIds(entityNodeMDB.getAppIds());
        result.setDescription(entityNodeMDB.getDescription());
        result.setName(entityNodeMDB.getName());
        return result;
    }
}
