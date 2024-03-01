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
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DeleteGroupRepoMongoDB extends BasicEntitlementsHelper implements DeleteGroupRepo {


    @Override
    public Set<String> deleteGroup(EntityNode groupNode) {

        if (groupHelper == null) {
            throw ExceptionGenerator.groupIsNull();
        }

        GroupDoc groupToRemove = groupHelper.getById(new IdDoc(groupNode.getNodeId(), groupNode.getDataPartitionId()));

        Set<IdDoc> usersToUpdateParentRelations = userHelper.getAllChildUsers(groupToRemove.getId());
        groupHelper.removeAllDirectChildrenRelations(groupToRemove.getId());
        userHelper.removeAllDirectChildrenRelations(groupToRemove.getId());
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


        groupHelper.delete(groupToRemove.getId());
        //return IDS then cash will work
        return new HashSet<>();
    }

    @Override
    public Set<String> deleteGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode) {
        return Collections.emptySet();
    }
}
