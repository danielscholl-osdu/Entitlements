package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.redis.operation;

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
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation.DeleteGroupOperationImpl;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties.DEFAULT_APPID_KEY;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DeleteGroupOperationImplTests {

    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private RedisConnector redisConnector;
    @Autowired
    private GcpAppProperties config;
    @Autowired
    private GenericObjectPoolConfig poolConfig;

    private static RedisServer dpRedisServer;
    private static RedisClient dpRedisClient;

    @BeforeClass
    public static void setupClass() throws IOException {
        dpRedisServer = new RedisServer(6379);
        dpRedisServer.start();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        dpRedisClient = RedisClient.create(uri);
    }

    @AfterClass
    public static void end() {
        dpRedisServer.stop();
    }

    @Before
    public void setup() {
        GenericObjectPool<StatefulRedisConnection<String, String>> poolDp = ConnectionPoolSupport.createGenericObjectPool(
                () -> RedisClient.create(RedisURI.create("localhost", 6379)).connect(), this.poolConfig);
        when(redisConnector.getPartitionRedisConnectionPool("dp")).thenReturn(new RedisConnectionPool(poolDp));
    }

    @After
    public void cleanup() {
        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.flushall();
    }

    @Test
    public void should_deleteGroupNode_whenExecute() {
        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"description\":\"\",\"dataPartitionId\":\"dp\",\"appIds\":[]}");
        EntityNode groupNode = EntityNode.builder().nodeId("users.x@dp.domain.com").name("users.x")
                .dataPartitionId("dp").type(NodeType.GROUP).build();

        Operation deleteGroupOperation = DeleteGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();
        deleteGroupOperation.execute();
        assertNull(commands.get("users.x@dp.domain.com"));
    }

    @Test
    public void should_createGroupNode_whenUndo() {
        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"description\":\"\",\"dataPartitionId\":\"dp\",\"appIds\":[]}");
        EntityNode groupNode = EntityNode.builder().nodeId("users.x@dp.domain.com").name("users.x")
                .dataPartitionId("dp").type(NodeType.GROUP).build();

        Operation deleteGroupOperation = DeleteGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();
        deleteGroupOperation.execute();
        assertNull(commands.get("users.x@dp.domain.com"));
        deleteGroupOperation.undo();
        assertEquals("{\"appIds\":[],\"name\":\"users.x\",\"description\":\"\",\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}", commands.get("users.x@dp.domain.com"));

    }

    @Test
    public void should_updateAppIdCache_whenDeletingGroupNode_andGroupHaveEmptyAppId_execute() {
        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"description\":\"\",\"dataPartitionId\":\"dp\",\"appIds\":[]}");
        EntityNode groupNode = EntityNode.builder().nodeId("users.x@dp.domain.com").name("users.x")
                .dataPartitionId("dp").type(NodeType.GROUP).build();
        commands.select(config.getPartitionAppIdDb());
        commands.sadd(DEFAULT_APPID_KEY, "users.x@dp.domain.com");

        Operation deleteGroupOperation = DeleteGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();
        deleteGroupOperation.execute();
        assertEquals(0, commands.smembers(DEFAULT_APPID_KEY).size());
    }

    @Test
    public void should_updateAppIdCache_whenDeletingGroupNode_andGroupHaveAppId_execute() {
        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"description\":\"\",\"dataPartitionId\":\"dp\",\"appIds\":[\"app1\", \"app2\"]}");
        EntityNode groupNode = EntityNode.builder().nodeId("users.x@dp.domain.com").name("users.x")
                .dataPartitionId("dp").appIds(new HashSet<>(Arrays.asList("app1", "app2"))).type(NodeType.GROUP).build();
        commands.select(config.getPartitionAppIdDb());
        commands.sadd("app1", "users.x@dp.domain.com");
        commands.sadd("app2", "users.x@dp.domain.com");

        Operation deleteGroupOperation = DeleteGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();
        deleteGroupOperation.execute();
        assertEquals(0, commands.smembers("app1").size());
        assertEquals(0, commands.smembers("app2").size());
    }

    @Test
    public void should_restoreAppIdCache_whenDeletingGroupNode_andGroupHaveEmptyAppId_undo() {
        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"description\":\"\",\"dataPartitionId\":\"dp\",\"appIds\":[]}");
        EntityNode groupNode = EntityNode.builder().nodeId("users.x@dp.domain.com").name("users.x")
                .dataPartitionId("dp").type(NodeType.GROUP).build();
        commands.select(config.getPartitionAppIdDb());
        commands.sadd(DEFAULT_APPID_KEY, "users.x@dp.domain.com");

        Operation deleteGroupOperation = DeleteGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();
        deleteGroupOperation.execute();
        assertEquals(0, commands.smembers(DEFAULT_APPID_KEY).size());
        deleteGroupOperation.undo();
        assertEquals(1, commands.smembers(DEFAULT_APPID_KEY).size());
    }

    @Test
    public void should_restoreAppIdCache_whenDeletingGroupNode_andGroupHaveAppId_undo() {
        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"description\":\"\",\"dataPartitionId\":\"dp\",\"appIds\":[\"app1\", \"app2\"]}");
        EntityNode groupNode = EntityNode.builder().nodeId("users.x@dp.domain.com").name("users.x")
                .dataPartitionId("dp").appIds(new HashSet<>(Arrays.asList("app1", "app2"))).type(NodeType.GROUP).build();
        commands.select(config.getPartitionAppIdDb());
        commands.sadd("app1", "users.x@dp.domain.com");
        commands.sadd("app2", "users.x@dp.domain.com");

        Operation deleteGroupOperation = DeleteGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();
        deleteGroupOperation.execute();
        assertEquals(0, commands.smembers("app1").size());
        assertEquals(0, commands.smembers("app2").size());
        deleteGroupOperation.undo();
        assertEquals(1, commands.smembers("app1").size());
        assertEquals(1, commands.smembers("app2").size());
    }
}
