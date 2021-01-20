package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation;

import io.lettuce.core.api.sync.RedisSetCommands;

public interface RedisSetCommandsMethodExecution {
    void execute(RedisSetCommands<String, String> commands, String id, String json);
}
