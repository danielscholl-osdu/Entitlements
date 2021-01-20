package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.redis.operation;

import io.github.resilience4j.retry.Retry;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation.NodeMetaDataUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class NodeMetaDataUpdateOperationImplTests {
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private RedisConnector redisConnector;
    @MockBean
    @Qualifier("dpPool")
    private GenericObjectPool<StatefulRedisConnection<String, String>> dpPool;
    @MockBean
    @Qualifier("centralPool")
    private GenericObjectPool<StatefulRedisConnection<String, String>> centralPool;
    @MockBean
    @Qualifier("dpConnection")
    private StatefulRedisConnection<String, String> dpConnection;
    @MockBean
    @Qualifier("commands")
    private RedisCommands<String, String> commands;
    @MockBean
    @Qualifier("result")
    private TransactionResult result;

    @Autowired
    private GcpAppProperties config;
    @Autowired
    private Retry retry;

    @Before
    public void setup() throws Exception {
        when(redisConnector.getPartitionRedisConnectionPool("dp")).thenReturn(new RedisConnectionPool(dpPool));
        when(dpPool.borrowObject()).thenReturn(dpConnection);
        when(dpConnection.sync()).thenReturn(commands);
        when(commands.exec()).thenReturn(result);
    }

    @Test
    public void should_updateAppId_whenExecute() throws Exception {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>(Collections.singletonList("app1"))).build();
        when(result.wasDiscarded()).thenReturn(false);

        NodeMetaDataUpdateOperation.execute();

        verify(commands).set("data.x@dp.domain.com", "{\"appIds\":[\"app1\"],\"name\":\"data.x\",\"description\":\"\",\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}");
        verify(commands).srem("no-app-id", "data.x@dp.domain.com");
        verify(commands).sadd("app1", "data.x@dp.domain.com");
    }

    @Test
    public void should_updateAppId_whenExecute_andRetrySucceed_failedOneTime() throws Exception {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>(Collections.singletonList("app1"))).build();
        when(result.wasDiscarded()).thenReturn(true).thenReturn(false);

        NodeMetaDataUpdateOperation.execute();

        verify(commands, times(2)).set("data.x@dp.domain.com", "{\"appIds\":[\"app1\"],\"name\":\"data.x\",\"description\":\"\",\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}");
        verify(commands, times(2)).srem("no-app-id", "data.x@dp.domain.com");
        verify(commands, times(2)).sadd("app1", "data.x@dp.domain.com");

    }

    @Test
    public void should_throw423_whenExecute_andRetryFailed_failed3Times() {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>(Collections.singletonList("app1"))).build();
        when(result.wasDiscarded()).thenReturn(true);

        try {
            NodeMetaDataUpdateOperation.execute();
            fail("should throw exception");
        } catch (AppException ex) {
            verify(commands, times(3)).set("data.x@dp.domain.com", "{\"appIds\":[\"app1\"],\"name\":\"data.x\",\"description\":\"\",\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}");
            verify(commands, times(3)).srem("no-app-id", "data.x@dp.domain.com");
            verify(commands, times(3)).sadd("app1", "data.x@dp.domain.com");
            assertThat(ex.getError().getCode()).isEqualTo(423);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex));
        }
    }

    @Test
    public void should_throw400_ifGivenNodeIsNotAGroup_whenExecute() {
        EntityNode groupNode = EntityNode.builder().nodeId("user@xxx.com").name("user@xxx.com").type(NodeType.USER).dataPartitionId("dp").build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>(Collections.singletonList("app1"))).build();
        when(result.wasDiscarded()).thenReturn(false);

        try {
            NodeMetaDataUpdateOperation.execute();
            fail("should throw exception");
        } catch (AppException ex) {
            verify(commands, never()).set(any(), any());
            assertThat(ex.getError().getCode()).isEqualTo(400);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex));
        }
    }

    @Test
    public void should_revertAppIds_whenUndo() throws Exception {
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>(Collections.singletonList("app1"))).build();
        when(result.wasDiscarded()).thenReturn(false).thenReturn(false);

        NodeMetaDataUpdateOperation.execute();
        NodeMetaDataUpdateOperation.undo();

        verify(commands, times(2)).exec();
    }

    @Test
    public void should_revertAppIds_whenUndo_andRetrySucceed_failedOneTime() throws Exception {
        when(commands.get("data.x@dp.domain.com")).thenReturn("{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"appIds\":[]}");
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>(Collections.singletonList("app1"))).build();
        when(result.wasDiscarded()).thenReturn(false).thenReturn(true).thenReturn(false);

        NodeMetaDataUpdateOperation.execute();
        NodeMetaDataUpdateOperation.undo();

        verify(commands, times(3)).exec();
    }

    @Test
    public void should_throw423_whenUndo_andRetryFailed_failed() throws Exception {
        when(commands.get("data.x@dp.domain.com")).thenReturn("{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"appIds\":[]}");
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").type(NodeType.GROUP).dataPartitionId("dp").build();
        Operation NodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry).log(log).config(config)
                .groupNode(groupNode).appIds(new HashSet<>(Collections.singletonList("app1"))).build();
        when(result.wasDiscarded()).thenReturn(false).thenReturn(true);

        NodeMetaDataUpdateOperation.execute();

        try {
            NodeMetaDataUpdateOperation.undo();
            fail("should throw exception");
        } catch (AppException ex) {
            assertThat(ex.getError().getCode()).isEqualTo(423);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex));
        }
    }
}
