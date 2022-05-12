package org.opengroup.osdu.entitlements.v2.aws.spi;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateUserNode;


@DataMongoTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EntitlementsTestConfig.class)
public class AddMemberRepoMongoDBTest extends ParentUtil {

    @Autowired
    private AddMemberRepoMongoDB addMemberRepo;


    @BeforeEach
    public void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    public void addingUserMember() {
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
    public void addingGroupMember() {
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
    public void addingMemberGroupNotExist() {
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
