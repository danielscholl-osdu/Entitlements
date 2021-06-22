/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.deletegroup;


import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.ibm.IBMAppProperties;
import org.opengroup.osdu.entitlements.v2.ibm.spi.BaseRepo;
import org.opengroup.osdu.entitlements.v2.ibm.spi.db.RedisConnector;
import org.opengroup.osdu.entitlements.v2.ibm.spi.operation.DeleteGroupOperationImpl;
import org.opengroup.osdu.entitlements.v2.ibm.spi.operation.RemoveMemberChildUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.ibm.spi.operation.RemoveMemberParentUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class DeleteGroupRedisRepo extends BaseRepo implements DeleteGroupRepo {

    private final RedisConnector redisConnector;
    private final JaxRsDpsLog log;
    private final IBMAppProperties config;
    private final AuditLogger auditLogger;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final Retry retry;

    @Override
    public Set<String> deleteGroup(EntityNode groupNode) {
        log.info(String.format("Deleting group %s and updating data model in redis", groupNode.getName()));
        Set<String> impactedUsers;
        try {
            impactedUsers = deleteGroup(executedCommands, groupNode);
        } catch (Exception ex) {
            rollback(ex);
            auditLogger.deleteGroup(AuditStatus.FAILURE, groupNode.getNodeId());
            throw ex;
        } finally {
            executedCommands.clear();
        }
        auditLogger.deleteGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
        return (impactedUsers == null) ? Collections.emptySet() : impactedUsers;
    }

    @Override
    public Set<String> deleteGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode) {
        List<String> impactedUsers = retrieveGroupRepo.loadAllChildrenUsers(groupNode).getChildrenUserIds();
        List<ChildrenReference> directChildren = retrieveGroupRepo.loadDirectChildren(groupNode.getDataPartitionId(), groupNode.getNodeId());
        for (ChildrenReference ref : directChildren) {
            executedCommandsDeque.push(executeParentUpdateForDeleteMember(groupNode, ref));
            executedCommandsDeque.push(executeChildUpdateForDeleteMember(groupNode, ref));
        }
        List<ParentReference> directParents = retrieveGroupRepo.loadDirectParents(groupNode.getDataPartitionId(), groupNode.getNodeId());
        for (ParentReference ref : directParents) {
            executedCommandsDeque.push(executeParentUpdate(groupNode, ref));
            executedCommandsDeque.push(executeChildUpdate(groupNode, ref));
        }
        executedCommandsDeque.push(executeDeleteGroup(groupNode));
        return new HashSet<>(impactedUsers);
    }

    private Operation executeParentUpdateForDeleteMember(EntityNode groupNode, ChildrenReference childrenReference) {
        Operation updateParentOperation = RemoveMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry)
                .log(log).config(config).groupNode(groupNode).childrenReference(childrenReference).build();
        updateParentOperation.execute();
        return updateParentOperation;
    }

    private Operation executeChildUpdateForDeleteMember(EntityNode groupNode, ChildrenReference childrenReference) {
        Operation updateChildrenOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry)
                .log(log).config(config).groupNode(groupNode).memberId(childrenReference.getId()).memberPartitionId(childrenReference.getDataPartitionId()).build();
        updateChildrenOperation.execute();
        return updateChildrenOperation;
    }

    private Operation executeParentUpdate(EntityNode groupNode, ParentReference parentReference) {
        EntityNode parentGroupNode = EntityNode.createNodeFromParentReference(parentReference);
        ChildrenReference childrenReference = ChildrenReference.createChildrenReference(groupNode, Role.MEMBER);
        Operation updateParentOperation = RemoveMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry)
                .log(log).config(config).groupNode(parentGroupNode).childrenReference(childrenReference).build();
        updateParentOperation.execute();
        return updateParentOperation;
    }

    private Operation executeChildUpdate(EntityNode groupNode, ParentReference parentReference) {
        EntityNode parentGroupNode = EntityNode.createNodeFromParentReference(parentReference);
        Operation updateChildrenOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry)
                .log(log).config(config).groupNode(parentGroupNode).memberId(groupNode.getNodeId()).memberPartitionId(groupNode.getDataPartitionId()).build();
        updateChildrenOperation.execute();
        return updateChildrenOperation;
    }

    private Operation executeDeleteGroup(EntityNode groupNode) {
        Operation deleteGroupOperation = DeleteGroupOperationImpl.builder().redisConnector(redisConnector).log(log).config(config)
                .groupNode(groupNode).build();
        deleteGroupOperation.execute();
        return deleteGroupOperation;
    }
}
