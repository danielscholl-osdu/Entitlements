package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.retrievegroup;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.DATA_PARTITION_ID;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataGroupNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getMemberNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getUsersGroupNode;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.Role;
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
public class RetrieveGroupRepoJdbcTest {

    @MockBean
    protected AuditLogger auditLogger;
    @MockBean
    protected RequestInfo requestInfo;
    @MockBean
    protected JaxRsDpsLog logger;

    @Autowired
    private RetrieveGroupRepoJdbc sut;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private JdbcTemplateRunner jdbcTemplateRunner;

    @Test
    public void shouldReturnGroupNodeIfGroupExist() {
        EntityNode group = getUsersGroupNode("x");

        groupRepository.save(GroupInfoEntity.fromEntityNode(group));

        assertNotNull(sut.groupExistenceValidation(group.getNodeId(), DATA_PARTITION_ID));
    }

    @Test
    public void shouldThrow404IfGroupDoesNotExist() {
        try {
            sut.groupExistenceValidation("users.test@domain.com", "dp");
            fail("Should throw exception");
        } catch (AppException ex) {
            assertEquals(404, ex.getError().getCode());
        } catch (Exception ex) {
            fail(String.format("Should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldReturnEmptyListIfNotParentsWhenLoadDirectParents() {
        EntityNode member = getMemberNode("member");

        jdbcTemplateRunner.saveMemberInfoEntity(MemberInfoEntity.fromEntityNode(member, Role.MEMBER));

        List<ParentReference> parents = sut.loadDirectParents(DATA_PARTITION_ID, member.getNodeId());
        assertEquals(0, parents.size());
    }


    @Test
    public void shouldReturnDirectParentListWhenLoadDirectParents() {
        EntityNode member = getMemberNode("member");
        EntityNode group1 = getUsersGroupNode("x");
        EntityNode group2 = getUsersGroupNode("y");
        EntityNode group3 = getUsersGroupNode("z");

        GroupInfoEntity savedGroup1 = groupRepository.save(GroupInfoEntity.fromEntityNode(group1));
        GroupInfoEntity savedGroup2 = groupRepository.save(GroupInfoEntity.fromEntityNode(group2));
        GroupInfoEntity savedGroup3 = groupRepository.save(GroupInfoEntity.fromEntityNode(group3));

        Long memberId = jdbcTemplateRunner.saveMemberInfoEntity(MemberInfoEntity.fromEntityNode(member, Role.MEMBER));

        groupRepository.addChildGroupById(savedGroup2.getId(), savedGroup3.getId());

        groupRepository.addMemberById(savedGroup1.getId(), memberId, Role.MEMBER.getValue());
        groupRepository.addMemberById(savedGroup3.getId(), memberId, Role.MEMBER.getValue());

        List<String> parentIds = sut.loadDirectParents(DATA_PARTITION_ID, member.getNodeId()).stream()
                .map(ParentReference::getId)
                .collect(Collectors.toList());
        assertEquals(2, parentIds.size());

        assertTrue(parentIds.contains(group1.getNodeId()));
        assertTrue(parentIds.contains(group3.getNodeId()));

        assertFalse(parentIds.contains(group2.getNodeId()));
    }

    @Test
    public void shouldReturnEmptySetIfNoParentsWhenLoadAllParents() {
        EntityNode member = getMemberNode("member");

        jdbcTemplateRunner.saveMemberInfoEntity(MemberInfoEntity.fromEntityNode(member, Role.MEMBER));

        ParentTreeDto parents = sut.loadAllParents(member);
        assertEquals(0, parents.getParentReferences().size());
    }

    @Test
    public void shouldReturnAllParentsAndMaxDepthWhenLoadAllParents() {
        EntityNode member = getMemberNode("member");
        EntityNode group1 = getUsersGroupNode("x");
        EntityNode group2 = getUsersGroupNode("y");
        EntityNode group3 = getUsersGroupNode("z");

        GroupInfoEntity savedGroup1 = groupRepository.save(GroupInfoEntity.fromEntityNode(group1));
        GroupInfoEntity savedGroup2 = groupRepository.save(GroupInfoEntity.fromEntityNode(group2));
        GroupInfoEntity savedGroup3 = groupRepository.save(GroupInfoEntity.fromEntityNode(group3));

        Long memberId = jdbcTemplateRunner.saveMemberInfoEntity(MemberInfoEntity.fromEntityNode(member, Role.MEMBER));

        groupRepository.addChildGroupById(savedGroup2.getId(), savedGroup3.getId());

        groupRepository.addMemberById(savedGroup1.getId(), memberId, Role.MEMBER.getValue());
        groupRepository.addMemberById(savedGroup3.getId(), memberId, Role.MEMBER.getValue());

        Set<String> parentIds = sut.loadAllParents(member).getParentReferences().stream()
                .map(ParentReference::getId)
                .collect(Collectors.toSet());
        assertEquals(3, parentIds.size());

        assertTrue(parentIds.contains(group1.getNodeId()));
        assertTrue(parentIds.contains(group2.getNodeId()));
        assertTrue(parentIds.contains(group3.getNodeId()));

    }
}
