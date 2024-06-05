/*
 *  Copyright 2024 Google LLC
 *  Copyright 2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.retrievegroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getCommonGroup;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getMemberNode;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.entitlements.v2.jdbc.JdbcAppProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.DbTestConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = DbTestConfig.class)
@AutoConfigureTestDatabase
@Sql(value = {"/sql/schema.sql", "/sql/retrieveGroupRepoJdbcTestData.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/drop_schema.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@ExtendWith(SpringExtension.class)
class RetrieveGroupRepoJdbcDdTest {

  private static final int FOUND_GROUPS_COUNT = 3;
  private static final String ROLE_OWNER = "OWNER";
  private static final String ROLE_MEMBER = "MEMBER";
  private static final String DIRECT_OWNER_GROUP_ID = "group1@dp.group.com";
  private static final String TRANSITIVE_MEMBER_GROUP_ID = "group2@dp.group.com";
  private static final String DIRECT_MEMBER_GROUP_ID = "group4@dp.group.com";
  private static final String TOP_GROUP_GROUP_ID = "group1@dp.group.com";
  private static final String MIDDLE_MEMBER_GROUP_ID = "group2@dp.group.com";
  private static final String USER_EXISTS_NAME = "user1";
  private static final String USER_ABSENT_NAME = "member";
  private static final String GROUP_EXISTS_NAME = "group4";

  @MockBean
  private GroupRepository groupRepository;
  @MockBean
  private JdbcAppProperties jdbcAppProperties;
  @MockBean
  private MemberRepository memberRepository;
  @MockBean
  private JdbcTemplateRunner jdbcTemplateRunner;

  @Autowired
  private RetrieveGroupRepoJdbc sut;

  @Test
  void shouldReturnAllParentsForUserRoleNotRequested() {
    EntityNode member = getMemberNode(USER_EXISTS_NAME);

    ParentTreeDto res = sut.loadAllParents(member);

    assertEquals(FOUND_GROUPS_COUNT, res.getParentReferences().size());

    res.getParentReferences().forEach(
        p -> assertNull(p.getRole())
    );
    Map<String, ParentReference> resMap = res.getParentReferences().stream()
        .collect(Collectors.toMap(ParentReference::getId, p -> p));
    assertTrue(resMap.containsKey(DIRECT_OWNER_GROUP_ID));
    assertTrue(resMap.containsKey(TRANSITIVE_MEMBER_GROUP_ID));
    assertTrue(resMap.containsKey(DIRECT_MEMBER_GROUP_ID));
  }

  @Test
  void shouldReturnAllParentsForUserRoleRequired() {
    EntityNode member = getMemberNode(USER_EXISTS_NAME);

    ParentTreeDto res = sut.loadAllParents(member, true);

    assertEquals(FOUND_GROUPS_COUNT, res.getParentReferences().size());

    Map<String, ParentReference> resMap = res.getParentReferences().stream()
        .collect(Collectors.toMap(ParentReference::getId, p -> p));
    assertEquals(ROLE_OWNER, resMap.get(DIRECT_OWNER_GROUP_ID).getRole());
    assertEquals(ROLE_MEMBER, resMap.get(TRANSITIVE_MEMBER_GROUP_ID).getRole());
    assertEquals(ROLE_MEMBER, resMap.get(DIRECT_MEMBER_GROUP_ID).getRole());
  }

  @Test
  void shouldReturnAllParentsForGroupRoleNotRequested() {
    EntityNode commonGroup = getCommonGroup(GROUP_EXISTS_NAME);

    ParentTreeDto res = sut.loadAllParents(commonGroup);

    assertEquals(FOUND_GROUPS_COUNT, res.getParentReferences().size());

    res.getParentReferences().forEach(
        p -> assertNull(p.getRole())
    );
    Set<String> resSet = res.getParentReferences().stream()
        .map(ParentReference::getId)
        .collect(Collectors.toSet());
    assertTrue(resSet.contains(TOP_GROUP_GROUP_ID));
    assertTrue(resSet.contains(MIDDLE_MEMBER_GROUP_ID));
    assertTrue(resSet.contains(commonGroup.getNodeId()));
  }

  @Test
  void shouldReturnAllParentsForGroupRoleRequired() {
    EntityNode commonGroup = getCommonGroup(GROUP_EXISTS_NAME);

    ParentTreeDto res = sut.loadAllParents(commonGroup, true);

    assertEquals(FOUND_GROUPS_COUNT, res.getParentReferences().size());

    res.getParentReferences().forEach(
        p -> assertEquals(ROLE_MEMBER, p.getRole())
    );
    Set<String> resSet = res.getParentReferences().stream()
        .map(ParentReference::getId)
        .collect(Collectors.toSet());
    assertTrue(resSet.contains(TOP_GROUP_GROUP_ID));
    assertTrue(resSet.contains(MIDDLE_MEMBER_GROUP_ID));
    assertTrue(resSet.contains(commonGroup.getNodeId()));
  }



  @Test
  void shouldReturnEmptySetIfNoParentsWhenLoadAllParents() {
    EntityNode member = getMemberNode(USER_ABSENT_NAME);

    jdbcTemplateRunner.saveMemberInfoEntity(MemberInfoEntity.fromEntityNode(member, Role.MEMBER));

    ParentTreeDto parents = sut.loadAllParents(member);
    assertEquals(0, parents.getParentReferences().size());
  }

}
