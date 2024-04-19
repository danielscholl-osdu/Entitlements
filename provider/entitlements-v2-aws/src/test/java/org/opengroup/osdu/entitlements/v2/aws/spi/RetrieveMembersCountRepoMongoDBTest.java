/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.entitlements.v2.aws.spi;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataMongoTest
@SpringJUnitConfig(classes = {EntitlementsTestConfig.class})
class RetrieveMembersCountRepoMongoDBTest extends ParentUtil {
    @MockBean
    private IEventPublisher messageBus;

    @Autowired
    private RetrieveMembersCountMongoDB retrieveMembersCountMongoDB;


    @BeforeEach
    void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }   
    
    @Test
    void runTest() {
        Role role = Role.MEMBER;
        MembersCountServiceDto request = MembersCountServiceDto.builder()
                .groupId(String.format(GROUP_TEMPLATE, "4"))
                .partitionId(DATA_PARTITION)
                .requesterId(null)
                .role(role)
                .build();

        MembersCountResponseDto response = retrieveMembersCountMongoDB.getMembersCount(request);

        assertEquals(2, response.getMembersCount());
    }
}
