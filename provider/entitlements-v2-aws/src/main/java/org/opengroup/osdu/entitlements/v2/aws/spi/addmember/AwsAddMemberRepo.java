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

package org.opengroup.osdu.entitlements.v2.aws.spi.addmember;

import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.aws.AwsAppProperties;
import org.opengroup.osdu.entitlements.v2.aws.spi.BaseRepo;
import org.opengroup.osdu.entitlements.v2.aws.spi.db.RedisConnector;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.AddMemberChildUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.AddMemberParentUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.AddUserPartitionAssociationOperationImpl;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ChildrenTreeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AwsAddMemberRepo extends BaseRepo implements AddMemberRepo {

    private static final int MAX_DEPTH = 10;
    private final RedisConnector redisConnector;
    private final AwsAppProperties config;
    private final JaxRsDpsLog log;
    private final AuditLogger auditLogger;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final Retry retry;
    private final WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;

    @Override
    public Set<String> addMember(EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
        log.info(String.format("Adding member %s into the group %s and updating the data model in redis", addMemberRepoDto.getMemberNode().getNodeId(), groupEntityNode.getNodeId()));
        Set<String> impactedUsers;
        try {
            impactedUsers = addMember(executedCommands, groupEntityNode, addMemberRepoDto);
        } catch (Exception ex) {
            rollback(ex);
            auditLogger.addMember(AuditStatus.FAILURE, groupEntityNode.getNodeId(), addMemberRepoDto.getMemberNode().getNodeId(), addMemberRepoDto.getRole());
            throw ex;
        } finally {
            executedCommands.clear();
        }
        auditLogger.addMember(AuditStatus.SUCCESS, groupEntityNode.getNodeId(), addMemberRepoDto.getMemberNode().getNodeId(), addMemberRepoDto.getRole());
        return (impactedUsers == null) ? Collections.emptySet() : impactedUsers;
    }

    @Override
    public Set<String> addMember(Deque<Operation> executedCommandsDeque, EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
        ParentTreeDto parentTreeDto = retrieveGroupRepo.loadAllParents(groupEntityNode);
        ChildrenTreeDto childrenUserDto = retrieveGroupRepo.loadAllChildrenUsers(addMemberRepoDto.getMemberNode());
        if (childrenUserDto.getMaxDepth() + parentTreeDto.getMaxDepth() > MAX_DEPTH) {
            throw new AppException(HttpStatus.PRECONDITION_FAILED.value(), HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), String.format("%s's relationship depth quota hit. The relationship depth can't be deeper than %d", addMemberRepoDto.getMemberNode().getNodeId(), MAX_DEPTH));
        }
        final List<String> impactedUsers = childrenUserDto.getChildrenUserIds();
        executedCommandsDeque.push(executeParentUpdate(groupEntityNode, addMemberRepoDto.getMemberNode(), addMemberRepoDto.getRole()));
        executedCommandsDeque.push(executeChildrenUpdate(groupEntityNode, addMemberRepoDto.getMemberNode()));
        executeUserPartitionAssociationUpdate(groupEntityNode, addMemberRepoDto.getMemberNode(), addMemberRepoDto.getPartitionId())
                .ifPresent(executedCommandsDeque::push);
        return new HashSet<>(impactedUsers);
    }

    private Operation executeParentUpdate(EntityNode groupEntityNode, EntityNode memberNode, Role role) {
        Operation updateParentOperation = AddMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupEntityNode).childrenReference(ChildrenReference.createChildrenReference(memberNode, role)).build();
        updateParentOperation.execute();
        return updateParentOperation;
    }

    private Operation executeChildrenUpdate(EntityNode groupEntityNode, EntityNode memberNode) {
        Operation updateChildrenOperation = AddMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupEntityNode).memberId(memberNode.getNodeId()).memberPartitionId(memberNode.getDataPartitionId()).build();
        updateChildrenOperation.execute();
        return updateChildrenOperation;
    }

    private Optional<Operation> executeUserPartitionAssociationUpdate(EntityNode groupEntityNode, EntityNode memberNode, String partitionId) {
        if (groupEntityNode.isRootUsersGroup() && !memberNode.isGroup()) {
            Operation addUserPartitionAssociationOperation = AddUserPartitionAssociationOperationImpl.builder().redisConnector(redisConnector)
                    .retry(retry).config(config).log(log).userId(memberNode.getNodeId()).partitionId(partitionId)
                    .whitelistSvcAccBeanConfiguration(whitelistSvcAccBeanConfiguration).build();
            addUserPartitionAssociationOperation.execute();
            return Optional.of(addUserPartitionAssociationOperation);
        }
        return Optional.empty();
    }
}
