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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.authenticator.IAuthenticator;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@AutoConfigureTestDatabase
@ExtendWith(SpringExtension.class)
@Sql(value = {"/sql/schema.sql", "/sql/retrieveGroupRepoTestData.sql"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class GroupRepositoryTest {

  private static final String PARTITION_ID = "partition_1";
  private static final String OTHER_PARTITION_ID = "partition_2";
  private static final String GROUP_EMAIL_MIXED_MEMBERS = "first_group@domen.com";
  private static final int COUNT_USERS = 3;
  private static final int COUNT_USERS_MEMBER = 2;
  private static final int COUNT_SUBGROUPS = 1;

  @MockBean
  IPartitionProvider iPartitionProvider;
  @MockBean
  IAuthenticator iAuthenticator;

  @Autowired
  private GroupRepository groupRepository;

  @Test
  @DisplayName("Do not count users from other partition")
  void shouldNotCountWrongPartitionUsers() {
    List<String> allRoles = List.of(Role.MEMBER.toString(), Role.OWNER.toString());
    assertEquals(0,
        groupRepository.countUsers(OTHER_PARTITION_ID, GROUP_EMAIL_MIXED_MEMBERS, allRoles));
  }

  @Test
  @DisplayName("Count users without role filtration.")
  void shouldCountUsers() {
    List<String> allRoles = List.of(Role.MEMBER.toString(), Role.OWNER.toString());
    assertEquals(COUNT_USERS,
        groupRepository.countUsers(PARTITION_ID, GROUP_EMAIL_MIXED_MEMBERS, allRoles));
  }

  @Test
  @DisplayName("Count users with role filtration.")
  void shouldCountUsersWithRole() {
    List<String> onlyMembersList = List.of(Role.MEMBER.toString());
    assertEquals(COUNT_USERS_MEMBER,
        groupRepository.countUsers(PARTITION_ID, GROUP_EMAIL_MIXED_MEMBERS, onlyMembersList));
  }

  @Test
  @DisplayName("Count subgroups.")
  void shouldCountSubGroup() {
    assertEquals(COUNT_SUBGROUPS,
        groupRepository.countSubGroups(PARTITION_ID, GROUP_EMAIL_MIXED_MEMBERS));
  }
}
