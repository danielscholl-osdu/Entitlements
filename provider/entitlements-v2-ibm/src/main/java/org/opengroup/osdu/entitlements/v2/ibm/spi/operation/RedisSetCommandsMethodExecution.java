/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.entitlements.v2.ibm.spi.operation;

import io.lettuce.core.api.sync.RedisSetCommands;

public interface RedisSetCommandsMethodExecution {
    void execute(RedisSetCommands<String, String> commands, String id, String json);
}
