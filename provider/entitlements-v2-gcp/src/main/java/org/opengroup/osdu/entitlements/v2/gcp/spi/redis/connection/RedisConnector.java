package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.service.PartitionRedisInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class RedisConnector {

    @Autowired
    private GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig;
    @Autowired
    private GcpAppProperties config;

    // TODO: Actualize this behavior using PartitionRedisInstanceService
    @Autowired
    private PartitionRedisInstanceService partitionRedisInstanceService;

    private ConcurrentMap<String, RedisClient> redisClientMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, RedisConnectionPool> redisConnectionPoolMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Object> connectionCreationLockMap = new ConcurrentHashMap<>();

    private static final String CONNECTION_KEY_FORMATTER = "%s:%d";

    @PostConstruct
    private void init() {
        getCentralRedisConnectionPool();
    }

    private RedisClient getRedisClient(String host, int port) {
        String key = String.format(CONNECTION_KEY_FORMATTER, host, port);
        if (!this.redisClientMap.containsKey(key)) {
            RedisClient redisClient = RedisClient.create(RedisURI.create(host, port));
            this.redisClientMap.putIfAbsent(key, redisClient);
            return redisClient;
        }
        return this.redisClientMap.get(key);
    }

    /**
     * To shorten the instance startup time, we don't initialize the partition redis instance connection during startup, but doing it lazily
     * And we are using double-checked locking pattern to make sure only one request will actually create the connection after the instance is up
     */
    private RedisConnectionPool getRedisConnectionPool(String host, int port) {
        String key = String.format(CONNECTION_KEY_FORMATTER, host, port);
        this.connectionCreationLockMap.putIfAbsent(key, new Object());
        if (!this.redisConnectionPoolMap.containsKey(key)) {
            synchronized (this.connectionCreationLockMap.get(key)) {
                if (!this.redisConnectionPoolMap.containsKey(key)) {
                    GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(
                            () -> this.getRedisClient(host, port).connect(), this.poolConfig);
                    RedisConnectionPool redisConnectionPool = new RedisConnectionPool(pool);
                    this.redisConnectionPoolMap.putIfAbsent(key, redisConnectionPool);
                }
            }
        }
        return this.redisConnectionPoolMap.get(key);
    }

    public RedisConnectionPool getPartitionRedisConnectionPool(String partitionId) {
        String host = partitionRedisInstanceService.getHostOfRedisInstanceForPartition(partitionId);
        return getRedisConnectionPool(host, 6379);
    }

    public RedisConnectionPool getCentralRedisConnectionPool() {
        String host = config.getCentralRedisInstIp();
        int port = config.getCentralRedisInstPort();
        return getRedisConnectionPool(host, port);
    }

}
