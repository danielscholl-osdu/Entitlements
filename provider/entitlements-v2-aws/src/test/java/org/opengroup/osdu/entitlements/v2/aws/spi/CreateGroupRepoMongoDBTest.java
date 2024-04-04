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
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateUserNode;

@DataMongoTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EntitlementsTestConfig.class, MockServletContext.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateGroupRepoMongoDBTest extends ParentUtil {

    @MockBean
    private IEventPublisher messageBus;

    @Autowired
    private CreateGroupRepoMongoDB createGroupRepoMongoDB;

    @BeforeEach
    void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    void whenUserNotExistUserInitiator() {
        //given
        EntityNode groupNode = generateGroupNode(102);
        EntityNode userNode = generateUserNode(103);
        CreateGroupRepoDto request = CreateGroupRepoDto.builder()
                .partitionId(DATA_PARTITION)
                .requesterNode(userNode)
                .addDataRootGroup(false)
                .dataRootGroupNode(null)
                .build();

        //when
        createGroupRepoMongoDB.createGroup(groupNode, request);

        //then
        UserDoc userDoc = mongoTemplateHelper.findById(createId(userNode), UserDoc.class);
        GroupDoc groupDoc = mongoTemplateHelper.findById(createId(groupNode), GroupDoc.class);

        assertNotNull(userDoc);
        assertNotNull(groupDoc);
        assertTrue(userDoc.getAllParents().stream().anyMatch(u -> u.getParentId().equals(groupDoc.getId())));
        assertEquals(1, groupDoc.getAppIds().size());
    }

    @Test
    void whenExistsRootGroup() {
        //given
        EntityNode groupNode = generateGroupNode(102);
        EntityNode rootNode = generateGroupNode(9999);
        EntityNode userNode = generateUserNode(103);
        CreateGroupRepoDto request = CreateGroupRepoDto.builder()
                .partitionId(DATA_PARTITION)
                .requesterNode(userNode)
                .addDataRootGroup(true)
                .dataRootGroupNode(rootNode)
                .build();

        //when
        createGroupRepoMongoDB.createGroup(groupNode, request);

        //then
        UserDoc userDoc = mongoTemplateHelper.findById(createId(userNode), UserDoc.class);
        GroupDoc groupDoc = mongoTemplateHelper.findById(createId(groupNode), GroupDoc.class);
        GroupDoc rootGroup = mongoTemplateHelper.findById(createId(rootNode), GroupDoc.class);

        assertNotNull(userDoc);
        assertNotNull(groupDoc);
        assertNotNull(rootGroup);
        assertTrue(userDoc.getAllParents().stream().anyMatch(u -> u.getParentId().equals(groupDoc.getId())));
        assertTrue(rootGroup.getDirectParents().stream().anyMatch(g -> g.getParentId().equals(groupDoc.getId())));
    }

    @Test
    void whenRootGroupRequiredButNull() {
        //given
        EntityNode groupNode = generateGroupNode(102);
        EntityNode userNode = generateUserNode(103);
        CreateGroupRepoDto request = CreateGroupRepoDto.builder()
                .partitionId(DATA_PARTITION)
                .requesterNode(userNode)
                .addDataRootGroup(true)
                .dataRootGroupNode(null)
                .build();

        //then
        Assertions.assertThrows(IllegalArgumentException.class, () -> createGroupRepoMongoDB.createGroup(groupNode, request));
    }
    @Test
    void whengroupExists() {
        //given
        EntityNode groupNode = generateGroupNode(4);
        EntityNode rootNode = generateGroupNode(15648);
        EntityNode userNode = generateUserNode(103);
        CreateGroupRepoDto request = CreateGroupRepoDto.builder()
                .partitionId(DATA_PARTITION)
                .requesterNode(userNode)
                .addDataRootGroup(true)
                .dataRootGroupNode(rootNode)
                .build();

        //when
        Assertions.assertThrows(AppException.class, () -> createGroupRepoMongoDB.createGroup(groupNode, request));
    }

    @Disabled // todo enabled
    @Test
    void whenRequiredRootGroupNotExists() {
        //given
        EntityNode groupNode = generateGroupNode(102);
        EntityNode rootNode = generateGroupNode(15648);
        EntityNode userNode = generateUserNode(103);
        CreateGroupRepoDto request = CreateGroupRepoDto.builder()
                .partitionId(DATA_PARTITION)
                .requesterNode(userNode)
                .addDataRootGroup(true)
                .dataRootGroupNode(rootNode)
                .build();

        //when
        Assertions.assertThrows(IllegalArgumentException.class, () -> createGroupRepoMongoDB.createGroup(groupNode, request));
    }
}
