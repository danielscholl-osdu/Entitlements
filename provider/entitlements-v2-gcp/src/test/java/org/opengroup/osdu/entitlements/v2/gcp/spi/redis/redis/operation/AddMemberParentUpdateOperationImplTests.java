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
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation.AddMemberParentUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
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
public class AddMemberParentUpdateOperationImplTests {
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
        when(commands.get("data.x.viewers@dp.domain.com")).thenReturn("{\"id\":\"data.x.viewers@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x.viewers\"," +
                "\"dataPartitionId\":\"dp\"," +
                "\"parents\":[],\"children\":[],\"parentsNodes\":[],\"childrenNodes\":[]}");
    }

    @Test
    public void should_succeed_whenExecute() throws Exception {
        EntityNode memberNode = EntityNode.builder().nodeId("users.x@dp.domain.com").type(NodeType.GROUP)
                .name("users.x").dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@dp.domain.com").type(NodeType.GROUP)
                .name("data.x.viewers").dataPartitionId("dp").build();
        Operation addMemberParentUpdateOperation = AddMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).childrenReference(ChildrenReference.createChildrenReference(memberNode, Role.MEMBER)).build();
        when(result.wasDiscarded()).thenReturn(false);

        addMemberParentUpdateOperation.execute();

        verify(commands, times(1)).exec();
    }

    @Test
    public void should_succeed_whenExecute_andRetrySucceed() throws Exception {
        EntityNode memberNode = EntityNode.builder().nodeId("users.x@dp.domain.com").type(NodeType.GROUP)
                .name("users.x").dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@dp.domain.com").type(NodeType.GROUP)
                .name("data.x.viewers").dataPartitionId("dp").build();
        Operation addMemberParentUpdateOperation = AddMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).childrenReference(ChildrenReference.createChildrenReference(memberNode, Role.MEMBER)).build();
        when(result.wasDiscarded()).thenReturn(true).thenReturn(true).thenReturn(false);

        addMemberParentUpdateOperation.execute();

        verify(commands, times(3)).exec();
    }

    @Test
    public void should_throw423_whenExecute_andRetryFailed() {
        EntityNode memberNode = EntityNode.builder().nodeId("users.x@dp.domain.com").type(NodeType.GROUP)
                .name("users.x").dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@dp.domain.com").type(NodeType.GROUP)
                .name("data.x.viewers").dataPartitionId("dp").build();
        Operation addMemberParentUpdateOperation = AddMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).childrenReference(ChildrenReference.createChildrenReference(memberNode, Role.MEMBER)).build();
        when(result.wasDiscarded()).thenReturn(true);

        try {
            addMemberParentUpdateOperation.execute();
            fail("should throw exception");
        } catch (AppException ex) {
            assertThat(ex.getError().getCode()).isEqualTo(423);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex));
        }
    }

    @Test
    public void should_succeed_whenUndo() throws Exception {
        EntityNode memberNode = EntityNode.builder().nodeId("users.x@dp.domain.com").type(NodeType.GROUP)
                .name("users.x").dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@dp.domain.com").type(NodeType.GROUP)
                .name("data.x.viewers").dataPartitionId("dp").build();
        Operation addMemberParentUpdateOperation = AddMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).childrenReference(ChildrenReference.createChildrenReference(memberNode, Role.MEMBER)).build();
        when(result.wasDiscarded()).thenReturn(false).thenReturn(false);

        addMemberParentUpdateOperation.execute();
        addMemberParentUpdateOperation.undo();

        verify(commands, times(2)).exec();
    }

    @Test
    public void should_succeed_whenUndo_andRetrySucceed() throws Exception {
        EntityNode memberNode = EntityNode.builder().nodeId("users.x@dp.domain.com").type(NodeType.GROUP)
                .name("users.x").dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@dp.domain.com").type(NodeType.GROUP)
                .name("data.x.viewers").dataPartitionId("dp").build();
        Operation addMemberParentUpdateOperation = AddMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).childrenReference(ChildrenReference.createChildrenReference(memberNode, Role.MEMBER)).build();
        when(result.wasDiscarded()).thenReturn(false).thenReturn(true).thenReturn(true).thenReturn(false);

        addMemberParentUpdateOperation.execute();
        addMemberParentUpdateOperation.undo();

        verify(commands, times(4)).exec();
    }

    @Test
    public void should_throw423_whenUndo_andRetryFailed() throws Exception {
        EntityNode memberNode = EntityNode.builder().nodeId("users.x@dp.domain.com").type(NodeType.GROUP)
                .name("users.x").dataPartitionId("dp").build();
        EntityNode groupNode = EntityNode.builder().nodeId("data.x.viewers@dp.domain.com").type(NodeType.GROUP)
                .name("data.x.viewers").dataPartitionId("dp").build();
        Operation addMemberParentUpdateOperation = AddMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupNode).childrenReference(ChildrenReference.createChildrenReference(memberNode, Role.MEMBER)).build();
        when(result.wasDiscarded()).thenReturn(false).thenReturn(true);
        addMemberParentUpdateOperation.execute();

        try {
            addMemberParentUpdateOperation.undo();
            fail("should throw exception");
        } catch (AppException ex) {
            assertThat(ex.getError().getCode()).isEqualTo(423);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex));
        }
    }
}
