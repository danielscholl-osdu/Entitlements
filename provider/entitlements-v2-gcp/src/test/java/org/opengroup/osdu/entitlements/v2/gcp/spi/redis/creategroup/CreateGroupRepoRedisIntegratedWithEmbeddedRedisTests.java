package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.creategroup;

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
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties.DEFAULT_APPID_KEY;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CreateGroupRepoRedisIntegratedWithEmbeddedRedisTests {
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private PartitionRedisInstanceService partitionRedisInstanceService;
    private static final String DATA_PARTITION_ID = "dp";

    private static RedisServer centralRedisServer;
    private static RedisClient centralRedisClient;
    private static RedisServer partitionRedisServer;
    private static RedisClient partitionRedisClient;

    @Autowired
    private GcpAppProperties config;
    @Autowired
    private CreateGroupRepoRedis createGroupRepoRedis;

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
        when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition(DATA_PARTITION_ID)).thenReturn("localhost");
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
    public void should_updateReference_whenCreateGroup_andNotAddDataRootGroup() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("callerdesid", "{\"id\":\"callerdesid\",\"name\":\"callerdesid\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"," +
                "\"dataPartitionId\":\"dp\"}");

        EntityNode groupNode = EntityNode.builder()
                .nodeId("newgroup@dp.domain.com")
                .type(NodeType.GROUP)
                .name("newgroup")
                .dataPartitionId("dp")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .nodeId("callerdesid")
                .name("callerdesid")
                .type(NodeType.USER)
                .dataPartitionId("dp")
                .build();
        CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .dataRootGroupNode(null)
                .addDataRootGroup(false)
                .partitionId("dp").build();

        Set<String> impactedUsers = createGroupRepoRedis.createGroup(groupNode, createGroupRepoDto);

        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("callerdesid"));
        String groupMemberNodeJson = commands.get("newgroup@dp.domain.com");
        assertEquals("{\"appIds\":[],\"name\":\"newgroup\",\"description\":\"\",\"nodeId\":\"newgroup@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}", groupMemberNodeJson);
        commands.select(config.getPartitionParentRefDb());
        assertEquals(1, commands.smembers("callerdesid").size());
        commands.select(config.getPartitionChildrenRefDb());
        assertEquals(1, commands.smembers("newgroup@dp.domain.com").size());
        verify(auditLogger).createGroup(AuditStatus.SUCCESS, "newgroup@dp.domain.com");
    }

    @Test
    public void should_updateReference_whenCreateGroup_andAddDataRootGroup() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("callerdesid", "{\"id\":\"callerdesid\",\"name\":\"callerdesid\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");
        commands.set("users.data.root@dp.domain.com", "{\"id\":\"users.data.root@dp.domain.com\",\"name\":\"users.data.root\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\",\"name\":\"users.data.root\"}");

        EntityNode groupNode = EntityNode.builder()
                .nodeId("users.x@dp.domain.com")
                .type(NodeType.GROUP)
                .name("users.x")
                .dataPartitionId("dp")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .nodeId("callerdesid")
                .name("callerdesid")
                .type(NodeType.USER)
                .dataPartitionId("dp")
                .build();
        EntityNode dataRootGroupNode = EntityNode.builder()
                .nodeId("users.data.root@dp.domain.com")
                .name("users.data.root")
                .type(NodeType.GROUP)
                .name("users.data.root")
                .dataPartitionId("dp")
                .build();
        CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .dataRootGroupNode(dataRootGroupNode)
                .addDataRootGroup(true)
                .partitionId("dp").build();

        Set<String> impactedUsers = createGroupRepoRedis.createGroup(groupNode, createGroupRepoDto);

        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("callerdesid"));
        String groupMemberNodeJson = commands.get("users.x@dp.domain.com");
        assertEquals("{\"appIds\":[],\"name\":\"users.x\",\"description\":\"\",\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}", groupMemberNodeJson);
        commands.select(config.getPartitionParentRefDb());
        assertEquals(1, commands.smembers("callerdesid").size());
        commands.select(config.getPartitionChildrenRefDb());
        assertEquals(2, commands.smembers("users.x@dp.domain.com").size());
        verify(auditLogger).createGroup(AuditStatus.SUCCESS, "users.x@dp.domain.com");
    }

    @Test
    public void should_updateAppIdCache_whenCreateGroup() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("callerdesid", "{\"id\":\"callerdesid\",\"name\":\"callerdesid\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"," +
                "\"dataPartitionId\":\"dp\"}");

        EntityNode groupNode = EntityNode.builder()
                .nodeId("newgroup@dp.domain.com")
                .type(NodeType.GROUP)
                .name("newgroup")
                .dataPartitionId("dp")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .nodeId("callerdesid")
                .name("callerdesid")
                .type(NodeType.USER)
                .dataPartitionId("dp")
                .build();
        CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .dataRootGroupNode(null)
                .addDataRootGroup(false)
                .partitionId("dp").build();

        Set<String> impactedUsers = createGroupRepoRedis.createGroup(groupNode, createGroupRepoDto);

        assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("callerdesid"));
        commands.select(config.getPartitionAppIdDb());
        assertEquals(1, commands.smembers(DEFAULT_APPID_KEY).size());
    }

    @Test
    public void should_throwException_ifSetNxCommandReturnsFalse() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("newgroup@dp.domain.com", "{\"id\":\"newgroup@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"newgroup\"}");

        EntityNode groupNode = EntityNode.builder()
                .nodeId("newgroup@dp.domain.com")
                .type(NodeType.GROUP)
                .name("newgroup")
                .dataPartitionId("dp")
                .build();
        EntityNode requesterNode = EntityNode.builder()
                .nodeId("callerdesid")
                .name("callerdesid")
                .type(NodeType.USER)
                .dataPartitionId("dp")
                .build();
        CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .dataRootGroupNode(null)
                .addDataRootGroup(false)
                .partitionId("dp").build();
        try {
            createGroupRepoRedis.createGroup(groupNode, createGroupRepoDto);
            fail("Should throw exception");
        } catch (AppException ex) {
            assertEquals(409, ex.getError().getCode());
            verify(auditLogger).createGroup(AuditStatus.FAILURE, "newgroup@dp.domain.com");
        } catch (Exception ex) {
            fail(String.format("Should not throw exception: %s", ex));
        }
    }
}
