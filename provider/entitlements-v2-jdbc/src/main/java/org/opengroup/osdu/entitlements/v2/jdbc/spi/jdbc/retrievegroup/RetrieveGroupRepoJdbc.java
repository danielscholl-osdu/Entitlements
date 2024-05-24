/*
 * Copyright 2020-2024 Google LLC
 * Copyright 2020-2024 EPAM Systems, Inc
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.JdbcAppProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.exception.DatabaseAccessException;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntityList;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ChildrenTreeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupsOfPartitionDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RetrieveGroupRepoJdbc implements RetrieveGroupRepo {

  public static final String QUERY_GROUP_SEARCH_FOR_GROUP = """
      WITH RECURSIVE search_children (id) AS (
           SELECT gr.id, gr.email, gr.name, gr.description, gr.partition_id
           FROM "group" gr,
             (
               SELECT embedded_group.parent_id, embedded_group.child_id
               FROM "group"
               JOIN embedded_group ON "group".id = embedded_group.parent_id
             ) as grid
           WHERE gr.email = :group_email

           UNION

           SELECT gr.id, gr.email, gr.name, gr.description, gr.partition_id
           FROM (
               SELECT *
               FROM "group"
               JOIN embedded_group ON embedded_group.parent_id = "group".id
             ) AS gr
           JOIN  search_children sgr ON sgr.id = gr.child_id
      )
      SELECT g.id, g.name, g.description, g.email, g.partition_id, 'MEMBER' AS role
      FROM search_children
      JOIN "group" AS g ON search_children.id = g.id
      WHERE g.partition_id = :partition""";

  public static final String QUERY_GROUP_SEARCH_FOR_MEMBER = """
      WITH RECURSIVE search_children (id) AS (
        SELECT member_to_group.group_id AS id
        FROM member_to_group
        JOIN member ON member.id = member_to_group.member_id
        WHERE member.email = :member_email

        UNION

        SELECT gr.id
        FROM (
          SELECT embedded_group.parent_id as id, embedded_group.child_id
          FROM embedded_group
        ) AS gr
        JOIN search_children AS sgr ON sgr.id = gr.child_id
      )
      SELECT g.id, g.name, g.description, g.email, g.partition_id,
        CASE WHEN member_roles.role IS NULL THEN 'MEMBER' ELSE member_roles.role END AS role
      FROM search_children AS sch
      JOIN "group" AS g ON sch.id = g.id
      LEFT JOIN (
        SELECT mg.group_id, mg.role
        FROM member_to_group AS mg
        JOIN member ON member.id = mg.member_id
        WHERE member.email = :member_email
      ) AS member_roles ON member_roles.group_id = g.id
      WHERE g.partition_id = :partition""";

  private static final String PARAMETER_PARTITION = "partition";
  private static final String PARAMETER_GROUP_EMAIL = "group_email";
  private static final String PARAMETER_MEMBER_EMAIL = "member_email";

  private final GroupRepository groupRepository;
  private final MemberRepository memberRepository;
  private final JdbcTemplateRunner jdbcTemplateRunner;
  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private final JaxRsDpsLog log;
  private final JdbcAppProperties config;

  @Override
  public EntityNode groupExistenceValidation(String groupId, String partitionId) {
    Optional<EntityNode> groupNode = getEntityNode(groupId, partitionId);
    return groupNode.orElseThrow(() -> {
      log.debug(String.format("Can't find group %s", groupId));
      return new DatabaseAccessException(
          HttpStatus.NOT_FOUND,
          String.format("Group %s is not found", groupId));
    });
  }

  @Override
  public Optional<EntityNode> getEntityNode(String entityEmail, String partitionId) {
    Optional<GroupInfoEntity> groupInfoEntity = groupRepository.findByEmail(entityEmail).stream()
        .findFirst();

    if (groupInfoEntity.isPresent()) {
      return Optional.of(groupInfoEntity.get().toEntityNode());
    } else {
      log.warning("Could not find the group with email: " + entityEmail);
      return Optional.empty();
    }
  }

  @Override
  public EntityNode getMemberNodeForRemovalFromGroup(String memberId, String partitionId) {
    if (!memberId.endsWith(String.format("@%s.%s", partitionId, config.getDomain()))) {
      return EntityNode.createMemberNodeForNewUser(memberId, partitionId);
    }
    return EntityNode.createNodeFromGroupEmail(memberId);
  }

  //Left without implementation as not necessary for provider
  @Override
  public Set<EntityNode> getEntityNodes(String partitionId, List<String> nodeIds) {
    return Collections.emptySet();
  }

  //Left without implementation as not necessary for provider
  @Override
  public Map<String, Set<String>> getUserPartitionAssociations(Set<String> userIds) {
    return Collections.emptyMap();
  }

  //Left without implementation as not necessary for provider
  @Override
  public Set<EntityNode> getAllGroupNodes(String partitionId, String partitionGroupId) {
    return Collections.emptySet();
  }

  @Override
  public Boolean hasDirectChild(EntityNode groupNode, ChildrenReference childrenReference) {
    GroupInfoEntity parent = groupRepository.findByEmail(groupNode.getNodeId()).stream()
        .findFirst()
        .orElseThrow(() -> DatabaseAccessException.createNotFound(groupNode.getNodeId()));

    return childrenReference.isGroup() ?
        hasChildGroupInGroup(parent, childrenReference) :
        hasMemberInGroup(parent, childrenReference);
  }

  @Override
  public List<ParentReference> loadDirectParents(String partitionId, String... nodeId) {
    //The method is used in JDBC module only for members and does not provide a solution
    // to identify the type of the node.
    //Should be reworked later
    List<Long> childrenIds = memberRepository.findByEmail(nodeId[0]).stream()
        .map(MemberInfoEntity::getId)
        .toList();

    return groupRepository.findDirectGroups(childrenIds).stream()
        .map(GroupInfoEntity::toParentReference)
        .toList();
  }

  @Override
  public ParentTreeDto loadAllParents(EntityNode memberNode) {
    return loadAllParents(memberNode, false);
  }

  @Override
  public ParentTreeDto loadAllParents(EntityNode memberNode, Boolean roleRequired) {
    MapSqlParameterSource mapParameter = new MapSqlParameterSource();
    mapParameter.addValue(PARAMETER_MEMBER_EMAIL, memberNode.getNodeId());
    mapParameter.addValue(PARAMETER_GROUP_EMAIL, memberNode.getNodeId());
    mapParameter.addValue(PARAMETER_PARTITION, memberNode.getDataPartitionId());

    String sqlRequest = memberNode.isGroup()
        ? QUERY_GROUP_SEARCH_FOR_GROUP
        : QUERY_GROUP_SEARCH_FOR_MEMBER;

    List<ParentReference> parents = namedParameterJdbcTemplate
        .query(sqlRequest, mapParameter, new ParentReferenceMapper(roleRequired));
    return ParentTreeDto.builder()
        .parentReferences(new HashSet<>(parents))
        .build();
  }

  @Override
  public List<ChildrenReference> loadDirectChildren(String partitionId, String... nodeId) {
    //The method is not used in JDBC module and does not provide a solution
    // to identify the type of the node.
    //Should be reworked later
    List<Long> parentIds = groupRepository.findByEmail(nodeId[0]).stream()
        .map(GroupInfoEntity::getId)
        .toList();

    List<ChildrenReference> children = groupRepository.findDirectChildren(parentIds).stream()
        .map(GroupInfoEntity::toChildrenReference)
        .collect(Collectors.toList());
    List<ChildrenReference> members = parentIds.stream()
        .flatMap(id -> memberRepository.findMembersByGroup(id).stream())
        .map(MemberInfoEntity::toChildrenReference)
        .toList();

    children.addAll(members);
    return children;
  }

  @Override
  public ChildrenTreeDto loadAllChildrenUsers(EntityNode node) {
    return ChildrenTreeDto.builder().childrenUserIds(Collections.emptyList()).build();
  }

  @Override
  public Set<ParentReference> filterParentsByAppId(Set<ParentReference> parentReferences,
      String partitionId, String appId) {
    return parentReferences.stream()
        .filter(pr -> pr.getAppIds().isEmpty() || pr.getAppIds().contains(appId))
        .collect(Collectors.toSet());
  }

  //Left without implementation as not necessary for provider
  @Override
  public Set<String> getGroupOwners(String partitionId, String nodeId) {
    return Collections.emptySet();
  }

  //Left without implementation as not necessary for provider
  @Override
  public Map<String, Integer> getAssociationCount(List<String> userIds) {
    return Collections.emptyMap();
  }

  //Left without implementation as not necessary for provider
  @Override
  public Map<String, Integer> getAllUserPartitionAssociations() {
    return Collections.emptyMap();
  }

  @Override
  public ListGroupsOfPartitionDto getGroupsInPartition(String dataPartitionId, GroupType groupType,
      String cursor, Integer limit) {
    int offsetValue = 0;
    if (Objects.nonNull(cursor) && !cursor.isEmpty()) {
      try {
        offsetValue = Integer.parseInt(cursor);
      } catch (NumberFormatException e) {
        throw new AppException(HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(), "Malformed cursor, must be integer value");
      }
    }
    GroupInfoEntityList groupsByPartition = jdbcTemplateRunner.getGroupsInPartition(dataPartitionId,
        groupType, offsetValue, limit);
    List<GroupInfoEntity> groupInfoEntities = groupsByPartition.getGroupInfoEntities();
    List<ParentReference> parentReferences = groupInfoEntities.stream()
        .map(GroupInfoEntity::toParentReference)
        .toList();

    return ListGroupsOfPartitionDto.builder()
        .groups(parentReferences)
        .totalCount(groupsByPartition.getTotalCount())
        .cursor(String.valueOf(offsetValue + limit))
        .build();
  }

  private boolean hasMemberInGroup(GroupInfoEntity parent, ChildrenReference childrenReference) {
    return !memberRepository.findMemberByEmailInGroup(parent.getId(), childrenReference.getId())
        .isEmpty();
  }

  private boolean hasChildGroupInGroup(GroupInfoEntity parent,
      ChildrenReference childrenReference) {
    return !groupRepository.findChildByEmail(parent.getId(), childrenReference.getId()).isEmpty();
  }

  @AllArgsConstructor
  private static class ParentReferenceMapper implements RowMapper<ParentReference> {

    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_PARTITION_ID = "partition_id";
    private static final String COLUMN_ROLE = "role";
    public static final String EMPTY = "";

    private boolean roleRequired;

    @Override
    public ParentReference mapRow(ResultSet rs, int rowNum) throws SQLException {
      return ParentReference.builder()
          .id(rs.getString(COLUMN_EMAIL))
          .name(rs.getString(COLUMN_NAME).toLowerCase())
          .description(rs.getString(COLUMN_DESCRIPTION))
          .dataPartitionId(rs.getString(COLUMN_PARTITION_ID))
          .role(roleRequired ? rs.getString(COLUMN_ROLE) : EMPTY)
          .build();
    }
  }
}
