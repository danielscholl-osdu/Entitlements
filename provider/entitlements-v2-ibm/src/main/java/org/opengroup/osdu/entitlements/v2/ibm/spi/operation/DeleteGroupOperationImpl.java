/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.operation;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class DeleteGroupOperationImpl extends BaseGroupOperation {
    @Override
    public void execute() {
        log.info(String.format("delete group node %s", groupNode.getNodeId()));
        deleteGroupTransaction(groupNode);
    }

    @Override
    public void undo() {
        log.info(String.format("revert group deletion %s", groupNode.getNodeId()));
        createGroupTransaction(groupNode);
    }
}
