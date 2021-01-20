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
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation.RemoveMemberChildUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RemoveMemberChildUpdateOperationImplTests {
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private RedisConnector redisConnector;
    @MockBean
    private GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;
    @MockBean
    private StatefulRedisConnection<String, String> connection;
    @MockBean
    private RedisCommands<String, String> commands;
    @MockBean
    private TransactionResult result;
    @Autowired
    private GcpAppProperties config;
    @Autowired
    private Retry retry;

    @Before
    public void setup() throws Exception {
        when(redisConnector.getPartitionRedisConnectionPool("dp")).thenReturn(new RedisConnectionPool(connectionPool));
        when(connectionPool.borrowObject()).thenReturn(connection);
        when(connection.sync()).thenReturn(commands);
        when(commands.exec()).thenReturn(result);
        when(commands.get("data.x@dp.domain.com")).thenReturn("{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"parents\":[]," +
                "\"children\":[{\"id\":\"member@xxx.com\",\"name\":\"member@xxx.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\",\"role\":\"MEMBER\",\"count\":1,\"direct\":true}]," +
                "\"parentsNodes\":[],\"childrenNodes\":[]}");

    }

    @Test
    public void should_succeed_whenExecute() throws Exception {
        when(commands.get("member@xxx.com")).thenReturn("{\"nodeId\":\"member@xxx.com\",\"type\":\"USER\",\"name\":\"member\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"parents\":[{\"id\":\"data.x@dp.domain.com\",\"name\":\"data.x\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\",\"role\":\"OWNER\",\"count\":1,\"direct\":true}]," +
                "\"children\":[],\"parentsNodes\":[],\"childrenNodes\":[]}");
        EntityNode memberNode = EntityNode.builder().nodeId("member@xxx.com").type(NodeType.USER).name("member")
                .dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").dataPartitionId("dp").type(NodeType.GROUP).build();

        Operation removeMemberChildUpdateOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).memberId(memberNode.getNodeId()).memberPartitionId(memberNode.getDataPartitionId()).build();
        when(result.wasDiscarded()).thenReturn(false);

        removeMemberChildUpdateOperation.execute();

        verify(commands, times(1)).exec();
    }

    @Test
    public void should_succeed_whenExecute_andRetrySucceed() throws Exception {
        when(commands.get("member@xxx.com")).thenReturn("{\"nodeId\":\"member@xxx.com\",\"type\":\"USER\",\"name\":\"member\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"parents\":[{\"id\":\"data.x@dp.domain.com\",\"name\":\"data.x\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\",\"role\":\"OWNER\",\"count\":1,\"direct\":true}]," +
                "\"children\":[],\"parentsNodes\":[],\"childrenNodes\":[]}");
        EntityNode memberNode = EntityNode.builder().nodeId("member@xxx.com").type(NodeType.USER).name("member")
                .dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").dataPartitionId("dp").type(NodeType.GROUP).build();

        Operation removeMemberChildUpdateOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).memberId(memberNode.getNodeId()).memberPartitionId(memberNode.getDataPartitionId()).build();
        when(result.wasDiscarded()).thenReturn(true).thenReturn(true).thenReturn(false);

        removeMemberChildUpdateOperation.execute();

        verify(commands, times(3)).exec();
    }

    @Test
    public void should_throw423_whenExecute_andRetryFailed() {
        when(commands.get("member@xxx.com")).thenReturn("{\"nodeId\":\"member@xxx.com\",\"type\":\"USER\",\"name\":\"member@xxx.com\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"parents\":[{\"id\":\"data.x@dp.domain.com\",\"name\":\"data.x\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\",\"role\":\"OWNER\",\"count\":1,\"direct\":true}]," +
                "\"children\":[],\"parentsNodes\":[],\"childrenNodes\":[]}");
        EntityNode memberNode = EntityNode.builder().nodeId("member@xxx.com").type(NodeType.USER).name("member")
                .dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").dataPartitionId("dp").type(NodeType.GROUP).build();
        Operation removeMemberChildUpdateOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).memberId(memberNode.getNodeId()).memberPartitionId(memberNode.getDataPartitionId()).build();
        when(result.wasDiscarded()).thenReturn(true);

        try {
            removeMemberChildUpdateOperation.execute();
            fail("should throw exception");
        } catch (AppException ex) {
            assertThat(ex.getError().getCode()).isEqualTo(423);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex));
        }
    }

    @Test
    public void should_succeed_whenUndo() throws Exception {
        when(commands.get("member@xxx.com")).thenReturn("{\"nodeId\":\"member@xxx.com\",\"type\":\"USER\",\"name\":\"member\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"parents\":[{\"id\":\"data.x@dp.domain.com\",\"name\":\"data.x\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\",\"role\":\"OWNER\",\"count\":1,\"direct\":true}," +
                "{\"id\":\"data.y@dp.domain.com\",\"name\":\"data.y\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\",\"role\":\"OWNER\",\"count\":1,\"direct\":true}]," +
                "\"children\":[],\"parentsNodes\":[],\"childrenNodes\":[]}");
        EntityNode memberNode = EntityNode.builder().nodeId("member@xxx.com").type(NodeType.USER).name("member")
                .dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").dataPartitionId("dp").type(NodeType.GROUP).build();
        Operation removeMemberChildUpdateOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).memberId(memberNode.getNodeId()).memberPartitionId(memberNode.getDataPartitionId()).build();
        when(result.wasDiscarded()).thenReturn(false);

        removeMemberChildUpdateOperation.execute();
        removeMemberChildUpdateOperation.undo();

        verify(commands, times(2)).exec();
    }

    @Test
    public void should_succeed_whenUndo_andRetrySucceed() throws Exception {
        when(commands.get("member@xxx.com")).thenReturn("{\"nodeId\":\"member@xxx.com\",\"type\":\"USER\",\"name\":\"member\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"parents\":[{\"id\":\"data.x@dp.domain.com\",\"name\":\"data.x\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\",\"role\":\"OWNER\",\"count\":1,\"direct\":true}," +
                "{\"id\":\"data.y@dp.domain.com\",\"name\":\"data.y\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\",\"role\":\"OWNER\",\"count\":1,\"direct\":true}]," +
                "\"children\":[],\"parentsNodes\":[],\"childrenNodes\":[]}");
        EntityNode memberNode = EntityNode.builder().nodeId("member@xxx.com").type(NodeType.USER).name("member")
                .dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").dataPartitionId("dp").type(NodeType.GROUP).build();
        Operation removeMemberChildUpdateOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).memberId(memberNode.getNodeId()).memberPartitionId(memberNode.getDataPartitionId()).build();
        when(result.wasDiscarded()).thenReturn(false).thenReturn(true).thenReturn(true).thenReturn(false);

        removeMemberChildUpdateOperation.execute();
        removeMemberChildUpdateOperation.undo();

        verify(commands, times(4)).exec();
    }

    @Test
    public void should_throw423_whenUndo_andRetryFailed() throws Exception {
        when(commands.get("member@xxx.com")).thenReturn("{\"nodeId\":\"member@xxx.com\",\"type\":\"USER\",\"name\":\"member@xxx.com\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"parents\":[{\"id\":\"data.x@dp.domain.com\",\"name\":\"data.x\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\",\"role\":\"OWNER\",\"count\":1,\"direct\":true}," +
                "{\"id\":\"data.y@dp.domain.com\",\"name\":\"data.y\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\",\"role\":\"OWNER\",\"count\":1,\"direct\":true}]," +
                "\"children\":[],\"parentsNodes\":[],\"childrenNodes\":[]}");
        EntityNode memberNode = EntityNode.builder().nodeId("member@xxx.com").type(NodeType.USER).name("member")
                .dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x@dp.domain.com").name("data.x").dataPartitionId("dp").type(NodeType.GROUP).build();
        Operation removeMemberChildUpdateOperation = RemoveMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).memberId(memberNode.getNodeId()).memberPartitionId(memberNode.getDataPartitionId()).build();

        when(result.wasDiscarded()).thenReturn(false).thenReturn(true);
        removeMemberChildUpdateOperation.execute();

        try {
            removeMemberChildUpdateOperation.undo();
            fail("should throw exception");
        } catch (AppException ex) {
            assertThat(ex.getError().getCode()).isEqualTo(423);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex));
        }
    }
}