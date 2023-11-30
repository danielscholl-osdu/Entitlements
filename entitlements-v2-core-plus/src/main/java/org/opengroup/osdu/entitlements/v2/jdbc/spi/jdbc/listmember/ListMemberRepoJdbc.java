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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.listmember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.jdbc.exception.DatabaseAccessException;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.listmember.ListMemberRepo;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ListMemberRepoJdbc implements ListMemberRepo {
    private final AuditLogger auditLogger;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<ChildrenReference> run(ListMemberServiceDto listMemberServiceDto) {
        try {
            List<ChildrenReference> membersList = executeListGroupOperation(listMemberServiceDto);
            auditLogger.listMember(AuditStatus.SUCCESS, listMemberServiceDto.getGroupId());
            return membersList;
        } catch (Exception e) {
            auditLogger.listMember(AuditStatus.FAILURE, listMemberServiceDto.getGroupId());
            throw e;
        }
    }

    private List<ChildrenReference> executeListGroupOperation(ListMemberServiceDto listMemberServiceDto) {
        GroupInfoEntity groupInfoEntity = groupRepository.findByEmail(listMemberServiceDto.getGroupId()).stream()
                .findFirst()
                .orElseThrow(() -> DatabaseAccessException.createNotFound(listMemberServiceDto.getGroupId()));
        List<MemberInfoEntity> memberInfos = memberRepository.findMembersByGroup(groupInfoEntity.getId());
        List<GroupInfoEntity> groupInfos = groupRepository.findDirectChildren(
                Collections.singletonList(groupInfoEntity.getId()));

        List<ChildrenReference> result = new ArrayList<>();
        result.addAll(memberInfos.stream().map(MemberInfoEntity::toChildrenReference).collect(Collectors.toList()));
        result.addAll(groupInfos.stream().map(GroupInfoEntity::toChildrenReference).collect(Collectors.toList()));

        return result;
    }
}
