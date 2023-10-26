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

package org.opengroup.osdu.entitlements.v2.aws.Util;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;

public abstract class DbUtil {

    public static final String DATA_PARTITION = "osdu";
    public static final String DOMAIN = "domain.com";
    public static final String GROUP_TEMPLATE = "group-%s.group@" + DOMAIN;
    public static final String USER_TEMPLATE = "user-%s.user@" + DOMAIN;
    public static final String USER_DOC_COLLECTION_PREFIX = "User-";
    public static final String GROUP_DOC_COLLECTION_PREFIX = "Group-";
    public static final String DEFAULT_APP = "default_app";
    public static final String SECOND_APP = "second_app";
    public static final String DESCRIPTION = "test description: ";

    public static String generateGroupId(int index) {
        return String.format(GROUP_TEMPLATE, index);
    }

    public static String generateUserId(int index) {
        return String.format(USER_TEMPLATE, index);
    }

    public static IdDoc createId(EntityNode entityNodeMDB) {
        return new IdDoc(entityNodeMDB.getNodeId(), entityNodeMDB.getDataPartitionId());
    }

    public static IdDoc createId(String id) {
        return new IdDoc(id, DATA_PARTITION);
    }

    public static IdDoc createIdForUser(int index) {
        return createId(generateUserId(index));
    }

    public static IdDoc createIdForGroup(int index) {
        return createId(generateGroupId(index));
    }

    public static NodeRelationDoc createRelation(IdDoc idDoc, Role role) {
        return new NodeRelationDoc(idDoc, role);
    }

    public static EntityNode convertUserDocToNode(UserDoc userDoc) {
        EntityNode result = new EntityNode();
        result.setNodeId(userDoc.getId().getNodeId());
        result.setType(NodeType.USER);
        result.setDataPartitionId(userDoc.getId().getDataPartitionId());
        return result;
    }

    public static EntityNode convertGroupDocToNode(GroupDoc groupDoc) {
        EntityNode result = new EntityNode();
        result.setNodeId(groupDoc.getId().getNodeId());
        result.setAppIds(groupDoc.getAppIds());
        result.setName(groupDoc.getName());
        result.setType(NodeType.GROUP);
        result.setDescription(groupDoc.getDescription());
        result.setDataPartitionId(groupDoc.getId().getDataPartitionId());
        return result;
    }
}
