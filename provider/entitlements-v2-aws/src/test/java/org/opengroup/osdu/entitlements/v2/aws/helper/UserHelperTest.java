package org.opengroup.osdu.entitlements.v2.aws.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.entitlements.v2.aws.Util.ParentUtil;
import org.opengroup.osdu.entitlements.v2.aws.config.EntitlementsTestConfig;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper.UserHelper;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.provider.interfaces.IMessageBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.generateUniqueNodeRelationDocs;
import static org.opengroup.osdu.entitlements.v2.aws.Util.NodeGenerator.getNodeRelationDocByIdDoc;
import static org.opengroup.osdu.entitlements.v2.aws.Util.UserDocGenerator.createUserDocsByIds;
import static org.opengroup.osdu.entitlements.v2.aws.Util.UserDocGenerator.generateUniqueUserDoc;
import static org.opengroup.osdu.entitlements.v2.aws.Util.UserDocGenerator.generateUniqueUserDocs;


@DataMongoTest
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@SpringJUnitConfig(classes = {EntitlementsTestConfig.class, MockServletContext.class})
public class UserHelperTest extends ParentUtil {

    @MockBean
    private IMessageBus messageBus;

    @Autowired
    UserHelper userHelper;

    @BeforeEach
    public void cleanup() {
        mongoTemplateHelper.dropCollections();
    }

    @Test
    public void create() {
        //given
        UserDoc userDoc = generateUniqueUserDoc();
        IdDoc docId = userDoc.getId();
        UserDoc fromDb;

        //when
        UserDoc created = userHelper.getOrCreate(userDoc);

        //then
        fromDb = mongoTemplateHelper.findById(docId, UserDoc.class);
        IdDoc existsOrCreatedId = created.getId();
        IdDoc fromDbId = fromDb.getId();
        assertEquals(existsOrCreatedId.getDataPartitionId(), fromDbId.getDataPartitionId());
        assertEquals(existsOrCreatedId.getNodeId(), fromDbId.getNodeId());
    }

    @Test
    public void getOrCreate() {
        //given
        UserDoc userDoc = generateUniqueUserDoc();
        mongoTemplateHelper.insert(userDoc);

        //when
        UserDoc existsOrCreated = userHelper.getOrCreate(userDoc);

        //then
        IdDoc existsOrCreatedId = existsOrCreated.getId();
        assertEquals(existsOrCreatedId.getDataPartitionId(), userDoc.getId().getDataPartitionId());
        assertEquals(existsOrCreatedId.getNodeId(), userDoc.getId().getNodeId());
        userHelper.getOrCreate(userDoc);
        List<UserDoc> all = mongoTemplateHelper.findAll(UserDoc.class);
        assertEquals(1, all.size());
    }

    @Test
    public void save() {
        //given
        UserDoc userDoc = generateUniqueUserDoc();
        IdDoc docId = userDoc.getId();

        //when
        userHelper.save(userDoc);

        //then
        UserDoc fromDb = mongoTemplateHelper.findById(docId, UserDoc.class);
        assertNotNull(fromDb);
        IdDoc fromDbId = fromDb.getId();
        assertEquals(docId.getDataPartitionId(), fromDbId.getDataPartitionId());
        assertEquals(docId.getNodeId(), fromDbId.getNodeId());
    }

    @Test
    public void doubleSave() {
//        given
        UserDoc userDoc = generateUniqueUserDoc();
        mongoTemplateHelper.insert(userDoc);

//        then
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> userHelper.save(userDoc));
    }

    @Test
    void getById() {
        //given
        UserDoc userDoc = generateUniqueUserDoc();
        mongoTemplateHelper.insert(userDoc);
        IdDoc docId = userDoc.getId();

        //when
        UserDoc byId = userHelper.getById(docId);

        //then
        assertEquals(docId.getNodeId(), byId.getId().getNodeId());
        assertEquals(docId.getDataPartitionId(), byId.getId().getDataPartitionId());
    }

    @Test
    void addDirectRelation() {
        //given
        UserDoc userDoc = generateUniqueUserDoc();
        IdDoc docId = userDoc.getId();
        mongoTemplateHelper.insert(userDoc);
        NodeRelationDoc nodeRelationDoc = getNodeRelationDocByIdDoc(null, null);

        //when
        userHelper.addDirectRelation(docId, nodeRelationDoc);

        //then
        UserDoc mongoTemplateById = mongoTemplateHelper.findById(docId, UserDoc.class);

        assertEquals(1, mongoTemplateById.getDirectParents().size());
    }

    @Test
    void addSameParent() {
        //given
        UserDoc userDoc = generateUniqueUserDoc();
        IdDoc docId = userDoc.getId();
        NodeRelationDoc nodeRelationDoc = getNodeRelationDocByIdDoc(docId, Role.MEMBER);
        userDoc.setDirectParents(new HashSet<>(Collections.singletonList(nodeRelationDoc)));
        mongoTemplateHelper.insert(userDoc);

        //when
        userHelper.addDirectRelation(docId, nodeRelationDoc);

        //then
        UserDoc mongoTemplateById = mongoTemplateHelper.findById(docId, UserDoc.class);

        assertEquals(1, mongoTemplateById.getDirectParents().size());
        Set<NodeRelationDoc> parents = mongoTemplateById.getDirectParents();
        NodeRelationDoc fromDbNodeRelationsDoc = parents.stream().findFirst().get();
        assertEquals(docId.getDataPartitionId(), fromDbNodeRelationsDoc.getParentId().getDataPartitionId());
    }

    @Test
    void addSameParentWithDifferentRoles() {
        //given
        UserDoc userDoc = generateUniqueUserDoc();
        IdDoc docId = userDoc.getId();
        NodeRelationDoc nodeRelationDoc = getNodeRelationDocByIdDoc(docId, Role.MEMBER);
        userDoc.setDirectParents(new HashSet<>(Collections.singletonList(nodeRelationDoc)));
        mongoTemplateHelper.insert(userDoc);
        nodeRelationDoc = getNodeRelationDocByIdDoc(docId, Role.OWNER);

        //when
        userHelper.addDirectRelation(docId, nodeRelationDoc);

        //then
        UserDoc mongoTemplateById = mongoTemplateHelper.findById(docId, UserDoc.class);
        assertEquals(2, mongoTemplateById.getDirectParents().size());
        Set<NodeRelationDoc> parents = mongoTemplateById.getDirectParents();
        NodeRelationDoc fromDbNodeRelationsDoc = parents.stream().findFirst().get();
        assertEquals(docId.getDataPartitionId(), fromDbNodeRelationsDoc.getParentId().getDataPartitionId());
    }

    @Test
    void addSameParentWithSameRoles() {
        //given
        UserDoc userDoc = generateUniqueUserDoc();
        IdDoc docId = userDoc.getId();
        NodeRelationDoc nodeRelationDoc = getNodeRelationDocByIdDoc(docId, Role.MEMBER);
        userDoc.setDirectParents(new HashSet<>(Collections.singletonList(nodeRelationDoc)));
        mongoTemplateHelper.insert(userDoc);
        nodeRelationDoc = getNodeRelationDocByIdDoc(docId, Role.MEMBER);

        //when
        userHelper.addDirectRelation(docId, nodeRelationDoc);

        //then
        UserDoc mongoTemplateById = mongoTemplateHelper.findById(docId, UserDoc.class);
        assertEquals(1, mongoTemplateById.getDirectParents().size());
        Set<NodeRelationDoc> parents = mongoTemplateById.getDirectParents();
        NodeRelationDoc fromDbNodeRelationsDoc = parents.stream().findFirst().get();
        assertEquals(docId.getDataPartitionId(), fromDbNodeRelationsDoc.getParentId().getDataPartitionId());
    }


    @Test
    void addMemberRelations() {
        //given
        UserDoc userDoc = generateUniqueUserDoc();
        IdDoc docId = userDoc.getId();
        Set<NodeRelationDoc> nodeRelationDocs = generateUniqueNodeRelationDocs(5, Role.MEMBER);
        userDoc.setAllParents(nodeRelationDocs);
        mongoTemplateHelper.insert(userDoc);
//        get new unique nodeRelationDocs
        Set<NodeRelationDoc> newNodeRelationDocs = generateUniqueNodeRelationDocs(2, Role.MEMBER);
//        merge nodeRelationDocs from db count 2 and news
        Set<NodeRelationDoc> members = Stream.concat(nodeRelationDocs.stream().limit(2), newNodeRelationDocs.stream()).collect(Collectors.toSet());

        //when
        userHelper.addMemberRelations(docId, members);

        //then
        UserDoc mongoTemplateById = mongoTemplateHelper.findById(docId, UserDoc.class);
        assertEquals(7, mongoTemplateById.getAllParents().size());
    }

    @Test
    void testAddMemberRelations() {
        //given
        Set<UserDoc> userDocs = generateUniqueUserDocs(5);
        Set<NodeRelationDoc> nodeRelationDocs = generateUniqueNodeRelationDocs(5, Role.MEMBER);
        userDocs.forEach(userDoc -> userDoc.setAllParents(nodeRelationDocs));
        mongoTemplateHelper.insert(userDocs);
//        get clean docs which we have in db
        Set<UserDoc> userDocsExistsInDb = createUserDocsByIds(userDocs.stream().map(UserDoc::getId).collect(Collectors.toSet()));
//        get new unique docs
        Set<UserDoc> newUserDocs = generateUniqueUserDocs(3);
        Set<UserDoc> userDocSet = Stream.concat(userDocsExistsInDb.stream(), newUserDocs.stream()).collect(Collectors.toSet());
        Set<NodeRelationDoc> otherNodeRelationDocs = generateUniqueNodeRelationDocs(3, Role.MEMBER);
        Set<NodeRelationDoc> collect = Stream.concat(nodeRelationDocs.stream().limit(2), otherNodeRelationDocs.stream()).collect(Collectors.toSet());
        Set<IdDoc> userDocDocIds = userDocSet.stream().map(UserDoc::getId).collect(Collectors.toSet());

        //when
        userHelper.addMemberRelations(userDocDocIds, collect);

        //then
        List<UserDoc> all = mongoTemplateHelper.findAll(UserDoc.class);
        assertEquals(5, all.size());
        all.forEach(userDoc -> assertEquals(8, userDoc.getAllParents().size()));
    }


}