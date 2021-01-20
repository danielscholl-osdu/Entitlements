package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.renamegroup;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
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

@SpringBootTest
@RunWith(SpringRunner.class)
public class RenameGroupRepoRedisIntegratedWithEmbeddedRedisTests {

    private static RedisServer centralRedisServer;
    private static RedisClient centralRedisClient;
    private static RedisServer partitionRedisServer;
    private static RedisClient partitionRedisClient;

    @MockBean
    private PartitionRedisInstanceService partitionRedisInstanceService;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;
    @MockBean
    private RequestInfoUtilService requestInfoUtilService;

    @Autowired
    private GcpAppProperties config;

    @Autowired
    private RenameGroupRepo renameGroupRepo;

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
        Mockito.when(requestInfoUtilService.getUserId(any())).thenReturn("callerdesid");
        Mockito.when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition("dp")).thenReturn("localhost");
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
    public void shouldRenameGroupSuccessfully() {
        StatefulRedisConnection<String, String> connection = partitionRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();

        final String newGroupName = "users.y";
        final String newGroupId = "users.y@dp.domain.com";
        final String initialGroupId = "users.x@dp.domain.com";

        final String parent1Id = "data.x@dp.domain.com";
        final String parent2Id = "data.y@dp.domain.com";
        final String parent3Id = "data.z@dp.domain.com";

        final String childOwnerId = "users.owner@dp.domain.com";
        final String childMemberId = "users.member@dp.domain.com";
        final String childGroupId = "data.w@dp.domain.com";

        final String parent1 = "{\"nodeId\":\"data.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.x\",\"dataPartitionId\":\"dp\"}";
        final String parent2 = "{\"nodeId\":\"data.y@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.y\",\"dataPartitionId\":\"dp\"}";
        final String parent3 = "{\"nodeId\":\"data.z@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.z\",\"dataPartitionId\":\"dp\"}";
        final String childOwner = "{\"nodeId\":\"users.owner@dp.domain.com\",\"type\":\"USER\",\"name\":\"users.owner\",\"dataPartitionId\":\"dp\"}";
        final String childMember = "{\"nodeId\":\"users.member@dp.domain.com\",\"type\":\"USER\",\"name\":\"users.member\",\"dataPartitionId\":\"dp\"}";
        final String childGroup = "{\"nodeId\":\"data.w@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"data.w\",\"dataPartitionId\":\"dp\"}";
        final String initialGroup = "{\"nodeId\":\"users.x@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.x\",\"dataPartitionId\":\"dp\"}";
        final String rootUser = "{\"nodeId\":\"users.data.root@dp.domain.com\",\"type\":\"GROUP\",\"name\":\"users.data.root\",\"dataPartitionId\":\"dp\"}";

        final String parent1AsParentRef = "{\"name\":\"data.x\",\"description\":\"\",\"id\":\"data.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}";
        final String parent2AsParentRef = "{\"name\":\"data.y\",\"description\":\"\",\"id\":\"data.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}";
        final String parent3AsParentRef = "{\"name\":\"data.z\",\"description\":\"\",\"id\":\"data.z@dp.domain.com\",\"dataPartitionId\":\"dp\"}";
        final String initialGroupAsParentRef = "{\"name\":\"users.x\",\"description\":\"some description\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\"}";

        final String childOwnerAsChildRef = "{\"role\":\"OWNER\",\"id\":\"users.owner@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}";
        final String childMemberAsChildRef = "{\"role\":\"MEMBER\",\"id\":\"users.member@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"USER\"}";
        final String childGroupAsChildRef = "{\"role\":\"MEMBER\",\"id\":\"data.w@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}";
        final String initialGroupAsChildRef = "{\"role\":\"MEMBER\",\"id\":\"users.x@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}";

        final String newGroup = "{\"appIds\":[],\"name\":\"users.y\",\"description\":\"some description\",\"nodeId\":\"users.y@dp.domain.com\",\"type\":\"GROUP\",\"dataPartitionId\":\"dp\"}";
        final String newGroupAsChildRef = "{\"role\":\"MEMBER\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\",\"type\":\"GROUP\"}";
        final String newGroupAsParentRef = "{\"name\":\"users.y\",\"description\":\"some description\",\"id\":\"users.y@dp.domain.com\",\"dataPartitionId\":\"dp\"}";

        final String rootUserId = "users.data.root@dp.domain.com";

        commands.select(config.getPartitionAppIdDb());
        commands.sadd(GcpAppProperties.DEFAULT_APPID_KEY, initialGroupId);
        commands.select(config.getPartitionEntityNodeDb());
        commands.set(parent1Id, parent1);
        commands.set(parent2Id, parent2);
        commands.set(parent3Id, parent3);
        commands.set(initialGroupId, initialGroup);
        commands.set(childOwnerId, childOwner);
        commands.set(childMemberId, childMember);
        commands.set(childGroupId, childGroup);
        commands.set(rootUserId, rootUser);

        commands.select(config.getPartitionParentRefDb());
        commands.sadd(initialGroupId, parent1AsParentRef, parent2AsParentRef, parent3AsParentRef);
        commands.sadd(childOwnerId, initialGroupAsParentRef);
        commands.sadd(childMemberId, initialGroupAsParentRef);
        commands.sadd(childGroupId, initialGroupAsParentRef);

        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd(parent1Id, initialGroupAsChildRef);
        commands.sadd(parent2Id, initialGroupAsChildRef);
        commands.sadd(parent3Id, initialGroupAsChildRef);
        commands.sadd(initialGroupId, childOwnerAsChildRef, childMemberAsChildRef, childGroupAsChildRef);

        final EntityNode initialGroupNode = EntityNode.builder()
                .nodeId("users.x@dp.domain.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .description("some description")
                .build();
        Set<String> impactedUsers = renameGroupRepo.run(initialGroupNode, newGroupName);

        Assert.assertEquals(new HashSet<>(Arrays.asList("users.owner@dp.domain.com",
                "users.member@dp.domain.com")), impactedUsers);

        commands = connection.sync();

        commands.select(config.getPartitionEntityNodeDb());
        Assert.assertNull(commands.get(initialGroupId));
        Assert.assertEquals(newGroup, commands.get(newGroupId));

        commands.select(config.getPartitionChildrenRefDb());
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsChildRef)), commands.smembers(parent1Id));
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsChildRef)), commands.smembers(parent2Id));
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsChildRef)), commands.smembers(parent3Id));

        commands.select(config.getPartitionParentRefDb());
        Assert.assertEquals(0, commands.smembers(initialGroupId).size());
        Assert.assertEquals(new HashSet<>(Arrays.asList(parent1AsParentRef, parent2AsParentRef, parent3AsParentRef)), commands.smembers(newGroupId));
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsParentRef)), commands.smembers(childMemberId));
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsParentRef)), commands.smembers(childOwnerId));
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupAsParentRef)), commands.smembers(childGroupId));

        commands.select(config.getPartitionAppIdDb());
        Assert.assertEquals(new HashSet<>(Collections.singletonList(newGroupId)), commands.smembers(GcpAppProperties.DEFAULT_APPID_KEY));

        Mockito.verify(auditLogger).updateGroup(AuditStatus.SUCCESS, initialGroupId);
    }
}
