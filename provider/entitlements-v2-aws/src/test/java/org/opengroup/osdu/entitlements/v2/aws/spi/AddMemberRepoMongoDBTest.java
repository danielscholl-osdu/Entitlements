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
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateUserNode;


@DataMongoTest
@SpringJUnitConfig(classes = {EntitlementsTestConfig.class})
class AddMemberRepoMongoDBTest extends ParentUtil {

    @MockBean
    private IEventPublisher messageBus;

    @Autowired
    private AddMemberRepoMongoDB addMemberRepo;


    @BeforeEach
    void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    void addingUserMember() {
        // given
        Role role = Role.MEMBER;
        EntityNode groupNode = generateGroupNode(4);
        EntityNode userNode = generateUserNode(5);
        AddMemberRepoDto dto = AddMemberRepoDto.builder()
                .partitionId(DATA_PARTITION)
                .memberNode(userNode)
                .role(role)
                .build();

        // when
        addMemberRepo.addMember(groupNode, dto);

        // then
        UserDoc userDoc = mongoTemplateHelper.findById(createId(userNode), UserDoc.class);
        GroupDoc groupDoc = mongoTemplateHelper.findById(createId(groupNode), GroupDoc.class);

        assertNotNull(userDoc);
        assertNotNull(groupDoc);
        assertTrue(userDoc.getAllParents().stream().anyMatch(e -> e.getParentId().equals(groupDoc.getId())));
    }

    @Test
    void addingGroupMember() {
        // given
        Role role = Role.MEMBER;
        EntityNode groupNode = generateGroupNode(4);
        EntityNode userNode = generateGroupNode(5);
        AddMemberRepoDto dto = AddMemberRepoDto.builder()
                .partitionId(DATA_PARTITION)
                .memberNode(userNode)
                .role(role)
                .build();

        // when
        addMemberRepo.addMember(groupNode, dto);

        // then
        GroupDoc groupChild = mongoTemplateHelper.findById(createId(userNode), GroupDoc.class);
        GroupDoc groupDoc = mongoTemplateHelper.findById(createId(groupNode), GroupDoc.class);

        assertNotNull(groupChild);
        assertNotNull(groupDoc);
        assertTrue(groupChild.getDirectParents().stream().anyMatch(e -> e.getParentId().equals(groupDoc.getId())));
    }

    @Test
    void addingMemberGroupNotExist() {
        // given
        Role role = Role.MEMBER;
        EntityNode groupNode = generateGroupNode(123); // this group not exist
        EntityNode userNode = generateUserNode(5);
        AddMemberRepoDto dto = AddMemberRepoDto.builder()
                .partitionId(DATA_PARTITION)
                .memberNode(userNode)
                .role(role)
                .build();

        // when
        Assertions.assertThrows(IllegalArgumentException.class, () -> addMemberRepo.addMember(groupNode, dto));
    }

}
