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

import java.util.List;
import org.opengroup.osdu.entitlements.v2.jdbc.mapper.MemberInfoEntityNullRoleMapper;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends CrudRepository<MemberInfoEntity, Long> {

	@Query(value = "SELECT \"member\".* FROM \"member\" WHERE \"member\".\"email\" = :email",
		   rowMapperClass = MemberInfoEntityNullRoleMapper.class)
	List<MemberInfoEntity> findByEmail(@Param("email") String email);

	@Query("SELECT member.*, mg.role FROM member LEFT JOIN member_to_group AS mg ON member.id = mg.member_id WHERE mg.group_id = :groupId")
	List<MemberInfoEntity> findMembersByGroup(@Param("groupId") Long groupId);

	@Query("SELECT member.*, mg.role FROM member LEFT JOIN member_to_group AS mg ON member.id = mg.member_id WHERE mg.group_id = :groupId AND member.email = :memberEmail")
	List<MemberInfoEntity> findMemberByEmailInGroup(@Param("groupId") Long groupId, @Param("memberEmail") String memberEmail);
}
