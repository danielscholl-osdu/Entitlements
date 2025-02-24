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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataMongoTest
@SpringJUnitConfig(classes = {EntitlementsTestConfig.class})
class ListMemberRepoMongoDBTest extends ParentUtil {

    @MockBean
    private IEventPublisher messageBus;

    @Autowired
    private ListMemberRepoMongoDB listMemberRepoMongoDB;


    @BeforeEach
    void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    void runTest() {
        //given
        ListMemberServiceDto request = ListMemberServiceDto.builder()
                .partitionId(DATA_PARTITION)
                .groupId(String.format(GROUP_TEMPLATE, "4"))
                .requesterId(null)
                .build();

        //when
        List<ChildrenReference> result = listMemberRepoMongoDB.run(request);

        //then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.stream().map(ChildrenReference::getId).distinct().count());
    }

    @Disabled // todo enabled
    @Test
    void runNotFoundGroup() {
        //given
        ListMemberServiceDto request = ListMemberServiceDto.builder()
                .partitionId(DATA_PARTITION)
                .groupId(String.format(GROUP_TEMPLATE, 666544))
                .requesterId(null)
                .build();

        //whe
        Assertions.assertThrows(IllegalArgumentException.class, () -> listMemberRepoMongoDB.run(request));
    }

}
