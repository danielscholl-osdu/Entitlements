/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util;
import static java.lang.String.format;

import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;

public class JdbcTestDataProvider {
	public static final String DATA_PARTITION_ID = "dp";



	public static EntityNode getRequesterNode(){
		return EntityNode.builder()
				.nodeId("callerdesid")
				.name("callerdesid")
				.type(NodeType.USER)
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
	}

	public static EntityNode getCommonGroup(String name){
		return EntityNode.builder()
				.nodeId(format("%s@%s.domain.com", name, DATA_PARTITION_ID))
				.type(NodeType.GROUP)
				.name(name)
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
	}

	public static EntityNode getUsersGroupNode(String modifier){
		return EntityNode.builder()
				.nodeId(format("users.%s@%s.domain.com", modifier, DATA_PARTITION_ID))
				.type(NodeType.GROUP)
				.name(format("users.%s", modifier))
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
	}

	public static EntityNode getUsersGroupNode(String modifier, String dataPartitionId){
		return EntityNode.builder()
			.nodeId(format("users.%s@%s.domain.com", modifier, dataPartitionId))
			.type(NodeType.GROUP)
			.name(format("users.%s", modifier))
			.dataPartitionId(dataPartitionId)
			.build();
	}

	public static EntityNode getDataGroupNode(String modifier){
		return EntityNode.builder()
				.nodeId(format("data.%s@%s.domain.com", modifier, DATA_PARTITION_ID))
				.name(format("data.%s", modifier))
				.type(NodeType.GROUP)
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
	}

	public static EntityNode getDataRootGroupNode(){
		return EntityNode.builder()
				.nodeId(format("users.data.root@%s.domain.com", DATA_PARTITION_ID))
				.name("users.data.root")
				.type(NodeType.GROUP)
				.name("users.data.root")
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
	}

	public static EntityNode getMemberNode(String name){
		return EntityNode.builder()
				.nodeId(format("%s@xxx.com", name))
				.name(format("%s@xxx.com", name))
				.type(NodeType.USER)
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
	}

	public static EntityNode getDataViewersGroupNode(String modifier){
		return EntityNode.builder()
				.nodeId(format("data.%s.viewers@%s.domain.com", modifier, DATA_PARTITION_ID))
				.name(format("data.%s.viewers", modifier))
				.type(NodeType.GROUP)
				.dataPartitionId(DATA_PARTITION_ID)
				.build();
	}

	public static ChildrenReference getUserChildrenReference(String childEmail, String dataPartitionId){
		return ChildrenReference.builder()
			.id(childEmail)
			.dataPartitionId(dataPartitionId)
			.type(NodeType.USER)
			.role(Role.MEMBER)
			.build();
	}

}
