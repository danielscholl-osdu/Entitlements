package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.retrievegroup;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.addmember.AddMemberRepoGremlin;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RetrieveGroupRepoGremlinTest {

    @Autowired
    private GremlinConnector gremlinConnector;
    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;
    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;
    @Autowired
    private AddMemberRepoGremlin addMemberRepoGremlin;
    @MockBean
    private AuditLogger auditLogger;

    @After
    public void cleanup() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.V().drop().iterate();
        graphTraversalSource.E().drop().iterate();
    }

    @Test
    public void shouldReturnGroupNodeIfGroupExist() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.test@domain.com")
                .property(VertexPropertyNames.NAME, "users.test")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();
        assertNotNull(retrieveGroupRepo.groupExistenceValidation("users.test@domain.com", "dp"));
    }

    @Test
    public void shouldThrow404IfGroupDoesNotExist() {
        try {
            assertNotNull(retrieveGroupRepo.groupExistenceValidation("users.test@domain.com", "dp"));
            fail("should throw exception");
        } catch (AppException ex) {
            Assert.assertEquals(404, ex.getError().getCode());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldReturnOptionalEmptyGroupNodeWhenGroupNodeDoesNotExist() {
        Optional<EntityNode> groupNode = retrieveGroupRepo.getEntityNode("users.test@domain.com", "dp");
        assertFalse(groupNode.isPresent());
    }

    @Test
    public void shouldReturnGroupNodeWhenGroupNodeExists() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.test@domain.com")
                .property(VertexPropertyNames.NAME, "users.test")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();
        Optional<EntityNode> groupNode = retrieveGroupRepo.getEntityNode("users.test@domain.com", "dp");
        assertTrue(groupNode.isPresent());
        assertEquals(NodeType.GROUP, groupNode.get().getType());
        assertEquals("users.test@domain.com", groupNode.get().getNodeId());
        assertEquals("users.test", groupNode.get().getName());
        assertEquals("xxx", groupNode.get().getDescription());
        assertEquals("dp", groupNode.get().getDataPartitionId());
        assertEquals(2, groupNode.get().getAppIds().size());
    }

    @Test
    public void shouldReturnEmptySetIfNoParentsWhenLoadAllParents() {
        String partitionId = "dp";
        String memberDesId = "member@xxx.com";

        EntityNode memberNode = EntityNode.createMemberNodeForNewUser(memberDesId, partitionId);
        ParentTreeDto parents = retrieveGroupRepo.loadAllParents(memberNode);
        Assert.assertEquals(0, parents.getParentReferences().size());
        // TODO: 584695 Uncomment when depth logic will be ready
//        Assert.assertEquals(1, parents.getMaxDepth());
    }

    @Test
    public void shouldReturnAllParentsAndMaxDepthWhenLoadAllParents1() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.x@dp.domain.com")
                .property(VertexPropertyNames.NAME, "users.x")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.y@dp.domain.com")
                .property(VertexPropertyNames.NAME, "users.y")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.x@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.x")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.y@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.y")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        addMember("users.x@dp.domain.com", NodeType.GROUP, "data.x@dp.domain.com", Role.MEMBER);
        addMember("users.x@dp.domain.com", NodeType.GROUP, "data.y@dp.domain.com", Role.MEMBER);
        addMember("users.y@dp.domain.com", NodeType.GROUP, "data.y@dp.domain.com", Role.MEMBER);
        addMember("member@xxx.com", NodeType.USER, "users.x@dp.domain.com", Role.MEMBER);
        addMember("member@xxx.com", NodeType.USER, "users.y@dp.domain.com", Role.MEMBER);

        EntityNode memberNode = EntityNode.createMemberNodeForNewUser("member@xxx.com", "dp");
        ParentTreeDto parents = retrieveGroupRepo.loadAllParents(memberNode);
        Assert.assertEquals(4, parents.getParentReferences().size());
        assertParentsEquals(parents,
                "data.x@dp.domain.com",
                "data.y@dp.domain.com",
                "users.x@dp.domain.com",
                "users.y@dp.domain.com");
        // TODO: 584695 Uncomment when depth logic will be ready
//        Assert.assertEquals(3, parents.getMaxDepth());
    }

    @Test
    public void shouldReturnAllParentsAndMaxDepthWhenLoadAllParents2() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.x@dp.domain.com")
                .property(VertexPropertyNames.NAME, "users.x")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.y@dp.domain.com")
                .property(VertexPropertyNames.NAME, "users.y")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.x@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.x")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.y@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.y")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.z@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.z")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.USER.toString())
                .property(VertexPropertyNames.NODE_ID, "member@xxx.com")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();

        addMember("member@xxx.com", NodeType.USER, "users.x@dp.domain.com", Role.MEMBER);
        addMember("member@xxx.com", NodeType.USER, "users.y@dp.domain.com", Role.MEMBER);
        addMember("users.x@dp.domain.com", NodeType.GROUP, "data.y@dp.domain.com", Role.MEMBER);
        addMember("users.y@dp.domain.com", NodeType.GROUP, "data.y@dp.domain.com", Role.MEMBER);
        addMember("users.y@dp.domain.com", NodeType.GROUP, "data.z@dp.domain.com", Role.MEMBER);
        addMember("data.y@dp.domain.com", NodeType.GROUP, "data.x@dp.domain.com", Role.MEMBER);
        addMember("data.z@dp.domain.com", NodeType.GROUP, "data.x@dp.domain.com", Role.MEMBER);

        EntityNode memberNode = EntityNode.createMemberNodeForNewUser("member@xxx.com", "dp");
        ParentTreeDto parents = retrieveGroupRepo.loadAllParents(memberNode);
        Assert.assertEquals(5, parents.getParentReferences().size());
        assertParentsEquals(parents,
                "data.x@dp.domain.com",
                "data.y@dp.domain.com",
                "data.z@dp.domain.com",
                "users.x@dp.domain.com",
                "users.y@dp.domain.com");
        // TODO: 584695 Uncomment when depth logic will be ready
//        Assert.assertEquals(4, parents.getMaxDepth());
    }

    private void assertParentsEquals(ParentTreeDto parents, String... expectedEmails) {
        Set<String> actualEmails = parents.getParentReferences().stream()
                .map(ParentReference::getId)
                .collect(Collectors.toSet());
        Assert.assertEquals(new HashSet<>(Arrays.asList(expectedEmails)), actualEmails);
    }

    @Test
    public void shouldReturnTrueIfDirectChildExists() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, "groupId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        graphTraversalSource.addV(NodeType.USER.toString()).property(VertexPropertyNames.NODE_ID, "userId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        addMember("userId", NodeType.USER, "groupId", Role.OWNER);
        EntityNode groupNode = EntityNode.builder().nodeId("groupId").dataPartitionId("dp").build();
        ChildrenReference childrenReference = ChildrenReference.builder()
                .dataPartitionId("dp")
                .role(Role.OWNER)
                .type(NodeType.USER)
                .id("userId").build();

        Assert.assertTrue(retrieveGroupRepo.hasDirectChild(groupNode, childrenReference));
    }

    @Test
    public void shouldReturnFalseIfDirectChildHasAnotherDp() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, "groupId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        graphTraversalSource.addV(NodeType.USER.toString()).property(VertexPropertyNames.NODE_ID, "userId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        addMember("userId", NodeType.USER, "groupId", Role.OWNER);
        EntityNode groupNode = EntityNode.builder().nodeId("groupId").build();
        ChildrenReference childrenReference = ChildrenReference.builder()
                .dataPartitionId("dp1")
                .role(Role.OWNER)
                .type(NodeType.USER)
                .id("userId").build();

        Assert.assertFalse(retrieveGroupRepo.hasDirectChild(groupNode, childrenReference));
    }

    @Test
    public void shouldReturnFalseIfDirectChildHasAnotherRole() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, "groupId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        graphTraversalSource.addV(NodeType.USER.toString()).property(VertexPropertyNames.NODE_ID, "userId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        addMember("userId", NodeType.USER, "groupId", Role.OWNER);
        EntityNode groupNode = EntityNode.builder().nodeId("groupId").build();
        ChildrenReference childrenReference = ChildrenReference.builder()
                .dataPartitionId("dp")
                .role(Role.MEMBER)
                .type(NodeType.USER)
                .id("userId").build();

        Assert.assertFalse(retrieveGroupRepo.hasDirectChild(groupNode, childrenReference));
    }

    @Test
    public void shouldReturnFalseIfDirectChildNotExisting() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, "groupId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        graphTraversalSource.addV(NodeType.USER.toString()).property(VertexPropertyNames.NODE_ID, "userId1")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        addMember("userId1", NodeType.USER, "groupId", Role.OWNER);
        EntityNode groupNode = EntityNode.builder().nodeId("groupId").build();
        ChildrenReference childrenReference = ChildrenReference.builder()
                .dataPartitionId("dp")
                .role(Role.OWNER)
                .type(NodeType.USER)
                .id("userId").build();

        Assert.assertFalse(retrieveGroupRepo.hasDirectChild(groupNode, childrenReference));
    }

    /**
     * Direct child may be a group and may be a user, this case checks if this property is checked
     */
    @Test
    public void shouldReturnFalseIfDirectChildNotRequiredType() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, "groupId1")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, "groupId2")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        addMember("groupId2", NodeType.USER, "groupId1", Role.MEMBER);
        EntityNode groupNode = EntityNode.builder().nodeId("groupId1").build();
        ChildrenReference childrenReference = ChildrenReference.builder()
                .dataPartitionId("dp")
                .role(Role.MEMBER)
                .type(NodeType.USER)
                .id("groupId2").build();

        Assert.assertFalse(retrieveGroupRepo.hasDirectChild(groupNode, childrenReference));
    }

    @Test
    public void shouldLoadDirectChildrenSuccessfully() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, "groupId1")
                .property(VertexPropertyNames.NAME, "groupId1")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        graphTraversalSource.addV(NodeType.GROUP.toString()).property(VertexPropertyNames.NODE_ID, "groupId2")
                .property(VertexPropertyNames.NAME, "groupId2")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        graphTraversalSource.addV(NodeType.USER.toString()).property(VertexPropertyNames.NODE_ID, "userId")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();

        addMember("userId", NodeType.USER, "groupId1", Role.OWNER);
        addMember("groupId2", NodeType.GROUP, "groupId1", Role.MEMBER);

        List<ChildrenReference> result = retrieveGroupRepo.loadDirectChildren("dp", "groupId1");

        Assert.assertEquals(2, result.size());
        ChildrenReference groupChild = result.stream().filter(cR -> "groupId2".equals(cR.getId())).findFirst().get();
        Assert.assertEquals(Role.MEMBER, groupChild.getRole());
        ChildrenReference userChild = result.stream().filter(cR -> "userId".equals(cR.getId())).findFirst().get();
        Assert.assertEquals(Role.OWNER, userChild.getRole());
    }

    @Test
    public void shouldReturnEmptyListIfNotParentsWhenLoadDirectParents() {
        List<ParentReference> parents = retrieveGroupRepo.loadDirectParents("dp", "member@xxx.com");
        Assert.assertEquals(0, parents.size());
    }

    /*
    g1(data.x)      g2(data.y)  <------------
        ^               ^                   |
        |---------------|                   |
                    g3(users.x)         g4(users.y)
                        ^                    ^
                        |--------------------|
                user(member@xxx.com)
     */
    @Test
    public void shouldReturnDirectParentListWhenLoadDirectParents() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.x@dp.domain.com")
                .property(VertexPropertyNames.NAME, "users.x")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.y@dp.domain.com")
                .property(VertexPropertyNames.NAME, "users.y")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.x@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.x")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.y@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.y")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        graphTraversalSource.addV(NodeType.USER.toString()).property(VertexPropertyNames.NODE_ID, "member@xxx.com")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();

        addMember("member@xxx.com", NodeType.USER, "users.x@dp.domain.com", Role.MEMBER);
        addMember("member@xxx.com", NodeType.USER, "users.y@dp.domain.com", Role.MEMBER);
        addMember("users.x@dp.domain.com", NodeType.GROUP, "data.x@dp.domain.com", Role.MEMBER);
        addMember("users.x@dp.domain.com", NodeType.GROUP, "data.y@dp.domain.com", Role.MEMBER);
        addMember("users.y@dp.domain.com", NodeType.GROUP, "data.y@dp.domain.com", Role.MEMBER);

        List<ParentReference> g3 = retrieveGroupRepo.loadDirectParents("dp", "users.x@dp.domain.com");
        Assert.assertEquals(2, g3.size());
        List<ParentReference> g4 = retrieveGroupRepo.loadDirectParents("dp", "users.y@dp.domain.com");
        Assert.assertEquals(1, g4.size());
        List<ParentReference> member = retrieveGroupRepo.loadDirectParents("dp", "member@xxx.com");
        Assert.assertEquals(2, member.size());
    }

    @Test
    public void shouldFilterAccessibleParentReferencesWhenGivenValidAppId() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Vertex group1Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId1")
                .property(VertexPropertyNames.NAME, "groupId1")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        Vertex group2Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId2")
                .property(VertexPropertyNames.NAME, "groupId2")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "appid")
                .next();
        Vertex group3Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId3")
                .property(VertexPropertyNames.NAME, "groupId3")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "otherappid")
                .next();
        Vertex group4Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId4")
                .property(VertexPropertyNames.NAME, "groupId4")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "appid")
                .property(VertexPropertyNames.APP_ID, "otherappid")
                .next();

        Set<ParentReference> parentReferences = new HashSet<>();
        parentReferences.add(ParentReference.builder()
                .id(group1Vertex.value(VertexPropertyNames.NODE_ID))
                .dataPartitionId("dp")
                .build());
        parentReferences.add(ParentReference.builder()
                .id(group2Vertex.value(VertexPropertyNames.NODE_ID))
                .dataPartitionId("dp")
                .appIds(new HashSet<>(Collections.singletonList("appid")))
                .build());
        parentReferences.add(ParentReference.builder()
                .id(group3Vertex.value(VertexPropertyNames.NODE_ID))
                .dataPartitionId("dp")
                .appIds(new HashSet<>(Collections.singletonList("otherappid")))
                .build());
        parentReferences.add(ParentReference.builder()
                .id(group4Vertex.value(VertexPropertyNames.NODE_ID))
                .dataPartitionId("dp")
                .appIds(new HashSet<>(Arrays.asList("otherappid","appid")))
                .build());

        Set<ParentReference> res = retrieveGroupRepo.filterParentsByAppId(parentReferences, "dp", "appid");
        Assert.assertEquals(3, res.size());
        Assert.assertEquals(0, res.stream().filter(r -> r.getId().equalsIgnoreCase("groupId3")).count());
    }

    @Test
    public void shouldReturnAllParentReferencesWhenNoOtherAppIds() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Vertex group1Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId1")
                .property(VertexPropertyNames.NAME, "groupId1")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        Vertex group2Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId2")
                .property(VertexPropertyNames.NAME, "groupId2")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "appid")
                .next();
        Vertex group3Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId3")
                .property(VertexPropertyNames.NAME, "groupId3")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "appid")
                .next();

        Set<ParentReference> parentReferences = new HashSet<>();
        parentReferences.add(ParentReference.builder()
                .id(group1Vertex.value(VertexPropertyNames.NODE_ID))
                .dataPartitionId("dp")
                .build());
        parentReferences.add(ParentReference.builder()
                .id(group2Vertex.value(VertexPropertyNames.NODE_ID))
                .dataPartitionId("dp")
                .appIds(new HashSet<>(Collections.singletonList("appid")))
                .build());
        parentReferences.add(ParentReference.builder()
                .id(group3Vertex.value(VertexPropertyNames.NODE_ID))
                .dataPartitionId("dp")
                .appIds(new HashSet<>(Collections.singletonList("appid")))
                .build());

        Set<ParentReference> res = retrieveGroupRepo.filterParentsByAppId(parentReferences, "dp", "appid");
        Assert.assertEquals(3, res.size());
    }

    @Test
    public void shouldReturnAllParentReferencesWhenGivenNullAppId() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Vertex group1Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId1")
                .property(VertexPropertyNames.NAME, "groupId1")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        Vertex group2Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId2")
                .property(VertexPropertyNames.NAME, "groupId2")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        Vertex group3Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "groupId3")
                .property(VertexPropertyNames.NAME, "groupId3")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();

        Set<ParentReference> parentReferences = new HashSet<>();
        parentReferences.add(ParentReference.builder().id(group1Vertex.value(VertexPropertyNames.NODE_ID)).dataPartitionId("dp").build());
        parentReferences.add(ParentReference.builder().id(group2Vertex.value(VertexPropertyNames.NODE_ID)).dataPartitionId("dp").build());
        parentReferences.add(ParentReference.builder().id(group3Vertex.value(VertexPropertyNames.NODE_ID)).dataPartitionId("dp").build());

        Set<ParentReference> res = retrieveGroupRepo.filterParentsByAppId(parentReferences, "dp", null);
        Assert.assertEquals(3, res.size());
    }

    @Test
    public void shouldReturnAllParentsFilteredByPartitionId() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Vertex users1Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.x@dp.domain.com")
                .property(VertexPropertyNames.NAME, "users.x")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        Vertex users2Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.y@dp.domain.com")
                .property(VertexPropertyNames.NAME, "users.y")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        Vertex users3Vertex = graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "users.x@dp2.domain.com")
                .property(VertexPropertyNames.NAME, "users.x")
                .property(VertexPropertyNames.DESCRIPTION, "xxx")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp2")
                .next();
        Vertex childVertexOfDp = graphTraversalSource.addV(NodeType.USER.toString()).property(VertexPropertyNames.NODE_ID, "member@xxx.com")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
        Vertex childVertexOfDp2 = graphTraversalSource.addV(NodeType.USER.toString()).property(VertexPropertyNames.NODE_ID, "member@xxx.com")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp2").next();

        childVertexOfDp.addEdge(EdgePropertyNames.PARENT_EDGE_LB, users1Vertex);
        childVertexOfDp.addEdge(EdgePropertyNames.PARENT_EDGE_LB, users2Vertex);
        childVertexOfDp2.addEdge(EdgePropertyNames.PARENT_EDGE_LB, users3Vertex);

        users1Vertex.addEdge(EdgePropertyNames.CHILD_EDGE_LB, childVertexOfDp, EdgePropertyNames.ROLE, Role.MEMBER.getValue());
        users2Vertex.addEdge(EdgePropertyNames.CHILD_EDGE_LB, childVertexOfDp, EdgePropertyNames.ROLE, Role.MEMBER.getValue());
        users3Vertex.addEdge(EdgePropertyNames.CHILD_EDGE_LB, childVertexOfDp2, EdgePropertyNames.ROLE, Role.MEMBER.getValue());

        EntityNode memberNode = EntityNode.createMemberNodeForNewUser("member@xxx.com", "dp");
        ParentTreeDto parents = retrieveGroupRepo.loadAllParents(memberNode);
        Assert.assertEquals(2, parents.getParentReferences().size());
        memberNode = EntityNode.createMemberNodeForNewUser("member@xxx.com", "dp2");
        parents = retrieveGroupRepo.loadAllParents(memberNode);
        Assert.assertEquals(1, parents.getParentReferences().size());
    }

    private void addMember(String childNodeId, NodeType typeOfChild, String parentNodeId, Role role) {
        EntityNode groupNode = EntityNode.builder().nodeId(parentNodeId).dataPartitionId("dp").build();
        EntityNode memberNode = EntityNode.builder().nodeId(childNodeId).dataPartitionId("dp").type(typeOfChild).build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder().memberNode(memberNode).role(role).build();
        addMemberRepoGremlin.addMember(groupNode, addMemberRepoDto);
    }
}
