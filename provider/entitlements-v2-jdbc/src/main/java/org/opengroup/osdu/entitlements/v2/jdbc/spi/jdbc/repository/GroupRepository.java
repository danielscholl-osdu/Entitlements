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
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends CrudRepository<GroupInfoEntity, Long> {

	@Query("SELECT *  FROM \"group\" AS g LEFT JOIN app_id ON g.id = app_id.group_id WHERE g.name = :name")
	List<GroupInfoEntity> findByName(@Param("name") String name);

	@Query("SELECT *  FROM \"group\" AS g LEFT JOIN app_id ON g.id = app_id.group_id WHERE g.email = :email")
	List<GroupInfoEntity> findByEmail(@Param("email") String email);

	@Query("SELECT g.* FROM embedded_group as gg LEFT JOIN \"group\" as g ON gg.child_id = g.id WHERE gg.parent_id = :parentId AND g.email = :childEmail")
	List<GroupInfoEntity> findChildByEmail(@Param("parentId") Long parentId, @Param("childEmail") String childEmail);

	@Query("SELECT g.* FROM embedded_group as gg LEFT JOIN \"group\" as g ON gg.parent_id = g.id WHERE gg.child_id IN (:childrenIds)")
	List<GroupInfoEntity> findDirectParents(@Param("childrenIds") List<Long> childrenIds);

	@Query("SELECT g.* FROM member_to_group as mg LEFT JOIN \"group\" as g ON mg.group_id = g.id WHERE mg.member_id IN (:memberIds)")
	List<GroupInfoEntity> findDirectGroups(@Param("memberIds") List<Long> childrenIds);

	@Query("SELECT g.* FROM embedded_group as gg LEFT JOIN \"group\" as g ON gg.child_id = g.id WHERE gg.parent_id IN (:parentIds)")
	List<GroupInfoEntity> findDirectChildren(@Param("parentIds") List<Long> parentIds);

	@Query("SELECT * FROM \"group\" as gg WHERE gg.partition_Id = :partitionId AND gg.id IN (:groupIds)")
	List<GroupInfoEntity> findGroupsByIdAndPartition(@Param("groupIds") List<Long> groupIds, @Param("partitionId") String partitionId);

	@Modifying
	@Query("INSERT INTO embedded_group VALUES (:parentId, :childId)")
	void addChildGroupById(@Param("parentId") Long parentId, @Param("childId") Long childId);

	@Modifying
	@Query("DELETE FROM embedded_group WHERE parent_id = :parentId AND child_id = :childId")
	void removeChildById(@Param("parentId") Long parentId, @Param("childId") Long childId);

	@Modifying
	@Query("INSERT INTO member_to_group VALUES (:groupId, :memberId, :role)")
	void addMemberById(@Param("groupId") Long groupId, @Param("memberId") Long memberId, @Param("role") String role);

	@Modifying
	@Query("DELETE FROM member_to_group WHERE member_id = :memberId AND group_id = :groupId")
	boolean removeMemberById(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

	@Modifying
	@Query("DELETE FROM embedded_group WHERE parent_id = :id OR child_id = :id")
	boolean deleteRelationsById(@Param("id") Long id);

	@Modifying
	@Query("DELETE FROM member_to_group WHERE group_id = :id")
	boolean deleteMemberRelationsById(@Param("id") Long id);

	@Modifying
	@Query("UPDATE \"group\" SET name = :name, email = :email WHERE id = :id")
	boolean update(@Param("id") Long id, @Param("name") String name, @Param("email") String email);

	@Modifying
	@Query("INSERT into app_id (group_id, app_id) VALUES (:groupId, :appId)")
	boolean updateAppId(@Param("groupId") Long groupId, @Param("appId") String appId);
}
