package org.opengroup.osdu.entitlements.v2.azure.service;

import com.google.gson.Gson;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeEdge;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.StepLabel;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VertexUtilService {
    private static final Gson GSON = new Gson();

    @SuppressWarnings("unchecked")
    public EntityNode createMemberNode(NodeVertex vertex) {
        if (NodeType.GROUP.equals(NodeType.valueOf(vertex.getLabel()))) {
            return EntityNode.builder()
                    .type(NodeType.valueOf(vertex.getLabel()))
                    .nodeId(vertex.getNodeId())
                    .name(vertex.getName())
                    .description(vertex.getDescription())
                    .dataPartitionId(vertex.getDataPartitionId())
                    .appIds(GSON.fromJson(vertex.getAppIds(), Set.class))
                    .build();
        } else {
            return EntityNode.builder()
                    .type(NodeType.valueOf(vertex.getLabel()))
                    .nodeId(vertex.getNodeId())
                    .dataPartitionId(vertex.getDataPartitionId())
                    .build();
        }
    }

    public ParentReference createParentReference(NodeVertex vertex) {
        return ParentReference.builder()
                .id(vertex.getNodeId())
                .name(vertex.getName())
                .description(vertex.getDescription())
                .dataPartitionId(vertex.getDataPartitionId())
                .build();
    }

    public ChildrenReference createChildReference(Map<NodeVertex, NodeEdge> kv) {
        ChildrenReference childrenReference = null;
        for (Map.Entry<NodeVertex, NodeEdge> ve : kv.entrySet()) {
            NodeVertex vertex = ve.getKey();
            NodeEdge edge = ve.getValue();
            childrenReference = ChildrenReference.builder()
                    .id(vertex.getNodeId())
                    .role(Role.valueOf(edge.getRole()))
                    .type(NodeType.valueOf(vertex.getLabel()))
                    .dataPartitionId(vertex.getDataPartitionId())
                    .build();
        }
        return childrenReference;
    }

    public List<Map<NodeVertex, NodeEdge>> getVerticesAndEdgesFromResultList(List<Result> resultList) {
        List<Map<NodeVertex, NodeEdge>> vertexToEdgeMap = new ArrayList<>();
        resultList.forEach(result -> {
            Map<NodeVertex, NodeEdge> map = new HashMap<>();
            Map<String, List<Map<String, String>>> resultObject = getResultObject(result);
            map.put(GSON.fromJson(GSON.toJson(resultObject.get(StepLabel.VERTEX)), NodeVertex.class),
                    GSON.fromJson(GSON.toJson(resultObject.get(StepLabel.EDGE)), NodeEdge.class));
            vertexToEdgeMap.add(map);
        });
        return vertexToEdgeMap;
    }

    public List<NodeVertex> getVerticesFromResultList(List<Result> resultList) {
        return resultList.stream()
                .map(result -> GSON.fromJson(GSON.toJson(getResultObject(result)), NodeVertex.class))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public NodeVertex getVertexFromResultList(final List<Result> resultList) {
        Map<String, Map<String, List<String>>> vertex = null;
        final Iterator<Result> iterator = resultList.iterator();
        if (iterator.hasNext()) {
            vertex = iterator.next().get(Map.class);
        }
        return GSON.fromJson(GSON.toJson(vertex), NodeVertex.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<Map<String, String>>> getResultObject(final Result result) {
        return (Map<String, List<Map<String, String>>>) result.getObject();
    }
}
