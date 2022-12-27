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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.deletegroup;

import java.util.Collections;
import java.util.Deque;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.jdbc.exception.DatabaseAccessException;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DeleteGroupRepoJdbc implements DeleteGroupRepo {

    private final AuditLogger auditLogger;
    private final GroupRepository groupRepository;

    private final JdbcTemplateRunner jdbcTemplateRunner;

    @Override
    public Set<String> deleteGroup(final EntityNode groupNode) {
        try {
            Set<String> affectedMembers = jdbcTemplateRunner.getAffectedMembersForGroup(groupNode);
            executeDeleteGroupOperation(groupNode);
            auditLogger.deleteGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
            return affectedMembers;
        } catch (Exception e) {
            auditLogger.deleteGroup(AuditStatus.FAILURE, groupNode.getNodeId());
            throw e;
        }
    }

    @Override
    public Set<String> deleteGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode) {
        return Collections.emptySet();
    }

    private void executeDeleteGroupOperation(final EntityNode groupNode) {
        GroupInfoEntity groupInfoEntity = groupRepository.findByEmail(groupNode.getNodeId()).stream()
                .findFirst()
                .orElseThrow(() -> DatabaseAccessException.createNotFound(groupNode.getNodeId()));
        groupRepository.deleteMemberRelationsById(groupInfoEntity.getId());
        groupRepository.deleteRelationsById(groupInfoEntity.getId());
        groupRepository.delete(groupInfoEntity);
    }
}
