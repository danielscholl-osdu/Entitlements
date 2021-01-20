package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.addmember;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class AddMemberRepoGremlinTest {

    @Autowired
    private GremlinConnector gremlinConnector;

    @Autowired
    private AddMemberRepo addMemberRepo;

    @After
    public void cleanup() {
        gremlinConnector.getGraphTraversalSource().V().drop().iterate();
        gremlinConnector.getGraphTraversalSource().E().drop().iterate();
    }

    @Test
    public void shouldAddMemberSuccessfully() {
        EntityNode group = EntityNode.builder().nodeId("groupId").build();
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, group.getNodeId()).next();
        EntityNode member = EntityNode.builder().nodeId("memberId").dataPartitionId("dp").type(NodeType.USER).build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder().memberNode(member)
                .partitionId("dp").role(Role.OWNER).build();

        addMemberRepo.addMember(group, addMemberRepoDto);

        List<Vertex> members = graphTraversalSource.V()
                .has(VertexPropertyNames.NODE_ID, group.getNodeId())
                .outE()
                .has(EdgePropertyNames.ROLE, Role.OWNER.getValue())
                .inV()
                .toList();
        Assert.assertEquals(1, members.size());
        Assert.assertEquals(member.getNodeId(), members.iterator().next().value(VertexPropertyNames.NODE_ID));
    }
}
