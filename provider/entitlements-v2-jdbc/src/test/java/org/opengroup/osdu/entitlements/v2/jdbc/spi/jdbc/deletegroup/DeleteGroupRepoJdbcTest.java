package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.deletegroup;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getCommonGroup;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataGroupNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getUsersGroupNode;

import java.util.Collections;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit4.SpringRunner;

@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:db/create-db-script.sql")
@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:db/drop-db-script.sql")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@SpringBootTest
@RunWith(SpringRunner.class)
public class DeleteGroupRepoJdbcTest {

    @MockBean
    private RequestInfo requestInfo;
    @MockBean
    private AuditLogger auditLogger;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private RequestInfoUtilService requestInfoUtilService;
    @Autowired
    private DeleteGroupRepoJdbc deleteGroupRepoJdbc;
    @Autowired
    private GroupRepository groupRepository;

    @Test
    public void shouldDeleteGroupAndPreserveParents() {
        EntityNode groupToDelete = getUsersGroupNode("x");

        EntityNode childGroup1 = getDataGroupNode("x");
        EntityNode childGroup2 = getDataGroupNode("y");
        EntityNode childGroup3 = getDataGroupNode("z");

        GroupInfoEntity savedChild1 = groupRepository.save(GroupInfoEntity.fromEntityNode(childGroup1));
        GroupInfoEntity savedChild2 = groupRepository.save(GroupInfoEntity.fromEntityNode(childGroup2));
        GroupInfoEntity savedChild3 = groupRepository.save(GroupInfoEntity.fromEntityNode(childGroup3));
        GroupInfoEntity savedGroupToDelete = groupRepository.save(GroupInfoEntity.fromEntityNode(groupToDelete));

        groupRepository.addChildGroupById(savedGroupToDelete.getId(), savedChild1.getId());
        groupRepository.addChildGroupById(savedGroupToDelete.getId(), savedChild2.getId());
        groupRepository.addChildGroupById(savedGroupToDelete.getId(), savedChild3.getId());

        //when
        deleteGroupRepoJdbc.deleteGroup(groupToDelete);

        //then
        assertTrue(groupRepository.findByEmail(groupToDelete.getNodeId()).isEmpty());

        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedChild1.getId())).isEmpty());
        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedChild2.getId())).isEmpty());
        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedChild3.getId())).isEmpty());

        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, groupToDelete.getNodeId());
    }

    @Test
    public void shouldDeleteGroupAndPreserveChildren() {
        EntityNode groupToDelete = getUsersGroupNode("x");

        EntityNode parentGroup1 = getDataGroupNode("x");
        EntityNode parentGroup2 = getDataGroupNode("y");
        EntityNode parentGroup3 = getDataGroupNode("z");

        GroupInfoEntity savedParent1 = groupRepository.save(GroupInfoEntity.fromEntityNode(parentGroup1));
        GroupInfoEntity savedParent2 = groupRepository.save(GroupInfoEntity.fromEntityNode(parentGroup2));
        GroupInfoEntity savedParent3 = groupRepository.save(GroupInfoEntity.fromEntityNode(parentGroup3));
        GroupInfoEntity savedGroupToDelete = groupRepository.save(GroupInfoEntity.fromEntityNode(groupToDelete));

        groupRepository.addChildGroupById(savedParent1.getId(), savedGroupToDelete.getId());
        groupRepository.addChildGroupById(savedParent2.getId(), savedGroupToDelete.getId());
        groupRepository.addChildGroupById(savedParent3.getId(), savedGroupToDelete.getId());

        //when
        deleteGroupRepoJdbc.deleteGroup(groupToDelete);

        //then
        assertTrue(groupRepository.findByEmail(groupToDelete.getNodeId()).isEmpty());

        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedParent1.getId())).isEmpty());
        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedParent2.getId())).isEmpty());
        assertTrue(groupRepository.findDirectParents(Collections.singletonList(savedParent3.getId())).isEmpty());

        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, groupToDelete.getNodeId());
    }

    @Test(expected = AppException.class)
    public void shouldReturnIfTheGivenGroupIsNotFoundWhenDeleteGroup() throws Exception{
        EntityNode groupNode = getCommonGroup("newgroup");

        deleteGroupRepoJdbc.deleteGroup(groupNode);

        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, groupNode.getNodeId());

    }

}
