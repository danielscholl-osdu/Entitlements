/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.operation;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class AddUserPartitionAssociationOperationImpl extends BaseRedisOperation {

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
