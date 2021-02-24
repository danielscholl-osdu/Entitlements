package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.addmember;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.azure.service.AddEdgeDto;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.springframework.stereotype.Repository;

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
        AddEdgeDto.AddEdgeDtoBuilder addEdgeRequestBuilder = AddEdgeDto.builder()
                .childNodeId(addMemberRepoDto.getMemberNode().getNodeId())
                .parentNodeId(groupNode.getNodeId())
                .dpOfChild(addMemberRepoDto.getPartitionId());
        if (Role.MEMBER.equals(addMemberRepoDto.getRole())) {
            addEdgeRequestBuilder.roleOfChild(Role.MEMBER);
            graphTraversalSourceUtilService.addEdge(addEdgeRequestBuilder.build());
        } else if (Role.OWNER.equals(addMemberRepoDto.getRole())) {
            addEdgeRequestBuilder.roleOfChild(Role.OWNER);
            graphTraversalSourceUtilService.addEdge(addEdgeRequestBuilder.build());
        }
        return new HashSet<>();
    }

    @Override
    public Set<String> addMember(Deque<Operation> executedCommandsDeque, EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
        return new HashSet<>();
    }
}
