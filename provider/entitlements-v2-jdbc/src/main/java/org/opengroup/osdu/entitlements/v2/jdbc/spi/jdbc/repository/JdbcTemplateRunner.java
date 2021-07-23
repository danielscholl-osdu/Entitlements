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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcTemplateRunner {

	private final JdbcTemplate jdbcTemplate;

	public Long saveMemberInfoEntity(MemberInfoEntity memberInfoEntity){
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



	public List<Long> getRecursiveParentIds(EntityNode entityNode){
		String sqlRequest = entityNode.isGroup() ?
				getRecursiveGroupsRequestForGroup():
				getRecursiveGroupsRequestForMember();

		return jdbcTemplate.queryForList(sqlRequest,Long.class, entityNode.getNodeId());
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
				+ "SELECT id FROM search_children\n"
				+ "ORDER BY id";
	}

	private String getRecursiveGroupsRequestForMember(){
		return "WITH RECURSIVE search_children AS (\n"
				+ "\tSELECT \n"
				+ "\t\tmember_groups.id, member_groups.email, member_groups.name, member_groups.description, member_groups.partition_id, member_groups.member_id\nFROM (\n"
				+ "\t\tSELECT \"group\".*, member_to_group.member_id, \"member\".email as \"member_email\"\n"
				+ "\t\tFROM \"member\" \n"
				+ "\t\tJOIN member_to_group ON \"member\".id = member_to_group.member_id\n"
				+ "\t\tJOIN \"group\" ON member_to_group.group_id = \"group\".id\n"
				+ "\t) as member_groups\n"
				+ "\tWHERE member_groups.member_email = ?\n"
				+ "\t\n"
				+ "\tUNION\n"
				+ "\t\n"
				+ "\tSELECT gr.id, gr.email, gr.name, gr.description, gr.partition_id, sgr.member_id FROM (\n"
				+ "\t\tSELECT * \n"
				+ "\t\tFROM \"group\" \n"
				+ "\t\tJOIN embedded_group ON embedded_group.parent_id = \"group\".id\n"
				+ "\t\tJOIN member_to_group ON embedded_group.child_id = member_to_group.group_id\n"
				+ "\t)\n"
				+ "\tAS gr, search_children sgr\n"
				+ "\tWHERE (gr.child_id = sgr.id)\n"
				+ ")\n"
				+ "\n"
				+ "SELECT id FROM search_children\n"
				+ "ORDER BY id";
	}
}
