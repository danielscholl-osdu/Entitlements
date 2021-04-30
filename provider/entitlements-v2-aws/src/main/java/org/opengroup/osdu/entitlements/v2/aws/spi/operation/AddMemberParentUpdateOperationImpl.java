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
package org.opengroup.osdu.entitlements.v2.aws.spi.operation;

import io.lettuce.core.api.sync.RedisSetCommands;
import lombok.experimental.SuperBuilder;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;

@SuperBuilder
public class AddMemberParentUpdateOperationImpl extends BaseReferenceUpdateOperation {

    private ChildrenReference childrenReference;

    @Override
    public void execute() {
        log.info(String.format("update node %s", groupNode.getNodeId()));
        retry.executeRunnable(() -> updateChildrenReferenceTransaction(RedisSetCommands::sadd, true, childrenReference));

    }

    @Override
    public void undo() {
        log.info(String.format("revert node %s and its parents", groupNode.getNodeId()));
        retry.executeRunnable(() -> updateChildrenReferenceTransaction(RedisSetCommands::srem, false, childrenReference));
    }
}
