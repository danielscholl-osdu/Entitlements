/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.operation;

import io.lettuce.core.api.sync.RedisSetCommands;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class RemoveMemberChildUpdateOperationImpl extends BaseReferenceUpdateOperation {

    private String memberId;
    private String memberPartitionId;

    @Override
    public void execute() {
        log.info(String.format("update node %s", memberId));
        retry.executeRunnable(() -> updateParentReferenceTransaction(RedisSetCommands::srem, false, memberId, memberPartitionId));
    }

    @Override
    public void undo() {
        log.info(String.format("revert node %s and its children", memberId));
        retry.executeRunnable(() -> updateParentReferenceTransaction(RedisSetCommands::sadd, false, memberId, memberPartitionId));
    }
}
