// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.entitlements.v2.aws.spi;

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
