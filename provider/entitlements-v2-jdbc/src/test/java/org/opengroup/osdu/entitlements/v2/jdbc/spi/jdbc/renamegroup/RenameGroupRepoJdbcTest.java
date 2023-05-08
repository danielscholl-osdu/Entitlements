package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.renamegroup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getUsersGroupNode;

import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.SpiJdbcTestConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.addmember.AddMemberRepoJdbc;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = SpiJdbcTestConfig.class)
@RunWith(SpringRunner.class)
public class RenameGroupRepoJdbcTest {

    @MockBean
    private AuditLogger auditLogger;

    @Autowired
    private RenameGroupRepoJdbc sut;

    @MockBean
    private GroupRepository groupRepository;
    @MockBean
    private MemberRepository memberRepository;
    @MockBean
    private JdbcTemplateRunner jdbcTemplateRunner;
    @Autowired
    private AddMemberRepoJdbc addMemberRepoJdbc;

    @Test
    public void shouldRenameGroupSuccessfully() {
        final String newGroupName = "users.y";
        final String newGroupId = "users.y@dp.group.com";

        EntityNode initialGroupNode = getUsersGroupNode("x");
        GroupInfoEntity savedInitialGroup = GroupInfoEntity.fromEntityNode(initialGroupNode);

        when(groupRepository.findByEmail(any())).thenReturn(Collections.singletonList(savedInitialGroup));

        //when
        sut.run(initialGroupNode, newGroupName);

        //then
        verify(groupRepository).update(savedInitialGroup.getId(), newGroupName, newGroupId);
        verify(auditLogger).updateGroup(AuditStatus.SUCCESS, initialGroupNode.getNodeId());
    }
}
