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

package org.opengroup.osdu.entitlements.v2.jdbc.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.springframework.jdbc.core.RowMapper;

public class MemberInfoEntityNullRoleMapper implements RowMapper<MemberInfoEntity> {

	@Override
	public MemberInfoEntity mapRow(ResultSet rs, int i) throws SQLException {
		return MemberInfoEntity.builder()
				.id(rs.getLong("id"))
				.email(rs.getString("email"))
				.partitionId(rs.getString("partition_id"))
				.build();
	}
}
