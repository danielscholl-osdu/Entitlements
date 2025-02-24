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
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.removemember.RemoveMemberRepo;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RemoveMemberRepoMongoDB extends BasicEntitlementsHelper implements RemoveMemberRepo {

    @Override
    public Set<String> removeMember(EntityNode groupNode, EntityNode memberNode, RemoveMemberServiceDto removeMemberServiceDto) {

        IdDoc groupToRemoveFromParents = new IdDoc(groupNode.getNodeId(), groupNode.getDataPartitionId());

        if (memberNode.getType() == NodeType.USER) {
            IdDoc userIdToRemoveParent = new IdDoc(memberNode.getNodeId(), memberNode.getDataPartitionId());
            userHelper.removeDirectParentRelation(userIdToRemoveParent, groupToRemoveFromParents);
            UserDoc userToRemoveParent = userHelper.getById(userIdToRemoveParent);
            Set<NodeRelationDoc> directParentRelations = userToRemoveParent.getDirectParents();
            Set<IdDoc> groupIDs = directParentRelations.stream().map(NodeRelationDoc::getParentId).collect(Collectors.toSet());
            Collection<GroupDoc> directParentGroups = groupHelper.getGroups(groupIDs);
            Set<NodeRelationDoc> userMemberOf = new HashSet<>(directParentRelations);
            for (GroupDoc parentGroup : directParentGroups) {
                userMemberOf.addAll(groupHelper.getAllParentRelations(parentGroup));
            }
            userHelper.rewriteMemberOfRelations(userIdToRemoveParent, userMemberOf);
        }

        if (memberNode.getType() == NodeType.GROUP) {
            IdDoc groupIdToRemoveParent = new IdDoc(memberNode.getNodeId(), memberNode.getDataPartitionId());
            groupHelper.removeDirectParentRelation(groupIdToRemoveParent, groupToRemoveFromParents);

            Set<IdDoc> usersToUpdateParentRelations = userHelper.getAllChildUsers(groupIdToRemoveParent);
            //slow but safe update for now (from user to group). Rewrite to faster implementation in reverse update (from group to user)
            for (IdDoc userIdToUpdateParentRelations : usersToUpdateParentRelations) {
                UserDoc userForUpdate = userHelper.getById(userIdToUpdateParentRelations);
                Set<NodeRelationDoc> directParentRelations = userForUpdate.getDirectParents();
                Set<IdDoc> groupIDs = directParentRelations.stream().map(NodeRelationDoc::getParentId).collect(Collectors.toSet());
                Collection<GroupDoc> directParentGroups = groupHelper.getGroups(groupIDs);
                Set<NodeRelationDoc> userMemberOf = new HashSet<>(directParentRelations);
                for (GroupDoc parentGroup : directParentGroups) {
                    userMemberOf.addAll(groupHelper.getAllParentRelations(parentGroup));
                }
                userHelper.rewriteMemberOfRelations(userIdToUpdateParentRelations, userMemberOf);
            }
        }


        //return IDS then cash will work
        return new HashSet<>();
    }
}
