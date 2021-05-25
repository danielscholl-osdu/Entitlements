package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.updateappids;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.azure.service.AddEdgeDto;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.service.VertexUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.spi.updateappids.UpdateAppIdsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class UpdateAppIdsRepoGremlinTest {
    private static final String TEST_PARTITION_ID = "dp";
    private static final String TEST_DOMAIN = TEST_PARTITION_ID + ".contoso.com";

    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private CacheConfig cacheConfig;
    @Autowired
    private UpdateAppIdsRepo updateAppIdsRepo;
    @Autowired
    private GremlinConnector gremlinConnector;
    @Autowired
    private VertexUtilService vertexUtilService;
    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;

    @After
    public void cleanup() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.V().drop().iterate();
        graphTraversalSource.E().drop().iterate();
    }

    @Test
    public void shouldLogFailureInAuditLogs() {
        Set<String> appIds = new HashSet<>(Arrays.asList("app1","app2"));
        EntityNode groupNode = EntityNode.builder().nodeId("group1").build();
        try {
            updateAppIdsRepo.updateAppIds(groupNode, appIds);
            Assert.fail("Exception is expected here");
        } catch (Exception e) {
            Mockito.verify(auditLogger).updateAppIds(AuditStatus.FAILURE, groupNode.getNodeId(), appIds);
        }
    }

    @Test
    public void shouldUpdateAppIdsSuccessfully1() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        createTestVertex("users.data.root", NodeType.GROUP);
        createTestVertex("users.x", NodeType.GROUP);
        createTestVertex("data.w", NodeType.GROUP);
        createTestVertex("data.x", NodeType.GROUP);
        createTestVertex("data.y", NodeType.GROUP);
        createTestVertex("data.z", NodeType.GROUP);
        createTestVertex("users.owner", NodeType.USER);
        createTestVertex("users.member", NodeType.USER);
        addTestEdgeAsMember("data.x", "users.data.root");
        addTestEdgeAsMember("data.y", "users.data.root");
        addTestEdgeAsMember("data.z", "users.data.root");
        addTestEdgeAsMember("data.w", "users.data.root");
        addTestEdgeAsMember("users.x", "users.data.root");
        addTestEdgeAsMember("users.member", "users.data.root");
        addTestEdgeAsMember("users.owner", "users.data.root");
        addTestEdgeAsMember("users.x", "data.x");
        addTestEdgeAsMember("users.x", "data.y");
        addTestEdgeAsMember("users.x", "data.z");
        addTestEdgeAsMember("data.w", "users.x");
        addTestEdgeAsOwner("users.owner", "users.x");
        addTestEdgeAsMember("users.member", "users.x");
        EntityNode groupNode = EntityNode.builder()
                .nodeId("users.x" + "@" + TEST_DOMAIN)
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId(TEST_PARTITION_ID)
                .description("")
                .build();

        Set<String> allowedAppIds = new HashSet<>();
        allowedAppIds.add("testApp1");
        allowedAppIds.add("testApp2");

        Set<String> impactedUsers = updateAppIdsRepo.updateAppIds(groupNode, allowedAppIds);

        Set<String> appIds = new HashSet<>();
        Iterator<String> values = graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "users.x@dp.contoso.com")
                .has(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next()
                .values(VertexPropertyNames.APP_ID);
        while (values.hasNext()) {
            appIds.add(values.next());
        }

        Assert.assertEquals(2, impactedUsers.size());
        Assert.assertTrue(impactedUsers.contains("users.owner@dp.contoso.com"));
        Assert.assertTrue(impactedUsers.contains("users.member@dp.contoso.com"));
        Assert.assertEquals(new HashSet<>(Arrays.asList("testApp1", "testApp2")), appIds);
        List<Vertex> usersYMembers = gremlinConnector.getGraphTraversalSource().V().has(VertexPropertyNames.NODE_ID, "users.x@dp.contoso.com")
                .outE(EdgePropertyNames.CHILD_EDGE_LB)
                .has(EdgePropertyNames.ROLE, Role.MEMBER.getValue())
                .inV()
                .toList();
        Assert.assertEquals(2, usersYMembers.size());
        List<Vertex> usersYAllMembers = gremlinConnector.getGraphTraversalSource().V().has(VertexPropertyNames.NODE_ID, "users.x@dp.contoso.com")
                .outE(EdgePropertyNames.CHILD_EDGE_LB)
                .inV()
                .toList();
        Assert.assertEquals(3, usersYAllMembers.size());
        Traversal<Vertex, Vertex> traversal = graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "users.x@dp.contoso.com")
                .emit(__.hasLabel(NodeType.GROUP.toString()))
                .repeat(__.in());

        Assert.assertEquals(new HashSet<>(Arrays.asList(
                "users.x@dp.contoso.com",
                "data.x@dp.contoso.com",
                "data.y@dp.contoso.com",
                "data.z@dp.contoso.com",
                "users.data.root@dp.contoso.com"
        )), gremlinConnector.getVertices(traversal).stream()
                .map(vertexUtilService::createParentReference)
                .map(ParentReference::getId)
                .collect(Collectors.toSet()));
        Mockito.verify(auditLogger).updateAppIds(AuditStatus.SUCCESS, "users.x" + "@" + TEST_DOMAIN, allowedAppIds);

    }

    private void addTestEdgeAsOwner(String childName, String parentName) {
        String childNodeId = childName + "@" + TEST_DOMAIN;
        String parentNodeId = parentName + "@" + TEST_DOMAIN;
        graphTraversalSourceUtilService.addEdge(createAddMemberRequest(childNodeId, parentNodeId, Role.OWNER));
    }

    private void addTestEdgeAsMember(String childName, String parentName) {
        String childNodeId = childName + "@" + TEST_DOMAIN;
        String parentNodeId = parentName + "@" + TEST_DOMAIN;
        graphTraversalSourceUtilService.addEdge(createAddMemberRequest(childNodeId, parentNodeId, Role.MEMBER));
    }

    private void createTestVertex(String name, NodeType nodeType) {
        gremlinConnector.getGraphTraversalSource().addV(nodeType.toString())
                .property(VertexPropertyNames.NODE_ID, name + "@" + TEST_DOMAIN)
                .property(VertexPropertyNames.NAME, name)
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, TEST_PARTITION_ID)
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();
    }

    @Test
    public void shouldUpdateAppIdsSuccessfully2() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        createTestVertex("users.data.root", NodeType.GROUP);
        createTestVertex("users.x", NodeType.GROUP);
        createTestVertex("users.y", NodeType.GROUP);
        graphTraversalSource.addV(NodeType.USER.toString())
                .property(VertexPropertyNames.NODE_ID, "member@xxx.com")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        graphTraversalSourceUtilService.addEdge(createAddMemberRequest("member@xxx.com", "users.x@dp.contoso.com", Role.OWNER));
        addTestEdgeAsMember("users.x", "users.y");
        addTestEdgeAsMember("users.x", "users.data.root");
        addTestEdgeAsMember("users.y", "users.data.root");
        EntityNode groupNode = EntityNode.builder()
                .nodeId("users.x@dp.contoso.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .description("")
                .build();

        Set<String> allowedAppIds = new HashSet<>();
        allowedAppIds.add("testApp1");
        allowedAppIds.add("testApp2");

        Set<String> impactedUsers = updateAppIdsRepo.updateAppIds(groupNode, allowedAppIds);

        Set<String> appIds = new HashSet<>();
        Iterator<String> values = graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "users.x@dp.contoso.com")
                .has(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next()
                .values(VertexPropertyNames.APP_ID);
        while (values.hasNext()) {
            appIds.add(values.next());
        }

        Assert.assertEquals(1, impactedUsers.size());
        Assert.assertTrue(impactedUsers.contains("member@xxx.com"));
        Assert.assertEquals(new HashSet<>(Arrays.asList("testApp1", "testApp2")), appIds);
        List<Vertex> members = gremlinConnector.getGraphTraversalSource().V().has(VertexPropertyNames.NODE_ID, "users.x@dp.contoso.com")
                .outE(EdgePropertyNames.CHILD_EDGE_LB)
                .inV()
                .toList();
        Assert.assertEquals(1, members.size());
        Traversal<Vertex, Vertex> traversal = graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "users.x@dp.contoso.com")
                .emit(__.hasLabel(NodeType.GROUP.toString()))
                .repeat(__.in());
        Assert.assertEquals(new HashSet<>(Arrays.asList(
                "users.y@dp.contoso.com",
                "users.x@dp.contoso.com",
                "users.data.root@dp.contoso.com"
        )), gremlinConnector.getVertices(traversal).stream()
                .map(vertexUtilService::createParentReference)
                .map(ParentReference::getId)
                .collect(Collectors.toSet()));
        Mockito.verify(auditLogger).updateAppIds(AuditStatus.SUCCESS, "users.x" + "@" + TEST_DOMAIN, allowedAppIds);
    }

    private AddEdgeDto createAddMemberRequest(String childNodeId, String parentNodeId, Role role) {
        return AddEdgeDto.builder()
                .toNodeId(childNodeId)
                .edgeProperties(Collections.singletonMap(EdgePropertyNames.ROLE, role.getValue()))
                .fromNodeId(parentNodeId)
                .dpOfToNodeId(TEST_PARTITION_ID)
                .dpOfFromNodeId(TEST_PARTITION_ID)
                .edgeLabel(EdgePropertyNames.CHILD_EDGE_LB)
                .build();
    }
}
