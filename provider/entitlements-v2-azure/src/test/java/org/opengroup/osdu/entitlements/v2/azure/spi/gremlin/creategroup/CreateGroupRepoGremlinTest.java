package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.creategroup;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.creategroup.CreateGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CreateGroupRepoGremlinTest {

    @MockBean
    private AuditLogger auditLogger;

    @MockBean
    private CacheConfig cacheConfig;

    @Autowired
    private CreateGroupRepo createGroupRepo;

    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    @Autowired
    private GremlinConnector gremlinConnector;

    @After
    public void cleanup() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.V().drop().iterate();
        graphTraversalSource.E().drop().iterate();
    }

    @Test
    public void shouldCreateGroupSuccessfully() {
        EntityNode entityNode = EntityNode.builder()
                .nodeId("groupId").name("name").description("desc").type(NodeType.GROUP)
                .dataPartitionId("dp").appIds(new HashSet<>(Arrays.asList("App1", "App2"))).build();

        EntityNode requesterNode = EntityNode.builder().nodeId("test@test.com").dataPartitionId("dp").type(NodeType.USER).build();
        CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .partitionId("dp").build();

        Set<String> impactedUsers = createGroupRepo.createGroup(entityNode, createGroupRepoDto);

        Vertex vertex = gremlinConnector.getGraphTraversalSource().V().has(VertexPropertyNames.NODE_ID, "groupId").next();
        assertEquals("groupId", vertex.value(VertexPropertyNames.NODE_ID));
        assertEquals("name", vertex.value(VertexPropertyNames.NAME));
        assertEquals("desc", vertex.value(VertexPropertyNames.DESCRIPTION));
        assertEquals("dp", vertex.value(VertexPropertyNames.DATA_PARTITION_ID));

        Set<String> appIds = new HashSet<>();
        Iterator<String> values = vertex.values(VertexPropertyNames.APP_ID);
        while (values.hasNext()) {
            appIds.add(values.next());
        }

        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("test@test.com"));
        assertEquals(new HashSet<>(Arrays.asList("App1", "App2")), appIds);
        ChildrenReference childrenReference = ChildrenReference.builder()
                .id("test@test.com").type(NodeType.USER).dataPartitionId("dp").role(Role.OWNER).build();
        assertTrue(retrieveGroupRepo.hasDirectChild(entityNode, childrenReference));
        Mockito.verify(auditLogger).createGroup(AuditStatus.SUCCESS, "groupId");
    }

    @Test
    public void shouldCreateGroupWithRootGroupSuccessfully() {
        EntityNode entityNode = EntityNode.builder()
                .nodeId("groupId").name("name").description("desc").type(NodeType.GROUP)
                .dataPartitionId("dp").appIds(new HashSet<>(Arrays.asList("App1", "App2"))).build();

        EntityNode requesterNode = EntityNode.builder().nodeId("test@test.com").dataPartitionId("dp").type(NodeType.USER).build();
        EntityNode dataRootGroupNode = EntityNode.builder().nodeId("users.data.root@test.com").name("name").type(NodeType.GROUP).dataPartitionId("dp").build();
        CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .addDataRootGroup(true)
                .dataRootGroupNode(dataRootGroupNode)
                .partitionId("dp").build();

        Set<String> impactedUsers = createGroupRepo.createGroup(entityNode, createGroupRepoDto);

        List<Vertex> vertices = gremlinConnector.getGraphTraversalSource().V().toList();
        Vertex vertex = vertices.stream().filter(v -> "groupId".equals(v.value(VertexPropertyNames.NODE_ID))).findFirst().get();
        assertEquals("groupId", vertex.value(VertexPropertyNames.NODE_ID));
        assertEquals("name", vertex.value(VertexPropertyNames.NAME));
        assertEquals("desc", vertex.value(VertexPropertyNames.DESCRIPTION));
        assertEquals("dp", vertex.value(VertexPropertyNames.DATA_PARTITION_ID));

        Set<String> appIds = new HashSet<>();
        Iterator<String> values = vertex.values(VertexPropertyNames.APP_ID);
        while (values.hasNext()) {
            appIds.add(values.next());
        }

        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("test@test.com"));
        assertEquals(new HashSet<>(Arrays.asList("App1", "App2")), appIds);
        ChildrenReference childrenReference = ChildrenReference.builder()
                .id("test@test.com").type(NodeType.USER).dataPartitionId("dp").role(Role.OWNER).build();
        assertTrue(retrieveGroupRepo.hasDirectChild(entityNode, childrenReference));
        ChildrenReference childrenReferenceOfDataGroup = ChildrenReference.builder()
                .id(dataRootGroupNode.getNodeId()).type(dataRootGroupNode.getType())
                .dataPartitionId(dataRootGroupNode.getDataPartitionId()).role(Role.MEMBER).build();
        assertTrue(retrieveGroupRepo.hasDirectChild(entityNode, childrenReferenceOfDataGroup));
        Mockito.verify(auditLogger).createGroup(AuditStatus.SUCCESS, "groupId");
    }

    @Test
    public void shouldReturnEmptySet() {
        Deque<Operation> executedCommandsDeque = new LinkedList<>();
        EntityNode entityNode = EntityNode.builder().build();
        CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder().build();

        Set<String> impactedUsers = createGroupRepo.createGroup(executedCommandsDeque, entityNode, createGroupRepoDto);

        assertTrue(impactedUsers.isEmpty());
    }
}
