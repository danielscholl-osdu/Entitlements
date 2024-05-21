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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.SpiJdbcTestConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = SpiJdbcTestConfig.class)
@ExtendWith(SpringExtension.class)
class MembersCountRepoJdbcTest {

  public static final String PARTITION_ID = "partitionId";
  public static final String GROUP_EMAIL = "first_group@domen.com";
  public static final int USERS_COUNT = 2;
  public static final int SUB_GROUPS_COUNT = 1;
  public static final int TOTAL_MEMBERS_COUNT = 3;
  public static final Role NO_ROLE = null;

  @MockBean
  private GroupRepository groupRepository;
  @Autowired
  private MembersCountRepoJdbc service;

  @ParameterizedTest
  @MethodSource
  void countMembers(int expectedCount, List<String> mockRoles, Role requestRole) {
    MembersCountServiceDto dto = MembersCountServiceDto.builder()
        .partitionId(PARTITION_ID)
        .groupId(GROUP_EMAIL)
        .role(requestRole)
        .build();

    when(groupRepository.countUsers(eq(PARTITION_ID), eq(GROUP_EMAIL), eq(mockRoles)))
        .thenReturn(USERS_COUNT);
    when(groupRepository.countSubGroups(eq(PARTITION_ID), eq(GROUP_EMAIL)))
        .thenReturn(SUB_GROUPS_COUNT);

    MembersCountResponseDto membersCountResponseDto = service.getMembersCount(dto);
    assertEquals(GROUP_EMAIL, membersCountResponseDto.getGroupEmail());
    assertEquals(expectedCount, membersCountResponseDto.getMembersCount());
  }

  private static Stream<Arguments> countMembers() {
    return Stream.of(
        Arguments.of(
            Named.of("Count members - users and subgroups regardless of role", TOTAL_MEMBERS_COUNT),
            List.of(Role.MEMBER.toString(), Role.OWNER.toString()),
            NO_ROLE),
        Arguments.of(
            Named.of("Count members - users and subgroups with MEMBER role", TOTAL_MEMBERS_COUNT),
            List.of(Role.MEMBER.toString()),
            Role.MEMBER),
        Arguments.of(Named.of("Count members - users and subgroups with OWNER role", USERS_COUNT),
            List.of(Role.OWNER.toString()),
            Role.OWNER));

  }

}