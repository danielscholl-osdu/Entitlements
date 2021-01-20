package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.redis.connection;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.service.PartitionRedisInstanceService;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RedisClient.class, ConnectionPoolSupport.class, RedisURI.class})
public class RedisConnectorTests {

    @Mock
    private JaxRsDpsLog log;
    @Mock
    private GcpAppProperties config;
    @Mock
    private GenericObjectPoolConfig poolConfig;
    @Mock
    private RedisURI redisURI;
    @Mock
    private RedisClient redisClient;
    @Mock
    private GenericObjectPool<StatefulRedisConnection<String, String>> pool;
    @Mock
    private PartitionRedisInstanceService partitionRedisInstanceService;

    @InjectMocks
    private RedisConnector redisConnector;

    @Before
    public void setup() {
        mockStatic(RedisClient.class);
        mockStatic(ConnectionPoolSupport.class);
        mockStatic(RedisURI.class);
    }

    @Test
    public void shouldCreateNewClientAndConnectionPoolWhenFirstTimeAccessIt() {
        when(config.getCentralRedisInstIp()).thenReturn("new ip");
        when(RedisURI.create("new ip", 6379)).thenReturn(redisURI);
        when(RedisClient.create(redisURI)).thenReturn(redisClient);
        when(ConnectionPoolSupport.createGenericObjectPool(any(), eq(poolConfig))).thenReturn(pool);

        RedisConnectionPool retPool = redisConnector.getCentralRedisConnectionPool();

        assertEquals(pool, retPool.getConnectionPool());
    }

    @Test
    public void shouldReturnExistConnectionPoolWhenNotFirstTimeAccessIt() {
        when(config.getCentralRedisInstIp()).thenReturn("ip");
        when(RedisURI.create("ip", 6379)).thenReturn(redisURI);
        when(RedisClient.create(redisURI)).thenReturn(redisClient);
        when(ConnectionPoolSupport.createGenericObjectPool(any(), eq(poolConfig))).thenReturn(pool);

        redisConnector.getCentralRedisConnectionPool();
        RedisConnectionPool retPool = redisConnector.getCentralRedisConnectionPool();

        assertEquals(pool, retPool.getConnectionPool());
    }

    @Test
    public void shouldOnlyOneThreadCreatePartitionRedisConnectionWhenInitialization() {
        when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition("dp1")).thenReturn("ip");
        when(RedisURI.create("ip", 6379)).thenReturn(redisURI);
        when(RedisClient.create(redisURI)).thenReturn(redisClient);
        when(ConnectionPoolSupport.createGenericObjectPool(any(), eq(poolConfig))).thenReturn(pool);
        IntStream.range(1, 10).parallel().forEach(i -> redisConnector.getPartitionRedisConnectionPool("dp1"));

        verifyStatic(ConnectionPoolSupport.class, VerificationModeFactory.times(1));
        ConnectionPoolSupport.createGenericObjectPool(any(), any());
    }

    @Test
    public void shouldOnly2ThreadsCreatePartitionRedisConnectionWhenInitialization() {
        when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition("dp1")).thenReturn("ip1");
        when(partitionRedisInstanceService.getHostOfRedisInstanceForPartition("dp2")).thenReturn("ip2");
        when(RedisURI.create("ip1", 6379)).thenReturn(redisURI);
        when(RedisURI.create("ip2", 6379)).thenReturn(redisURI);
        when(RedisClient.create(redisURI)).thenReturn(redisClient);
        when(ConnectionPoolSupport.createGenericObjectPool(any(), eq(poolConfig))).thenReturn(pool);
        IntStream.range(1, 10).parallel().forEach(i -> {
            redisConnector.getPartitionRedisConnectionPool("dp1");
            redisConnector.getPartitionRedisConnectionPool("dp2");
        });

        verifyStatic(ConnectionPoolSupport.class, VerificationModeFactory.times(2));
        ConnectionPoolSupport.createGenericObjectPool(any(), any());
    }

}
