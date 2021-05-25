package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.addmember;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.azure.service.AddEdgeDto;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenTreeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AddMemberRepoGremlin implements AddMemberRepo {
    private final GraphTraversalSourceUtilService graphTraversalSourceUtilService;
    private final AuditLogger auditlogger;
    private final RetrieveGroupRepo retrieveGroupRepo;

    /**
     * Adds a member by adding two edges in the database.
     * Find a way to add two edges in a single request to database.
     */
    @Override
    public Set<String> addMember(EntityNode groupNode, AddMemberRepoDto addMemberRepoDto) {
        ChildrenTreeDto childrenUserDto = retrieveGroupRepo.loadAllChildrenUsers(addMemberRepoDto.getMemberNode());
        Set<String> impactedUsers = new HashSet<>(childrenUserDto.getChildrenUserIds());
        graphTraversalSourceUtilService.createVertexFromEntityNodeIdempotent(addMemberRepoDto.getMemberNode());
        AddEdgeDto.AddEdgeDtoBuilder addChildEdgeRequestBuilder = AddEdgeDto.builder()
                .fromNodeId(groupNode.getNodeId())
                .dpOfFromNodeId(groupNode.getDataPartitionId())
                .toNodeId(addMemberRepoDto.getMemberNode().getNodeId())
                .dpOfToNodeId(addMemberRepoDto.getMemberNode().getDataPartitionId())
                .edgeLabel(EdgePropertyNames.CHILD_EDGE_LB);
        if (Role.MEMBER.equals(addMemberRepoDto.getRole())) {
            addChildEdgeRequestBuilder.edgeProperties(Collections.singletonMap(EdgePropertyNames.ROLE, Role.MEMBER.getValue()));
        } else if (Role.OWNER.equals(addMemberRepoDto.getRole())) {
            addChildEdgeRequestBuilder.edgeProperties(Collections.singletonMap(EdgePropertyNames.ROLE, Role.OWNER.getValue()));
        } else {
            throw new IllegalArgumentException("Role parameter is required to add a member");
        }
        AddEdgeDto addParentEdgeRequestBuilder = AddEdgeDto.builder()
                .fromNodeId(addMemberRepoDto.getMemberNode().getNodeId())
                .dpOfFromNodeId(addMemberRepoDto.getMemberNode().getDataPartitionId())
                .toNodeId(groupNode.getNodeId())
                .dpOfToNodeId(groupNode.getDataPartitionId())
                .edgeLabel(EdgePropertyNames.PARENT_EDGE_LB)
                .build();

        try {
            graphTraversalSourceUtilService.addEdge(addChildEdgeRequestBuilder.build());
            graphTraversalSourceUtilService.addEdge(addParentEdgeRequestBuilder);
            auditlogger.addMember(AuditStatus.SUCCESS, groupNode.getNodeId(), addMemberRepoDto.getMemberNode().getNodeId(),
                    addMemberRepoDto.getRole());
        } catch (Exception e) {
            auditlogger.addMember(AuditStatus.FAILURE, groupNode.getNodeId(), addMemberRepoDto.getMemberNode().getNodeId(),
                    addMemberRepoDto.getRole());
            throw e;
        }
        return new HashSet<>(impactedUsers);
    }

    @Override
    public Set<String> addMember(Deque<Operation> executedCommandsDeque, EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
        return new HashSet<>();
    }
}
