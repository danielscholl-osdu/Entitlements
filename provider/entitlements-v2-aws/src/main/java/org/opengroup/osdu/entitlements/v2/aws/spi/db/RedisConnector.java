// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.entitlements.v2.aws.spi.db;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.opengroup.osdu.entitlements.v2.aws.AwsAppProperties;
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
    private AwsAppProperties config;



    private ConcurrentMap<String, RedisClient> redisClientMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, RedisConnectionPool> redisConnectionPoolMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Object> connectionCreationLockMap = new ConcurrentHashMap<>();

    private static final String CONNECTION_KEY_FORMATTER = "%s:%d";

    @PostConstruct
    private void init() {

        getRedisConnectionPool();
    }

    private RedisClient getRedisClient(String host, int port, String token) {
        String key = String.format(CONNECTION_KEY_FORMATTER, host, port);
        if (!this.redisClientMap.containsKey(key)) {
            String temp ="rediss://"+token+"@"+host+":"+port;
	        RedisClient redisClient = RedisClient.create(RedisURI.create(temp));
            this.redisClientMap.putIfAbsent(key, redisClient);
            return redisClient;
        }
        return this.redisClientMap.get(key);
    }

    private RedisConnectionPool getRedisConnectionPool(String host, int port, String token) {
        String key = String.format(CONNECTION_KEY_FORMATTER, host, port);
        this.connectionCreationLockMap.putIfAbsent(key, new Object());
        if (!this.redisConnectionPoolMap.containsKey(key)) {
            synchronized (this.connectionCreationLockMap.get(key)) {
                if (!this.redisConnectionPoolMap.containsKey(key)) {
                    GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(() -> this.getRedisClient(host, port, token).connect(), this.poolConfig);
                    RedisConnectionPool redisConnectionPool = new RedisConnectionPool(pool);
                    this.redisConnectionPoolMap.putIfAbsent(key, redisConnectionPool);
                }
            }
        }
        return this.redisConnectionPoolMap.get(key);
    }


    //This needs to change once we update infra for multi-tenancy. Currently retrieving common Redis IP and port
    // Going further the partition service should be queried for a 'RedisIP/RedisPort' property
    public RedisConnectionPool getPartitionRedisConnectionPool(String partitionId) {
         String host = config.getRedisHost();
        int port = config.getRedisPort();
        String key = config.getRedisKey();
        return getRedisConnectionPool(host, port, key);
    }

    public RedisConnectionPool getRedisConnectionPool() {
        String host = config.getRedisHost();
        int port = config.getRedisPort();
        String key = config.getRedisKey();
        return getRedisConnectionPool(host, port, key);
    }

}
