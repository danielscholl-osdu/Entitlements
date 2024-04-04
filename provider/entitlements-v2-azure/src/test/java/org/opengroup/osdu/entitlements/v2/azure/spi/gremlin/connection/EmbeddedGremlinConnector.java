package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeEdge;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.StepLabel;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.NodeType;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EmbeddedGremlinConnector implements GremlinConnector {
    private Graph graph;

    @PostConstruct
    protected void init() {
        graph = TinkerGraph.open();
    }

    @Override
    public GraphTraversalSource getGraphTraversalSource() {
        return graph.traversal();
    }

    @Override
    public void addEdge(Traversal<Vertex, Edge> traversal) {
        traversal.next();
    }

    @Override
    public void removeEdge(Traversal<Vertex, Edge> traversal) {
        traversal.iterate();
    }

    @Override
    public NodeVertex addVertex(Traversal<Vertex, Vertex> traversal) {
        return createNodeVertex(traversal.next());
    }

    @Override
    public List<NodeVertex> getVertices(Traversal<Vertex, Vertex> traversal) {
        List<NodeVertex> vertices = new ArrayList<>();
        traversal.forEachRemaining(vertex -> vertices.add(createNodeVertex(vertex)));
        return vertices;
    }

    @Override
    public Optional<NodeVertex> getVertex(Traversal<Vertex, Vertex> traversal) {
        try {
            Vertex vertex = traversal.next();
            return Optional.ofNullable(createNodeVertex(vertex));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean hasVertex(Traversal<Vertex, Vertex> traversal) {
        return traversal.hasNext();
    }

    @Override
    public List<Map<NodeVertex, NodeEdge>> getVerticesAndEdges(Traversal<Vertex, Map<String, Object>> traversal) {
        List<Map<NodeVertex, NodeEdge>> verticesAndEdges = new ArrayList<>();
        traversal.forEachRemaining(ve -> verticesAndEdges.add(createNodeVertexAndEdgeMap(ve)));
        return verticesAndEdges;
    }

    @Override
    public void removeVertex(Traversal<Vertex, Vertex> traversal) {
        traversal.iterate();
    }

    @Override
    public void updateVertex(Traversal<Vertex, Vertex> traversal) {
        traversal.next();
    }

    @Override
    public List<NodeEdge> getEdge(GraphTraversal<Vertex, List<Edge>> traversal) {
        List<NodeEdge> edgesList = new ArrayList<>();
        traversal.forEachRemaining(edges -> edges.forEach(item -> edgesList.add(createNodeEdge(item))));
        return edgesList;
    }

    public NodeVertex createNodeVertex(Vertex vertex) {
        Map<String, List<Map<String, String>>> properties = new HashMap<>();
        if (vertex.label().equalsIgnoreCase(String.valueOf(NodeType.USER))) {
            properties.put(VertexPropertyNames.NODE_ID, createWrapperForValue(vertex.value(VertexPropertyNames.NODE_ID)));
            properties.put(VertexPropertyNames.DATA_PARTITION_ID, createWrapperForValue(vertex.value(VertexPropertyNames.DATA_PARTITION_ID)));
        } else {
            properties.put(VertexPropertyNames.NODE_ID, createWrapperForValue(vertex.value(VertexPropertyNames.NODE_ID)));
            properties.put(VertexPropertyNames.NAME, createWrapperForValue(vertex.value(VertexPropertyNames.NAME)));
            properties.put(VertexPropertyNames.DESCRIPTION, createWrapperForValue(vertex.value(VertexPropertyNames.DESCRIPTION)));
            properties.put(VertexPropertyNames.DATA_PARTITION_ID, createWrapperForValue(vertex.value(VertexPropertyNames.DATA_PARTITION_ID)));
            properties.put(VertexPropertyNames.APP_ID, createWrapperForValues(vertex.values(VertexPropertyNames.APP_ID)));
        }
        return NodeVertex.builder()
                .id(vertex.id().toString())
                .label(vertex.label())
                .type(vertex.label())
                .properties(properties)
                .build();
    }

    public NodeEdge createNodeEdge(Edge edge) {
        Map<String, String> properties = new HashMap<>();
        properties.put(EdgePropertyNames.ROLE, edge.value(EdgePropertyNames.ROLE));
        return NodeEdge.builder()
                .id(edge.id().toString())
                .label(edge.label())
                .type(edge.label())
                .properties(properties)
                .build();
    }

    public Map<NodeVertex, NodeEdge> createNodeVertexAndEdgeMap(Map<String, Object> map) {
        Vertex vertex = (Vertex) map.get(StepLabel.VERTEX);
        Edge edge = (Edge) map.get(StepLabel.EDGE);
        Map<NodeVertex, NodeEdge> nodeVertexEdgeMap = new HashMap<>();
        nodeVertexEdgeMap.put(createNodeVertex(vertex), createNodeEdge(edge));
        return nodeVertexEdgeMap;
    }

    private List<Map<String, String>> createWrapperForValue(String value) {
        Map<String, String> valueMap = new HashMap<String, String>() {{
            put("value", value);
        }};
        return Collections.singletonList(valueMap);
    }

    private List<Map<String, String>> createWrapperForValues(Iterator<String> values) {
        List<Map<String, String>> list = new ArrayList<>();
        while (values.hasNext()) {
            list.add(new HashMap<String, String>() {{
                put("value", values.next());
            }});
        }
        return list;
    }
}