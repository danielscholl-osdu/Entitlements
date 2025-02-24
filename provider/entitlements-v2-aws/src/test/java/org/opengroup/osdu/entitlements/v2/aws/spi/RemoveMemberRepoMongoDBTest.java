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
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateUserNode;

@DataMongoTest
@SpringJUnitConfig(classes = {EntitlementsTestConfig.class})
class RemoveMemberRepoMongoDBTest extends ParentUtil {

    @MockBean
    private IEventPublisher messageBus;

    @Autowired
    private RemoveMemberRepoMongoDB removeMemberRepoMongoDB;

    @BeforeEach
    public void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    void removeUserMember() {
        //given
        EntityNode groupNode = generateGroupNode(2);
        EntityNode userNode = generateUserNode(2);
        RemoveMemberServiceDto removeMemberServiceDto = null;
        UserDoc userDoc4 = mongoTemplateHelper.findById(createId(userNode), UserDoc.class);

        assertEquals(1, userDoc4.getDirectParents().size());
        assertEquals(5, userDoc4.getAllParents().size());

        //when
        removeMemberRepoMongoDB.removeMember(groupNode, userNode, removeMemberServiceDto);

        //then
        userDoc4 = mongoTemplateHelper.findById(createId(userNode), UserDoc.class);
        assertEquals(0, userDoc4.getDirectParents().size());
        assertEquals(0, userDoc4.getAllParents().size());
    }

    @Test
    void removeGroupMember() {
        //given
        EntityNode groupNode4 = generateGroupNode(4);
        EntityNode groupNode2 = generateGroupNode(2);
        RemoveMemberServiceDto removeMemberServiceDto = null;

        IdDoc idForUser2 = createIdForUser(2);
        UserDoc userDoc2 = mongoTemplateHelper.findById(idForUser2, UserDoc.class);

        assertEquals(1, userDoc2.getDirectParents().size());
        assertEquals(5, userDoc2.getAllParents().size());

        //when
        removeMemberRepoMongoDB.removeMember(groupNode4, groupNode2, removeMemberServiceDto);

        //then
        GroupDoc groupDoc2 = mongoTemplateHelper.findById(createId(groupNode2), GroupDoc.class);

        assertEquals(1, groupDoc2.getDirectParents().size());

        userDoc2 = mongoTemplateHelper.findById(idForUser2, UserDoc.class);

        assertEquals(1, userDoc2.getDirectParents().size());
        assertEquals(2, userDoc2.getAllParents().size());

        UserDoc userDoc1 = mongoTemplateHelper.findById(createIdForUser(1), UserDoc.class);

        assertEquals(1, userDoc1.getDirectParents().size());
        assertEquals(8, userDoc1.getAllParents().size());
    }

    @Disabled // todo enabled
    @Test
    void removeUserMemberUserNotInGroup() {
        //given
        EntityNode groupNode = generateGroupNode(4);
        EntityNode userNode = generateUserNode(46646);
        RemoveMemberServiceDto removeMemberServiceDto = null;

        //then
        Assertions.assertThrows(IllegalArgumentException.class, () -> removeMemberRepoMongoDB.removeMember(groupNode, userNode, removeMemberServiceDto));
    }

    @Disabled // todo enabled
    @Test
    void removeUserMemberNotFoundGroup() {
        //given
        EntityNode groupNode = generateGroupNode(56464);
        EntityNode userNode = generateUserNode(4);
        RemoveMemberServiceDto removeMemberServiceDto = null;

        //then
        Assertions.assertThrows(IllegalArgumentException.class, () -> removeMemberRepoMongoDB.removeMember(groupNode, userNode, removeMemberServiceDto));
    }
}