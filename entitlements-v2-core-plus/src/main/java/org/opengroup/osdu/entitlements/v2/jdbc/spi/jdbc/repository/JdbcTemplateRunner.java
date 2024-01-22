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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.jdbc.mapper.GroupInfoEntityListMapper;
import org.opengroup.osdu.entitlements.v2.jdbc.mapper.GroupInfoEntityMapper;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
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

    private final GroupInfoEntityListMapper groupInfoEntityListMapper;
    private final GroupInfoEntityMapper groupInfoEntityMapper;
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


    public List<GroupInfoEntity> getGroupInfoEntitiesRecursive(EntityNode entityNode) {
        String sqlRequest = entityNode.isGroup() ?
            getRecursiveGroupsRequestForGroup() :
            getRecursiveGroupsRequestForMember();

		return jdbcTemplate.query(con -> {
			PreparedStatement ps = con.prepareStatement(sqlRequest);
				ps.setString(1, entityNode.getNodeId());
				ps.setString(2, entityNode.getDataPartitionId());
				return ps;
		}, groupInfoEntityMapper);
	}

    public GroupInfoEntityList getGroupsInPartition(String dataPartitionId, GroupType groupType, Integer offset, Integer limit) {
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("partition", dataPartitionId);
        mapSqlParameterSource.addValue("name_prefix", groupType.toString().toLowerCase() + "%");
        mapSqlParameterSource.addValue("limit", limit);
        mapSqlParameterSource.addValue("from_row", offset);
        return namedParameterJdbcTemplate.queryForObject(getAllGroupsInPartitionRequest(groupType), mapSqlParameterSource, groupInfoEntityListMapper);
    }

    public Set<String> getAffectedMembersForGroup(EntityNode entityNode) {
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("partition", entityNode.getDataPartitionId());
        mapSqlParameterSource.addValue("group_email", entityNode.getNodeId());
        return new HashSet<>(namedParameterJdbcTemplate.queryForList(getAffectedMembersForGroupRecursiveRequest(), mapSqlParameterSource, String.class));
    }

    private String getAllGroupsInPartitionRequest(GroupType groupType) {
        String groupNameFilter = !Objects.equals(groupType, GroupType.NONE) ? "AND name LIKE :name_prefix " : "";
        return "SELECT "
            + "("
						+ "SELECT COUNT(*) FROM \"group\" "
            + "WHERE partition_id = :partition "
						+ groupNameFilter
						+ ") "
            + "as totalCount, "
            + "(SELECT json_agg(t.*) FROM "
            + "(SELECT * FROM \"group\" "
            + "WHERE partition_id = :partition "
            + groupNameFilter
            + "ORDER BY id ASC "
            + "LIMIT :limit "
            + "OFFSET :from_row) "
            + "AS t) "
            + "AS groupInfoEntities";
    }

	private String getRecursiveGroupsRequestForGroup(){
		return "WITH RECURSIVE search_children AS (\n"
				+ "\tSELECT gr.id, gr.email, gr.name, gr.description, gr.partition_id\n"
				+ "\tFROM \"group\" gr, (\n"
				+ "\t\tSELECT embedded_group.parent_id, embedded_group.child_id\n"
				+ "\t\tFROM \"group\" JOIN embedded_group\n"
				+ "\t\tON \"group\".id = embedded_group.parent_id\n"
				+ "\t) as grid\n"
				+ "\tWHERE gr.email = ?\n"
				+ "\t\n"
				+ "\tUNION\n"
				+ "\t\n"
				+ "\tSELECT gr.id, gr.email, gr.name, gr.description, gr.partition_id FROM (\n"
				+ "\t\tSELECT * \n"
				+ "\t\tFROM \"group\" \n"
				+ "\t\tJOIN embedded_group ON embedded_group.parent_id = \"group\".id\n"
				+ "\t)\n"
				+ "\tAS gr, search_children sgr\n"
				+ "\tWHERE sgr.id = gr.child_id\n"
				+ ")\n"
				+ "SELECT \"group\".id, \"group\".name, \"group\".description, \"group\".email, \"group\".partition_id FROM search_children\n"
				+ "JOIN \"group\" ON search_children.id = \"group\".id\n"
				+ "WHERE \"group\".partition_id = ?";
	}

	private String getRecursiveGroupsRequestForMember() {
		return "WITH RECURSIVE search_children AS (\n"
				+ "\t\n"
				+ "\t\tSELECT member_to_group.group_id as id\n"
				+ "\t\tFROM \"member_to_group\"\n"
				+ "\t\tJOIN member ON \"member\".id = member_to_group.member_id\n"
				+ "\t\tWHERE member.email = ?\n"
				+ "\t\n"
				+ "\tUNION\n"
				+ "\t\n"
				+ "\t\tSELECT gr.id FROM (\n"
				+ "\t\t\tSELECT \"embedded_group\".parent_id as id, embedded_group.child_id\n"
				+ "\t\t\tFROM \"embedded_group\" \n"
				+ "\t\t)\n"
				+ "\t\tAS gr, search_children sgr\n"
				+ "\t\tWHERE (gr.child_id = sgr.id)\n"
				+ ")\n"
				+ "\n"
				+ "SELECT \"group\".id, \"group\".name, \"group\".description, \"group\".email, \"group\".partition_id FROM search_children\n"
				+ "JOIN \"group\" ON search_children.id = \"group\".id\n"
				+ "WHERE \"group\".partition_id = ?";
	}

    private String getAffectedMembersForGroupRecursiveRequest() {
        return "SELECT \"member\".email "
                + "FROM \"member\" "
                + "JOIN \"member_to_group\" ON \"member\".id = \"member_to_group\".member_id "
                + "INNER JOIN "
                + "(WITH RECURSIVE search_children AS "
                + "(SELECT embedded_group.child_id AS ID "
                + "FROM embedded_group "
                + "JOIN \"group\" ON \"group\".ID = embedded_group.parent_id"
                + " WHERE \"group\".EMAIL = :group_email "
                + "UNION SELECT gr.ID "
                + "FROM "
                + "(SELECT embedded_group.child_id AS ID, embedded_group.parent_id "
                + "FROM embedded_group) AS gr, "
                + "search_children Sgr "
                + "WHERE (gr.parent_id = Sgr.ID)) "
                + "SELECT  search_children.ID "
                + "FROM  search_children "
                + "JOIN \"group\" ON search_children.ID = \"group\".ID  "
                + "WHERE \"group\".PARTITION_ID = :partition "
                + "UNION SELECT \"group\".id FROM \"group\" "
                + "WHERE \"group\".EMAIL = :group_email) as ref_group ON ref_group.ID = \"member_to_group\".group_id";
    }
}
