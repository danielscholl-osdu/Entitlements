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
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DeleteGroupRepoMongoDB extends BasicEntitlementsHelper implements DeleteGroupRepo {


    @Override
    public Set<String> deleteGroup(EntityNode groupNode) {

        GroupDoc groupToRemove = groupHelper.getById(new IdDoc(groupNode.getNodeId(), groupNode.getDataPartitionId()));
        if (groupHelper == null) {
            throw ExceptionGenerator.groupIsNull();
        }

        Set<IdDoc> usersToUpdateParentRelations = userHelper.getAllChildUsers(groupToRemove.getId());
        groupHelper.removeAllDirectChildrenRelations(groupToRemove.getId());
        //TODO: slow but safe update for now (from user to group). Rewrite to faster implementation in reverse update (from group to user)
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
        //TODO: return IDS then cash will work
        return new HashSet<>();
    }

    @Override
    public Set<String> deleteGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode) {
        return null;
    }
}
