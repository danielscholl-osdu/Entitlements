/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.removemember;


import io.github.resilience4j.retry.Retry;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.ibm.IBMAppProperties;
import org.opengroup.osdu.entitlements.v2.ibm.spi.BaseRepo;
import org.opengroup.osdu.entitlements.v2.ibm.spi.db.RedisConnector;
import org.opengroup.osdu.entitlements.v2.ibm.spi.operation.RemoveMemberChildUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.ibm.spi.operation.RemoveMemberParentUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.ibm.spi.operation.RemoveUserPartitionAssociationOperationImpl;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.removemember.RemoveMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class RemoveMemberRedisRepo extends BaseRepo implements RemoveMemberRepo {

    @Autowired
    private RedisConnector redisConnector;

    @Autowired
    private IBMAppProperties config;

    @Autowired
    private JaxRsDpsLog log;

    @Autowired
    private WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;

    @Autowired
    private AuditLogger auditLogger;

    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    @Autowired
    private Retry retry;

    @Override
    public Set<String> removeMember(EntityNode groupNode, EntityNode memberNode, RemoveMemberServiceDto removeMemberServiceDto) {
        log.info(String.format("Removing member %s from the group %s and updating the data model in redis", memberNode.getNodeId(), groupNode.getNodeId()));
        List<String> impactedUsers;
        try {
            impactedUsers = retrieveGroupRepo.loadAllChildrenUsers(memberNode).getChildrenUserIds();
            Set<ParentReference> preExistingParents = retrieveGroupRepo.loadAllParents(memberNode).getParentReferences();
            executeParentUpdate(groupNode, removeMemberServiceDto.getChildrenReference());
            executeChildrenUpdate(groupNode, memberNode);
            executeUserPartitionAssociationUpdate(groupNode, memberNode, removeMemberServiceDto.getPartitionId());
        } catch (Exception ex) {
            rollback(ex);
            auditLogger.removeMember(AuditStatus.FAILURE, groupNode.getNodeId(), memberNode.getNodeId(), removeMemberServiceDto.getRequesterId());
            throw ex;
        } finally {
            executedCommands.clear();
        }
        auditLogger.removeMember(AuditStatus.SUCCESS, groupNode.getNodeId(), memberNode.getNodeId(), removeMemberServiceDto.getRequesterId());
        return (impactedUsers == null) ? Collections.emptySet() : new HashSet<>(impactedUsers);
    }

    private void executeParentUpdate(EntityNode groupEntityNode, ChildrenReference childrenReference) {
        Operation updateParentOperation = RemoveMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry)
                .log(log).config(config).groupNode(groupEntityNode).childrenReference(childrenReference).build();
        updateParentOperation.execute();
        executedCommands.push(updateParentOperation);
    }

    private void executeChildrenUpdate(EntityNode groupEntityNode, EntityNode memberNode) {
        Operation updateChildrenOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupEntityNode).memberId(memberNode.getNodeId()).memberPartitionId(memberNode.getDataPartitionId()).build();
        updateChildrenOperation.execute();
        executedCommands.push(updateChildrenOperation);
    }

    private void executeUserPartitionAssociationUpdate(EntityNode groupEntityNode, EntityNode memberNode, String partitionId) {
        if (groupEntityNode.isRootUsersGroup() && !memberNode.isGroup()) {
            Operation removeUserPartitionAssociationOperation = RemoveUserPartitionAssociationOperationImpl.builder().redisConnector(redisConnector)
                    .retry(retry).config(config).log(log).userId(memberNode.getNodeId()).partitionId(partitionId)
                    .whitelistSvcAccBeanConfiguration(whitelistSvcAccBeanConfiguration).build();
            removeUserPartitionAssociationOperation.execute();
            executedCommands.push(removeUserPartitionAssociationOperation);
        }
    }
}
