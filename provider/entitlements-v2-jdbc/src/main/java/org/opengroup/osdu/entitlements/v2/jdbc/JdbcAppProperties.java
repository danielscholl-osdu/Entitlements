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

package org.opengroup.osdu.entitlements.v2.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@AllArgsConstructor
public class JdbcAppProperties extends AppProperties {

	@Override
	public List<String> getInitialGroups() {
		List<String> initialGroups = new ArrayList<>();
		initialGroups.add("/provisioning/groups/datalake_user_groups.json");
		initialGroups.add("/provisioning/groups/datalake_service_groups.json");
		initialGroups.add("/provisioning/groups/data_groups.json");
		return initialGroups;
	}

	@Override
	public String getGroupsOfServicePrincipal() {
		return "/provisioning/accounts/groups_of_service_principal.json";
	}

	@Override
	public List<String> getGroupsOfInitialUsers() {
		List<String> groupsOfInitialUsers = new ArrayList<>();
		groupsOfInitialUsers.add(getGroupsOfServicePrincipal());

		if (Objects.nonNull(initServiceDto) && !CollectionUtils.isEmpty(initServiceDto.getAliasMappings())) {
			initServiceDto.getAliasMappings().forEach(
					(e) -> groupsOfInitialUsers.add(String.format("/provisioning/accounts/groups_of_%s.json", e.getAliasId().toLowerCase()))
			);
		}

		return groupsOfInitialUsers;
	}

	@Override
	public List<String> getProtectedMembers() {
		List<String> filePaths = new ArrayList<>();
		filePaths.add("/provisioning/groups/data_groups.json");
		filePaths.add("/provisioning/groups/datalake_service_groups.json");
		return filePaths;
	}
}
