package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class RemoveUserPartitionAssociationOperationImpl extends BaseCentralRedisOperation {

    private String userId;
    private String partitionId;

    @Override
    public void execute() {
        retry.executeRunnable(() -> removePartitionAssociationFromUser(userId, partitionId));
    }

    @Override
    public void undo() {
        retry.executeRunnable(() -> addPartitionAssociationToUser(userId, partitionId));
    }
}
