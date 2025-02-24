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
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;


@DataMongoTest
@SpringJUnitConfig(classes = {EntitlementsTestConfig.class})
class UpdateAppIdsRepoMongoDBTest extends ParentUtil {

    @MockBean
    private IEventPublisher messageBus;

    @Autowired
    UpdateAppIdsRepoMongoDB appIdsRepoMongoDB;


    @BeforeEach
    public void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    void updateAppIds() {
        //given
        EntityNode groupNode = generateGroupNode(4);
        GroupDoc groupDoc = mongoTemplateHelper.findById(createId(groupNode), GroupDoc.class);
        Set<String> appIds = groupDoc.getAppIds();
        assertEquals(1, appIds.size());
        String testId = "testId";
        appIds = new HashSet<>(Arrays.asList(SECOND_APP, testId));
        //when
        appIdsRepoMongoDB.updateAppIds(groupNode, appIds);

        //then
        groupDoc = mongoTemplateHelper.findById(createId(groupNode), GroupDoc.class);

        Set<String> appIdsDB = groupDoc.getAppIds();
        assertEquals(2, appIdsDB.size());
        assertTrue(appIdsDB.containsAll(appIds));
    }

    @Test
    void updateAppIdsToEmpty() {
        //given
        EntityNode groupNode = generateGroupNode(4);
        GroupDoc groupDoc = mongoTemplateHelper.findById(createId(groupNode), GroupDoc.class);
        Set<String> appIds = groupDoc.getAppIds();
        assertEquals(1, appIds.size());
        String testId = "testId";
        appIds.add(testId);

        //when
        appIdsRepoMongoDB.updateAppIds(groupNode, Collections.emptySet());

        //then
        groupDoc = mongoTemplateHelper.findById(createId(groupNode), GroupDoc.class);

        assertEquals(0, groupDoc.getAppIds().size());
    }

    @Disabled // todo enabled
    @Test
    void updateAppIdsGroupNotFoundOrNull() {
        //given
        EntityNode groupNode = generateGroupNode(64646);
        String testId = "testId";
        //then
        Assertions.assertThrows(IllegalArgumentException.class, () -> appIdsRepoMongoDB.updateAppIds(groupNode, Collections.emptySet()));
    }
}