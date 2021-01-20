package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation;

import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import io.lettuce.core.api.sync.RedisSetCommands;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class RemoveMemberParentUpdateOperationImpl extends BaseReferenceUpdateOperation {

    private ChildrenReference childrenReference;

    @Override
    public void execute() {
        log.info(String.format("update node %s", groupNode.getNodeId()));
        retry.executeRunnable(() -> updateChildrenReferenceTransaction(RedisSetCommands::srem, false, childrenReference));
    }

    @Override
    public void undo() {
        log.info(String.format("revert node %s", groupNode.getNodeId()));
        retry.executeRunnable(() -> updateChildrenReferenceTransaction(RedisSetCommands::sadd, false, childrenReference));
    }
}
