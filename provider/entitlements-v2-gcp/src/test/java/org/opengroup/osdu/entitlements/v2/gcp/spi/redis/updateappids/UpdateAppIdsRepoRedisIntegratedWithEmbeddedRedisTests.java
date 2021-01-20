package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.updateappids;

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
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class UpdateAppIdsRepoRedisIntegratedWithEmbeddedRedisTests {
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private RedisConnector redisConnector;
    @Autowired
    private GcpAppProperties config;
    @Autowired
    private GenericObjectPoolConfig poolConfig;

    @Autowired
    private UpdateAppIdsRepoRedis updateAppIdsRepoRedis;

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
    public void should_updateAppIds() {
        StatefulRedisConnection<String, String> connection = dpRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x@dp.domain.com", "{\"id\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\",\"description\":\"\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"appIds\":[]}");
        commands.select(config.getPartitionAppIdDb());
        commands.sadd("no-app-id", "data.x@dp.domain.com");
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").build();
        updateAppIdsRepoRedis.run(groupNode, new HashSet<>(Arrays.asList("app1", "app2")));

        commands.select(config.getPartitionEntityNodeDb());
        assertEquals("{\"appIds\":[\"app2\",\"app1\"],\"name\":\"data.x\",\"description\":\"\",\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}", commands.get("data.x@dp.domain.com"));
        commands.select(config.getPartitionAppIdDb());
        assertTrue(commands.smembers("no-app-id").isEmpty());
        assertTrue(commands.smembers("app1").contains("data.x@dp.domain.com"));
        assertTrue(commands.smembers("app2").contains("data.x@dp.domain.com"));
    }

}
