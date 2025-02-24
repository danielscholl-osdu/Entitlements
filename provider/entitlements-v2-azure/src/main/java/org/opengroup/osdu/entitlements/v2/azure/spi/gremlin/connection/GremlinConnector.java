package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeEdge;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GremlinConnector {

    GraphTraversalSource getGraphTraversalSource();

    void addEdge(Traversal<Vertex, Edge> traversal);

    NodeVertex addVertex(Traversal<Vertex, Vertex> traversal);

    void removeEdge(Traversal<Vertex, Edge> traversal);

    List<NodeVertex> getVertices(Traversal<Vertex, Vertex> traversal);

    List<Map<NodeVertex, NodeEdge>> getVerticesAndEdges(Traversal<Vertex, Map<String, Object>> traversal);

    Optional<NodeVertex> getVertex(Traversal<Vertex, Vertex> traversal);

    boolean hasVertex(Traversal<Vertex, Vertex> traversal);

    void removeVertex(Traversal<Vertex, Vertex> traversal);

    void updateVertex(Traversal<Vertex, Vertex> traversal);

    List<NodeEdge> getEdge(GraphTraversal<Vertex, List<Edge>> traversal);
}
