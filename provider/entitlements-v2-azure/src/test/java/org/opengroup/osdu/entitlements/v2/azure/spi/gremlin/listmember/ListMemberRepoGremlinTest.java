package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.listmember;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses.HitsNMissesMetricService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.addmember.AddMemberRepoGremlin;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReferences;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
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

    @Autowired
    private AddMemberRepoGremlin addMemberRepoGremlin;

    @MockBean
    private RedisAzureCache<String, ChildrenReferences> redisMemberCache;

    @MockBean
    private HitsNMissesMetricService metricService;

    @MockBean
    private JaxRsDpsLog log;

    @MockBean
    private AuditLogger auditLogger;

    @MockBean
    private CacheConfig cacheConfig;

    @After
    public void cleanup() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.V().drop().iterate();
        graphTraversalSource.E().drop().iterate();
    }

    @Test
    public void shouldLoadDirectChildrenSuccessfully() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId1")
                .property(VertexPropertyNames.NAME, "groupId1")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId2")
                .property(VertexPropertyNames.NAME, "groupId1")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        graphTraversalSource.addV(NodeType.USER.toString())
                .property(VertexPropertyNames.NODE_ID, "userId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();

        addMember("userId", NodeType.USER, "groupId1", Role.OWNER);
        addMember("groupId2", NodeType.GROUP, "groupId1", Role.MEMBER);

        ListMemberServiceDto listMemberServiceDto = ListMemberServiceDto.builder().groupId("groupId1").partitionId("dp").build();
        List<ChildrenReference> result = listMemberRepo.run(listMemberServiceDto);

        Assert.assertEquals(2, result.size());
        ChildrenReference groupChild = result.stream().filter(cR -> "groupId2".equals(cR.getId())).findFirst().get();
        Assert.assertEquals(Role.MEMBER, groupChild.getRole());
        ChildrenReference userChild = result.stream().filter(cR -> "userId".equals(cR.getId())).findFirst().get();
        Assert.assertEquals(Role.OWNER, userChild.getRole());
    }

    private void addMember(String childNodeId, NodeType typeOfChild, String parentNodeId, Role role) {
        EntityNode groupNode = EntityNode.builder().nodeId(parentNodeId).dataPartitionId("dp").build();
        EntityNode memberNode = EntityNode.builder().nodeId(childNodeId).dataPartitionId("dp").type(typeOfChild).build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder().memberNode(memberNode).role(role).build();
        addMemberRepoGremlin.addMember(groupNode, addMemberRepoDto);
    }
}
