package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.redis.operation;

import io.github.resilience4j.retry.Retry;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation.RemoveMemberChildUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RemoveMemberChildUpdateOperationImplIntegratedWithEmbededRedisTests {
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private RedisConnector redisConnector;
    @Autowired
    private GenericObjectPoolConfig poolConfig;
    @Autowired
    private GcpAppProperties config;
    @Autowired
    private Retry retry;

    private static RedisServer dp1RedisServer;
    private static RedisClient dp1RedisClient;
    private static RedisServer dp2RedisServer;
    private static RedisClient dp2RedisClient;

    @BeforeClass
    public static void setupClass() throws IOException {
        dp1RedisServer = new RedisServer(6379);
        dp1RedisServer.start();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        dp1RedisClient = RedisClient.create(uri);
        dp2RedisServer = new RedisServer(6380);
        dp2RedisServer.start();
        uri = RedisURI.builder().withHost("localhost").withPort(6380).build();
        dp2RedisClient = RedisClient.create(uri);
    }

    @AfterClass
    public static void end() {
        dp1RedisServer.stop();
        dp2RedisServer.stop();
    }

    @Before
    public void setup() {
        GenericObjectPool<StatefulRedisConnection<String, String>> poolDp1 = ConnectionPoolSupport.createGenericObjectPool(
                () -> RedisClient.create(RedisURI.create("localhost", 6379)).connect(), this.poolConfig);
        when(redisConnector.getPartitionRedisConnectionPool("dp1")).thenReturn(new RedisConnectionPool(poolDp1));
        GenericObjectPool<StatefulRedisConnection<String, String>> poolDp2 = ConnectionPoolSupport.createGenericObjectPool(
                () -> RedisClient.create(RedisURI.create("localhost", 6380)).connect(), this.poolConfig);
        when(redisConnector.getPartitionRedisConnectionPool("dp2")).thenReturn(new RedisConnectionPool(poolDp2));
    }

    @After
    public void cleanup() {
        StatefulRedisConnection<String, String> connection = dp1RedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.flushall();
        connection = dp2RedisClient.connect();
        commands = connection.sync();
        commands.flushall();
    }

    @Test
    public void should_updateAllChildren_whenExecute_singlePartition() throws Exception {
        StatefulRedisConnection<String, String> connection = dp1RedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp1.domain.com", "{\"nodeId\":\"users.x@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\"," +
                "\"dataPartitionId\":\"dp1\"}");
        commands.set("users.y@dp1.domain.com", "{\"nodeId\":\"users.y@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"users.y\"," +
                "\"dataPartitionId\":\"dp1\"}");
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("users.y@dp1.domain.com", "{\"name\":\"users.x\",\"description\":\"\",\"id\":\"users.x@dp1.domain.com\",\"dataPartitionId\":\"dp1\"}");

        EntityNode g1 = EntityNode.builder()
                .nodeId("users.y@dp1.domain.com")
                .name("users.y")
                .type(NodeType.GROUP)
                .dataPartitionId("dp1")
                .build();
        EntityNode g2 = EntityNode.builder()
                .nodeId("users.x@dp1.domain.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp1")
                .build();

        Operation removeMemberChildUpdateOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(g2).memberId(g1.getNodeId()).memberPartitionId(g1.getDataPartitionId()).build();

        removeMemberChildUpdateOperation.execute();
        commands.select(config.getPartitionParentRefDb());
        assertTrue(commands.smembers("users.y@dp1.domain.com").isEmpty());
    }

    @Test
    public void should_revertChanges_whenUndo_singlePartition() throws Exception {
        StatefulRedisConnection<String, String> connection = dp1RedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp1.domain.com", "{\"nodeId\":\"users.x@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\"," +
                "\"dataPartitionId\":\"dp1\"}");
        commands.set("users.y@dp1.domain.com", "{\"nodeId\":\"users.y@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"users.y\"," +
                "\"dataPartitionId\":\"dp1\"}");
        commands.select(config.getPartitionParentRefDb());
        commands.sadd("users.y@dp1.domain.com", "{\"name\":\"users.x\",\"description\":\"\",\"id\":\"users.x@dp1.domain.com\",\"dataPartitionId\":\"dp1\"}");

        EntityNode g1 = EntityNode.builder()
                .nodeId("users.y@dp1.domain.com")
                .name("users.y")
                .type(NodeType.GROUP)
                .dataPartitionId("dp1")
                .build();
        EntityNode g2 = EntityNode.builder()
                .nodeId("users.x@dp1.domain.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp1")
                .build();

        Operation removeMemberChildUpdateOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(g2).memberId(g1.getNodeId()).memberPartitionId(g1.getDataPartitionId()).build();

        commands.select(config.getPartitionParentRefDb());
        removeMemberChildUpdateOperation.execute();
        assertTrue(commands.smembers("users.y@dp1.domain.com").isEmpty());
        removeMemberChildUpdateOperation.undo();
        assertReferenceEquals(new String[]{"{\"name\":\"users.x\",\"description\":\"\",\"id\":\"users.x@dp1.domain.com\",\"dataPartitionId\":\"dp1\"}"}, commands.smembers("users.y@dp1.domain.com"));
    }

    @Test
    public void should_updateAllChildren_whenExecute_crossPartition() throws Exception {
        StatefulRedisConnection<String, String> connection1 = dp1RedisClient.connect();
        RedisCommands<String, String> commands1 = connection1.sync();
        commands1.select(config.getPartitionEntityNodeDb());
        StatefulRedisConnection<String, String> connection2 = dp2RedisClient.connect();
        RedisCommands<String, String> commands2 = connection2.sync();
        commands2.select(config.getPartitionEntityNodeDb());
        commands2.set("users.x@dp2.domain.com", "{\"nodeId\":\"users.x@dp2.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\"," +
                "\"dataPartitionId\":\"dp2\"}");
        commands1.set("users.y@dp1.domain.com", "{\"nodeId\":\"users.y@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"users.y\"," +
                "\"dataPartitionId\":\"dp1\"}");
        commands1.select(config.getPartitionParentRefDb());
        commands1.sadd("users.y@dp1.domain.com", "{\"name\":\"users.x\",\"description\":\"\",\"id\":\"users.x@dp2.domain.com\",\"dataPartitionId\":\"dp2\"}");


        EntityNode g1 = EntityNode.builder()
                .nodeId("users.y@dp1.domain.com")
                .name("users.y")
                .type(NodeType.GROUP)
                .dataPartitionId("dp1")
                .build();
        EntityNode g2 = EntityNode.builder()
                .nodeId("users.x@dp2.domain.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp2")
                .build();

        Operation removeMemberChildUpdateOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(g2).memberId(g1.getNodeId()).memberPartitionId(g1.getDataPartitionId()).build();

        commands1.select(config.getPartitionParentRefDb());
        removeMemberChildUpdateOperation.execute();
        assertTrue(commands1.smembers("users.y@dp1.domain.com").isEmpty());
    }

    @Test
    public void should_revertChanges_whenUndo_crossPartition() throws Exception {
        StatefulRedisConnection<String, String> connection1 = dp1RedisClient.connect();
        RedisCommands<String, String> commands1 = connection1.sync();
        commands1.select(config.getPartitionEntityNodeDb());
        StatefulRedisConnection<String, String> connection2 = dp2RedisClient.connect();
        RedisCommands<String, String> commands2 = connection2.sync();
        commands2.select(config.getPartitionEntityNodeDb());
        commands2.set("users.x@dp2.domain.com", "{\"nodeId\":\"users.x@dp2.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\"," +
                "\"dataPartitionId\":\"dp2\"}");
        commands1.set("users.y@dp1.domain.com", "{\"nodeId\":\"users.y@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"users.y\"," +
                "\"dataPartitionId\":\"dp1\"}");
        commands1.select(config.getPartitionParentRefDb());
        commands1.sadd("users.y@dp1.domain.com", "{\"name\":\"users.x\",\"description\":\"\",\"id\":\"users.x@dp2.domain.com\",\"dataPartitionId\":\"dp2\"}");


        EntityNode g1 = EntityNode.builder()
                .nodeId("users.y@dp1.domain.com")
                .name("users.y")
                .type(NodeType.GROUP)
                .dataPartitionId("dp1")
                .build();
        EntityNode g2 = EntityNode.builder()
                .nodeId("users.x@dp2.domain.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp2")
                .build();

        Operation removeMemberChildUpdateOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(g2).memberId(g1.getNodeId()).memberPartitionId(g1.getDataPartitionId()).build();

        commands1.select(config.getPartitionParentRefDb());
        removeMemberChildUpdateOperation.execute();
        assertTrue(commands1.smembers("users.y@dp1.domain.com").isEmpty());
        removeMemberChildUpdateOperation.undo();
        assertReferenceEquals(new String[]{"{\"name\":\"users.x\",\"description\":\"\",\"id\":\"users.x@dp2.domain.com\",\"dataPartitionId\":\"dp2\"}"}, commands1.smembers("users.y@dp1.domain.com"));
    }

    private void assertReferenceEquals(final String[] expectedReferences, Set<String> references) {
        assertEquals(new HashSet<>(Arrays.asList(expectedReferences)), references);
    }
}
