package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateUserNode;

@DataMongoTest
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@SpringJUnitConfig(classes = {EntitlementsTestConfig.class})
class CreateGroupRepoMongoDBTest extends ParentUtil {

    @Autowired
    private CreateGroupRepoMongoDB createGroupRepoMongoDB;

    @BeforeEach
    public void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    public void whenUserNotExistUserInitiator() {
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
    public void whenExistsRootGroup() {
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
        assertTrue(groupDoc.getDirectParents().stream().anyMatch(g -> g.getParentId().equals(rootGroup.getId())));
        assertTrue(groupDoc.getDirectParents().stream().anyMatch(g -> g.getParentId().equals(rootGroup.getId())));
    }

    @Test
    public void whenRootGroupRequiredButNull() {
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

    @Disabled // todo enabled
    @Test
    public void whenRequiredRootGroupNotExists() {
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
