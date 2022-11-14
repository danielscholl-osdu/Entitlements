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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.creategroup;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.jdbc.exception.DatabaseAccessException;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.creategroup.CreateGroupRepo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CreateGroupRepoJdbc implements CreateGroupRepo {

	private final JaxRsDpsLog log;
	private final AuditLogger auditLogger;
	private final GroupRepository groupRepository;
	private final MemberRepository memberRepository;
	private final JdbcTemplateRunner jdbcTemplateRunner;

	@Override
	public Set<String> createGroup(EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto) {
		try {
			log.debug(String.format("Creating group %s and updating data model in postgres",
				groupNode.getName()));

			executeCreateGroupOperation(groupNode, createGroupRepoDto);

			auditLogger.createGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
			return ImmutableSet.of(createGroupRepoDto.getRequesterNode().getNodeId());

		} catch (Exception e) {
			auditLogger.createGroup(AuditStatus.FAILURE, groupNode.getNodeId());
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new DatabaseAccessException(HttpStatus.CONFLICT, "This group already exists");
			}
			throw e;
		}
	}

	@Override
	public Set<String> createGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode,
			CreateGroupRepoDto createGroupRepoDto) {
		return Collections.emptySet();
	}

	private void executeCreateGroupOperation(EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto){
		GroupInfoEntity createdGroup = groupRepository.save(GroupInfoEntity.fromEntityNode(groupNode));
		addRequesterAsOwnerMemberToGroup(createdGroup, createGroupRepoDto);

		if (createGroupRepoDto.isAddDataRootGroup()) {
			addRootGroupNodeAsMemberOfGroupNewGroup(createdGroup, createGroupRepoDto);
		}
	}

	private void addRootGroupNodeAsMemberOfGroupNewGroup(GroupInfoEntity createdGroup, CreateGroupRepoDto createGroupRepoDto) {
		GroupInfoEntity parentGroup = groupRepository
				.findByEmail(createGroupRepoDto.getDataRootGroupNode().getNodeId())
				.stream()
				.findFirst()
				.orElseThrow(() ->
						new DatabaseAccessException(
								HttpStatus.NOT_FOUND,
								"Could not find the group with email: " +
										createGroupRepoDto.getDataRootGroupNode().getNodeId()));

		groupRepository.addChildGroupById(parentGroup.getId(), createdGroup.getId());
	}

	private void addRequesterAsOwnerMemberToGroup(GroupInfoEntity createdGroup, CreateGroupRepoDto createGroupRepoDto) {
		Optional<MemberInfoEntity> requester = memberRepository.findByEmail(createGroupRepoDto.getRequesterNode().getNodeId())
				.stream()
				.findFirst();

		Long requesterId = requester.isPresent() ?
				requester.get().getId():
				jdbcTemplateRunner.saveMemberInfoEntity(MemberInfoEntity.fromEntityNode(
						createGroupRepoDto.getRequesterNode(),
						Role.OWNER
				));
		groupRepository.addMemberById(createdGroup.getId(), requesterId, Role.OWNER.getValue());
	}

}
