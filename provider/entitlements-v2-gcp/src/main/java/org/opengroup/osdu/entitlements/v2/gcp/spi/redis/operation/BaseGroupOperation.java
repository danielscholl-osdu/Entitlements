package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.experimental.SuperBuilder;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.util.JsonConverter;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.Set;

@SuperBuilder
public abstract class BaseGroupOperation implements Operation {

    protected RedisConnector redisConnector;
    protected GcpAppProperties config;
    protected JaxRsDpsLog log;
    protected EntityNode groupNode;

    protected void createGroupTransaction(EntityNode groupNode) {
        RedisConnectionPool connectionPool = redisConnector.getPartitionRedisConnectionPool(groupNode.getDataPartitionId());
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            createGroup(commands, groupNode);
            updateAppIds(commands, groupNode);
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    protected void deleteGroupTransaction(EntityNode groupNode) {
        RedisConnectionPool connectionPool = redisConnector.getPartitionRedisConnectionPool(groupNode.getDataPartitionId());
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionEntityNodeDb());
            commands.del(groupNode.getNodeId());
            commands.select(config.getPartitionParentRefDb());
            commands.del(groupNode.getNodeId());
            commands.select(config.getPartitionChildrenRefDb());
            commands.del(groupNode.getNodeId());
            commands.select(config.getPartitionAppIdDb());
            final Set<String> appIds = getAppIdsForUpdate(groupNode.getAppIds());
            for (String appId : appIds) {
                commands.srem(appId, groupNode.getNodeId());
            }
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    private void createGroup(RedisCommands<String, String> commands, EntityNode group) {
        commands.select(config.getPartitionEntityNodeDb());
        if (Boolean.FALSE.equals(commands.setnx(group.getNodeId(), JsonConverter.toJson(group)))) {
            throw new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), String.format("This group already exists. " + group.getNodeId()));
        }
    }

    /**
     * Update redis <app-id, group-email>
     */
    private void updateAppIds(RedisCommands<String, String> commands, EntityNode group) {
        final Set<String> appIds = getAppIdsForUpdate(group.getAppIds());
        commands.select(config.getPartitionAppIdDb());
        for (String appId : appIds) {
            commands.sadd(appId, group.getNodeId());
        }
    }

    private Set<String> getAppIdsForUpdate(final Set<String> appIdsOfGroup) {
        final Set<String> appIds = new HashSet<>();
        if (appIdsOfGroup.isEmpty()) {
            appIds.add(GcpAppProperties.DEFAULT_APPID_KEY);
        } else {
            appIds.addAll(appIdsOfGroup);
        }
        return appIds;
    }
}
