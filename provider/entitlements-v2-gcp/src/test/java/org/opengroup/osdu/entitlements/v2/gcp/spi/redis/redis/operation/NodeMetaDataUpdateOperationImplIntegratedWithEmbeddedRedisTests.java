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
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation.NodeMetaDataUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class NodeMetaDataUpdateOperationImplIntegratedWithEmbeddedRedisTests {

    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private RedisConnector redisConnector;
    @Autowired
    private GenericObjectPoolConfig poolConfig;
    @Autowired
    private Retry retry;
    @Autowired
    private GcpAppProperties config;
    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    private static RedisServer partitionRedisServer;
    private static RedisClient partitionRedisClient;

    @BeforeClass
    public static void setupClass() throws IOException {
        partitionRedisServer = new RedisServer(6379);
        partitionRedisServer.start();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        partitionRedisClient = RedisClient.create(uri);
    }

    @AfterClass
    public static void end() {
        partitionRedisServer.stop();
    }

    @Before
    public void setup() {
        GenericObjectPool<StatefulRedisConnection<String, String>> poolDp = ConnectionPoolSupport.createGenericObjectPool(
                () -> RedisClient.create(RedisURI.create("localhost", 6379)).connect(), this.poolConfig);
        when(redisConnector.getPartitionRedisConnectionPool("dp")).thenReturn(new RedisConnectionPool(poolDp));
    }

    @After
    public void cleanup() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.flushall();
    }

    @Test
    public void should_updateAppIds_whenExecute_nonEmptyAppIdSet() throws Exception {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x@dp.domain.com", "{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"appIds\":[]}");
        commands.select(config.getPartitionAppIdDb());
        commands.sadd("no-app-id", "data.x@dp.domain.com");
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>(Arrays.asList("app1", "app2"))).build();

        NodeMetaDataUpdateOperation.execute();

        commands.select(config.getPartitionEntityNodeDb());
        assertThat(commands.get("data.x@dp.domain.com")).isEqualTo("{\"appIds\":[\"app2\",\"app1\"],\"name\":\"data.x\",\"description\":\"\",\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionAppIdDb());
        assertThat(commands.smembers("no-app-id")).isEmpty();
        assertThat(commands.smembers("app1")).contains("data.x@dp.domain.com");
        assertThat(commands.smembers("app2")).contains("data.x@dp.domain.com");
    }

    @Test
    public void should_updateAppIds_whenExecute_emptyAppIdSet() throws Exception {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x@dp.domain.com", "{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"appIds\":[\"app1\",\"app2\"]}");
        commands.select(config.getPartitionAppIdDb());
        commands.sadd("app1", "data.x@dp.domain.com");
        commands.sadd("app2", "data.x@dp.domain.com");
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").appIds(new HashSet<>(Arrays.asList("app1", "app2"))).build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>()).build();

        NodeMetaDataUpdateOperation.execute();

        commands.select(config.getPartitionEntityNodeDb());
        assertThat(commands.get("data.x@dp.domain.com")).isEqualTo("{\"appIds\":[],\"name\":\"data.x\",\"description\":\"\",\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionAppIdDb());
        assertThat(commands.smembers("no-app-id")).contains("data.x@dp.domain.com");
        assertThat(commands.smembers("app1")).isEmpty();
        assertThat(commands.smembers("app2")).isEmpty();
    }

    @Test
    public void should_revertAppIds_whenUndo_emptyAppIdSet() throws Exception {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x@dp.domain.com", "{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"appIds\":[]}");
        commands.select(config.getPartitionAppIdDb());
        commands.sadd("no-app-id", "data.x@dp.domain.com");
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>(Arrays.asList("app1", "app2"))).build();

        NodeMetaDataUpdateOperation.execute();
        NodeMetaDataUpdateOperation.undo();

        commands.select(config.getPartitionEntityNodeDb());
        assertThat(commands.get("data.x@dp.domain.com")).isEqualTo("{\"appIds\":[],\"name\":\"data.x\",\"description\":\"\",\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionAppIdDb());
        assertThat(commands.smembers("no-app-id")).contains("data.x@dp.domain.com");
        assertThat(commands.smembers("app1")).isEmpty();
        assertThat(commands.smembers("app2")).isEmpty();
    }

    @Test
    public void should_revertAppIds_whenUndo_nonEmptyAppIdSet() throws Exception {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x@dp.domain.com", "{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"appIds\":[\"app1\",\"app2\"]}");
        commands.select(config.getPartitionAppIdDb());
        commands.sadd("app1", "data.x@dp.domain.com");
        commands.sadd("app2", "data.x@dp.domain.com");
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").appIds(new HashSet<>(Arrays.asList("app1", "app2"))).build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>()).build();

        NodeMetaDataUpdateOperation.execute();
        NodeMetaDataUpdateOperation.undo();

        commands.select(config.getPartitionEntityNodeDb());
        assertThat(commands.get("data.x@dp.domain.com")).isEqualTo("{\"appIds\":[\"app2\",\"app1\"],\"name\":\"data.x\",\"description\":\"\",\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionAppIdDb());
        assertThat(commands.smembers("no-app-id")).isEmpty();
        assertThat(commands.smembers("app1")).contains("data.x@dp.domain.com");
        assertThat(commands.smembers("app2")).contains("data.x@dp.domain.com");
    }
}
