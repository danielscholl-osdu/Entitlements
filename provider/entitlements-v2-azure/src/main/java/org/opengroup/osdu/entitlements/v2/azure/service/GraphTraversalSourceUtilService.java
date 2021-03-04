package org.opengroup.osdu.entitlements.v2.azure.service;

import lombok.RequiredArgsConstructor;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GraphTraversalSourceUtilService {
    private final GremlinConnector gremlinConnector;

    public void addEdge(AddEdgeDto addEdgeDto) {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        GraphTraversal<Vertex, Edge> traversal = graphTraversalSource.V()
                .has(VertexPropertyNames.DATA_PARTITION_ID, addEdgeDto.getDpOfFromNodeId())
                .has(VertexPropertyNames.NODE_ID, addEdgeDto.getFromNodeId())
                .addE(addEdgeDto.getEdgeLabel());
        if (addEdgeDto.getEdgeProperties() != null) {
            for (Map.Entry<String, String> entry: addEdgeDto.getEdgeProperties().entrySet()) {
                traversal = traversal.property(entry.getKey(), entry.getValue());
            }
        }
        traversal = traversal.to(graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, addEdgeDto.getToNodeId())
                .has(VertexPropertyNames.DATA_PARTITION_ID, addEdgeDto.getDpOfToNodeId()));
        gremlinConnector.addEdge(traversal);
    }

    public void removeEdge(RemoveEdgeDto removeEdgeDto) {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Traversal<Vertex, Edge> traversal = graphTraversalSource.V()
                .has(VertexPropertyNames.NODE_ID, removeEdgeDto.getFromNodeId())
                .has(VertexPropertyNames.DATA_PARTITION_ID, removeEdgeDto.getFromDataPartitionId())
                .outE()
                .hasLabel(removeEdgeDto.getEdgeLabel())
                .where(__.otherV()
                        .has(VertexPropertyNames.NODE_ID, removeEdgeDto.getToNodeId())
                        .has(VertexPropertyNames.DATA_PARTITION_ID, removeEdgeDto.getToDataPartitionId()))
                .drop();
        gremlinConnector.removeEdge(traversal);
    }

    public NodeVertex createGroupVertexFromEntityNode(EntityNode entityNode) {
        final GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        GraphTraversal<Vertex, Vertex> traversal = graphTraversalSource.addV(entityNode.getType().toString())
                .property(VertexPropertyNames.NODE_ID, entityNode.getNodeId())
                .property(VertexPropertyNames.NAME, entityNode.getName())
                .property(VertexPropertyNames.DESCRIPTION, entityNode.getDescription())
                .property(VertexPropertyNames.DATA_PARTITION_ID, entityNode.getDataPartitionId());
        entityNode.getAppIds().forEach(appId -> traversal.property(VertexPropertyNames.APP_ID, appId));
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
}
