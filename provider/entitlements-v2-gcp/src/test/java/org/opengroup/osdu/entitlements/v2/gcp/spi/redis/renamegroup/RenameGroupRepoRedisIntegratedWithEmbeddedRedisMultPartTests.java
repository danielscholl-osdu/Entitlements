package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.renamegroup;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.service.PartitionRedisInstanceService;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.spi.renamegroup.RenameGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RenameGroupRepoRedisIntegratedWithEmbeddedRedisMultPartTests {

    private static RedisServer centralRedisServer;
    private static RedisClient centralRedisClient;
    private static RedisServer partition1RedisServer;
    private static RedisClient partition1RedisClient;
    private static RedisServer partition2RedisServer;
    private static RedisClient partition2RedisClient;
    private static RedisServer partition3RedisServer;
    private static RedisClient partition3RedisClient;

    @MockBean
    private PartitionRedisInstanceService partitionRedisInstanceService;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private RedisConnector redisConnector;
    @MockBean
    private WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;
    @MockBean
    private RequestInfoUtilService requestInfoUtilService;

    @Autowired
    private GcpAppProperties config;

    @Autowired
    private GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig;

    @Autowired
    private RenameGroupRepo renameGroupRepo;

    @BeforeClass
    public static void setupClass() throws IOException {
        centralRedisServer = new RedisServer(7000);
        centralRedisServer.start();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(7000).build();
        centralRedisClient = RedisClient.create(uri);

        partition1RedisServer = new RedisServer(6377);
        partition1RedisServer.start();
        uri = RedisURI.builder().withHost("localhost").withPort(6377).build();
        partition1RedisClient = RedisClient.create(uri);

        partition2RedisServer = new RedisServer(6378);
        partition2RedisServer.start();
        uri = RedisURI.builder().withHost("localhost").withPort(6378).build();
        partition2RedisClient = RedisClient.create(uri);

        partition3RedisServer = new RedisServer(6379);
        partition3RedisServer.start();
        uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        partition3RedisClient = RedisClient.create(uri);
    }

    @AfterClass
    public static void end() {
        centralRedisServer.stop();
        partition1RedisServer.stop();
        partition2RedisServer.stop();
        partition3RedisServer.stop();
    }

    @Before
    public void setup() {
        Mockito.when(requestInfoUtilService.getUserId(any())).thenReturn("callerdesid");
        GenericObjectPool<StatefulRedisConnection<String, String>> poolDp1 = ConnectionPoolSupport.createGenericObjectPool(
                () -> RedisClient.create(RedisURI.create("localhost", 6377)).connect(), poolConfig);
        when(redisConnector.getPartitionRedisConnectionPool("dp1")).thenReturn(new RedisConnectionPool(poolDp1));
        GenericObjectPool<StatefulRedisConnection<String, String>> poolDp2 = ConnectionPoolSupport.createGenericObjectPool(
                () -> RedisClient.create(RedisURI.create("localhost", 6378)).connect(), poolConfig);
        when(redisConnector.getPartitionRedisConnectionPool("dp2")).thenReturn(new RedisConnectionPool(poolDp2));
        GenericObjectPool<StatefulRedisConnection<String, String>> poolDp3 = ConnectionPoolSupport.createGenericObjectPool(
                () -> RedisClient.create(RedisURI.create("localhost", 6379)).connect(), poolConfig);
        when(redisConnector.getPartitionRedisConnectionPool("dp3")).thenReturn(new RedisConnectionPool(poolDp3));
        Mockito.when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition("dp1")).thenReturn("localhost");
        Mockito.when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition("dp2")).thenReturn("localhost");
        Mockito.when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition("dp3")).thenReturn("localhost");
    }

    @After
    public void cleanup() {
        StatefulRedisConnection<String, String> connection = centralRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.flushall();
        connection = partition1RedisClient.connect();
        commands = connection.sync();
        commands.flushall();
        connection = partition2RedisClient.connect();
        commands = connection.sync();
        commands.flushall();
        connection = partition3RedisClient.connect();
        commands = connection.sync();
        commands.flushall();
    }

    @Test
    public void shouldRenameGroupSuccessfully() {
        final String newGroupName = "users.y";
        final String newGroupId = "users.y@dp1.domain.com";
        final String initialGroupId = "users.x@dp1.domain.com";

        final String parent1Id = "data.x@dp1.domain.com";
        final String parent2Id = "data.y@dp1.domain.com";
        final String parent3Id = "data.z@dp2.domain.com";

        final String childOwnerId = "users.owner@dp1.domain.com";
        final String childMemberId = "users.member@dp3.domain.com";
        final String childGroupId = "data.w@dp1.domain.com";

        final String parent1 = "{\"nodeId\":\"data.x@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\",\"dataPartitionId\":\"dp1\"}";
        final String parent2 = "{\"nodeId\":\"data.y@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"data.y\",\"dataPartitionId\":\"dp1\"}";
        final String parent3 = "{\"nodeId\":\"data.z@dp2.domain.com\",\"type\":\"GROUP\",\"name\":\"data.z\",\"dataPartitionId\":\"dp2\"}";
        final String childOwner = "{\"nodeId\":\"users.owner@dp1.domain.com\",\"type\":\"USER\",\"name\":\"users.owner\",\"dataPartitionId\":\"dp1\"}";
        final String childMember = "{\"nodeId\":\"users.member@dp3.domain.com\",\"type\":\"USER\",\"name\":\"users.member\",\"dataPartitionId\":\"dp3\"}";
        final String childGroup = "{\"nodeId\":\"data.w@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"data.w\",\"dataPartitionId\":\"dp1\"}";
        final String initialGroup = "{\"nodeId\":\"users.x@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"dataPartitionId\":\"dp1\"}";
        final String rootUser = "{\"nodeId\":\"users.data.root@dp1.domain.com\",\"type\":\"GROUP\",\"name\":\"users.data.root\",\"dataPartitionId\":\"dp1\"}";

        final String parent1AsParentRef = "{\"name\":\"data.x\",\"description\":\"\",\"id\":\"data.x@dp1.domain.com\",\"dataPartitionId\":\"dp1\"}";
        final String parent2AsParentRef = "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp1.domain.com\",\"dataPartitionId\":\"dp1\"}";
        final String parent3AsParentRef = "{\"name\":\"data.z\",\"description\":\"\",\"id\":\"data.z@dp2.domain.com\",\"dataPartitionId\":\"dp2\"}";
        final String initialGroupAsParentRef = "{\"name\":\"users.x\",\"description\":\"some description\",\"id\":\"users.x@dp1.domain.com\",\"dataPartitionId\":\"dp1\"}";

        final String childOwnerAsChildRef = "{\"role\":\"OWNER\",\"id\":\"users.owner@dp1.domain.com\",\"dataPartitionId\":\"dp1\",\"type\":\"USER\"}";
        final String childMemberAsChildRef = "{\"role\":\"MEMBER\",\"id\":\"users.member@dp3.domain.com\",\"dataPartitionId\":\"dp3\",\"type\":\"USER\"}";
        final String childGroupAsChildRef = "{\"role\":\"MEMBER\",\"id\":\"data.w@dp1.domain.com\",\"dataPartitionId\":\"dp1\",\"type\":\"GROUP\"}";
        final String initialGroupAsChildRef = "{\"role\":\"MEMBER\",\"id\":\"users.x@dp1.domain.com\",\"dataPartitionId\":\"dp1\",\"type\":\"GROUP\"}";

        final String newGroup = "{\"appIds\":[],\"name\":\"users.y\",\"description\":\"some description\",\"nodeId\":\"users.y@dp1.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp1\"}";
        final String newGroupAsChildRef = "{\"role\":\"MEMBER\",\"id\":\"users.y@dp1.domain.com\",\"dataPartitionId\":\"dp1\",\"type\":\"GROUP\"}";
        final String newGroupAsParentRef = "{\"name\":\"users.y\",\"description\":\"some description\",\"id\":\"users.y@dp1.domain.com\",\"dataPartitionId\":\"dp1\"}";

        final String rootUserId = "users.data.root@dp1.domain.com";

        StatefulRedisConnection<String, String> connectionOfDP1 = partition1RedisClient.connect();
        RedisCommands<String, String> commandsOfDP1 = connectionOfDP1.sync();
        commandsOfDP1.select(config.getPartitionAppIdDb());
        commandsOfDP1.sadd(GcpAppProperties.DEFAULT_APPID_KEY, initialGroupId);
        commandsOfDP1.select(config.getPartitionEntityNodeDb());
        commandsOfDP1.set(parent1Id, parent1);
        commandsOfDP1.set(parent2Id, parent2);
        commandsOfDP1.set(initialGroupId, initialGroup);
        commandsOfDP1.set(childOwnerId, childOwner);
        commandsOfDP1.set(childGroupId, childGroup);
        commandsOfDP1.set(rootUserId, rootUser);

        commandsOfDP1.select(config.getPartitionParentRefDb());
        commandsOfDP1.sadd(initialGroupId, parent1AsParentRef, parent2AsParentRef, parent3AsParentRef);
        commandsOfDP1.sadd(childOwnerId, initialGroupAsParentRef);
        commandsOfDP1.sadd(childGroupId, initialGroupAsParentRef);

        commandsOfDP1.select(config.getPartitionChildrenRefDb());
        commandsOfDP1.sadd(parent1Id, initialGroupAsChildRef);
        commandsOfDP1.sadd(parent2Id, initialGroupAsChildRef);
        commandsOfDP1.sadd(initialGroupId, childOwnerAsChildRef, childMemberAsChildRef, childGroupAsChildRef);

        StatefulRedisConnection<String, String> connectionOfDP2 = partition2RedisClient.connect();
        RedisCommands<String, String> commandsOfDP2 = connectionOfDP2.sync();
        commandsOfDP2.select(config.getPartitionEntityNodeDb());
        commandsOfDP2.set(parent3Id, parent3);

        commandsOfDP2.select(config.getPartitionChildrenRefDb());
        commandsOfDP2.sadd(parent3Id, initialGroupAsChildRef);

        StatefulRedisConnection<String, String> connectionOfDP3 = partition3RedisClient.connect();
        RedisCommands<String, String> commandsOfDP3 = connectionOfDP3.sync();
        commandsOfDP3.select(config.getPartitionEntityNodeDb());
        commandsOfDP3.set(childMemberId, childMember);

        commandsOfDP3.select(config.getPartitionParentRefDb());
        commandsOfDP3.sadd(childMemberId, initialGroupAsParentRef);

        final EntityNode initialGroupNode = EntityNode.builder()
                .nodeId("users.x@dp1.domain.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp1")
                .description("some description")
                .build();
        Set<String> impactedUsers = renameGroupRepo.run(initialGroupNode, newGroupName);

        Assert.assertEquals(new HashSet<>(Arrays.asList("users.owner@dp1.domain.com",
                "users.member@dp3.domain.com")), impactedUsers);

        commandsOfDP1 = connectionOfDP1.sync();

        commandsOfDP1.select(config.getPartitionEntityNodeDb());
        Assert.assertNull(commandsOfDP1.get(initialGroupId));
        Assert.assertEquals(newGroup, commandsOfDP1.get(newGroupId));

        commandsOfDP1.select(config.getPartitionChildrenRefDb());
        commandsOfDP2.select(config.getPartitionChildrenRefDb());
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsChildRef)), commandsOfDP1.smembers(parent1Id));
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsChildRef)), commandsOfDP1.smembers(parent2Id));
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsChildRef)), commandsOfDP2.smembers(parent3Id));

        commandsOfDP1.select(config.getPartitionParentRefDb());
        Assert.assertEquals(0, commandsOfDP1.smembers(initialGroupId).size());
        Assert.assertEquals(new HashSet<>(Arrays.asList(parent1AsParentRef, parent2AsParentRef, parent3AsParentRef)), commandsOfDP1.smembers(newGroupId));

        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsParentRef)), commandsOfDP3.smembers(childMemberId));
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsParentRef)), commandsOfDP1.smembers(childOwnerId));
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsParentRef)), commandsOfDP1.smembers(childGroupId));

        commandsOfDP1.select(config.getPartitionAppIdDb());
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupId)), commandsOfDP1.smembers(GcpAppProperties.DEFAULT_APPID_KEY));

        Mockito.verify(auditLogger).updateGroup(AuditStatus.SUCCESS, initialGroupId);
    }
}
