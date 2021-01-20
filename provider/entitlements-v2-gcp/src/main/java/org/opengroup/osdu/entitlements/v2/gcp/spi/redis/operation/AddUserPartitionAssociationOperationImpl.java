package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class AddUserPartitionAssociationOperationImpl extends BaseCentralRedisOperation {

    private String userId;
    private String partitionId;

    @Override
    public void execute() {
        retry.executeRunnable(() -> addPartitionAssociationToUser(userId, partitionId));
    }

    @Override
    public void undo() {
        retry.executeRunnable(() -> removePartitionAssociationFromUser(userId, partitionId));
    }
}
