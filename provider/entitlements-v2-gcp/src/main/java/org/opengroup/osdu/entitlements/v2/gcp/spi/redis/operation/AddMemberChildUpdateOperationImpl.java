package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation;

import io.lettuce.core.api.sync.RedisSetCommands;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class AddMemberChildUpdateOperationImpl extends BaseReferenceUpdateOperation {

    private String memberId;
    private String memberPartitionId;

    @Override
    public void execute() {
        log.info(String.format("update child: %s", memberId));
        retry.executeRunnable(() -> updateParentReferenceTransaction(RedisSetCommands::sadd, true, memberId, memberPartitionId));
    }

    @Override
    public void undo() {
        log.info(String.format("revert node: %s", memberId));
        retry.executeRunnable(() -> updateParentReferenceTransaction(RedisSetCommands::srem, false, memberId, memberPartitionId));
    }
}
