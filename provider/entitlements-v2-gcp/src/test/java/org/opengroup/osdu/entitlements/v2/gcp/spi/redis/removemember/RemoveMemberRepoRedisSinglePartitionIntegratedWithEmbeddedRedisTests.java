package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.removemember;

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
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.service.PartitionRedisInstanceService;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RemoveMemberRepoRedisSinglePartitionIntegratedWithEmbeddedRedisTests {

    @MockBean
    private GcpAppProperties config;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private AuditLogger auditLogger;
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
    private RemoveMemberRepoRedis removeMemberRepoRedis;

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
    public void should_updateReference_WhenRemoveAUser_fromAGroup() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.set("data.x.viewers@dp.domain.com", "{\"nodeId\":\"data.x.viewers@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x.viewers\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("member@xxx.com", "{\"nodeId\":\"member@xxx.com\",\"type\":\"USER\"," +
                "\"name\":\"member@xxx.com\"}");
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("data.x.viewers@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"member@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("member@xxx.com", "{\"name\":\"data.x.viewers\",\"description\":\"\",\"id\":\"data.x.viewers@dp.domain.com\",\"dataPartitionId\":\"dp\"}");

        EntityNode memberNode = EntityNode.builder().nodeId("member@xxx.com").type(NodeType.USER).name("member@xxx.com").dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder()
                .nodeId("data.x.viewers@dp.domain.com")
                .name("data.x.viewers")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .requesterId("requesterId").partitionId("dp")
                .childrenReference(ChildrenReference.builder().id("member@xxx.com").role(Role.MEMBER).dataPartitionId("dp").type(NodeType.USER).build())
                .build();

        Set<String> impactedUsers = removeMemberRepoRedis.removeMember(groupNode, memberNode, removeMemberServiceDto);

        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("member@xxx.com"));
        commands.select(config.getPartitionChildrenRefDb());
        assertEquals(0, commands.smembers("data.x.viewers@dp.domain.com").size());
        commands.select(config.getPartitionParentRefDb());
        assertEquals(0, commands.smembers("member@xxx.com").size());
        verify(auditLogger).removeMember(AuditStatus.SUCCESS, "data.x.viewers@dp.domain.com", "member@xxx.com", "requesterId");
    }

    @Test
    public void should_updateReferences_whenRemovingAGroupFromAnotherGroup() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.y\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("data.x.viewers@dp.domain.com", "{\"nodeId\":\"data.x.viewers@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x.viewers\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("users.y@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("users.x@dp.domain.com", "{\"name\":\"users.y\",\"description\":\"\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");

        EntityNode memberNode = EntityNode.builder()
                .nodeId("users.x@dp.domain.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();
        EntityNode groupNode = EntityNode.builder()
                .nodeId("users.y@dp.domain.com")
                .name("users.y")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .requesterId("requesterId").partitionId("dp")
                .childrenReference(ChildrenReference.builder().id("users.x@dp.domain.com").role(Role.MEMBER).dataPartitionId("dp").type(NodeType.GROUP).build())
                .build();

        Set<String> impactedUsers = removeMemberRepoRedis.removeMember(groupNode, memberNode, removeMemberServiceDto);

        assertTrue(impactedUsers.isEmpty());
        commands.select(config.getPartitionChildrenRefDb());
        assertEquals(0, commands.smembers("users.y@dp.domain.com").size());
        commands.select(config.getPartitionParentRefDb());
        assertEquals(0, commands.smembers("users.x@dp.domain.com").size());
        verify(auditLogger).removeMember(AuditStatus.SUCCESS, "users.y@dp.domain.com", "users.x@dp.domain.com", "requesterId");
    }

    @Test
    public void should_preserveUserPartitionList_whenRemovingUserFromRootUserGroup() throws Exception {
        StatefulRedisConnection<String, String> centralDbConnection = centralRedisClient.connect();
        RedisCommands<String, String> centralDbCommands = centralDbConnection.sync();
        centralDbCommands.sadd("user@xxx.com", "dp");
        StatefulRedisConnection<String, String> partitionDbConnection = partitionRedisClient.connect();
        RedisCommands<String, String> partitionDbCommands = partitionDbConnection.sync();
        partitionDbCommands.select(config.getPartitionEntityNodeDb());
        partitionDbCommands.set("users@dp.domain.com", "{\"nodeId\":\"users@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users\"}");
        partitionDbCommands.set("user@xxx.com", "{\"nodeId\":\"user@xxx.com\",\"type\":\"USER\",\"name\":\"user@xxx.com\"," +
                "\"dataPartitionId\":\"dp\"}");
        partitionDbCommands.select(config.getPartitionChildrenRefDb());
        partitionDbCommands.sadd("users@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"user@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        partitionDbCommands.select(config.getPartitionParentRefDb());
        partitionDbCommands.sadd("user@xxx.com", "{\"name\":\"users\",\"description\":\"\",\"id\":\"users@dp.domain.com\",\"dataPartitionId\":\"dp\"}");


        EntityNode memberNode = EntityNode.builder()
                .nodeId("user@xxx.com")
                .name("user@xxx.com")
                .type(NodeType.USER)
                .dataPartitionId("dp")
                .build();
        EntityNode groupNode = EntityNode.builder()
                .nodeId("users@dp.domain.com")
                .type(NodeType.GROUP)
                .name("users")
                .dataPartitionId("dp")
                .build();
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .requesterId("requesterId").partitionId("dp")
                .childrenReference(ChildrenReference.builder().id("user@xxx.com").role(Role.MEMBER).dataPartitionId("dp").type(NodeType.USER).build())
                .build();

        Set<String> impactedUsers = removeMemberRepoRedis.removeMember(groupNode, memberNode, removeMemberServiceDto);

        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("user@xxx.com"));
        Set<String> partitionIdList = centralDbCommands.smembers("user@xxx.com");
        assertEquals(0, partitionIdList.size());
        verify(auditLogger).removeMember(AuditStatus.SUCCESS, "users@dp.domain.com", "user@xxx.com", "requesterId");
    }
}
