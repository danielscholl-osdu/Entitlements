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

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.BooleanUtils.negate;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("group")
public class GroupInfoEntity {

	@Id
	private Long id;
	private String name;
	private String email;
	private String description;
	private String partitionId;
	@MappedCollection(keyColumn = "group_id", idColumn = "group_id")
	private Set<AppId> appIds;

	public EntityNode toEntityNode() {
		return EntityNode.builder()
				.type(NodeType.GROUP)
				.nodeId(email.toLowerCase())
				.name(name.toLowerCase())
				.description(description)
				.dataPartitionId(partitionId)
				.appIds(appIds.stream()
						.map(AppId::getAppIdValue)
						.collect(Collectors.toSet()))
				.build();
	}

	public static GroupInfoEntity fromEntityNode(EntityNode entityNode){
		return GroupInfoEntity.builder()
				.name(entityNode.getName())
				.description(entityNode.getDescription())
				.email(entityNode.getNodeId())
				.partitionId(entityNode.getDataPartitionId())
				.appIds(entityNode.getAppIds().stream()
						.map((id ->
								AppId.builder()
										.appIdValue(id)
										.build()))
						.collect(Collectors.toSet()))
				.build();
	}

	public ChildrenReference toChildrenReference(){
		return ChildrenReference.builder()
				.id(email.toLowerCase())
				.role(Role.MEMBER)
				.dataPartitionId(partitionId)
				.type(NodeType.GROUP)
				.build();
	}

	public ParentReference toParentReference(){
		ParentReference parentReference = ParentReference.builder()
				.id(email.toLowerCase())
				.name(name)
				.description(description)
				.dataPartitionId(partitionId)
				.build();

		if (!isNull(appIds)){
			parentReference.setAppIds(appIds.stream()
					.map(AppId::getAppIdValue)
					.collect(Collectors.toSet()));
		}

		return parentReference;
	}
}
