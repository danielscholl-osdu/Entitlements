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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;

@DataMongoTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EntitlementsTestConfig.class, MockServletContext.class})
public class RenameGroupRepoMongoDBTest extends ParentUtil {

    @MockBean
    private IEventPublisher messageBus;

    @Autowired
    private RenameGroupRepoMongoDB renameGroupRepoMongoDB;


    @BeforeEach
    public void generateDataset() {
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
