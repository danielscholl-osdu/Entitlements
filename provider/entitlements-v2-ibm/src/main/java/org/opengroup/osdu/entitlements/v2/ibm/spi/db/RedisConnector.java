/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.db;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.opengroup.osdu.entitlements.v2.ibm.IBMAppProperties;
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
    private IBMAppProperties config;


    private ConcurrentMap<String, RedisClient> redisClientMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, RedisConnectionPool> redisConnectionPoolMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Object> connectionCreationLockMap = new ConcurrentHashMap<>();

    private static final String CONNECTION_KEY_FORMATTER = "%s:%d";

    @PostConstruct
    private void init() {
        getRedisConnectionPool();
    }

    private RedisClient getRedisClient(String host, int port, String redisKey) {
        String key = String.format(CONNECTION_KEY_FORMATTER, host, port);
        RedisClient redisClient;
        if (!this.redisClientMap.containsKey(key)) {
        	if(redisKey != null && redisKey.isEmpty()) {
        		redisClient = RedisClient.create(RedisURI.create(host, port));
        	}
        	else {
        		String redisURI = String.format("redis://%s@%s:%d", redisKey, host, port);
            	redisClient = RedisClient.create(redisURI);
        	}
            this.redisClientMap.putIfAbsent(key, redisClient);
            return redisClient;
        }
        return this.redisClientMap.get(key);
    }
    
    private RedisConnectionPool getRedisConnectionPool(String host, int port, String redisKey) {
        String key = String.format(CONNECTION_KEY_FORMATTER, host, port);
        this.connectionCreationLockMap.putIfAbsent(key, new Object());
        if (!this.redisConnectionPoolMap.containsKey(key)) {
            synchronized (this.connectionCreationLockMap.get(key)) {
                if (!this.redisConnectionPoolMap.containsKey(key)) {
                    GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(() -> this.getRedisClient(host, port, redisKey).connect(), this.poolConfig);
                    RedisConnectionPool redisConnectionPool = new RedisConnectionPool(pool);
                    this.redisConnectionPoolMap.putIfAbsent(key, redisConnectionPool);
                }
            }
        }
        return this.redisConnectionPoolMap.get(key);
    }
    
    public RedisConnectionPool getPartitionRedisConnectionPool(String partitionId) {
         String host = config.getRedisHost();
        int port = config.getRedisPort();
        String redisKey = config.getRedisKey();
        return getRedisConnectionPool(host, port, redisKey);
    }

    public RedisConnectionPool getRedisConnectionPool() {
        String host = config.getRedisHost();
        int port = config.getRedisPort();
        String redisKey = config.getRedisKey();
        return getRedisConnectionPool(host, port, redisKey);
    }

}
