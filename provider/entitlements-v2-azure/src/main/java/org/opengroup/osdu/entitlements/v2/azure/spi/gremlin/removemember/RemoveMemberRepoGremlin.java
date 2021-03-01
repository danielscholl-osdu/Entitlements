package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.removemember;

import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.service.RemoveEdgeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.removemember.RemoveMemberRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

@Repository
public class RemoveMemberRepoGremlin implements RemoveMemberRepo {

    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;

    @Override
    public Set<String> removeMember(EntityNode groupNode, EntityNode memberNode, RemoveMemberServiceDto removeMemberServiceDto) {
        RemoveEdgeDto removeEdgeDto = RemoveEdgeDto.builder()
                .fromNodeId(groupNode.getNodeId())
                .fromDataPartitionId(groupNode.getDataPartitionId())
                .toNodeId(memberNode.getNodeId())
                .toDataPartitionId(memberNode.getDataPartitionId())
                .build();
        graphTraversalSourceUtilService.removeEdge(removeEdgeDto);
        return new HashSet<>();
    }
}
