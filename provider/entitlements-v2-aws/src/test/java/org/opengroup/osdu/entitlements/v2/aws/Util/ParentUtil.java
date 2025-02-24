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

package org.opengroup.osdu.entitlements.v2.aws.Util;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Arrays;
import java.util.Collections;

import static org.opengroup.osdu.entitlements.v2.aws.Util.GroupDocGenerator.generateGroupDoc;


@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class ParentUtil extends DbUtil {

    protected MongoTemplateHelper mongoTemplateHelper;

    @Autowired
    private void setMongoTemplate(MongoTemplate mongoTemplate) {
        mongoTemplateHelper = new MongoTemplateHelper(mongoTemplate);
    }

    /*
     * ----GROUPS-----
     *                     All groups are member of other groups
     * -rootGroup1 , rootGroup2  - ---- (M)
     * -superGroup1 -> rootGroup1 , superGroup2 -> rootGroup2    (M)
     * -group5 -> superGroup1 , superGroup2  (M)
     * -group4 -> superGroup2 , rootGroup2 (M)
     * -group3 -----
     * -group2 -> group3 , group4 (M)
     * -group1 -> group2 , group5 , superGroup1 (M)
     *
     * -----USERS-----
     *                   All users have a parent
     * -rootUser1 -> rootGroup1 (P) (O) , -> rootGroup1 (O)
     * -rootUser2 -> rootGroup2 (P) (O) , -> rootGroup2 (O)
     * -superUser1 -> superGroup1 (P) (O), -> superGroup1 (O) , rootGroup1 (M)
     * -superUser2 -> superGroup2 (P) (O), -> superGroup2 (O) , rootGroup2 (M)
     * -userDoc1 -> group1 (P) (O) -> group1 (O) , group2 , group3 ,
     *       group4 , group5 , superGroup2  , rootGroup2 , superGroup1 , rootGroup1 (M)
     * -userDoc2 -> group2 (P) (O) -> group2 (O) , group3 ,
     *       group4 , superGroup2  , rootGroup2 (M)
     * -userDoc3 ->  group3 (P) (O) -> group3 (O)
     * -userDoc4 -> group4(O) group5 (M)(P) -> group4 (O) , group5(M) , superGroup2 , rootGroup2 , superGroup1, rootGroup1 (M)
     *
     * */
    protected void initDefaultDataSet() {
        // create groups
        GroupDoc rootGroup1 = generateGroupDoc(String.format(GROUP_TEMPLATE, "99999"));
        GroupDoc rootGroup2 = generateGroupDoc(String.format(GROUP_TEMPLATE, "9999"));
        GroupDoc superGroup1 = generateGroupDoc(String.format(GROUP_TEMPLATE, "999"));
        GroupDoc superGroup2 = generateGroupDoc(String.format(GROUP_TEMPLATE, "99"));
        GroupDoc group1 = generateGroupDoc(String.format(GROUP_TEMPLATE, "1"));
        GroupDoc group2 = generateGroupDoc(String.format(GROUP_TEMPLATE, "2"));
        GroupDoc group3 = generateGroupDoc(String.format(GROUP_TEMPLATE, "3"));
        GroupDoc group4 = generateGroupDoc(String.format(GROUP_TEMPLATE, "4"));
        GroupDoc group5 = generateGroupDoc(String.format(GROUP_TEMPLATE, "5"));

        // set group parents
        superGroup1.getDirectParents().addAll(Collections.singletonList(createRelation(rootGroup1.getId(), Role.MEMBER)));
        superGroup2.getDirectParents().addAll(Collections.singletonList(createRelation(rootGroup2.getId(), Role.MEMBER)));

        group5.getDirectParents().addAll(Arrays.asList(createRelation(superGroup1.getId(), Role.MEMBER),
                createRelation(superGroup2.getId(), Role.MEMBER)));

        group4.getDirectParents().addAll(Arrays.asList(createRelation(superGroup2.getId(), Role.MEMBER),
                createRelation(rootGroup2.getId(), Role.MEMBER)));

        group2.getDirectParents().addAll(Arrays.asList(createRelation(group3.getId(), Role.MEMBER),
                createRelation(group4.getId(), Role.MEMBER)));

        group1.getDirectParents().addAll(Arrays.asList(createRelation(group2.getId(), Role.MEMBER),
                createRelation(group5.getId(), Role.MEMBER),
                createRelation(superGroup1.getId(), Role.MEMBER)));

        mongoTemplateHelper.insert(rootGroup1, rootGroup2,
                superGroup1, superGroup2,
                group1, group2, group3,
                group4, group5);

        UserDoc rootUser1 = UserDocGenerator.createUserDocById(String.format(USER_TEMPLATE, "99999"));
        rootUser1.getDirectParents().add(createRelation(rootGroup1.getId(), Role.OWNER));
        //set user is member of
        rootUser1.getAllParents().add(createRelation(rootGroup1.getId(), Role.OWNER));

        UserDoc rootUser2 = UserDocGenerator.createUserDocById(String.format(USER_TEMPLATE, "9999"));
        rootUser2.getDirectParents().add(createRelation(rootGroup2.getId(), Role.OWNER));
        //set user is member of
        rootUser2.getAllParents().add(createRelation(rootGroup2.getId(), Role.OWNER));

        UserDoc superUser1 = UserDocGenerator.createUserDocById(String.format(USER_TEMPLATE, "999"));
        superUser1.getDirectParents().add(createRelation(superGroup1.getId(), Role.OWNER));
        //set user is member of
        superUser1.getAllParents().addAll(Arrays.asList(createRelation(superGroup1.getId(), Role.OWNER),
                createRelation(rootGroup1.getId(), Role.MEMBER)));

        UserDoc superUser2 = UserDocGenerator.createUserDocById(String.format(USER_TEMPLATE, "99"));
        superUser2.getDirectParents().add(createRelation(superGroup2.getId(), Role.OWNER));
        //set user is member of
        superUser2.getAllParents().addAll(Arrays.asList(createRelation(superGroup2.getId(), Role.OWNER),
                createRelation(rootGroup2.getId(), Role.MEMBER)));

        UserDoc userDoc1 = UserDocGenerator.createUserDocById(String.format(USER_TEMPLATE, "1"));
        userDoc1.getDirectParents().add(createRelation(group1.getId(), Role.OWNER));
        //set user is member of
        userDoc1.getAllParents().addAll(Arrays.asList(createRelation(group1.getId(), Role.OWNER),
                createRelation(group2.getId(), Role.MEMBER),
                createRelation(group5.getId(), Role.MEMBER),
                createRelation(group3.getId(), Role.MEMBER),
                createRelation(group4.getId(), Role.MEMBER),
                createRelation(rootGroup1.getId(), Role.MEMBER),
                createRelation(superGroup2.getId(), Role.MEMBER),
                createRelation(rootGroup2.getId(), Role.MEMBER),
                createRelation(superGroup1.getId(), Role.MEMBER)));

        UserDoc userDoc2 = UserDocGenerator.createUserDocById(String.format(USER_TEMPLATE, "2"));
        userDoc2.getDirectParents().add(createRelation(group2.getId(), Role.OWNER));
        //set user is member of
        userDoc2.getAllParents().addAll(Arrays.asList(createRelation(group2.getId(), Role.OWNER),
                createRelation(group3.getId(), Role.MEMBER),
                createRelation(group4.getId(), Role.MEMBER),
                createRelation(superGroup2.getId(), Role.MEMBER),
                createRelation(rootGroup2.getId(), Role.MEMBER)));

        UserDoc userDoc3 = UserDocGenerator.createUserDocById(String.format(USER_TEMPLATE, "3"));
        userDoc3.getDirectParents().add(createRelation(group3.getId(), Role.OWNER));
        //set user is member of
        userDoc3.getAllParents().addAll(Collections.singletonList(createRelation(group3.getId(), Role.OWNER)));

        UserDoc userDoc4 = UserDocGenerator.createUserDocById(String.format(USER_TEMPLATE, "4"));
        userDoc4.getDirectParents().add(createRelation(group4.getId(), Role.OWNER));
        userDoc4.getDirectParents().add(createRelation(group5.getId(), Role.MEMBER));
        //set user is member of
        userDoc4.getAllParents().addAll(Arrays.asList(createRelation(group4.getId(), Role.OWNER),
                createRelation(superGroup2.getId(), Role.MEMBER),
                createRelation(group5.getId(), Role.MEMBER),
                createRelation(superGroup1.getId(), Role.MEMBER),
                createRelation(rootGroup1.getId(), Role.MEMBER),
                createRelation(rootGroup2.getId(), Role.MEMBER)));

        UserDoc userDoc5 = UserDocGenerator.createUserDocById(String.format(USER_TEMPLATE, "5"));
        userDoc5.getDirectParents().add(createRelation(group5.getId(), Role.OWNER));
        //set user is member of
        userDoc5.getAllParents().addAll(Arrays.asList(createRelation(group5.getId(), Role.OWNER),
                createRelation(superGroup1.getId(), Role.MEMBER),
                createRelation(rootGroup1.getId(), Role.MEMBER),
                createRelation(superGroup2.getId(), Role.MEMBER),
                createRelation(rootGroup2.getId(), Role.MEMBER)));

        mongoTemplateHelper.insert(rootUser1, rootUser2,
                superUser1, superUser2,
                userDoc1, userDoc2,
                userDoc3, userDoc4, userDoc5);
    }
}
