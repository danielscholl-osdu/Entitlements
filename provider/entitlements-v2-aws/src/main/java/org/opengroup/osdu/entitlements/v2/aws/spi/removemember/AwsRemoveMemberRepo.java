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
package org.opengroup.osdu.entitlements.v2.aws.spi.removemember;


import io.github.resilience4j.retry.Retry;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.aws.AwsAppProperties;
import org.opengroup.osdu.entitlements.v2.aws.spi.BaseRepo;
import org.opengroup.osdu.entitlements.v2.aws.spi.db.RedisConnector;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.RemoveMemberChildUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.RemoveMemberParentUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.RemoveUserPartitionAssociationOperationImpl;
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
public class AwsRemoveMemberRepo extends BaseRepo implements RemoveMemberRepo {

    @Autowired
    private RedisConnector redisConnector;

    @Autowired
    private AwsAppProperties config;

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
