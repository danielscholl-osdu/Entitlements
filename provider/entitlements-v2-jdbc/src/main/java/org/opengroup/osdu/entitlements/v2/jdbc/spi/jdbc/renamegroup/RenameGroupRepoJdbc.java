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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.renamegroup;

import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.jdbc.exception.DatabaseAccessException;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.renamegroup.RenameGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.GroupCreationUtil;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RenameGroupRepoJdbc implements RenameGroupRepo {

	private final AuditLogger auditLogger;
	private final GroupRepository groupRepository;

	@Override
	public Set<String> run(EntityNode groupNode, String newGroupName) {
		try {
			executeRenameGroupOperation(groupNode, newGroupName);
			auditLogger.updateGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
			return Collections.emptySet();
		} catch (Exception e) {
			auditLogger.updateGroup(AuditStatus.FAILURE, groupNode.getNodeId());
			throw e;
		}
	}

	private void executeRenameGroupOperation(EntityNode groupNode, String newGroupName) {
		GroupInfoEntity groupInfoEntity = groupRepository.findByEmail(groupNode.getNodeId()).stream()
				.findFirst()
				.orElseThrow(() -> DatabaseAccessException.createNotFound(groupNode.getNodeId()));

		String partitionDomain = groupNode.getNodeId().split("@")[1];
		String newNodeId = GroupCreationUtil.createGroupEmail(newGroupName, partitionDomain);

		groupRepository.update(groupInfoEntity.getId(), newGroupName.toLowerCase(), newNodeId);
	}
}
