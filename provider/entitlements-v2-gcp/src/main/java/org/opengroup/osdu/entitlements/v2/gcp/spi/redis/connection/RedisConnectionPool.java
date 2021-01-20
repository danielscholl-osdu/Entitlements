package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection;

import io.lettuce.core.api.StatefulRedisConnection;
import lombok.Data;
import lombok.Generated;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.http.HttpStatus;

@Data
@Generated
public class RedisConnectionPool {

    private GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;

    public RedisConnectionPool(GenericObjectPool<StatefulRedisConnection<String, String>> pool) {
        connectionPool = pool;
    }

    public StatefulRedisConnection<String, String> getConnection() {
        try {
            return connectionPool.borrowObject();
        } catch (Exception ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Can't get connection");
        }
    }

    public void returnConnection(StatefulRedisConnection<String, String> connection, JaxRsDpsLog log) {
        try {
            if (null != connection) {
                connectionPool.returnObject(connection);
            }
        } catch (Exception ex) {
            log.error(String.format("Error happened when returning the connection: %s", ex));
        }
    }

}
