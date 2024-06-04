/*
 * Copyright 2021-2024 Google LLC
 * Copyright 2021-2024 EPAM Systems, Inc
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

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.jdbc.mapper.GroupInfoEntityListMapper;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntityList;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcTemplateRunner {

  private static final String QUERY_AFFECTED_MEMBERS_FOR_GROUP = """
      SELECT "member".email
      FROM "member"
      JOIN "member_to_group" ON "member".id = "member_to_group".member_id
      INNER JOIN (
        WITH RECURSIVE search_children AS (
          SELECT embedded_group.child_id AS ID
          FROM embedded_group
          JOIN "group" ON "group".ID = embedded_group.parent_id
          WHERE "group".EMAIL = :group_email

          UNION
          SELECT gr.ID
          FROM (
              SELECT embedded_group.child_id AS ID, embedded_group.parent_id
              FROM embedded_group
            ) AS gr,
            search_children Sgr
          WHERE (gr.parent_id = Sgr.ID)
        )
      
        SELECT  search_children.ID
        FROM  search_children
        JOIN "group" ON search_children.ID = "group".ID
        WHERE "group".PARTITION_ID = :partition
        UNION
        SELECT "group".id FROM "group"
        WHERE "group".EMAIL = :group_email
      ) as ref_group ON ref_group.ID = "member_to_group".group_id""";


  private static final String PARAMETER_PARTITION = "partition";
  private static final String PARAMETER_NAME_PREFIX = "name_prefix";
  private static final String PARAMETER_LIMIT = "limit";
  private static final String PARAMETER_FROM_ROW = "from_row";
  private static final String PARAMETER_GROUP_EMAIL = "group_email";

  private final GroupInfoEntityListMapper groupInfoEntityListMapper;
  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  public Long saveMemberInfoEntity(MemberInfoEntity memberInfoEntity) {
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(
          "INSERT INTO member(email, partition_id) VALUES (?, ?) RETURNING id",
          Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, memberInfoEntity.getEmail());
      ps.setString(2, memberInfoEntity.getPartitionId());
      return ps;
    }, keyHolder);

    return (long) keyHolder.getKey();
  }


  public GroupInfoEntityList getGroupsInPartition(String dataPartitionId, GroupType groupType,
      Integer offset, Integer limit) {
    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    mapSqlParameterSource.addValue(PARAMETER_PARTITION, dataPartitionId);
    mapSqlParameterSource.addValue(PARAMETER_NAME_PREFIX, groupType.toString().toLowerCase() + "%");
    mapSqlParameterSource.addValue(PARAMETER_LIMIT, limit);
    mapSqlParameterSource.addValue(PARAMETER_FROM_ROW, offset);
    return namedParameterJdbcTemplate.queryForObject(getAllGroupsInPartitionRequest(groupType),
        mapSqlParameterSource, groupInfoEntityListMapper);
  }

  public Set<String> getAffectedMembersForGroup(EntityNode entityNode) {
    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    mapSqlParameterSource.addValue(PARAMETER_PARTITION, entityNode.getDataPartitionId());
    mapSqlParameterSource.addValue(PARAMETER_GROUP_EMAIL, entityNode.getNodeId());
    return new HashSet<>(
        namedParameterJdbcTemplate.queryForList(QUERY_AFFECTED_MEMBERS_FOR_GROUP,
            mapSqlParameterSource, String.class));
  }

  private String getAllGroupsInPartitionRequest(GroupType groupType) {
    String groupNameFilter =
        !Objects.equals(groupType, GroupType.NONE) ? " AND name LIKE :name_prefix " : "";
    return """
        SELECT (
          SELECT COUNT(*) FROM "group"
          WHERE partition_id = :partition""" + groupNameFilter + """
        ) as totalCount,
        (
          SELECT json_agg(t.*)
          FROM (
            SELECT *
            FROM "group"
            WHERE partition_id = :partition""" + groupNameFilter + """
            ORDER BY id ASC
            LIMIT :limit
            OFFSET :from_row
          ) AS t
        ) AS groupInfoEntities""";
  }

}
