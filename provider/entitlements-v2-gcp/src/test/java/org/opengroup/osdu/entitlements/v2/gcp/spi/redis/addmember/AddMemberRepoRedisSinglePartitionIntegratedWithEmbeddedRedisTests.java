package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.addmember;

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
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.service.PartitionRedisInstanceService;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class AddMemberRepoRedisSinglePartitionIntegratedWithEmbeddedRedisTests {

    @MockBean
    private GcpAppProperties config;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;

    @MockBean
    private PartitionRedisInstanceService partitionRedisInstanceService;
    private static final String DATA_PARTITION_ID = "dp";

    private static RedisServer centralRedisServer;
    private static RedisClient centralRedisClient;
    private static RedisServer partitionRedisServer;
    private static RedisClient partitionRedisClient;

    @Autowired
    private AddMemberRepoRedis addMemberRepoRedis;

    @BeforeClass
    public static void setupClass() throws IOException {
        centralRedisServer = new RedisServer(7000);
        centralRedisServer.start();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(7000).build();
        centralRedisClient = RedisClient.create(uri);

        partitionRedisServer = new RedisServer(6379);
        partitionRedisServer.start();
        uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        partitionRedisClient = RedisClient.create(uri);
    }

    @AfterClass
    public static void end() {
        centralRedisServer.stop();
        partitionRedisServer.stop();
    }

    @Before
    public void setup() {
        when(config.getCentralRedisInstIp()).thenReturn("localhost");
        when(config.getCentralRedisInstPort()).thenReturn(7000);
        when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition(DATA_PARTITION_ID)).thenReturn("localhost");
        when(config.getPartitionChildrenRefDb()).thenReturn(1);
        when(config.getPartitionParentRefDb()).thenReturn(2);
        when(config.getPartitionChildrenRefDb()).thenReturn(3);
    }

    @After
    public void cleanup() {
        StatefulRedisConnection<String, String> connection = centralRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.flushall();
        connection = partitionRedisClient.connect();
        commands = connection.sync();
        commands.flushall();
    }

    @Test
    public void should_createAndSetMemberReference_whenInsertAUser_andAddedMemberNodeDoesNotExist() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x.viewers@dp.domain.com", "{\"nodeId\":\"data.x.viewers@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x.viewers\"," +
                "\"dataPartitionId\":\"dp\"}");

        EntityNode memberNode = EntityNode.builder()
                .nodeId("member@xxx.com")
                .name("member@xxx.com")
                .type(NodeType.USER)
                .dataPartitionId("dp")
                .build();
        EntityNode groupNode = EntityNode.builder()
                .nodeId("data.x.viewers@dp.domain.com")
                .name("data.x.viewers")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
                .memberNode(memberNode)
                .role(Role.MEMBER)
                .existingParents(new HashSet<>())
                .partitionId("dp").build();

        Set<String> impactedUsers = addMemberRepoRedis.addMember(groupNode, addMemberRepoDto);

        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("member@xxx.com"));
        commands.select(config.getPartitionChildrenRefDb());
        assertEquals(1, commands.smembers("data.x.viewers@dp.domain.com").size());
        commands.select(config.getPartitionParentRefDb());
        assertEquals(1, commands.smembers("member@xxx.com").size());
        verify(auditLogger).addMember(AuditStatus.SUCCESS, "data.x.viewers@dp.domain.com", "member@xxx.com", Role.MEMBER);
    }

    @Test
    public void should_updateReferences_whenInsertAUser_andAddedMemberExist() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.set("data.x.viewers@dp.domain.com", "{\"nodeId\":\"data.x.viewers@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x.viewers\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("member@xxx.com", "{\"nodeId\":\"member@xxx.com\",\"type\":\"USER\"," +
                "\"name\":\"member@xxx.com\"}");

        EntityNode memberNode = EntityNode.builder()
                .nodeId("member@xxx.com")
                .name("member@xxx.com")
                .type(NodeType.USER)
                .dataPartitionId("dp")
                .build();
        EntityNode groupNode = EntityNode.builder()
                .nodeId("data.x.viewers@dp.domain.com")
                .name("data.x.viewers")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
                .memberNode(memberNode)
                .role(Role.MEMBER)
                .existingParents(new HashSet<>())
                .partitionId("dp").build();

        Set<String> impactedUsers = addMemberRepoRedis.addMember(groupNode, addMemberRepoDto);

        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("member@xxx.com"));
        String addedMemberNode = commands.get("member@xxx.com");
        assertEquals("{\"nodeId\":\"member@xxx.com\",\"type\":\"USER\",\"name\":\"member@xxx.com\"}", addedMemberNode);
        commands.select(config.getPartitionChildrenRefDb());
        assertEquals(1, commands.smembers("data.x.viewers@dp.domain.com").size());
        commands.select(config.getPartitionParentRefDb());
        assertEquals(1, commands.smembers("member@xxx.com").size());
        verify(auditLogger).addMember(AuditStatus.SUCCESS, "data.x.viewers@dp.domain.com", "member@xxx.com", Role.MEMBER);
    }

    @Test
    public void should_updateReferences_whenInsertAGroupToAnotherGroup() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.y\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("data.x.viewers@dp.domain.com", "{\"nodeId\":\"data.x.viewers@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x.viewers\"," +
                "\"dataPartitionId\":\"dp\"}");

        EntityNode memberNode = EntityNode.builder()
                .nodeId("users.x@dp.domain.com")
                .name("users.x").type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();
        EntityNode groupNode = EntityNode.builder()
                .nodeId("data.x.viewers@dp.domain.com")
                .name("data.x.viewers")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
                .memberNode(memberNode)
                .role(Role.MEMBER)
                .existingParents(new HashSet<>())
                .partitionId("dp").build();

        Set<String> impactedUsers = addMemberRepoRedis.addMember(groupNode, addMemberRepoDto);

        assertTrue(impactedUsers.isEmpty());
        commands.select(config.getPartitionChildrenRefDb());
        assertEquals(1, commands.smembers("data.x.viewers@dp.domain.com").size());
        commands.select(config.getPartitionParentRefDb());
        assertEquals(1, commands.smembers("users.x@dp.domain.com").size());
        verify(auditLogger).addMember(AuditStatus.SUCCESS, "data.x.viewers@dp.domain.com", "users.x@dp.domain.com", Role.MEMBER);
    }

    @Test
    public void should_preserveUserPartitionList_whenAddUserToRootUserGroup() throws Exception {
        StatefulRedisConnection<String, String> partitionDbConnection = partitionRedisClient.connect();
        RedisCommands<String, String> partitionDbCommands = partitionDbConnection.sync();
        partitionDbCommands.select(config.getPartitionEntityNodeDb());
        partitionDbCommands.set("users@dp.domain.com", "{\"nodeId\":\"users@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users\"," +
                "\"dataPartitionId\":\"dp\"}");

        EntityNode memberNode = EntityNode.builder()
                .nodeId("user@xxx.com")
                .name("user@xxx.com")
                .type(NodeType.USER)
                .dataPartitionId("dp")
                .build();
        EntityNode groupNode = EntityNode.builder()
                .nodeId("users@dp.domain.com")
                .name("users")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
                .memberNode(memberNode)
                .role(Role.MEMBER)
                .existingParents(new HashSet<>())
                .partitionId("dp").build();

        Set<String> impactedUsers = addMemberRepoRedis.addMember(groupNode, addMemberRepoDto);

        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("user@xxx.com"));
        StatefulRedisConnection<String, String> centralDbConnection = centralRedisClient.connect();
        RedisCommands<String, String> centralDbCommands = centralDbConnection.sync();
        Set<String> partitionIdList = centralDbCommands.smembers("user@xxx.com");
        assertEquals(1, partitionIdList.size());
        assertTrue(partitionIdList.contains("dp"));
        verify(auditLogger).addMember(AuditStatus.SUCCESS, "users@dp.domain.com", "user@xxx.com", Role.MEMBER);
    }

    @Test
    public void should_throw412_whenMaxDepthReached() {
        StatefulRedisConnection<String, String> partitionDbConnection = partitionRedisClient.connect();
        RedisCommands<String, String> partitionDbCommands = partitionDbConnection.sync();
        partitionDbCommands.select(config.getPartitionParentRefDb());
        partitionDbCommands.sadd("data.1@dp.domain.com", "{\"name\":\"data.2\",\"description\":\"\",\"id\":\"data.2@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        partitionDbCommands.sadd("data.2@dp.domain.com", "{\"name\":\"data.3\",\"description\":\"\",\"id\":\"data.3@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        partitionDbCommands.sadd("data.3@dp.domain.com", "{\"name\":\"data.4\",\"description\":\"\",\"id\":\"data.4@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        partitionDbCommands.sadd("data.4@dp.domain.com", "{\"name\":\"data.5\",\"description\":\"\",\"id\":\"data.5@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        partitionDbCommands.sadd("data.5@dp.domain.com", "{\"name\":\"data.6\",\"description\":\"\",\"id\":\"data.6@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        partitionDbCommands.select(config.getPartitionChildrenRefDb());
        partitionDbCommands.sadd("users.5@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.4@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        partitionDbCommands.sadd("users.4@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.3@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        partitionDbCommands.sadd("users.3@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.2@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        partitionDbCommands.sadd("users.2@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.1@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        partitionDbCommands.sadd("users.1@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"member@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");

        EntityNode memberNode = EntityNode.builder()
                .nodeId("users.5@dp.domain.com")
                .name("users.5")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();
        EntityNode groupNode = EntityNode.builder()
                .nodeId("data.1@dp.domain.com")
                .name("data.1")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder()
                .memberNode(memberNode)
                .role(Role.MEMBER)
                .existingParents(new HashSet<>())
                .partitionId("dp").build();

        try {
            addMemberRepoRedis.addMember(groupNode, addMemberRepoDto);
            fail(String.format("should throw exception"));
        } catch (AppException ex) {
            assertEquals(412, ex.getError().getCode());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex));
        }
    }
}
