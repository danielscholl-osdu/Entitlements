package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.removemember;

import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.service.RemoveEdgeDto;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
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

    @Autowired
    private AuditLogger auditLogger;

    /**
     * Removes child edge and parent edge
     */
    @Override
    public Set<String> removeMember(EntityNode groupNode, EntityNode memberNode, RemoveMemberServiceDto removeMemberServiceDto) {
        try {
            executeRemoveMemberOperation(groupNode, memberNode);
            auditLogger.removeMember(AuditStatus.SUCCESS, groupNode.getNodeId(), memberNode.getNodeId(), removeMemberServiceDto.getRequesterId());
            return new HashSet<>();
        } catch (Exception e) {
            auditLogger.removeMember(AuditStatus.FAILURE, groupNode.getNodeId(), memberNode.getNodeId(), removeMemberServiceDto.getRequesterId());
            throw e;
        }
    }

    private void executeRemoveMemberOperation(EntityNode groupNode, EntityNode memberNode) {
        RemoveEdgeDto removeChildEdgeDto = RemoveEdgeDto.builder()
                .fromNodeId(groupNode.getNodeId())
                .fromDataPartitionId(groupNode.getDataPartitionId())
                .toNodeId(memberNode.getNodeId())
                .toDataPartitionId(memberNode.getDataPartitionId())
                .edgeLabel(EdgePropertyNames.CHILD_EDGE_LB)
                .build();
        RemoveEdgeDto removeParentEdgeDto = RemoveEdgeDto.builder()
                .fromNodeId(memberNode.getNodeId())
                .fromDataPartitionId(memberNode.getDataPartitionId())
                .toNodeId(groupNode.getNodeId())
                .toDataPartitionId(groupNode.getDataPartitionId())
                .edgeLabel(EdgePropertyNames.PARENT_EDGE_LB)
                .build();
        graphTraversalSourceUtilService.removeEdge(removeChildEdgeDto);
        graphTraversalSourceUtilService.removeEdge(removeParentEdgeDto);
    }
}
