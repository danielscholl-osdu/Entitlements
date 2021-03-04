package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.addmember;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.azure.service.AddEdgeDto;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AddMemberRepoGremlin implements AddMemberRepo {
    private final GraphTraversalSourceUtilService graphTraversalSourceUtilService;

    @Override
    public Set<String> addMember(EntityNode groupNode, AddMemberRepoDto addMemberRepoDto) {
        graphTraversalSourceUtilService.createVertexFromEntityNodeIdempotent(addMemberRepoDto.getMemberNode());
        AddEdgeDto.AddEdgeDtoBuilder addChildEdgeRequestBuilder = AddEdgeDto.builder()
                .fromNodeId(groupNode.getNodeId())
                .dpOfFromNodeId(groupNode.getDataPartitionId())
                .toNodeId(addMemberRepoDto.getMemberNode().getNodeId())
                .dpOfToNodeId(addMemberRepoDto.getMemberNode().getDataPartitionId())
                .edgeLabel(EdgePropertyNames.CHILD_EDGE_LB);
        if (Role.MEMBER.equals(addMemberRepoDto.getRole())) {
            addChildEdgeRequestBuilder.edgeProperties(Collections.singletonMap(EdgePropertyNames.ROLE, Role.MEMBER.getValue()));
            graphTraversalSourceUtilService.addEdge(addChildEdgeRequestBuilder.build());
        } else if (Role.OWNER.equals(addMemberRepoDto.getRole())) {
            addChildEdgeRequestBuilder.edgeProperties(Collections.singletonMap(EdgePropertyNames.ROLE, Role.OWNER.getValue()));
            graphTraversalSourceUtilService.addEdge(addChildEdgeRequestBuilder.build());
        }
        AddEdgeDto addParentEdgeRequestBuilder = AddEdgeDto.builder()
                .fromNodeId(addMemberRepoDto.getMemberNode().getNodeId())
                .dpOfFromNodeId(addMemberRepoDto.getMemberNode().getDataPartitionId())
                .toNodeId(groupNode.getNodeId())
                .dpOfToNodeId(groupNode.getDataPartitionId())
                .edgeLabel(EdgePropertyNames.PARENT_EDGE_LB)
                .build();
        graphTraversalSourceUtilService.addEdge(addParentEdgeRequestBuilder);
        return new HashSet<>();
    }

    @Override
    public Set<String> addMember(Deque<Operation> executedCommandsDeque, EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
        return new HashSet<>();
    }
}
