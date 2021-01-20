package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.addmember;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@Repository
public class AddMemberRepoGremlin implements AddMemberRepo {

    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;

    @Autowired
    private GremlinConnector gremlinConnector;

    @Override
    public Set<String> addMember(EntityNode groupNode, AddMemberRepoDto addMemberRepoDto) {
        graphTraversalSourceUtilService.createVertexFromEntityNodeIdempotent(addMemberRepoDto.getMemberNode());
        if (Role.MEMBER.equals(addMemberRepoDto.getRole())) {
            graphTraversalSourceUtilService.addEdgeAsMember(addMemberRepoDto.getMemberNode().getNodeId(), groupNode.getNodeId());
        } else if (Role.OWNER.equals(addMemberRepoDto.getRole())) {
            graphTraversalSourceUtilService.addEdgeAsOwner(addMemberRepoDto.getMemberNode().getNodeId(), groupNode.getNodeId());
        }
        return new HashSet<>();
    }

    @Override
    public Set<String> addMember(Deque<Operation> executedCommandsDeque, EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
        return new HashSet<>();
    }
}
