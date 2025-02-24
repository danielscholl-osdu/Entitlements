/**
* Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
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

package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.opengroup.osdu.entitlements.v2.aws.util.ExceptionGenerator;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@Component
public class AddMemberRepoMongoDB extends BasicEntitlementsHelper implements AddMemberRepo {


    @Override
    public Set<String> addMember(EntityNode groupNode, AddMemberRepoDto addMemberRepoDtoMDB) {

        GroupDoc groupToAddMember = groupHelper.getById(new IdDoc(groupNode.getNodeId(), groupNode.getDataPartitionId()));
        if (groupToAddMember == null) {
            throw ExceptionGenerator.groupIsNull();
        }

        // adding User node to group
        if (addMemberRepoDtoMDB.getMemberNode().getType() == NodeType.USER) {
            UserDoc userForAddingToGroup = userHelper.getOrCreate(conversionService.convert(addMemberRepoDtoMDB.getMemberNode(), UserDoc.class));

            NodeRelationDoc userToGroupRelation = new NodeRelationDoc(groupToAddMember.getId(), addMemberRepoDtoMDB.getRole());
            userHelper.addDirectRelation(userForAddingToGroup.getId(), userToGroupRelation);

            Set<NodeRelationDoc> parentRelations = groupHelper.getAllParentRelations(groupToAddMember);
            for (NodeRelationDoc parentRelation : parentRelations) {
                parentRelation.setRole(Role.MEMBER);
            }
            parentRelations.add(userToGroupRelation);
            userHelper.addMemberRelations(userForAddingToGroup.getId(), parentRelations);
        }

        // adding Group node to other group
        if (addMemberRepoDtoMDB.getMemberNode().getType() == NodeType.GROUP) {
            GroupDoc groupForAddingToGroup = conversionService.convert(addMemberRepoDtoMDB.getMemberNode(), GroupDoc.class);
            groupForAddingToGroup = groupHelper.getById(groupForAddingToGroup.getId());

            NodeRelationDoc groupToParentGroupRelation = new NodeRelationDoc(groupToAddMember.getId(), Role.MEMBER);
            groupHelper.addDirectRelation(groupForAddingToGroup.getId(), groupToParentGroupRelation);

            Set<NodeRelationDoc> parentRelations = groupHelper.getAllParentRelations(groupToAddMember);
            for (NodeRelationDoc parentRelation : parentRelations) {
                parentRelation.setRole(Role.MEMBER);
            }
            parentRelations.add(groupToParentGroupRelation);
            Set<IdDoc> childUserIds = userHelper.getAllChildUsers(groupForAddingToGroup.getId());
            userHelper.addMemberRelations(childUserIds, parentRelations);
        }

        //return IDS then cash will work
        return new HashSet<>();
    }

    @Override
    public Set<String> addMember(Deque<Operation> executedCommandsDeque, EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
        return Collections.emptySet();
    }
}
