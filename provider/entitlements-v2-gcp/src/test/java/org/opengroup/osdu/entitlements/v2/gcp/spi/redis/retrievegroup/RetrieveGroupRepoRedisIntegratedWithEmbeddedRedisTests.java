package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.retrievegroup;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.service.PartitionRedisInstanceService;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ChildrenTreeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties.DEFAULT_APPID_KEY;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RetrieveGroupRepoRedisIntegratedWithEmbeddedRedisTests {
    private static RedisServer partitionRedisServer;
    private static RedisClient partitionRedisClient;
    private static RedisServer centralRedisServer;
    private static RedisClient centralRedisClient;
    private static final String DATA_PARTITION_ID = "dp";
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private PartitionRedisInstanceService partitionRedisInstanceService;
    @MockBean
    private GcpAppProperties config;
    @Autowired
    private RetrieveGroupRepoRedis retrieveGroupRepoRedis;

    @BeforeClass
    public static void setupClass() throws IOException {
        partitionRedisServer = new RedisServer(6379);
        partitionRedisServer.start();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        partitionRedisClient = RedisClient.create(uri);
        centralRedisServer = new RedisServer(7000);
        centralRedisServer.start();
        uri = RedisURI.builder().withHost("localhost").withPort(7000).build();
        centralRedisClient = RedisClient.create(uri);
    }

    @AfterClass
    public static void end() {
        partitionRedisServer.stop();
        centralRedisServer.stop();
    }

    @Before
    public void setup() {
        when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition(DATA_PARTITION_ID)).thenReturn("localhost");
        when(config.getCentralRedisInstIp()).thenReturn("localhost");
        when(config.getCentralRedisInstPort()).thenReturn(7000);
        when(config.getPartitionEntityNodeDb()).thenReturn(1);
        when(config.getPartitionParentRefDb()).thenReturn(2);
        when(config.getPartitionChildrenRefDb()).thenReturn(3);
    }

    @After
    public void cleanup() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.flushall();
        connection = centralRedisClient.connect();
        commands = connection.sync();
        commands.flushall();
    }

    @Test
    public void shouldReturnGroupNodeIfGroupExistInRedis() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("exist-group", "{\"id\":\"exist-group@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"exist-group\",\"description\":\"\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"appIds\":[]}");
        assertNotNull(retrieveGroupRepoRedis.groupExistenceValidation("exist-group", "dp"));
    }

    @Test
    public void shouldThrow404IfGroupDoesNotExistInRedis() {
        try {
            retrieveGroupRepoRedis.groupExistenceValidation("not-exist-group", "dp");
            fail("should throw exception");
        } catch (AppException ex) {
            assertEquals(404, ex.getError().getCode());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldReturnGroupNodeIfGetEntityNodeAndItExistsInRedis() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.set("exist-group", "{\"id\":\"exist-group@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"exist-group\",\"description\":\"\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"appIds\":[],\"parents\":[],\"children\":[]}");
        assertNotNull(retrieveGroupRepoRedis.getEntityNode("exist-group", "dp"));
    }

    @Test
    public void shouldReturnNullIfNodeDoesNotExistInRedis() {
        assertFalse(retrieveGroupRepoRedis.getEntityNode("not-exist-group", "dp").isPresent());
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.set("not-exist-group", "");
        assertFalse(retrieveGroupRepoRedis.getEntityNode("not-exist-group", "dp").isPresent());
    }

    @Test
    public void shouldReturnGroupNodeList() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x@dp.domain.com", "{\"id\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("data.y@dp.domain.com", "{\"id\":\"data.y@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.y\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("data.z@dp.domain.com", "{\"id\":\"data.z@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.z\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("users.x@dp.domain.com", "{\"id\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\"," +
                "\"dataPartitionId\":\"dp\"}");

        Set<EntityNode> nodeList = retrieveGroupRepoRedis.getEntityNodes("dp", Arrays.asList("data.x@dp.domain.com",
                "data.y@dp.domain.com", "data.z@dp.domain.com"));

        assertEquals(3, nodeList.size());
    }

    @Test
    public void shouldReturnAllGroupsOfTheTenant() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x@dp.domain.com", "{\"id\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("data.y@dp.domain.com", "{\"id\":\"data.y@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.y\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("data.z@dp.domain.com", "{\"id\":\"data.z@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.z\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("users.x@dp.domain.com", "{\"id\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("users1@xxx.com", "{\"id\":\"users1@xxx.com\",\"type\":\"USER\",\"name\":\"users1@xxx.com\"," +
                "\"dataPartitionId\":\"dp\"}");

        Set<EntityNode> nodeList = retrieveGroupRepoRedis.getAllGroupNodes("dp", "dp.domain.com");

        assertEquals(4, nodeList.size());
    }

    @Test
    public void shouldReturnEmptySetIfGivenNodeListIsEmpty() {
        Set<EntityNode> nodeList = retrieveGroupRepoRedis.getAllGroupNodes("dp", "dp.domain.com");

        assertEquals(0, nodeList.size());
    }

    @Test
    public void shouldReturnUserPartitionAssociationsInABatch() {
        StatefulRedisConnection<String, String> connection = centralRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.sadd("member1@xxx.com", "dp1", "dp2");
        commands.sadd("member2@xxx.com", "dp2", "dp3");

        final Set<String> givenUserIds = new HashSet<>(Arrays.asList("member1@xxx.com", "member2@xxx.com", "member3@xxx.com"));
        final Map<String, Set<String>> result = retrieveGroupRepoRedis.getUserPartitionAssociations(givenUserIds);

        assertEquals(new HashSet<>(Arrays.asList("dp1", "dp2")), result.get("member1@xxx.com"));
        assertEquals(new HashSet<>(Arrays.asList("dp2", "dp3")), result.get("member2@xxx.com"));
        assertTrue(result.get("member3@xxx.com").isEmpty());
    }

    @Test
    public void shouldReturnAllUserPartitionAssociationMap() {
        StatefulRedisConnection<String, String> connection = centralRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.sadd("member1@xxx.com", "dp1", "dp2");
        commands.sadd("member2@xxx.com", "dp1", "dp2", "dp3", "dp4", "dp5");
        commands.sadd("member3@xxx.com", "dp1", "dp2", "dp3", "dp4", "dp5", "dp6", "dp7");
        Map<String, Integer> partitionAssociationMap = new HashMap<>();
        partitionAssociationMap.put("member1@xxx.com", 2);
        partitionAssociationMap.put("member2@xxx.com", 5);
        partitionAssociationMap.put("member3@xxx.com", 7);

        assertEquals(partitionAssociationMap, retrieveGroupRepoRedis.getAllUserPartitionAssociations());

    }

    @Test
    public void shouldReturnTrueIfHasDirectChild() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("data.x@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"OWNER\",\"id\":\"member@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        EntityNode groupNode = EntityNode.createNodeFromGroupEmail("data.x@dp.domain.com");
        assertTrue(retrieveGroupRepoRedis.hasDirectChild(groupNode, ChildrenReference.builder().id("users.y@dp.domain.com").dataPartitionId("dp").type(NodeType.GROUP).role(Role.MEMBER).build()));
        assertTrue(retrieveGroupRepoRedis.hasDirectChild(groupNode, ChildrenReference.builder().id("member@xxx.com").dataPartitionId("dp").type(NodeType.USER).role(Role.OWNER).build()));
    }

    @Test
    public void shouldReturnFalseIfDoesNotHasDirectChild() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("data.x@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"OWNER\",\"id\":\"member@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        EntityNode groupNode = EntityNode.createNodeFromGroupEmail("data.x@dp.domain.com");
        assertFalse(retrieveGroupRepoRedis.hasDirectChild(groupNode, ChildrenReference.builder().id("users.x@dp.domain.com").dataPartitionId("dp").type(NodeType.GROUP).role(Role.MEMBER).build()));
        assertFalse(retrieveGroupRepoRedis.hasDirectChild(groupNode, ChildrenReference.builder().id("member1@xxx.com").dataPartitionId("dp").type(NodeType.USER).role(Role.OWNER).build()));
    }

    @Test
    public void shouldReturnEmptyListIfNotParentsWhenLoadDirectParents() {
        List<ParentReference> parents = retrieveGroupRepoRedis.loadDirectParents("dp", "member@xxx.com");
        assertEquals(0, parents.size());
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
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionParentRefDb());
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("users.x@dp.domain.com", "{\"name\":\"data.x\",\"description\":\"\",\"id\":\"data.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("users.y@dp.domain.com", "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("member@xxx.com", "{\"name\":\"users.x\",\"description\":\"\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"users.y\",\"description\":\"\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        List<ParentReference> g3 = retrieveGroupRepoRedis.loadDirectParents("dp", "users.x@dp.domain.com");
        assertEquals(2, g3.size());
        List<ParentReference> g4 = retrieveGroupRepoRedis.loadDirectParents("dp", "users.y@dp.domain.com");
        assertEquals(1, g4.size());
        List<ParentReference> member = retrieveGroupRepoRedis.loadDirectParents("dp", "member@xxx.com");
        assertEquals(2, member.size());
    }

    @Test
    public void shouldReturnEmptySetIfNoParentsWhenLoadAllParents() {
        EntityNode memberNode = EntityNode.createMemberNodeForNewUser("member@xxx.com", "dp");
        ParentTreeDto parents = retrieveGroupRepoRedis.loadAllParents(memberNode);
        assertEquals(0, parents.getParentReferences().size());
        assertEquals(1, parents.getMaxDepth());
    }

    @Test
    public void shouldReturnAllParentsSetWhenLoadAllParents() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("users.x@dp.domain.com", "{\"name\":\"data.x\",\"description\":\"\",\"id\":\"data.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("users.y@dp.domain.com", "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("member@xxx.com", "{\"name\":\"users.x\",\"description\":\"\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"users.y\",\"description\":\"\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        EntityNode memberNode = EntityNode.createMemberNodeForNewUser("member@xxx.com", "dp");
        ParentTreeDto parents = retrieveGroupRepoRedis.loadAllParents(memberNode);
        assertEquals(4, parents.getParentReferences().size());
        assertEquals(3, parents.getMaxDepth());
    }

    /*
                       data.x@dp.domain.com
                               ^
                               |-------------------------------|
                      data.y@dp.domain.com            data.z@dp.domain.com
                               ^                               ^
           |-------------------|                               |
users.x@dp.domain.com  users.y@dp.domain.com -------------------
           ^                   ^
           |-------------------|
    member@xxx.com
     */
    @Test
    public void shouldReturnAllParentsAndMaxDepthWhenLoadAllParents() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("member@xxx.com", "{\"name\":\"users.x\",\"description\":\"\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"users.y\",\"description\":\"\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("users.x@dp.domain.com", "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("users.y@dp.domain.com", "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"data.z\",\"description\":\"\",\"id\":\"data.z@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("data.y@dp.domain.com", "{\"name\":\"data.x\",\"description\":\"\",\"id\":\"data.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("data.z@dp.domain.com", "{\"name\":\"data.x\",\"description\":\"\",\"id\":\"data.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        EntityNode memberNode = EntityNode.createMemberNodeForNewUser("member@xxx.com", "dp");
        ParentTreeDto parents = retrieveGroupRepoRedis.loadAllParents(memberNode);
        assertEquals(5, parents.getParentReferences().size());
        assertEquals(4, parents.getMaxDepth());
    }

    @Test
    public void shouldReturnEmptyListIfNoChildrenWhenLoadDirectChildren() {
        List<ChildrenReference> children = retrieveGroupRepoRedis.loadDirectChildren("dp", "data.x@dp.domain.com");
        assertEquals(0, children.size());
    }

    @Test
    public void shouldReturnItSelfIfNoChildrenWhenLoadAllChildrenUsers() {
        EntityNode groupNode = EntityNode.createMemberNodeForNewUser("member@xxx.com", "dp");
        ChildrenTreeDto childrenUsers = retrieveGroupRepoRedis.loadAllChildrenUsers(groupNode);
        assertEquals(1, childrenUsers.getChildrenUserIds().size());
        assertEquals(1, childrenUsers.getMaxDepth());
    }

    @Test
    public void shouldReturnAllChildrenUsersWhenLoadAllChildrenUsers() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("data.x@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"OWNER\",\"id\":\"member1@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        commands.sadd("data.y@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"MEMBER\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"MEMBER\",\"id\":\"member2@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        commands.sadd("users.x@dp.domain.com", "{\"role\":\"OWNER\",\"id\":\"member2@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        EntityNode groupNode = EntityNode.createNodeFromGroupEmail("data.x@dp.domain.com");
        ChildrenTreeDto childrenUserDto = retrieveGroupRepoRedis.loadAllChildrenUsers(groupNode);
        assertEquals(2, childrenUserDto.getChildrenUserIds().size());
        assertTrue(childrenUserDto.getChildrenUserIds().containsAll(Arrays.asList("member1@xxx.com", "member2@xxx.com")));
        assertEquals(3, childrenUserDto.getMaxDepth());
    }

    /*
                                              data.x@dp.domain.com
                                                        ^
                                                        |
                               |-------------------------------|---------------------------|
                      data.y@dp.domain.com            data.z@dp.domain.com           member1@xxx.com
                               ^                               ^-----------
           |-------------------|---------------------|                    |
users.x@dp.domain.com  users.y@dp.domain.com    member2@xxx.com       member2@xxx.com
           ^                   ^
           |                   |--------------|
    member3@xxx.com     member3@xxx.com member4@xxx.com
     */
    @Test
    public void shouldReturnAllChildrenUsersAndMaxDepthWhenLoadAllChildrenUsers() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("data.x@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"MEMBER\",\"id\":\"data.z@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"OWNER\",\"id\":\"member1@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        commands.sadd("data.z@dp.domain.com", "{\"role\":\"OWNER\",\"id\":\"member2@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        commands.sadd("data.y@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"MEMBER\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"MEMBER\",\"id\":\"member2@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        commands.sadd("users.x@dp.domain.com", "{\"role\":\"OWNER\",\"id\":\"member3@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        commands.sadd("users.y@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"member3@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}",
                "{\"role\":\"OWNER\",\"id\":\"member4@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        EntityNode groupNode = EntityNode.createNodeFromGroupEmail("data.x@dp.domain.com");
        ChildrenTreeDto childrenUsers = retrieveGroupRepoRedis.loadAllChildrenUsers(groupNode);
        assertEquals(4, childrenUsers.getChildrenUserIds().size());
        assertTrue(childrenUsers.getChildrenUserIds().containsAll(Arrays.asList("member1@xxx.com", "member2@xxx.com", "member3@xxx.com", "member4@xxx.com")));
        assertEquals(3, childrenUsers.getMaxDepth());
    }

    @Test
    public void shouldFilterAccessibleParentReferencesWhenGivenValidAppId() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        String appId = "appid";
        commands.select(config.getPartitionAppIdDb());
        commands.sadd(appId, "users.x@dp.domain.com", "users.z@dp.domain.com");
        commands.sadd("otherappid", "users.y@dp.domain.com", "users.z@dp.domain.com");
        commands.sadd(DEFAULT_APPID_KEY, "users@dp.domain.com");
        Set<ParentReference> parentReferences = new HashSet<>();

        parentReferences.add(ParentReference.builder().id("users.x@dp.domain.com").build());
        parentReferences.add(ParentReference.builder().id("users.y@dp.domain.com").build());
        parentReferences.add(ParentReference.builder().id("users.z@dp.domain.com").build());
        parentReferences.add(ParentReference.builder().id("users@dp.domain.com").build());


        Set<ParentReference> res = retrieveGroupRepoRedis.filterParentsByAppId(parentReferences, "dp", appId);

        assertEquals(3, res.size());
        assertEquals(0, res.stream().filter(r -> r.getId().equalsIgnoreCase("users.y@dp.domain.com")).count());
    }

    @Test
    public void shouldReturnAllParentReferencesWhenNoOtherAppIds() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        String appId = "appid";
        commands.select(config.getPartitionAppIdDb());
        commands.sadd(appId, "users.x@dp.domain.com");
        commands.sadd(DEFAULT_APPID_KEY, "users@dp.domain.com", "users.y@dp.domain.com");
        Set<ParentReference> parentReferences = new HashSet<>();

        parentReferences.add(ParentReference.builder().id("users.x@dp.domain.com").build());
        parentReferences.add(ParentReference.builder().id("users.y@dp.domain.com").build());
        parentReferences.add(ParentReference.builder().id("users@dp.domain.com").build());


        Set<ParentReference> res = retrieveGroupRepoRedis.filterParentsByAppId(parentReferences, "dp", appId);

        assertEquals(3, res.size());
    }

    @Test
    public void shouldReturnAllParentReferencesWhenGivenNullAppId() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        String appId = null;
        commands.select(config.getPartitionAppIdDb());
        commands.sadd(DEFAULT_APPID_KEY, "users@dp.domain.com", "users.x@dp.domain.com", "users.y@dp.domain.com");
        Set<ParentReference> parentReferences = new HashSet<>();

        parentReferences.add(ParentReference.builder().id("users.x@dp.domain.com").build());
        parentReferences.add(ParentReference.builder().id("users.y@dp.domain.com").build());
        parentReferences.add(ParentReference.builder().id("users@dp.domain.com").build());


        Set<ParentReference> res = retrieveGroupRepoRedis.filterParentsByAppId(parentReferences, "dp", appId);

        assertEquals(3, res.size());
    }

    @Test
    public void shouldReturnGroupOwners() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("data.x@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"OWNER\",\"id\":\"member1@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}",
                "{\"role\":\"OWNER\",\"id\":\"member2@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}",
                "{\"role\":\"MEMBER\",\"id\":\"member3@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");

        Set<String> resGroupOwners = retrieveGroupRepoRedis.getGroupOwners("dp", "data.x@dp.domain.com");
        assertEquals(2, resGroupOwners.size());
        assertEquals(resGroupOwners, new HashSet<>(Arrays.asList("member1@xxx.com", "member2@xxx.com")));
    }
}
