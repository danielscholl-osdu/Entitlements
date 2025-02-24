/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.operation;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class CreateGroupOperationImpl extends BaseGroupOperation {

    @Override
    public void execute() {
        log.info(String.format("create group node %s", groupNode.getNodeId()));
        createGroupTransaction(groupNode);
    }

    @Override
    public void undo() {
        log.info(String.format("revert group creation %s", groupNode.getNodeId()));
        deleteGroupTransaction(groupNode);
    }
}
