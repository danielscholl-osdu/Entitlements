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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.retrievegroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.SpiJdbcTestConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.*;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getUsersGroupNode;
import static org.powermock.api.mockito.PowerMockito.when;

@SpringBootTest(classes = SpiJdbcTestConfig.class)
@ExtendWith(SpringExtension.class)
class RetrieveGroupRepoJdbcTest {

  @MockBean
  protected AuditLogger auditLogger;

  @Autowired
  private RetrieveGroupRepoJdbc sut;
  @MockBean
  private GroupRepository groupRepository;
  @MockBean
  private MemberRepository memberRepository;
  @MockBean
  private JdbcTemplateRunner jdbcTemplateRunner;

  @Test
  void shouldThrow404IfGroupDoesNotExist() {
    try {
      sut.groupExistenceValidation("users.test@group.com", "dp");
      fail("Should throw exception");
    } catch (AppException ex) {
      assertEquals(404, ex.getError().getCode());
    } catch (Exception ex) {
      fail(String.format("Should not throw exception: %s", ex.getMessage()));
    }
  }

  @Test
  void shouldReturnEmptyListIfNotParentsWhenLoadDirectParents() {
    EntityNode member = getMemberNode("member");

    jdbcTemplateRunner.saveMemberInfoEntity(MemberInfoEntity.fromEntityNode(member, Role.MEMBER));

    List<ParentReference> parents = sut.loadDirectParents(DATA_PARTITION_ID, member.getNodeId());
    assertEquals(0, parents.size());
  }


  @Test
  void shouldReturnDirectParentListWhenLoadDirectParents() {
    EntityNode member = getMemberNode("member");
    EntityNode group1 = getUsersGroupNode("x");
    EntityNode group2 = getUsersGroupNode("y");
    EntityNode group3 = getUsersGroupNode("z");

    GroupInfoEntity savedGroup1 = GroupInfoEntity.fromEntityNode(group1);
    GroupInfoEntity savedGroup2 = GroupInfoEntity.fromEntityNode(group2);
    GroupInfoEntity savedGroup3 = GroupInfoEntity.fromEntityNode(group3);

    when(memberRepository.findByEmail(any())).thenReturn(
        Collections.singletonList(MemberInfoEntity.fromEntityNode(member, Role.MEMBER)));
    when(groupRepository.findDirectGroups(anyList())).thenReturn(
        Arrays.asList(savedGroup1, savedGroup3));

    List<String> parentIds = sut.loadDirectParents(DATA_PARTITION_ID, member.getNodeId()).stream()
        .map(ParentReference::getId)
        .collect(Collectors.toList());
    assertEquals(2, parentIds.size());

    assertTrue(parentIds.contains(group1.getNodeId()));
    assertTrue(parentIds.contains(group3.getNodeId()));

    assertFalse(parentIds.contains(group2.getNodeId()));
  }

  @Test
  void shouldReturnEmptySetIfNoParentsWhenLoadAllParents() {
    EntityNode member = getMemberNode("member");

    jdbcTemplateRunner.saveMemberInfoEntity(MemberInfoEntity.fromEntityNode(member, Role.MEMBER));

    ParentTreeDto parents = sut.loadAllParents(member);
    assertEquals(0, parents.getParentReferences().size());
  }

  @Test
  void shouldReturnAllParentsAndMaxDepthWhenLoadAllParents() {
    EntityNode member = getMemberNode("member");
    EntityNode group1 = getUsersGroupNode("x");
    EntityNode group2 = getUsersGroupNode("y");
    EntityNode group3 = getUsersGroupNode("z");

    GroupInfoEntity savedGroup1 = GroupInfoEntity.fromEntityNode(group1);
    GroupInfoEntity savedGroup2 = GroupInfoEntity.fromEntityNode(group2);
    GroupInfoEntity savedGroup3 = GroupInfoEntity.fromEntityNode(group3);

    when(jdbcTemplateRunner.getGroupInfoEntitiesRecursive(any())).thenReturn(
        Arrays.asList(savedGroup1, savedGroup2, savedGroup3));

    Set<String> parentIds = sut.loadAllParents(member).getParentReferences().stream()
        .map(ParentReference::getId)
        .collect(Collectors.toSet());
    assertEquals(3, parentIds.size());

    assertTrue(parentIds.contains(group1.getNodeId()));
    assertTrue(parentIds.contains(group2.getNodeId()));
    assertTrue(parentIds.contains(group3.getNodeId()));

  }

  @Test
  void shouldReturnFalseWhenNoDirectChildInTenant() {
    EntityNode groupNode = getUsersGroupNode("x", "tenant-1");
    ChildrenReference childrenReference = getUserChildrenReference("test@email.com", "tenant-2");
    GroupInfoEntity savedGroup = GroupInfoEntity.fromEntityNode(groupNode);

    when(groupRepository.findByEmail(groupNode.getNodeId())).thenReturn(
        Collections.singletonList(savedGroup));
    when(memberRepository.findMemberByEmailInGroup(savedGroup.getId(),
        childrenReference.getId())).thenReturn(Collections.emptyList());

    assertFalse(sut.hasDirectChild(groupNode, childrenReference));
  }
}
