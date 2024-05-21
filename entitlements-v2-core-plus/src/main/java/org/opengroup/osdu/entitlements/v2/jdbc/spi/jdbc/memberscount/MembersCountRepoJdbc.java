/*
 * Copyright 2024 Google LLC
 * Copyright 2024 EPAM Systems, Inc
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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.memberscount;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.memberscount.MembersCountRepo;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
@Primary
public class MembersCountRepoJdbc implements MembersCountRepo {

  public static final int ZERO_AS_GROUP_ALWAYS_A_MEMBER = 0;
  private final GroupRepository groupRepository;

  @Override
  public MembersCountResponseDto getMembersCount(MembersCountServiceDto dto) {

    int usersCount = groupRepository.countUsers(dto.getPartitionId(), dto.getGroupId(),
        getSearchRoles(dto.getRole()));
    int subGroupsCount = isOwner(dto.getRole())
        ? ZERO_AS_GROUP_ALWAYS_A_MEMBER
        : groupRepository.countSubGroups(dto.getPartitionId(), dto.getGroupId());

    return MembersCountResponseDto.builder()
        .membersCount(usersCount + subGroupsCount)
        .groupEmail(dto.getGroupId())
        .build();
  }

  private static boolean isOwner(Role role) {
    return Role.OWNER.equals(role);
  }

  private static List<String> getSearchRoles(Role role) {
    if (isNull(role)) {
      return List.of(Role.MEMBER.toString(), Role.OWNER.toString());
    }
    return List.of(role.toString());
  }
}
