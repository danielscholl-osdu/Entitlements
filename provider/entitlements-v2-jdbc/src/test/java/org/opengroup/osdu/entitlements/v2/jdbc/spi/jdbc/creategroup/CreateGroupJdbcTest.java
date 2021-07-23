package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.creategroup;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.DATA_PARTITION_ID;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataRootGroupNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getRequesterNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getUsersGroupNode;

import java.util.Collections;
import java.util.List;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
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
public class CreateGroupJdbcTest {
    @MockBean
    protected AuditLogger auditLogger;
    @MockBean
    protected RequestInfo requestInfo;
    @MockBean
    protected JaxRsDpsLog logger;

    @Autowired
    private CreateGroupRepoJdbc sut;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private GroupRepository groupRepository;

    @Test
    public void should_updateReference_whenCreateGroup_andNotAddDataRootGroup() {
        EntityNode groupNode = getUsersGroupNode("x");
        EntityNode requesterNode = getRequesterNode();
        CreateGroupRepoDto createGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .dataRootGroupNode(null)
                .addDataRootGroup(false)
                .partitionId(DATA_PARTITION_ID).build();

        //when
        sut.createGroup(groupNode, createGroupRepoDto);

        //then
        List<GroupInfoEntity> actual = groupRepository.findByEmail(groupNode.getNodeId());

        assertEquals(1, actual.size());

        GroupInfoEntity actualGroup = actual.get(0);

        assertEquals(groupNode.getNodeId(), actualGroup.getEmail());
        assertEquals(groupNode.getName(), actualGroup.getName());
        assertEquals(groupNode.getDataPartitionId(), actualGroup.getPartitionId());

        List<MemberInfoEntity> actualGroupOwners = memberRepository.findMembersByGroup(actualGroup.getId());

        assertEquals(1, actualGroupOwners.size());

        MemberInfoEntity actualOwner = actualGroupOwners.get(0);

        assertEquals(requesterNode.getNodeId(), actualOwner.getEmail());
        assertEquals(Role.OWNER.getValue(), actualOwner.getRole());

        verify(auditLogger).createGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
    }

    @Test
    public void should_updateReference_whenCreateGroup_andAddDataRootGroup() {
        EntityNode groupNode = getUsersGroupNode("x");
        EntityNode requesterNode = getRequesterNode();
        EntityNode dataRootGroupNode = getDataRootGroupNode();

        CreateGroupRepoDto createRootGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .dataRootGroupNode(null)
                .addDataRootGroup(false)
                .partitionId(DATA_PARTITION_ID).build();
        CreateGroupRepoDto createChildGroupRepoDto = CreateGroupRepoDto.builder()
                .requesterNode(requesterNode)
                .dataRootGroupNode(dataRootGroupNode)
                .addDataRootGroup(true)
                .partitionId(DATA_PARTITION_ID).build();


        sut.createGroup(dataRootGroupNode, createRootGroupRepoDto);

        //when
        sut.createGroup(groupNode, createChildGroupRepoDto);

        //then
        List<GroupInfoEntity> actual = groupRepository.findByEmail(groupNode.getNodeId());

        assertEquals(1, actual.size());

        GroupInfoEntity actualGroup = actual.get(0);

        assertEquals(groupNode.getNodeId(), actualGroup.getEmail());
        assertEquals(groupNode.getName(), actualGroup.getName());
        assertEquals(groupNode.getDataPartitionId(), actualGroup.getPartitionId());

        List<GroupInfoEntity> actualParents = groupRepository.findDirectParents(Collections.singletonList(actualGroup.getId()));

        assertEquals(1, actualParents.size());

        GroupInfoEntity actualRootGroup = actualParents.get(0);

        assertEquals(dataRootGroupNode.getNodeId(), actualRootGroup.getEmail());
        assertEquals(dataRootGroupNode.getName(), actualRootGroup.getName());
        assertEquals(dataRootGroupNode.getDataPartitionId(), actualRootGroup.getPartitionId());

        List<MemberInfoEntity> actualGroupOwners = memberRepository.findMembersByGroup(actualGroup.getId());

        assertEquals(1, actualGroupOwners.size());

        MemberInfoEntity actualOwner = actualGroupOwners.get(0);

        assertEquals(requesterNode.getNodeId(), actualOwner.getEmail());
        assertEquals(Role.OWNER.getValue(), actualOwner.getRole());

        verify(auditLogger).createGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
    }
}
