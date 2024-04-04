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
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;

@DataMongoTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EntitlementsTestConfig.class, MockServletContext.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RenameGroupRepoMongoDBTest extends ParentUtil {

    @MockBean
    private IEventPublisher messageBus;

    @Autowired
    private RenameGroupRepoMongoDB renameGroupRepoMongoDB;


    @BeforeEach
    void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    void run() {
        //given
        EntityNode groupNode = generateGroupNode(4);
        String newGroupName = "renamedGroup4";

        //when
        renameGroupRepoMongoDB.run(groupNode, newGroupName);

        //then
        GroupDoc renamedGroup = mongoTemplateHelper.findById(createId(groupNode), GroupDoc.class);
        assertEquals(newGroupName, renamedGroup.getName());
    }

    @Disabled // enable
    @Test
    void runGroupNotFoundOrNull() {
        //given
        EntityNode groupNode = generateGroupNode(464656);
        String newGroupName = "renamedGroup4";

        //then
        Assertions.assertThrows(IllegalArgumentException.class, () -> renameGroupRepoMongoDB.run(null, newGroupName));
        Assertions.assertThrows(IllegalArgumentException.class, () -> renameGroupRepoMongoDB.run(groupNode, newGroupName));
    }

    @Disabled // enabled
    @Test
    void runNullOrEmptyId() {
        //given
        EntityNode groupNode4 = generateGroupNode(4);

        //then
        Assertions.assertThrows(IllegalArgumentException.class, () -> renameGroupRepoMongoDB.run(groupNode4, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> renameGroupRepoMongoDB.run(groupNode4, " "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> renameGroupRepoMongoDB.run(groupNode4, ""));
    }
}
