package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.listmember;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.listmember.ListMemberRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ListMemberRepoGremlinTest {

    @Autowired
    private GremlinConnector gremlinConnector;

    @Autowired
    private ListMemberRepo listMemberRepo;

    @MockBean
    private AuditLogger auditLogger;

    @Test
    public void shouldLoadDirectChildrenSuccessfully() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Vertex group1Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId1")
                .property(VertexPropertyNames.NAME, "groupId1")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_IDS, "[]").next();
        Vertex group2Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId2")
                .property(VertexPropertyNames.NAME, "groupId1")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_IDS, "[]").next();
        Vertex childVertex = graphTraversalSource.addV(NodeType.USER.toString())
                .property(VertexPropertyNames.NODE_ID, "userId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        group1Vertex.addEdge(EdgePropertyNames.EDGE_LB, childVertex, EdgePropertyNames.ROLE, Role.OWNER.getValue());
        group1Vertex.addEdge(EdgePropertyNames.EDGE_LB, group2Vertex, EdgePropertyNames.ROLE, Role.MEMBER.getValue());

        ListMemberServiceDto listMemberServiceDto = ListMemberServiceDto.builder().groupId("groupId1").partitionId("dp").build();
        List<ChildrenReference> result = listMemberRepo.run(listMemberServiceDto);

        Assert.assertEquals(2, result.size());
        ChildrenReference groupChild = result.stream().filter(cR -> "groupId2".equals(cR.getId())).findFirst().get();
        Assert.assertEquals(Role.MEMBER, groupChild.getRole());
        ChildrenReference userChild = result.stream().filter(cR -> "userId".equals(cR.getId())).findFirst().get();
        Assert.assertEquals(Role.OWNER, userChild.getRole());
    }
}
