package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.deletegroup;

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
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.service.PartitionRedisInstanceService;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties.DEFAULT_APPID_KEY;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DeleteGroupRepoRedisIntegratedWithEmbeddedRedisTests {

    private static final String DATA_PARTITION_ID = "dp";
    private static RedisServer centralRedisServer;
    private static RedisClient centralRedisClient;
    private static RedisServer partitionRedisServer;
    private static RedisClient partitionRedisClient;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private RequestInfoUtilService requestInfoUtilService;
    @MockBean
    private PartitionRedisInstanceService partitionRedisInstanceService;
    @Autowired
    private GcpAppProperties config;
    @Autowired
    private DeleteGroupRepoRedis deleteGroupRepoRedis;


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
        when(requestInfoUtilService.getUserId(any())).thenReturn("callerdesid");
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
    public void shouldDeleteGroupAndPreserveParents() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x@dp.domain.com", "{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("data.y@dp.domain.com", "{\"nodeId\":\"data.y@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.y\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("data.z@dp.domain.com", "{\"nodeId\":\"data.z@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.z\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("users.x@dp.domain.com", "{\"name\":\"data.x\",\"description\":\"\",\"id\":\"data.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"data.z\",\"description\":\"\",\"id\":\"data.z@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("data.x@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        commands.sadd("data.y@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        commands.sadd("data.z@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");

        EntityNode g2 = EntityNode.builder()
                .nodeId("users.x@dp.domain.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();

        deleteGroupRepoRedis.deleteGroup(g2);

        commands.select(config.getPartitionEntityNodeDb());
        assertNull(commands.get("users.x@dp.domain.com"));
        commands.select(config.getPartitionChildrenRefDb());
        assertEquals(0, commands.smembers("data.x@dp.domain.com").size());
        assertEquals(0, commands.smembers("data.y@dp.domain.com").size());
        assertEquals(0, commands.smembers("data.z@dp.domain.com").size());
        commands.select(config.getPartitionParentRefDb());
        assertEquals(0, commands.smembers("users.x@dp.domain.com").size());
        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, "users.x@dp.domain.com");
    }

    @Test
    public void shouldDeleteGroupAndPreserveChildren() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.y@dp.domain.com", "{\"nodeId\":\"users.y@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.y\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("users.y1@dp.domain.com", "{\"nodeId\":\"users.y1@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.y1\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("users.y2@dp.domain.com", "{\"nodeId\":\"users.y2@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.y2\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("member@xxx.com", "{\"nodeId\":\"member@xxx.com\",\"type\":\"USER\",\"name\":\"member@xxx.com\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("users.y1@dp.domain.com", "{\"name\":\"users.y\",\"description\":\"\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("users.y2@dp.domain.com", "{\"name\":\"users.y\",\"description\":\"\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("member@xxx.com", "{\"name\":\"users.y\",\"description\":\"\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("users.y@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.y1@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"MEMBER\",\"id\":\"users.y2@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}",
                "{\"role\":\"MEMBER\",\"id\":\"member@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}");

        EntityNode g1 = EntityNode.builder()
                .nodeId("users.y@dp.domain.com")
                .name("users.y")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();

        deleteGroupRepoRedis.deleteGroup(g1);

        commands.select(config.getPartitionEntityNodeDb());
        assertNull(commands.get("users.y@dp.domain.com"));
        commands.select(config.getPartitionChildrenRefDb());
        assertEquals(0, commands.smembers("users.y@dp.domain.com").size());
        commands.select(config.getPartitionParentRefDb());
        assertEquals(0, commands.smembers("users.y1@dp.domain.com").size());
        assertEquals(0, commands.smembers("users.y2@dp.domain.com").size());
        assertEquals(0, commands.smembers("member@xxx.com").size());
        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, "users.y@dp.domain.com");
    }

    @Test
    public void shouldUpdateAppIdCacheWhenDeletingAGroupHaveEmptyAppIds() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x@dp.domain.com", "{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("data.y@dp.domain.com", "{\"nodeId\":\"data.y@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.y\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("data.z@dp.domain.com", "{\"nodeId\":\"data.z@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.z\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("users.x@dp.domain.com", "{\"name\":\"data.x\",\"description\":\"\",\"id\":\"data.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"data.z\",\"description\":\"\",\"id\":\"data.z@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("data.x@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        commands.sadd("data.y@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        commands.sadd("data.z@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        commands.select(config.getPartitionAppIdDb());
        commands.sadd(DEFAULT_APPID_KEY, "users.x@dp.domain.com", "data.x@dp.domain.com", "data.y@dp.domain.com", "data.z@dp.domain.com");

        EntityNode g2 = EntityNode.builder()
                .nodeId("users.x@dp.domain.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();

        deleteGroupRepoRedis.deleteGroup(g2);

        commands.select(config.getPartitionAppIdDb());
        assertEquals(3, commands.smembers(DEFAULT_APPID_KEY).size());
    }

    @Test
    public void shouldUpdateAppIdCacheWhenDeletingAGroupHaveAppIds() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x@dp.domain.com", "{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\", \"appIds\":[\"app1\"]}\"}");
        commands.set("data.y@dp.domain.com", "{\"nodeId\":\"data.y@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.y\"," +
                "\"dataPartitionId\":\"dp\", \"appIds\":[\"app2\"]}\"}");
        commands.set("data.z@dp.domain.com", "{\"nodeId\":\"data.z@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.z\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\"," +
                "\"dataPartitionId\":\"dp\", \"appIds\":[\"app1\",\"app2\"]}");
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("users.x@dp.domain.com", "{\"name\":\"data.x\",\"description\":\"\",\"id\":\"data.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}",
                "{\"name\":\"data.z\",\"description\":\"\",\"id\":\"data.z@dp.domain.com\",\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("data.x@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        commands.sadd("data.y@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        commands.sadd("data.z@dp.domain.com", "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}");
        commands.select(config.getPartitionAppIdDb());
        commands.sadd("app1", "users.x@dp.domain.com", "data.x@dp.domain.com");
        commands.sadd("app2", "users.x@dp.domain.com", "data.y@dp.domain.com");
        commands.sadd(DEFAULT_APPID_KEY, "data.z@dp.domain.com");

        EntityNode g2 = EntityNode.builder()
                .nodeId("users.x@dp.domain.com")
                .name("users.x")
                .appIds(new HashSet<>(Arrays.asList("app1", "app2")))
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .build();

        deleteGroupRepoRedis.deleteGroup(g2);

        commands.select(config.getPartitionAppIdDb());
        assertEquals(1, commands.smembers(DEFAULT_APPID_KEY).size());
        assertEquals(1, commands.smembers("app1").size());
        assertEquals(1, commands.smembers("app2").size());
    }

    @Test
    public void shouldReturnIfTheGivenGroupIsNotFoundInRedisWhenDeleteGroup() {
        EntityNode groupNode = EntityNode.builder().nodeId("newgroup@dp.domain.com").name("newgroup")
                .type(NodeType.GROUP).dataPartitionId("dp").build();
        deleteGroupRepoRedis.deleteGroup(groupNode);
        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, "newgroup@dp.domain.com");
        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, "newgroup@dp.domain.com");
    }

}
