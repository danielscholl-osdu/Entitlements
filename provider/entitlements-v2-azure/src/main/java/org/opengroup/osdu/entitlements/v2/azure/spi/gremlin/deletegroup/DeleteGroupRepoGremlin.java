package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.deletegroup;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@Repository
public class DeleteGroupRepoGremlin implements DeleteGroupRepo {

    @Autowired
    private GremlinConnector gremlinConnector;

    @Autowired
    private AuditLogger auditLogger;

    /**
     * Deleting a vertex removes all incoming and outgoing edges
     */
    @Override
    public Set<String> deleteGroup(final EntityNode groupNode) {
        try {
            executeDeleteGroupOperation(groupNode);
            auditLogger.deleteGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
            return new HashSet<>();
        } catch (Exception e) {
            auditLogger.deleteGroup(AuditStatus.FAILURE, groupNode.getNodeId());
            throw e;
        }
    }

    @Override
    public Set<String> deleteGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode) {
        return new HashSet<>();
    }

    private void executeDeleteGroupOperation(final EntityNode groupNode) {
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.NODE_ID, groupNode.getNodeId())
                .has(VertexPropertyNames.DATA_PARTITION_ID, groupNode.getDataPartitionId())
                .drop();
        gremlinConnector.removeVertex(traversal);
    }
}
