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

package org.opengroup.osdu.entitlements.v2.jdbc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("member")
public class MemberInfoEntity {

	@Id
	private Long id;
	private String email;
	private String partitionId;
	private String role;

	public EntityNode toEntityNode() {
		return EntityNode.builder()
				.type(NodeType.USER)
				.nodeId(email.toLowerCase())
				.name(email.toLowerCase())
				.dataPartitionId(partitionId)
				.description(email)
				.build();
	}

	public static MemberInfoEntity fromEntityNode(EntityNode entityNode, Role role){
		return MemberInfoEntity.builder()
				.email(entityNode.getNodeId())
				.role(role.getValue())
				.partitionId(entityNode.getDataPartitionId())
				.build();
	}

	public ChildrenReference toChildrenReference(){
		return ChildrenReference.builder()
				.id(email.toLowerCase())
				.role(Role.valueOf(role))
				.dataPartitionId(partitionId)
				.type(NodeType.USER)
				.build();
	}
}
