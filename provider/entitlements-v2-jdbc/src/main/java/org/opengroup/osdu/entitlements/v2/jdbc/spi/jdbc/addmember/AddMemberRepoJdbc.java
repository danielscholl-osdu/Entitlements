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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.addmember;

import static java.lang.String.format;

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
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AddMemberRepoJdbc implements AddMemberRepo {

	private final JaxRsDpsLog log;
	private final AuditLogger auditLogger;
	private final GroupRepository groupRepository;
	private final MemberRepository memberRepository;
	private final JdbcTemplateRunner jdbcTemplateRunner;

	@Override
	public Set<String> addMember(EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
		try {
			log.debug(format("Adding member %s into the group %s and updating the data model in database", addMemberRepoDto.getMemberNode().getNodeId(), groupEntityNode.getNodeId()));
			Set<String> affectedMembers = executeAddMemberOperation(groupEntityNode, addMemberRepoDto);
			auditLogger.addMember(AuditStatus.SUCCESS,
					groupEntityNode.getNodeId(),
					addMemberRepoDto.getMemberNode().getNodeId(),
					addMemberRepoDto.getRole());
			return affectedMembers;
		} catch (Exception e){
			auditLogger.addMember(AuditStatus.FAILURE,
					groupEntityNode.getNodeId(),
					addMemberRepoDto.getMemberNode().getNodeId(),
					addMemberRepoDto.getRole());
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new DatabaseAccessException(HttpStatus.CONFLICT, format(
						"%s is already a member of group %s",
						addMemberRepoDto.getMemberNode().getNodeId(),
						groupEntityNode.getNodeId()));
			}
			if (e instanceof DuplicateKeyException) {
				throw new DatabaseAccessException(HttpStatus.CONFLICT, format(
						"%s is already a member of group %s",
						addMemberRepoDto.getMemberNode().getNodeId(),
						groupEntityNode.getNodeId()));
			}
			throw e;
		}
	}

	@Override
	public Set<String> addMember(Deque<Operation> executedCommands, EntityNode groupEntityNode,
			AddMemberRepoDto addMemberRepoDto) {
		return Collections.emptySet();
	}

	private Set<String> executeAddMemberOperation(EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
		if (addMemberRepoDto.getMemberNode().isGroup()) {
			addGroupAsChild(groupEntityNode, addMemberRepoDto);
			return jdbcTemplateRunner.getAffectedMembersForGroup(addMemberRepoDto.getMemberNode());
		} else {
			addMemberInGroup(groupEntityNode, addMemberRepoDto);
			return ImmutableSet.of(addMemberRepoDto.getMemberNode().getNodeId());
		}
	}

	private void addMemberInGroup(EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
		Optional<MemberInfoEntity> memberInfoEntity = memberRepository.findByEmail(addMemberRepoDto.getMemberNode().getNodeId())
				.stream()
				.findFirst();

		Long memberId = memberInfoEntity.isPresent() ?
				memberInfoEntity.get().getId():
				jdbcTemplateRunner.saveMemberInfoEntity(
						MemberInfoEntity.fromEntityNode(
								addMemberRepoDto.getMemberNode(),
								addMemberRepoDto.getRole()));

		GroupInfoEntity groupInfoEntity = groupRepository.findByEmail(groupEntityNode.getNodeId()).stream()
				.findFirst()
				.orElseThrow(() -> DatabaseAccessException.createNotFound(groupEntityNode.getNodeId()));

		groupRepository.addMemberById(groupInfoEntity.getId(), memberId, addMemberRepoDto.getRole().getValue());
	}

	private void addGroupAsChild(EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto) {
		GroupInfoEntity childInfoEntity = groupRepository.findByEmail(addMemberRepoDto.getMemberNode().getNodeId())
				.stream()
				.findFirst()
				.orElseThrow(() -> DatabaseAccessException.createNotFound(groupEntityNode.getNodeId()));

		GroupInfoEntity groupInfoEntity = groupRepository.findByEmail(groupEntityNode.getNodeId()).stream()
				.findFirst()
				.orElseThrow(() -> DatabaseAccessException.createNotFound(groupEntityNode.getNodeId()));

		groupRepository.addChildGroupById(groupInfoEntity.getId(), childInfoEntity.getId());
	}
}
