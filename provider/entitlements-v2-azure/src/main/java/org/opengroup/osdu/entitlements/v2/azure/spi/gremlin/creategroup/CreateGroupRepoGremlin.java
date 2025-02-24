package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.creategroup;

import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.creategroup.CreateGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class CreateGroupRepoGremlin implements CreateGroupRepo {

    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;
    @Autowired
    private AddMemberRepo addMemberRepo;
    @Autowired
    private AuditLogger auditLogger;
    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    @Override
    public Set<String> createGroup(EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto) {
        Set<String> impactedUsers;
        try {
            impactedUsers = executeCreateGroupOperation(groupNode, createGroupRepoDto);
            auditLogger.createGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
            return impactedUsers;
        } catch (Exception e) {
            auditLogger.createGroup(AuditStatus.FAILURE, groupNode.getNodeId());
            throw e;
        }
    }

    @Override
    public Set<String> createGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto) {
        return new HashSet<>();
    }

    private void addRootGroupNodeAsMemberOfGroupNewGroup(EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto) {
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
                .memberNode(createGroupRepoDto.getDataRootGroupNode())
                .partitionId(createGroupRepoDto.getPartitionId())
                .role(Role.MEMBER).build();
        addMemberRepo.addMember(groupNode, addMemberRepoDto);
    }

    private void addRequesterAsOwnerMemberToGroup(EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto) {
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
                .memberNode(createGroupRepoDto.getRequesterNode())
                .partitionId(createGroupRepoDto.getPartitionId())
                .role(Role.OWNER).build();
        addMemberRepo.addMember(groupNode, addMemberRepoDto);
    }

    private Set<String> executeCreateGroupOperation(EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto) {
        List<String> impactedUsers = new ArrayList<>();
        impactedUsers.add(createGroupRepoDto.getRequesterNode().getNodeId());
        graphTraversalSourceUtilService.createGroupVertex(groupNode);
        addRequesterAsOwnerMemberToGroup(groupNode, createGroupRepoDto);
        if (createGroupRepoDto.isAddDataRootGroup()) {
            addRootGroupNodeAsMemberOfGroupNewGroup(groupNode, createGroupRepoDto);
            impactedUsers.addAll(retrieveGroupRepo.loadAllChildrenUsers(createGroupRepoDto.getDataRootGroupNode()).getChildrenUserIds());
        }
        return new HashSet<>(impactedUsers);
    }
}
