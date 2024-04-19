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
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.BaseDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;

@DataMongoTest
@SpringJUnitConfig(classes = {EntitlementsTestConfig.class})
class DeleteGroupRepoMongoDBTest extends ParentUtil {

    @MockBean
    private IEventPublisher messageBus;

    @Autowired
    private DeleteGroupRepoMongoDB deleteGroupRepoMongoDB;

    @BeforeEach
    void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    void deleteGroup() {

        // given
        EntityNode deletedGroup4Node = generateGroupNode(4);
        IdDoc deletedGroupIdDoc = createId(deletedGroup4Node);

        UserDoc userDoc1 = mongoTemplateHelper.findById(createIdForUser(1), UserDoc.class);
        int userDoc1AllParentsCount = userDoc1.getAllParents().size();
        assertEquals(9, userDoc1AllParentsCount);
        UserDoc userDoc2 = mongoTemplateHelper.findById(createIdForUser(2), UserDoc.class);
        int userDoc2AllParentsCount = userDoc2.getAllParents().size();
        assertEquals(5, userDoc2AllParentsCount);
        UserDoc userDoc4 = mongoTemplateHelper.findById(createIdForUser(4), UserDoc.class);
        int userDoc4AllParentsCount = userDoc4.getAllParents().size();
        assertEquals(6, userDoc4AllParentsCount);


        //when
        deleteGroupRepoMongoDB.deleteGroup(deletedGroup4Node);

        // then
        GroupDoc rootGroup1 = mongoTemplateHelper.findById(createIdForGroup(99999), GroupDoc.class);
        GroupDoc rootGroup2 = mongoTemplateHelper.findById(createIdForGroup(9999), GroupDoc.class);
        GroupDoc superGroup1 = mongoTemplateHelper.findById(createIdForGroup(999), GroupDoc.class);
        GroupDoc superGroup2 = mongoTemplateHelper.findById(createIdForGroup(99), GroupDoc.class);
        GroupDoc groupDoc1 = mongoTemplateHelper.findById(createIdForGroup(1), GroupDoc.class);
        GroupDoc groupDoc2 = mongoTemplateHelper.findById(createIdForGroup(2), GroupDoc.class);
        GroupDoc groupDoc3 = mongoTemplateHelper.findById(createIdForGroup(3), GroupDoc.class);
        GroupDoc groupDoc5 = mongoTemplateHelper.findById(createIdForGroup(5), GroupDoc.class);
        GroupDoc deletedGroup4 = mongoTemplateHelper.findById(deletedGroupIdDoc, GroupDoc.class);

        UserDoc rootUser1 = mongoTemplateHelper.findById(createIdForUser(99999), UserDoc.class);
        UserDoc rootUser2 = mongoTemplateHelper.findById(createIdForUser(9999), UserDoc.class);
        UserDoc superUser1 = mongoTemplateHelper.findById(createIdForUser(999), UserDoc.class);
        UserDoc superUser2 = mongoTemplateHelper.findById(createIdForUser(99), UserDoc.class);
        userDoc1 = mongoTemplateHelper.findById(createIdForUser(1), UserDoc.class);
        userDoc2 = mongoTemplateHelper.findById(createIdForUser(2), UserDoc.class);
        UserDoc userDoc3 = mongoTemplateHelper.findById(createIdForUser(3), UserDoc.class);
        userDoc4 = mongoTemplateHelper.findById(createIdForUser(4), UserDoc.class);
        UserDoc userDoc5 = mongoTemplateHelper.findById(createIdForUser(5), UserDoc.class);

        List<UserDoc> users = Arrays.asList(rootUser1, rootUser2, superUser1, superUser2, userDoc1, userDoc2, userDoc3, userDoc4, userDoc5);
        List<GroupDoc> groups = Arrays.asList(rootGroup1, rootGroup2, superGroup1, superGroup2, groupDoc1, groupDoc2, groupDoc3, groupDoc5);

        assertNull(deletedGroup4);

        userDoc1AllParentsCount = userDoc1.getAllParents().size();
        assertEquals(8, userDoc1AllParentsCount);
        userDoc2AllParentsCount = userDoc2.getAllParents().size();
        assertEquals(2, userDoc2AllParentsCount);
        userDoc4AllParentsCount = userDoc4.getAllParents().size();
        assertEquals(5, userDoc4AllParentsCount);

        //deleted group not parent for any group
        assertTrue(groups.stream()
                        .map(BaseDoc::getDirectParents)
                        .flatMap(Collection::stream)
                        .noneMatch(nodeRelationDoc -> nodeRelationDoc.getParentId()
                                .equals(deletedGroupIdDoc)
                        )
                , "deleted group exists as direct parent for group");
        //deleted group not parent for any user
        assertTrue(users.stream()
                        .map(BaseDoc::getDirectParents)
                        .flatMap(Collection::stream)
                        .noneMatch(nodeRelationDoc -> nodeRelationDoc.getParentId()
                                .equals(deletedGroupIdDoc)
                        )
                , "deleted group exists as direct parent for user");
        //not any user as member of deleted group
        assertTrue(users.stream()
                        .map(UserDoc::getAllParents)
                        .flatMap(Collection::stream)
                        .noneMatch(nodeRelationDoc -> nodeRelationDoc.getParentId()
                                .equals(deletedGroupIdDoc)
                        )
                , "in all parents exists deleted group");
    }
}
