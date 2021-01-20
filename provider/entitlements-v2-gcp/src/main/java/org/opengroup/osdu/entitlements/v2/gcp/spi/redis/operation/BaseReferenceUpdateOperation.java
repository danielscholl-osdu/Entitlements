package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation;

import io.github.resilience4j.retry.Retry;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.experimental.SuperBuilder;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.util.JsonConverter;
import org.springframework.http.HttpStatus;

@SuperBuilder
public abstract class BaseReferenceUpdateOperation implements Operation {

    protected Retry retry;
    protected JaxRsDpsLog log;
    protected EntityNode groupNode;

    private GcpAppProperties config;
    private RedisConnector redisConnector;

    protected void updateParentReferenceTransaction(RedisSetCommandsMethodExecution redisSetMethod, boolean isValidationNeeded, String memberId, String memberPartitionId) {
        RedisConnectionPool connectionPool = redisConnector.getPartitionRedisConnectionPool(memberPartitionId);
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionParentRefDb());
            commands.watch(memberId);
            String parentRefJson = JsonConverter.toJson(ParentReference.createParentReference(groupNode));
            if (isValidationNeeded && Boolean.TRUE.equals(commands.sismember(memberId, parentRefJson))) {
                throw new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), String.format("%s is already a member of group %s", memberId, groupNode.getNodeId()));
            }
            log.info(String.format("commit changes of node %s", memberId));
            commands.multi();
            redisSetMethod.execute(commands, memberId, JsonConverter.toJson(ParentReference.createParentReference(groupNode)));
            validateTransactionResult(commands.exec(), memberId);
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    protected void updateChildrenReferenceTransaction(RedisSetCommandsMethodExecution redisSetMethod, boolean isValidationNeeded, ChildrenReference childrenReference) {
        RedisConnectionPool connectionPool = redisConnector.getPartitionRedisConnectionPool(groupNode.getDataPartitionId());
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionChildrenRefDb());
            commands.watch(groupNode.getNodeId());
            String childRefJson = JsonConverter.toJson(childrenReference);
            if (isValidationNeeded && Boolean.TRUE.equals(commands.sismember(groupNode.getNodeId(), childRefJson))) {
                throw new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), String.format("%s is already a member of group %s", childrenReference.getId(), groupNode.getNodeId()));
            }
            log.info(String.format("commit changes of node %s", groupNode.getNodeId()));
            commands.multi();
            redisSetMethod.execute(commands, groupNode.getNodeId(), JsonConverter.toJson(childrenReference));
            validateTransactionResult(commands.exec(), groupNode.getNodeId());
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    private void validateTransactionResult(final TransactionResult result, final String id) {
        if (result.wasDiscarded()) {
            log.warning(String.format("transaction failed when updating %s", id));
            throw new AppException(HttpStatus.LOCKED.value(), HttpStatus.LOCKED.getReasonPhrase(), "Concurrent operation for the same resources");
        }
    }
}
