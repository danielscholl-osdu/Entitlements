package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.entitlements.v2.model.deletemember.DeleteMemberDto;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.validation.BootstrapGroupsConfigurationService;

import java.util.Set;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeleteMemberServiceTest {
    private static final String DATA_PARTITION_ID = "common";
    private static final String MEMBER_EMAIL = "member@xxx.com";
    private static final String REQUEST_ID = "test-requester-id";
    @Mock
    private JaxRsDpsLog log;
    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;
    @Mock
    private RemoveMemberService removeMemberService;
    @Mock
    private GroupCacheService groupCacheService;
    @Mock
    private BootstrapGroupsConfigurationService bootstrapGroupsConfigurationService;
    @InjectMocks
    private DeleteMemberService deleteMemberService;

    @Test
    public void shouldSuccessfullyRemoveExistingMember() {
        String domain = "common.contoso.com";
        String groupName1 = "data.x1";
        String groupEmail1 = groupName1 + "@" + domain;
        String groupName2 = "data.x2";
        String groupEmail2 = groupName2 + "@" + domain;

        Set<String> parentEmails = Set.of(
                groupEmail1,
                groupEmail2
        );
        when(removeMemberService.getDirectParentsEmails(DATA_PARTITION_ID, MEMBER_EMAIL)).thenReturn(parentEmails);
        when(bootstrapGroupsConfigurationService.getElementaryDataPartitionUsersGroup(DATA_PARTITION_ID)).thenReturn(String.format("users@%s.%s", DATA_PARTITION_ID, domain));

        DeleteMemberDto deleteMemberDto = DeleteMemberDto.builder()
                .memberEmail(MEMBER_EMAIL)
                .partitionId(DATA_PARTITION_ID)
                .requesterId(REQUEST_ID)
                .build();

        deleteMemberService.deleteMember(deleteMemberDto);

        verify(removeMemberService, times(1)).getDirectParentsEmails(DATA_PARTITION_ID, MEMBER_EMAIL);
        verify(removeMemberService, times(1)).removeMember(buildRemoveMemberServiceDto(groupEmail1));
        verify(removeMemberService, times(1)).removeMember(buildRemoveMemberServiceDto(groupEmail2));
        verify(groupCacheService).flushListGroupCacheForUser(MEMBER_EMAIL, DATA_PARTITION_ID);
    }

    private RemoveMemberServiceDto buildRemoveMemberServiceDto(String groupEmail) {
        return RemoveMemberServiceDto.builder()
                .groupEmail(groupEmail)
                .memberEmail(MEMBER_EMAIL)
                .requesterId(REQUEST_ID)
                .partitionId(DATA_PARTITION_ID)
                .build();
    }
}