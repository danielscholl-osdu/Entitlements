package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.listmember;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.service.PartitionRedisInstanceService;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ListMemberRepoRedisIntegratedWithEmbeddedRedisTests {

    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private PartitionRedisInstanceService partitionRedisInstanceService;
    private static final String DATA_PARTITION_ID = "dp";

    private static RedisServer redisServer;
    private static RedisClient testRedisClient;

    @Autowired
    private GcpAppProperties config;
    @Autowired
    private ListMemberRepoRedis listMemberRepoRedis;

    @BeforeClass
    public static void setupClass() throws IOException {
        redisServer = new RedisServer(6379);
        redisServer.start();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        testRedisClient = RedisClient.create(uri);
    }

    @AfterClass
    public static void end() {
        redisServer.stop();
    }

    @Before
    public void setup() {
        when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition(DATA_PARTITION_ID)).thenReturn("localhost");
    }

    @After
    public void cleanup() {
        StatefulRedisConnection<String, String> connection = testRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.flushall();
    }

    @Test
    public void should_returnDirectMemberNodes() throws Exception {
        StatefulRedisConnection<String, String> connection = testRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.select(config.getPartitionEntityNodeDb());
        commands.set("data.x.viewer@dp.domain.com", "{\"nodeId\":\"data.x.viewer@dp.domain.com\",\"name\":\"data.x.viewer\",\"type\":\"GROUP\"," +
                "\"dataPartitionId\":\"dp\"}");
        commands.select(config.getPartitionChildrenRefDb());
        commands.sadd("data.x.viewer@dp.domain.com", "{\"id\":\"g1@dp.domain.com\",\"type\":\"GROUP\",\"role\":\"MEMBER\",\"dataPartitionId\":\"dp\"}",
                "{\"id\":\"g3@dp.domain.com\",\"type\":\"GROUP\",\"role\":\"MEMBER\",\"dataPartitionId\":\"dp\"}",
                "{\"id\":\"member@xxx.com\",\"type\":\"USER\",\"role\":\"OWNER\",\"dataPartitionId\":\"dp\"}");
        commands.sadd("g1@dp.domain.com", "{\"id\":\"g2@dp.domain.com\",\"type\":\"GROUP\",\"role\":\"MEMBER\",\"dataPartitionId\":\"dp\"}");

        ListMemberServiceDto listMemberServiceDto = ListMemberServiceDto.builder()
                .groupId("data.x.viewer@dp.domain.com").partitionId("dp").build();

        List<ChildrenReference> members = listMemberRepoRedis.run(listMemberServiceDto);

        assertEquals(3, members.size());
        verify(auditLogger).listMember(AuditStatus.SUCCESS, "data.x.viewer@dp.domain.com");
    }

}
