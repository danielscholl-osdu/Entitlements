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
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation.CreateGroupOperationImpl;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CreateGroupOperationImplTests {

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
    public void should_createGroupNode_whenExecute() {
        EntityNode groupNode = EntityNode.builder().nodeId("users.x@dp.domain.com").name("users.x")
                .dataPartitionId("dp").type(NodeType.GROUP).build();

        Operation createGroupOperation = CreateGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();

        createGroupOperation.execute();

        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        assertEquals("{\"appIds\":[],\"name\":\"users.x\",\"description\":\"\",\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}", commands.get("users.x@dp.domain.com"));
        commands.select(config.getPartitionAppIdDb());
        Set<String> groupEmails = new HashSet<>();
        groupEmails.add("users.x@dp.domain.com");
        assertEquals(groupEmails, commands.smembers("no-app-id"));
    }

    @Test
    public void should_throw409_ifGroupNodeExist_whenExecute() {
        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"description\":\"\",\"dataPartitionId\":\"dp\",\"appIds\":[]}");
        EntityNode groupNode = EntityNode.builder().nodeId("users.x@dp.domain.com").name("users.x")
                .dataPartitionId("dp").type(NodeType.GROUP).build();

        Operation createGroupOperation = CreateGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();

        try {
            createGroupOperation.execute();
            fail("should throw exception");
        } catch (AppException ex) {
            assertEquals(409, ex.getError().getCode());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex));
        }
    }

    @Test
    public void should_deleteGroupNode_whenUndo() {
        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"description\":\"\",\"dataPartitionId\":\"dp\",\"appIds\":[]}");
        commands.select(config.getPartitionAppIdDb());
        commands.sadd("no-app-id", "users.x@dp.domain.com");
        EntityNode groupNode = EntityNode.builder().nodeId("users.x@dp.domain.com").name("users.x")
                .dataPartitionId("dp").type(NodeType.GROUP).build();

        Operation createGroupOperation = CreateGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();
        createGroupOperation.undo();

        commands.select(config.getPartitionEntityNodeDb());
        assertNull(commands.get("users.x@dp.domain.com"));
        commands.select(config.getPartitionAppIdDb());
        assertEquals(0, commands.smembers("no-app-id").size());
    }

    @Test
    public void should_deleteGroupNodeWithAppIds_whenUndo() {
        Set<String> appIds = new HashSet<>();
        appIds.add("app-id-1");
        appIds.add("app-id-2");

        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("users.x@dp.domain.com", "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"description\":\"\",\"dataPartitionId\":\"dp\",\"appIds\":[\"app-id-1\",\"app-id-2\"]}");
        commands.select(config.getPartitionAppIdDb());
        commands.sadd("app-id-1", "users.x@dp.domain.com");
        commands.sadd("app-id-2", "users.x@dp.domain.com");

        EntityNode groupNode = EntityNode.builder().nodeId("users.x@dp.domain.com").name("users.x")
                .dataPartitionId("dp").type(NodeType.GROUP).appIds(appIds).build();

        Operation createGroupOperation = CreateGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();
        createGroupOperation.undo();

        commands.select(config.getPartitionEntityNodeDb());
        assertNull(commands.get("users.x@dp.domain.com"));
        commands.select(config.getPartitionAppIdDb());
        assertEquals(0, commands.smembers("app-id-1").size());
        assertEquals(0, commands.smembers("app-id-2").size());

    }
}
