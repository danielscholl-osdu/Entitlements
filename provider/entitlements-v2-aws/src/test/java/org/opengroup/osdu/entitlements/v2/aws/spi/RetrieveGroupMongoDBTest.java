package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateGroupNode;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateUserNode;


@DataMongoTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EntitlementsTestConfig.class, MockServletContext.class})
class RetrieveGroupMongoDBTest extends ParentUtil {

    @Autowired
    RetrieveGroupMongoDB retrieveGroupMongoDB;

    @BeforeEach
    public void generateDataset() {
        mongoTemplateHelper.dropCollections();
        initDefaultDataSet();
    }

    @Test
    void groupExistenceValidation() {
        //given
        IdDoc idDoc = createIdForGroup(4);

        //when
        EntityNode groupNode = retrieveGroupMongoDB.groupExistenceValidation(idDoc.getNodeId(), idDoc.getDataPartitionId());

        //then
        GroupDoc groupDoc = mongoTemplateHelper.findById(idDoc, GroupDoc.class);

        assertEquals(groupDoc.getId().getNodeId(), groupNode.getNodeId());
        assertEquals(groupDoc.getId().getDataPartitionId(), groupNode.getDataPartitionId());
        assertEquals(groupDoc.getAppIds().size(), groupNode.getAppIds().size());
        assertEquals(groupDoc.getName(), groupNode.getName());
        assertEquals(NodeType.GROUP, groupNode.getType());
        assertEquals(groupDoc.getDescription(), groupNode.getDescription());

    }

    @Test
    void groupExistenceValidationGroupNotFound() {
        //given
        IdDoc idDoc = createIdForGroup(656564);
        //then
        assertThrows(AppException.class, () -> retrieveGroupMongoDB.groupExistenceValidation(idDoc.getNodeId(), idDoc.getDataPartitionId()));
    }

    @Test
    void groupExistenceValidationEmptyRequest() {
        //then
        assertThrows(AppException.class, () -> retrieveGroupMongoDB.groupExistenceValidation(null, null));
    }

    @Test
    void getEntityNodeGroup() {
        //given
        IdDoc idForGroup = createIdForGroup(1);

        //when
        Optional<EntityNode> groupEntityNodeOptional = retrieveGroupMongoDB.getEntityNode(idForGroup.getNodeId(), idForGroup.getDataPartitionId());

        //then
        assertTrue(groupEntityNodeOptional.isPresent());

        EntityNode groupNode = groupEntityNodeOptional.get();
        GroupDoc groupDoc = mongoTemplateHelper.findById(idForGroup, GroupDoc.class);

        assertEquals(groupDoc.getId().getNodeId(), groupNode.getNodeId());
        assertEquals(groupDoc.getId().getDataPartitionId(), groupNode.getDataPartitionId());
        assertEquals(groupDoc.getAppIds().size(), groupNode.getAppIds().size());
        assertEquals(groupDoc.getName(), groupNode.getName());
        assertEquals(NodeType.GROUP, groupNode.getType());
        assertEquals(groupDoc.getDescription(), groupNode.getDescription());
    }

    @Test
    void getEntityNodeUser() {
        //given
        IdDoc idForUser = createIdForUser(1);
        Optional<EntityNode> userEntityNodeOptional = retrieveGroupMongoDB.getEntityNode(idForUser.getNodeId(), idForUser.getDataPartitionId());
        assertTrue(userEntityNodeOptional.isPresent());
        EntityNode groupNode = userEntityNodeOptional.get();
        //when
        UserDoc userDoc = mongoTemplateHelper.findById(idForUser, UserDoc.class);
        //then
        assertEquals(userDoc.getId().getNodeId(), groupNode.getNodeId());
        assertEquals(userDoc.getId().getDataPartitionId(), groupNode.getDataPartitionId());
        assertEquals(NodeType.USER, groupNode.getType());
    }

    @Test
    void getEntityNodeGroupOrUserNotFound() {
        //given
        IdDoc idForGroup = createIdForUser(8656);
        IdDoc idForUser = createIdForUser(46546);

        //when
        Optional<EntityNode> groupEntityNodeOptional = retrieveGroupMongoDB.getEntityNode(idForGroup.getNodeId(), idForGroup.getDataPartitionId());
        Optional<EntityNode> userEntityNodeOptional = retrieveGroupMongoDB.getEntityNode(idForUser.getNodeId(), idForUser.getDataPartitionId());
        Optional<EntityNode> nodeOptional = retrieveGroupMongoDB.getEntityNode(null, null);

        //then
        assertFalse(groupEntityNodeOptional.isPresent());
        assertFalse(userEntityNodeOptional.isPresent());
        assertFalse(nodeOptional.isPresent());
    }

    @Test
    void getEntityNodes() {
        //given
        IdDoc idForGroup1 = createIdForGroup(1);
        IdDoc idForGroup2 = createIdForGroup(2);
        IdDoc idForUser1 = createIdForUser(1);
        IdDoc idForUser2 = createIdForUser(2);
        IdDoc idForNotExistGroup = createIdForGroup(54687);
        List<IdDoc> idDocs = Arrays.asList(
                idForGroup1, idForGroup2, idForUser1, idForUser2, idForNotExistGroup);
        List<String> stringIds = idDocs
                .stream()
                .map(IdDoc::getNodeId)
                .collect(Collectors.toList());
        //when
        Set<EntityNode> entityNodes = retrieveGroupMongoDB.getEntityNodes(DATA_PARTITION, stringIds);
        //then
        assertEquals(4, entityNodes.size());
        Set<EntityNode> biIds = mongoTemplateHelper.findByIds(idDocs);
        assertTrue(biIds.containsAll(entityNodes));

    }

    @Test
    void hasDirectChild() {
        EntityNode nodeGroup4 = generateGroupNode(4);
        IdDoc idForUser2 = createIdForUser(4);
        ChildrenReference childrenReference = ChildrenReference
                .builder()
                .id(idForUser2.getNodeId())
                .dataPartitionId(idForUser2.getDataPartitionId())
                .role(Role.OWNER)
                .type(NodeType.USER)
                .build();

        Boolean isChild = retrieveGroupMongoDB.hasDirectChild(nodeGroup4, childrenReference);
        assertTrue(isChild);
    }

    @Test
    void hasDirectChildNotChild() {
        EntityNode nodeGroup2 = generateGroupNode(2);
        IdDoc idForUser4 = createIdForUser(4);
        ChildrenReference childrenReference = ChildrenReference
                .builder()
                .id(idForUser4.getNodeId())
                .dataPartitionId(idForUser4.getDataPartitionId())
                .role(Role.OWNER)
                .type(NodeType.USER)
                .build();

        Boolean isChild = retrieveGroupMongoDB.hasDirectChild(nodeGroup2, childrenReference);
        assertFalse(isChild);
    }

    @Test
    void hasDirectChildNotFoundChild() {
        EntityNode nodeGroup4 = generateGroupNode(2);
        IdDoc idForNotExistUser = createIdForUser(634646456);
        ChildrenReference childrenReference = ChildrenReference
                .builder()
                .id(idForNotExistUser.getNodeId())
                .dataPartitionId(idForNotExistUser.getDataPartitionId())
                .role(Role.OWNER)
                .type(NodeType.USER)
                .build();

        Boolean isChild = retrieveGroupMongoDB.hasDirectChild(nodeGroup4, childrenReference);
        assertFalse(isChild);
    }

    @Test
    void hasDirectChildNotFoundGroup() {
        EntityNode nodeGroup4 = generateGroupNode(546566);
        IdDoc idForUser2 = createIdForUser(2);
        ChildrenReference childrenReference = ChildrenReference
                .builder()
                .id(idForUser2.getNodeId())
                .dataPartitionId(idForUser2.getDataPartitionId())
                .role(Role.OWNER)
                .type(NodeType.USER)
                .build();

        Boolean isChild = retrieveGroupMongoDB.hasDirectChild(nodeGroup4, childrenReference);
        assertFalse(isChild);
    }

    @Test
    void loadDirectParents() {
        //given
        IdDoc idForGroup1 = createIdForGroup(1);
        IdDoc idForGroup2 = createIdForGroup(2);
        IdDoc idForGroup3 = createIdForGroup(3);
        List<IdDoc> idDocs = Arrays.asList(idForGroup1, idForGroup2, idForGroup3);
        //when
        List<ParentReference> parentReferences = retrieveGroupMongoDB.loadDirectParents(idForGroup1.getDataPartitionId(),
                idForGroup1.getNodeId(),
                idForGroup2.getNodeId(),
                idForGroup3.getNodeId());
        //then
        List<String> fromDb = idDocs.stream().flatMap(idDoc -> mongoTemplateHelper.findById(idDoc, GroupDoc.class).getDirectParents().stream()).map(nodeRelationDoc -> nodeRelationDoc.getParentId().getNodeId()).collect(Collectors.toList());
        assertEquals(fromDb.size(), parentReferences.size());
        List<String> listIds = parentReferences.stream().map(ParentReference::getId).collect(Collectors.toList());
        assertTrue(fromDb.containsAll(listIds));
    }

    @Disabled //todo enable
    @Test
    void loadAllParentsUser() {
        //given
        EntityNode nodeUser1 = generateUserNode(1);
        //when
        ParentTreeDto parentTreeDto = retrieveGroupMongoDB.loadAllParents(nodeUser1);
        //then
        assertEquals(9, parentTreeDto.getParentReferences().size());
        assertEquals(5, parentTreeDto.getMaxDepth());
    }

    @Test
    void loadAllParentsNotFoundUser() {
        //given
        EntityNode nodeUser1 = generateUserNode(3546456);
        //when
        ParentTreeDto parentTreeDto = retrieveGroupMongoDB.loadAllParents(nodeUser1);
        //then
        assertEquals(0, parentTreeDto.getParentReferences().size());
    }

    @Disabled //todo enable
    @Test
    void loadAllParentsGroup() {
        //given
        EntityNode nodeGroup1 = generateGroupNode(1);
        //when
        ParentTreeDto parentTreeDto = retrieveGroupMongoDB.loadAllParents(nodeGroup1);
        //then
        assertEquals(8, parentTreeDto.getParentReferences().size());
        assertEquals(4, parentTreeDto.getMaxDepth());
    }

    @Test
    void filterParentsByAppId() {
        //given
        EntityNode nodeGroup1 = generateGroupNode(1);
        ParentTreeDto parentTreeDto = retrieveGroupMongoDB.loadAllParents(nodeGroup1);
        IdDoc idDoc = createId(nodeGroup1);
        GroupDoc groupDoc1 = mongoTemplateHelper.findById(idDoc, GroupDoc.class);
        //when
        Set<ParentReference> parentReferences = retrieveGroupMongoDB.filterParentsByAppId(parentTreeDto.getParentReferences(), idDoc.getDataPartitionId(), groupDoc1.getAppIds().stream().findFirst().get());
        //then
        assertEquals(8, parentReferences.size());
    }
}