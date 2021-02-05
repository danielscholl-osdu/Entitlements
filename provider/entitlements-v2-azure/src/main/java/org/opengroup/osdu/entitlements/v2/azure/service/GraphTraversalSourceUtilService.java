package org.opengroup.osdu.entitlements.v2.azure.service;

import lombok.RequiredArgsConstructor;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraphTraversalSourceUtilService {
    private final GremlinConnector gremlinConnector;

    public void addEdge(AddEdgeDto addEdgeDto) {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Traversal<Vertex, Edge> traversal = graphTraversalSource.V()
                .has(VertexPropertyNames.NODE_ID, addEdgeDto.getParentNodeId())
                .addE(EdgePropertyNames.EDGE_LB)
                .property(EdgePropertyNames.ROLE, addEdgeDto.getRoleOfChild().getValue())
                .to(graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, addEdgeDto.getChildNodeId())
                        .has(VertexPropertyNames.DATA_PARTITION_ID, addEdgeDto.getDpOfChild()));
        gremlinConnector.addEdge(traversal);
    }

    public void removeEdge(String parentNodeId, String childNodeId) {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Traversal<Vertex, Edge> traversal = graphTraversalSource.V()
                .has(VertexPropertyNames.NODE_ID, parentNodeId)
                .outE()
                .hasLabel(EdgePropertyNames.EDGE_LB)
                .where(__.otherV().has(VertexPropertyNames.NODE_ID, childNodeId))
                .drop();
        gremlinConnector.removeEdge(traversal);
    }

    public NodeVertex createGroupVertexFromEntityNode(EntityNode entityNode) {
        final GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Traversal<Vertex, Vertex> traversal = graphTraversalSource.addV(entityNode.getType().toString())
                .property(VertexPropertyNames.NODE_ID, entityNode.getNodeId())
                .property(VertexPropertyNames.NAME, entityNode.getName())
                .property(VertexPropertyNames.DESCRIPTION, entityNode.getDescription())
                .property(VertexPropertyNames.DATA_PARTITION_ID, entityNode.getDataPartitionId())
                .property(VertexPropertyNames.APP_IDS, entityNode.getAppIds().toString());
        return gremlinConnector.addVertex(traversal);
    }

    public NodeVertex createUserVertexFromEntityNode(EntityNode entityNode) {
        final GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Traversal<Vertex, Vertex> traversal = graphTraversalSource.addV(entityNode.getType().toString())
                .property(VertexPropertyNames.NODE_ID, entityNode.getNodeId())
                .property(VertexPropertyNames.DATA_PARTITION_ID, entityNode.getDataPartitionId());
        return gremlinConnector.addVertex(traversal);
    }

    public void createGroupVertex(EntityNode entityNode) {
        try {
            getVertex(entityNode.getNodeId(), entityNode.getDataPartitionId());
        } catch (AppException e) {
            if (e.getError().getCode() == HttpStatus.NOT_FOUND.value()) {
                createGroupVertexFromEntityNode(entityNode);
                return;
            }
        }
        throw new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), "This group already exists");
    }

    public NodeVertex createVertexFromEntityNodeIdempotent(EntityNode entityNode) {
        try {
            return getVertex(entityNode.getNodeId(), entityNode.getDataPartitionId());
        } catch (AppException e) {
            if (e.getError().getCode() == HttpStatus.NOT_FOUND.value()) {
                return entityNode.isUser() ? createUserVertexFromEntityNode(entityNode) : createGroupVertexFromEntityNode(entityNode);
            }
            return getVertex(entityNode.getNodeId(), entityNode.getDataPartitionId());
        }
    }

    public NodeVertex getVertex(String nodeId, String dataPartitionId) {
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource()
                .V().has(VertexPropertyNames.NODE_ID, nodeId)
                .has(VertexPropertyNames.DATA_PARTITION_ID, dataPartitionId);
        return gremlinConnector.getVertex(traversal).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), "Cannot find Vertex"));
    }

    public void updateProperty(String nodeId, String key, String value) {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Traversal<Vertex, Vertex> traversal = graphTraversalSource.V()
                .has(VertexPropertyNames.NODE_ID, nodeId)
                .property(VertexProperty.Cardinality.single, key, value);
        gremlinConnector.updateVertex(traversal);
    }
}
