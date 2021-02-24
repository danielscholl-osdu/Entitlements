package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.retrievegroup;

import lombok.RequiredArgsConstructor;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.service.VertexUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.StepLabel;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ChildrenTreeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RetrieveGroupRepoGremlin implements RetrieveGroupRepo {
    private final GremlinConnector gremlinConnector;
    private final VertexUtilService vertexUtilService;

    @Override
    public EntityNode groupExistenceValidation(String groupId, String partitionId) {
        return getEntityNode(groupId, partitionId).orElseThrow(() ->
                new AppException(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), ""));
    }

    @Override
    public Optional<EntityNode> getEntityNode(String entityEmail, String partitionId) {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Traversal<Vertex, Vertex> traversal = graphTraversalSource.V()
                .has(VertexPropertyNames.NODE_ID, entityEmail)
                .has(VertexPropertyNames.DATA_PARTITION_ID, partitionId);
        return gremlinConnector.getVertex(traversal).map(vertexUtilService::createMemberNode);
    }

    @Override
    public Set<EntityNode> getEntityNodes(String partitionId, List<String> nodeIds) {
        return new HashSet<>();
    }

    @Override
    public Map<String, Set<String>> getUserPartitionAssociations(Set<String> userIds) {
        return new HashMap<>();
    }

    @Override
    public Set<EntityNode> getAllGroupNodes(String partitionId, String partitionDomain) {
        return new HashSet<>();
    }

    @Override
    public Boolean hasDirectChild(EntityNode groupNode, ChildrenReference childrenReference) {
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.NODE_ID, groupNode.getNodeId())
                .outE(EdgePropertyNames.EDGE_LB)
                .has(EdgePropertyNames.ROLE, childrenReference.getRole().getValue())
                .inV()
                .has(VertexPropertyNames.NODE_ID, childrenReference.getId())
                .hasLabel(childrenReference.getType().toString())
                .has(VertexPropertyNames.DATA_PARTITION_ID, childrenReference.getDataPartitionId());
        return gremlinConnector.hasVertex(traversal);
    }

    @Override
    public List<ParentReference> loadDirectParents(String partitionId, String... nodeId) {
        final List<ParentReference> resultList = new ArrayList<>();
        // TODO: Handle all nodeId's, not just the first. Use '.or(buildOrTraversalsByNodeIds(...))'
        String singleNodeId = nodeId[0];
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.NODE_ID, singleNodeId)
                .inE(EdgePropertyNames.EDGE_LB)
                .outV()
                .has(VertexPropertyNames.DATA_PARTITION_ID, partitionId);
        gremlinConnector.getVertices(traversal)
                .forEach(v -> resultList.add(vertexUtilService.createParentReference(v)));
        return resultList;
    }

    @Override
    public ParentTreeDto loadAllParents(EntityNode memberNode) {
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.NODE_ID, memberNode.getNodeId())
                .emit(__.hasLabel(NodeType.GROUP.toString())
                        .has(VertexPropertyNames.DATA_PARTITION_ID, memberNode.getDataPartitionId()))
                .repeat(__.in());
        List<NodeVertex> vertices = gremlinConnector.getVertices(traversal);
        Set<ParentReference> parentReferences = vertices.stream()
                .map(vertexUtilService::createParentReference)
                .collect(Collectors.toSet());
        return ParentTreeDto.builder()
                .parentReferences(parentReferences)
                .maxDepth(calculateMaxDepth())
                .build();
    }

    @Override
    public List<ChildrenReference> loadDirectChildren(String partitionId, String... nodeId) {
        final List<ChildrenReference> resultList = new ArrayList<>();
        // TODO: Handle all nodeId's, not just the first. Use '.or(buildOrTraversalsByNodeIds(...))'
        String singleNodeId = nodeId[0];
        Traversal<Vertex, Map<String, Object>> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.NODE_ID, singleNodeId)
                .outE(EdgePropertyNames.EDGE_LB)
                .as(StepLabel.EDGE)
                .inV()
                .as(StepLabel.VERTEX)
                .has(VertexPropertyNames.DATA_PARTITION_ID, partitionId)
                .select(StepLabel.EDGE, StepLabel.VERTEX);
        gremlinConnector.getVerticesAndEdges(traversal)
                .forEach(kv -> resultList.add(vertexUtilService.createChildReference(kv)));
        return resultList;
    }

    @Override
    public ChildrenTreeDto loadAllChildrenUsers(EntityNode node) {
        return ChildrenTreeDto.builder().childrenUserIds(new ArrayList<>()).build();
    }

    @Override
    public Set<ParentReference> filterParentsByAppId(
            Set<ParentReference> parentReferences, String partitionId, String appId) {
        Map<String, ParentReference> parentReferenceById = parentReferences.stream()
                .collect(Collectors.toMap(ParentReference::getId, Function.identity()));
        String[] nodeIds = parentReferenceById.keySet().toArray(new String[0]);
        GraphTraversal<Vertex, Vertex> graphTraversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.DATA_PARTITION_ID, partitionId)
                .or(buildOrTraversalsByNodeIds(nodeIds))
                .or(__.hasNot(VertexPropertyNames.APP_ID), __.has(VertexPropertyNames.APP_ID, appId));
        return gremlinConnector.getVertices(graphTraversal).stream()
                .map(NodeVertex::getNodeId)
                .flatMap(nodeId -> Arrays.stream(nodeIds).filter(nodeId::equals))
                .map(parentReferenceById::get)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getGroupOwners(String partitionId, String nodeId) {
        return new HashSet<>();
    }

    @Override
    public Map<String, Integer> getAssociationCount(List<String> userIds) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Integer> getAllUserPartitionAssociations() {
        return new HashMap<>();
    }

    private int calculateMaxDepth() {
        // TODO: 584695 Implement this method
        return 0;
    }

    private Traversal<?, ?>[] buildOrTraversalsByNodeIds(String... nodeIds) {
        return Arrays.stream(nodeIds)
                .map(id -> __.has(VertexPropertyNames.NODE_ID, id))
                .toArray(Traversal[]::new);
    }
}
