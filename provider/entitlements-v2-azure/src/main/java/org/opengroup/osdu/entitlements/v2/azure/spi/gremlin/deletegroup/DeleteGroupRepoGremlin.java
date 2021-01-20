package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.deletegroup;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class DeleteGroupRepoGremlin implements DeleteGroupRepo {

    @Autowired
    private GremlinConnector gremlinConnector;

    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;

    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    @Override
    public Set<String> deleteGroup(final EntityNode groupNode) {
        List<ParentReference> directParents = retrieveGroupRepo.loadDirectParents(groupNode.getDataPartitionId(), groupNode.getNodeId());
        for (ParentReference ref : directParents) {
            graphTraversalSourceUtilService.removeEdge(ref.getId(), groupNode.getNodeId());
        }
        List<ChildrenReference> directChildren = retrieveGroupRepo.loadDirectChildren(groupNode.getDataPartitionId(), groupNode.getNodeId());
        for (ChildrenReference ref : directChildren) {
            graphTraversalSourceUtilService.removeEdge(groupNode.getNodeId(), ref.getId());
        }
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.NODE_ID, groupNode.getNodeId())
                .drop();
        gremlinConnector.removeVertex(traversal);
        return new HashSet<>();
    }

    @Override
    public Set<String> deleteGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode) {
        return new HashSet<>();
    }
}
