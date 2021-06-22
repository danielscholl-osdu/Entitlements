/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.operation;

import io.github.resilience4j.retry.Retry;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.experimental.SuperBuilder;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.ibm.IBMAppProperties;
import org.opengroup.osdu.entitlements.v2.ibm.spi.db.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.ibm.spi.db.RedisConnector;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.springframework.http.HttpStatus;

import java.util.Set;

@SuperBuilder
public abstract class BaseRedisOperation implements Operation {

    private static final String FAILED_TO_ADD_LOG_MSG_FT = "transaction failed when add user partition association (%s, %s)";
    private static final String FAILED_TO_REMOVE_LOG_MSG_FT = "transaction failed when remove user partition association (%s, %s)";
    private static final int MAX_PARTITIONS = 30;

    protected JaxRsDpsLog log;
    protected Retry retry;

    private RedisConnector redisConnector;
    private IBMAppProperties config;
    private WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;

    protected void addPartitionAssociationToUser(String userId, String partitionId) {
        RedisConnectionPool connectionPool = this.redisConnector.getRedisConnectionPool();
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        log.info(String.format("Get redis connection correctly for central redis instance %s", config.getRedisHost()));
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getRedisPartitionAssociation());
            Set<String> partitionList = commands.smembers(userId);
            log.info(String.format("Partition list: %s, whiteListConfigBean : %s ", partitionList, whitelistSvcAccBeanConfiguration));
            if (!whitelistSvcAccBeanConfiguration.isWhitelistedServiceAccount(userId) && partitionList.size() >= MAX_PARTITIONS && !partitionList.contains(partitionId)) {
                throw new AppException(HttpStatus.PRECONDITION_FAILED.value(), HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), String.format("%s's partition quota hit. Identity can't belong to more than %d partitions", userId, MAX_PARTITIONS));
            }
            commands.multi();
            commands.sadd(userId, partitionId);
            validateTransactionResult(commands.exec(), String.format(FAILED_TO_ADD_LOG_MSG_FT, userId, partitionId));
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    public void removePartitionAssociationFromUser(String userId, String partitionId) {
        RedisConnectionPool connectionPool = this.redisConnector.getRedisConnectionPool();
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        log.info(String.format("Get redis connection correctly for central redis instance %s", config.getRedisHost()));
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getRedisPartitionAssociation());
            commands.multi();
            commands.srem(userId, partitionId);
            validateTransactionResult(commands.exec(), String.format(FAILED_TO_REMOVE_LOG_MSG_FT, userId, partitionId));
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    private void validateTransactionResult(final TransactionResult result, final String errorMessage) {
        if (result.wasDiscarded()) {
            log.warning(errorMessage);
            throw new AppException(HttpStatus.LOCKED.value(), HttpStatus.LOCKED.getReasonPhrase(), "Concurrent operation for the same resources");
        }
    }
}
