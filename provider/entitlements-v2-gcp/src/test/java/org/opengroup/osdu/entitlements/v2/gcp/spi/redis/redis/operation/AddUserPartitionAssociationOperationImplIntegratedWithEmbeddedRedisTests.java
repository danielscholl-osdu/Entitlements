package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.redis.operation;

import io.github.resilience4j.retry.Retry;
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
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.service.PartitionRedisInstanceService;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation.AddUserPartitionAssociationOperationImpl;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class AddUserPartitionAssociationOperationImplIntegratedWithEmbeddedRedisTests {
    @MockBean
    private GcpAppProperties config;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;
    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private PartitionRedisInstanceService partitionRedisInstanceService;
    private static final String DATA_PARTITION_ID = "dp";

    private static RedisServer centralRedisServer;
    private static RedisClient centralRedisClient;

    @Autowired
    private RedisConnector redisConnector;
    @Autowired
    private Retry retry;

    @BeforeClass
    public static void setupClass() throws IOException {
        centralRedisServer = new RedisServer(7000);
        centralRedisServer.start();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(7000).build();
        centralRedisClient = RedisClient.create(uri);
    }

    @AfterClass
    public static void end() {
        centralRedisServer.stop();
    }

    @Before
    public void setup() {
        when(config.getCentralRedisInstIp()).thenReturn("localhost");
        when(config.getCentralRedisInstPort()).thenReturn(7000);
        when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition(DATA_PARTITION_ID)).thenReturn("localhost");
    }

    @After
    public void cleanup() {
        StatefulRedisConnection<String, String> connection = centralRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.flushall();
    }

    @Test
    public void should_addPartitionToList_whenExecute() {
        Operation addUserPartitionAssociationOperation1 = AddUserPartitionAssociationOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).config(config).log(log).userId("user@xxx.com").partitionId("dp1")
                .whitelistSvcAccBeanConfiguration(whitelistSvcAccBeanConfiguration).build();
        Operation addUserPartitionAssociationOperation2 = AddUserPartitionAssociationOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).config(config).log(log).userId("user@xxx.com").partitionId("dp2")
                .whitelistSvcAccBeanConfiguration(whitelistSvcAccBeanConfiguration).build();

        addUserPartitionAssociationOperation1.execute();
        addUserPartitionAssociationOperation2.execute();

        StatefulRedisConnection<String, String> connection = centralRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        Set<String> partitionList = commands.smembers("user@xxx.com");

        assertThat(partitionList.size()).isEqualTo(2);
    }

    @Test
    public void should_throw412_ifUserPartitionAssociationQuotaHit_whenExecute() {
        StatefulRedisConnection<String, String> connection = centralRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.sadd("user@xxx.com", new String[]{"dp1", "dp2", "dp3", "dp4", "dp5", "dp6", "dp7", "dp8", "dp9", "dp10",
                "dp11", "dp12", "dp13", "dp14", "dp15", "dp16", "dp17", "dp18", "dp19", "dp20", "dp21", "dp22", "dp23", "dp24", "dp25", "dp26", "dp27", "dp28", "dp29", "dp30"});

        when(whitelistSvcAccBeanConfiguration.isWhitelistedServiceAccount("user@xxx.com")).thenReturn(Boolean.FALSE);
        Operation addUserPartitionAssociationOperation = AddUserPartitionAssociationOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).config(config).log(log).userId("user@xxx.com").partitionId("dp31").whitelistSvcAccBeanConfiguration(whitelistSvcAccBeanConfiguration).build();

        try {
            addUserPartitionAssociationOperation.execute();
            fail("should throw exception");
        } catch (AppException ex) {
            assertThat(ex.getError().getCode()).isEqualTo(412);
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex));
        }
    }

    @Test
    public void should_addPartitionToList_whenUserPartitionAssociationQuotaHit_butIsWhiteListed_whenExecute() {
        StatefulRedisConnection<String, String> connection = centralRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.sadd("user@xxx.com", new String[]{"dp1", "dp2", "dp3", "dp4", "dp5", "dp6", "dp7", "dp8", "dp9", "dp10",
                "dp11", "dp12", "dp13", "dp14", "dp15", "dp16", "dp17", "dp18", "dp19", "dp20", "dp21", "dp22", "dp23", "dp24", "dp25", "dp26", "dp27", "dp28", "dp29", "dp30"});

        when(whitelistSvcAccBeanConfiguration.isWhitelistedServiceAccount("user@xxx.com")).thenReturn(Boolean.TRUE);
        Operation addUserPartitionAssociationOperation = AddUserPartitionAssociationOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).config(config).log(log).userId("user@xxx.com").partitionId("dp31").whitelistSvcAccBeanConfiguration(whitelistSvcAccBeanConfiguration).build();

        addUserPartitionAssociationOperation.execute();

        Set<String> partitionList = commands.smembers("user@xxx.com");
        assertThat(partitionList.size()).isEqualTo(31);

    }

    @Test
    public void should_removePartitionFromList_whenUndo() {
        Operation addUserPartitionAssociationOperation = AddUserPartitionAssociationOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).config(config).log(log).userId("user@xxx.com").partitionId("dp1")
                .whitelistSvcAccBeanConfiguration(whitelistSvcAccBeanConfiguration).build();

        addUserPartitionAssociationOperation.execute();
        addUserPartitionAssociationOperation.undo();

        StatefulRedisConnection<String, String> connection = centralRedisClient.connect();
        RedisCommands<String, String> commands = connection.sync();
        Set<String> partitionList = commands.smembers("user@xxx.com");

        assertThat(partitionList.size()).isEqualTo(0);
    }
}
