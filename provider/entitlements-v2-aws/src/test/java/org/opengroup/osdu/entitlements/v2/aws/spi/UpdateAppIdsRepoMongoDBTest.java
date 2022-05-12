package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;


@DataMongoTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EntitlementsTestConfig.class)
class UpdateAppIdsRepoMongoDBTest extends ParentUtil {

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