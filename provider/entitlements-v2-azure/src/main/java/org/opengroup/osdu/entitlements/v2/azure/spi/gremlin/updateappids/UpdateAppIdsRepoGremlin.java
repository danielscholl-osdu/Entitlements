package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.updateappids;

import lombok.RequiredArgsConstructor;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.updateappids.UpdateAppIdsRepo;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
@RequiredArgsConstructor
public class UpdateAppIdsRepoGremlin implements UpdateAppIdsRepo {
    private final GremlinConnector gremlinConnector;
    private final AuditLogger auditLogger;

    @Override
    public void updateAppIds(EntityNode groupNode, Set<String> appIds) {
        try {
            executeUpdateAppIdsOperation(groupNode, appIds);
            auditLogger.updateAppIds(AuditStatus.SUCCESS, groupNode.getNodeId(), appIds);
        } catch (Exception e) {
            auditLogger.updateAppIds(AuditStatus.FAILURE, groupNode.getNodeId(), appIds);
            throw e;
        }
    }

    private void executeUpdateAppIdsOperation(EntityNode groupNode, Set<String> appIds) {
        GraphTraversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.NODE_ID, groupNode.getNodeId())
                .has(VertexPropertyNames.DATA_PARTITION_ID, groupNode.getDataPartitionId())
                .sideEffect(__.properties(VertexPropertyNames.APP_ID).drop())
                .barrier();
        appIds.forEach(appId -> traversal.property(Cardinality.list, VertexPropertyNames.APP_ID, appId));
        gremlinConnector.updateVertex(traversal);
    }
}
