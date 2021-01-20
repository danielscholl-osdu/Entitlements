package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.removemember;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RemoveMemberRepoGremlinTest {

    @Autowired
    private GremlinConnector gremlinConnector;

    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    @Autowired
    private RemoveMemberRepoGremlin removeMemberRepoGremlin;

    @After
    public void cleanup() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.V().drop().iterate();
        graphTraversalSource.E().drop().iterate();
    }

    @Test
    public void shouldSuccessfullyRemoveMember() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Vertex groupVertex = graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, "groupId").next();
        Vertex childVertex = graphTraversalSource.addV(NodeType.USER.toString()).property(VertexPropertyNames.NODE_ID, "userId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        groupVertex.addEdge(EdgePropertyNames.EDGE_LB, childVertex, EdgePropertyNames.ROLE, Role.OWNER.getValue());
        EntityNode groupNode = EntityNode.builder().nodeId("groupId").build();
        ChildrenReference childrenReference = ChildrenReference.builder()
                .dataPartitionId("dp")
                .role(Role.OWNER)
                .type(NodeType.USER)
                .id("userId").build();

        Assert.assertTrue(retrieveGroupRepo.hasDirectChild(groupNode, childrenReference));

        EntityNode memberNode = EntityNode.builder().nodeId("userId").build();
        removeMemberRepoGremlin.removeMember(groupNode, memberNode, null);

        Assert.assertFalse(retrieveGroupRepo.hasDirectChild(groupNode, childrenReference));
    }
}
