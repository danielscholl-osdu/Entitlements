/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.entitlements.v2.ibm.spi;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.springframework.http.HttpStatus;

import java.util.Deque;
import java.util.LinkedList;

public abstract class BaseRepo {

    protected Deque<Operation> executedCommands = new LinkedList<>();

    /**
     * If the transaction threw 409 or 404 exception, we should not roll back
     */
    protected void rollback(Exception ex) {
        if (ex instanceof AppException) {
            int errorCode = ((AppException) ex).getError().getCode();
            if ((HttpStatus.CONFLICT.value() == errorCode) || (HttpStatus.NOT_FOUND.value() == errorCode)) {
                return;
            }
        }
        while (!executedCommands.isEmpty()) {
            Operation executedCommand = executedCommands.pop();
            executedCommand.undo();
        }
    }
}
