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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.removemember;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.exception.DatabaseAccessException;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.removemember.RemoveMemberRepo;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RemoveMemberRepoJdbc implements RemoveMemberRepo {

    private final AuditLogger auditLogger;
    @Lazy
    private final RequestInfo requestInfo;

    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;

    @Override
    public Set<String> removeMember(EntityNode groupNode, EntityNode memberNode, RemoveMemberServiceDto removeMemberServiceDto) {
        try {
            executeRemoveMemberOperation(groupNode, memberNode);
            auditLogger.removeMember(AuditStatus.SUCCESS, groupNode.getNodeId(), memberNode.getNodeId(), removeMemberServiceDto.getRequesterId());
            return Collections.emptySet();
        } catch (Exception e) {
            auditLogger.removeMember(AuditStatus.FAILURE, groupNode.getNodeId(), memberNode.getNodeId(), removeMemberServiceDto.getRequesterId());
            throw e;
        }
    }

    private void executeRemoveMemberOperation(EntityNode groupNode, EntityNode memberNode) {
        if (memberNode.isGroup()){
            executeRemoveChildGroupOperation(groupNode, memberNode);
        } else {
            executeRemoveMemberFromGroupOperation(groupNode, memberNode);
        }
    }

    private void executeRemoveMemberFromGroupOperation(EntityNode groupNode,
            EntityNode memberNode) {
        MemberInfoEntity memberInfoEntity = memberRepository.findByEmail(memberNode.getNodeId()).stream()
                .findFirst()
                .orElseThrow(() -> DatabaseAccessException.createNotFound(groupNode.getNodeId()));
        GroupInfoEntity groupInfoEntity = groupRepository.findByEmail(groupNode.getNodeId()).stream()
                .findFirst()
                .orElseThrow(() -> DatabaseAccessException.createNotFound(groupNode.getNodeId()));

        groupRepository.removeMemberById(groupInfoEntity.getId(), memberInfoEntity.getId());

        List<GroupInfoEntity> remainingGroups = groupRepository.findDirectGroups(
                Collections.singletonList(memberInfoEntity.getId()));

        if (remainingGroups.isEmpty()){
            memberRepository.deleteById(memberInfoEntity.getId());
        }
    }

    private void executeRemoveChildGroupOperation(EntityNode groupNode, EntityNode memberNode) {
        GroupInfoEntity childInfoEntity = groupRepository.findByEmail(memberNode.getNodeId()).stream()
                .findFirst()
                .orElseThrow(() -> DatabaseAccessException.createNotFound(memberNode.getNodeId()));
        GroupInfoEntity parentInfoEntity = groupRepository.findByEmail(groupNode.getNodeId()).stream()
                .findFirst()
                .orElseThrow(() -> DatabaseAccessException.createNotFound(groupNode.getNodeId()));

        groupRepository.removeChildById(parentInfoEntity.getId(), childInfoEntity.getId());
    }
}
