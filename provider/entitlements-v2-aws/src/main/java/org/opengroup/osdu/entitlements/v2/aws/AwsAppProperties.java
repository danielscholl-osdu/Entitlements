// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package org.opengroup.osdu.entitlements.v2.aws;


import lombok.Getter;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
public class AwsAppProperties extends AppProperties {

    public static final String DEFAULT_APPID_KEY = "NA";

    @Value("${redishost}")
    private String redishost;
    @Value("${redisport}")
    private String redisport;
    @Value("${rediskey}")
    private String rediskey;
    @Value("${redis.partition.association}")
    private int redispartitionAssociation;
    @Value("${partition.entitynode}")
    private int partitionEntityNode;
    @Value("${partition.parent.ref}")
    private int partitionParentRef;
    @Value("${partition.children.ref}")
    private int partitionChildrenRef;
    @Value("${partition.appid}")
    private int partitionAppId;

    public String getRedisHost() {
        return redishost;
    }

    public int getRedisPort() {
        return Integer.parseInt(redisport);
    }

    public String getRedisKey() {
        return rediskey;
    }

    public int getRedisPartitionAssociation() {
        return redispartitionAssociation;
    }



    public int getPartitionEntityNode() {
        return partitionEntityNode;
    }

    public int getPartitionParentRef() {
        return partitionParentRef;
    }

    public int getPartitionChildrenRef() {
        return partitionChildrenRef;
    }

    public int getPartitionAppId() {
        return partitionAppId;
    }

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
    public List<String> getProtectedMembers() {
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/provisioning/groups/data_groups.json");
        filePaths.add("/provisioning/groups/datalake_service_groups.json");
        return filePaths;
    }

    @Override
    public List<String> getGroupsOfInitialUsers() {
        List<String> groupsOfInitialUsers = new ArrayList<>();
        groupsOfInitialUsers.add(getGroupsOfServicePrincipal());
        return groupsOfInitialUsers;
    }
}
