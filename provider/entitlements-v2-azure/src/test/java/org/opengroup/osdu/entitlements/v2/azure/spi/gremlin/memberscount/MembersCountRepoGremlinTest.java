package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.memberscount;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.addmember.AddMemberRepoGremlin;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@SpringBootTest
@RunWith(SpringRunner.class)
public class MembersCountRepoGremlinTest {


    @Autowired
    private GremlinConnector gremlinConnector;

    @Autowired
    private AddMemberRepoGremlin addMemberRepoGremlin;

    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;

    @MockBean
    private AuditLogger auditLogger;

    @MockBean
    private CacheConfig cacheConfig;

    @Autowired
    private MembersCountRepoGremlin membersCountRepoGremlin;

    @Before
    public void setUp(){
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        EntityNode groupNode = EntityNode.builder().nodeId("groupId").dataPartitionId("dp").type(NodeType.GROUP).build();
        ChildrenReference childrenReference = ChildrenReference.builder()
                .dataPartitionId("dp")
                .role(Role.OWNER)
                .type(NodeType.USER)
                .id("userId").build();

        EntityNode memberNode = EntityNode.builder().nodeId("userId").dataPartitionId("dp").type(NodeType.USER).build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder().memberNode(memberNode)
                .role(Role.OWNER).partitionId("dp").build();
        addMemberRepoGremlin.addMember(groupNode, addMemberRepoDto);
        Assert.assertTrue(retrieveGroupRepo.hasDirectChild(groupNode, childrenReference));
    }

    @Test
    public void shouldRetrieveMemberCountSuccessfully() {
        MembersCountServiceDto membersCountServiceDto = MembersCountServiceDto.builder()
                .requesterId(null)
                .groupId("groupId")
                .partitionId("dp")
                .role(Role.OWNER)
                .build();
        MembersCountResponseDto membersCountResponseDto = membersCountRepoGremlin.getMembersCount(membersCountServiceDto);
        assertNotNull(membersCountResponseDto);
        assertEquals(1, membersCountResponseDto.getMembersCount());
    }

    @After
    public void cleanup() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.V().drop().iterate();
        graphTraversalSource.E().drop().iterate();
    }
}