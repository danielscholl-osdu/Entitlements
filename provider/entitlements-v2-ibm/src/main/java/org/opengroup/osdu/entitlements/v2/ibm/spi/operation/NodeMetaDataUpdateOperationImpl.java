/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.operation;

import io.github.resilience4j.retry.Retry;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.Builder;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.ibm.IBMAppProperties;
import org.opengroup.osdu.entitlements.v2.ibm.spi.db.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.ibm.spi.db.RedisConnector;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.util.JsonConverter;
import org.springframework.http.HttpStatus;

import java.util.Set;

@Builder
public class NodeMetaDataUpdateOperationImpl implements Operation {

    private RedisConnector redisConnector;
    private Retry retry;
    private JaxRsDpsLog log;
    private IBMAppProperties config;
    private EntityNode groupNode;
    private Set<String> appIds;
    private Set<String> originalAppIds;

    @Override
    public void execute() {
        log.info(String.format("update node %s and its metadata", groupNode.getNodeId()));
        updateAppIds();
        retry.executeRunnable(this::executeAndUndoTransaction);
    }

    @Override
    public void undo() {
        log.info(String.format("revert node %s and its metadata", groupNode.getNodeId()));
        revertUpdateOfAppIds();
        retry.executeRunnable(this::executeAndUndoTransaction);
    }

    private void updateAppIds() {
        validateNodeType();
        originalAppIds = groupNode.getAppIds();
        groupNode.setAppIds(appIds);
    }

    private void revertUpdateOfAppIds() {
        validateNodeType();
        appIds = originalAppIds;
        originalAppIds = groupNode.getAppIds();
        groupNode.setAppIds(appIds);
    }

    private void executeAndUndoTransaction() {
        RedisConnectionPool connectionPool = this.redisConnector.getPartitionRedisConnectionPool(groupNode.getDataPartitionId());
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionEntityNode());
            commands.watch(groupNode.getNodeId());
            String groupNodeJson = JsonConverter.toJson(groupNode);
            executeAppIdCacheUpdateTransaction(commands);
            commands.select(config.getPartitionEntityNode());
            commands.multi();
            commands.set(groupNode.getNodeId(), groupNodeJson);
            final TransactionResult result = commands.exec();
            if (result.wasDiscarded()) {
                log.warning(String.format("transaction failed when updating %s on partition %s", groupNode.getNodeId(), groupNode.getDataPartitionId()));
                throw new AppException(HttpStatus.LOCKED.value(), HttpStatus.LOCKED.getReasonPhrase(), "Concurrent operation for the same resources");
            }
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    private void executeAppIdCacheUpdateTransaction(RedisCommands<String, String> commands) {
        Set<String> groupNodeAppIds = groupNode.getAppIds();
        String groupEmail = groupNode.getNodeId();
        log.info(String.format("commit changes of node %s on partition %s", groupNode.getNodeId(), groupNode.getDataPartitionId()));

        addDefaultKeyToEmptyAppIdSet(groupNodeAppIds);
        addDefaultKeyToEmptyAppIdSet(originalAppIds);
        commands.select(config.getPartitionAppId());
        for (String originalAppId : originalAppIds) {
            commands.srem(originalAppId, groupEmail);
        }
        for (String appId : groupNodeAppIds) {
            commands.sadd(appId, groupEmail);
        }
        // remove the added default appid key if any
        originalAppIds.remove(IBMAppProperties.DEFAULT_APPID_KEY);
        groupNodeAppIds.remove(IBMAppProperties.DEFAULT_APPID_KEY);
    }

    private void addDefaultKeyToEmptyAppIdSet(Set<String> appIdSet) {
        if (appIdSet.isEmpty()) {
            appIdSet.add(IBMAppProperties.DEFAULT_APPID_KEY);
        }
    }

    private void validateNodeType() {
        if (!NodeType.GROUP.equals(groupNode.getType())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), String.format("Not a valid group id %s", groupNode.getNodeId()));
        }
    }
}
