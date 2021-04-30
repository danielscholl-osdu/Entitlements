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
package org.opengroup.osdu.entitlements.v2.aws.spi.renamegroup;


import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.service.ChildReferenceService;
import org.opengroup.osdu.entitlements.v2.service.ParentReferenceService;
import org.opengroup.osdu.entitlements.v2.aws.spi.BaseRepo;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.creategroup.CreateGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.renamegroup.RenameGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class AwsRenameGroupRepo extends BaseRepo implements RenameGroupRepo {

    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    @Autowired
    private CreateGroupRepo createGroupRepo;

    @Autowired
    private DeleteGroupRepo deleteGroupRepo;

    @Autowired
    private AddMemberRepo addMemberRepo;

    @Autowired
    private ChildReferenceService childReferenceService;

    @Autowired
    private ParentReferenceService parentReferenceService;

    @Autowired
    private JaxRsDpsLog log;

    @Autowired
    private WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;

    @Autowired
    private AuditLogger auditLogger;

    @Override
    public Set<String> run(EntityNode existingGroup, String newGroupName) {
        Set<String> impactedUsers = new HashSet<>();
        try {
            final List<ChildrenReference> directChildren = retrieveGroupRepo.loadDirectChildren(existingGroup.getDataPartitionId(), existingGroup.getNodeId());
            final String partitionDomain = existingGroup.getNodeId().split("@")[1];
            Map<Role, List<EntityNode>> directChildrenNodes = childReferenceService.groupChildrenByRole(directChildren, partitionDomain);
            final List<EntityNode> owners = directChildrenNodes.get(Role.OWNER);
            final EntityNode owner = owners.iterator().next();
            owners.remove(owner);

            final EntityNode newGroup = buildNewGroup(existingGroup, partitionDomain, newGroupName);
            impactedUsers.addAll(createGroupRepo.createGroup(executedCommands, newGroup, buildCreateGroupRepoDto(existingGroup, owner)));

            directChildrenNodes.get(Role.OWNER).forEach(ownerNode -> impactedUsers.addAll(addMemberToGroup(newGroup, ownerNode, Role.OWNER)));
            directChildrenNodes.get(Role.MEMBER).forEach(member -> impactedUsers.addAll(addMemberToGroup(newGroup, member, Role.MEMBER)));

            final List<ParentReference> directParents = retrieveGroupRepo.loadDirectParents(existingGroup.getDataPartitionId(), existingGroup.getNodeId());
            final Map<Role, List<EntityNode>> parentsGroupedByGroupRole = parentReferenceService.groupParentsByChildRole(directParents, existingGroup, partitionDomain);
            parentsGroupedByGroupRole.get(Role.MEMBER).forEach(parent -> impactedUsers.addAll(addMemberToGroup(parent, newGroup, Role.MEMBER)));
            parentsGroupedByGroupRole.get(Role.OWNER).forEach(parent -> impactedUsers.addAll(addMemberToGroup(parent, newGroup, Role.OWNER)));

            impactedUsers.addAll(deleteGroupRepo.deleteGroup(executedCommands, existingGroup));
        } catch (Exception ex) {
            rollback(ex);
            auditLogger.updateGroup(AuditStatus.FAILURE, existingGroup.getNodeId());
            throw ex;
        } finally {
            executedCommands.clear();
        }
        auditLogger.updateGroup(AuditStatus.SUCCESS, existingGroup.getNodeId());
        return impactedUsers;
    }

    private Set<String> addMemberToGroup(EntityNode newGroup, EntityNode member, Role role) {
        final Set<ParentReference> allExistingParents = retrieveGroupRepo.loadAllParents(member).getParentReferences();
        final AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
                .memberNode(member)
                .role(role)
                .partitionId(newGroup.getDataPartitionId())
                .existingParents(allExistingParents).build();
        return addMemberRepo.addMember(executedCommands, newGroup, addMemberRepoDto);
    }

    private EntityNode buildNewGroup(EntityNode existingGroup, String partitionDomain, String newGroupName) {
        CreateGroupDto createGroupDto = new CreateGroupDto(newGroupName, existingGroup.getDescription());
        return CreateGroupDto.createGroupNode(createGroupDto, partitionDomain, existingGroup.getDataPartitionId());
    }

    private CreateGroupRepoDto buildCreateGroupRepoDto(EntityNode existingGroup, EntityNode groupOwner) {
        return CreateGroupRepoDto.builder()
                .requesterNode(groupOwner)
                .addDataRootGroup(false)
                .partitionId(existingGroup.getDataPartitionId())
                .build();
    }
}
