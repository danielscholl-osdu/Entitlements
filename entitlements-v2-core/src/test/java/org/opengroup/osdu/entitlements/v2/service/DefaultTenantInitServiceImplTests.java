package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberDto;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.util.FileReaderService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultTenantInitServiceImplTests {

    private final String OWNERS = "{\n" +
            "  \"users\": [\n" +
            "    {\n" +
            "      \"email\": \"SERVICE_PRINCIPAL\",\n" +
            "      \"role\": \"OWNER\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"ownersOf\": [\n" +
            "    {\n" +
            "      \"groupName\": \"groupId1\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"groupName\": \"groupId2\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private final String MEMBERS = "{\n" +
            "  \"users\": [\n" +
            "    {\n" +
            "      \"email\": \"SERVICE_PRINCIPAL\",\n" +
            "      \"role\": \"MEMBER\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"membersOf\": [\n" +
            "    {\n" +
            "      \"groupName\": \"groupId1\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"groupName\": \"groupId2\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private final String GROUPS_WITH_NO_MEMBERS = "{\n" +
            "  \"groups\": [\n" +
            "    {\n" +
            "      \"name\": \"groupId1\",\n" +
            "      \"description\": \"desc\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"groupId2\",\n" +
            "      \"description\": \"desc\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    private final String GROUPS_WITH_MEMBERS = "{\n" +
            "  \"groups\": [\n" +
            "    {\n" +
            "      \"name\": \"groupId1\",\n" +
            "      \"description\": \"desc\",\n" +
            "      \"members\": [\n" +
            "        {\n" +
            "          \"name\": \"member1\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"groupId2\",\n" +
            "      \"description\": \"desc\",\n" +
            "      \"members\": [\n" +
            "        {\n" +
            "          \"name\": \"member2\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Mock
    private RequestInfo requestInfo;
    @Mock
    private JaxRsDpsLog log;
    @Mock
    private FileReaderService fileReaderService;
    @Mock
    private CreateGroupService createGroupService;
    @Mock
    private AddMemberService addMemberService;
    @Mock
    private AppProperties appProperties;
    @Mock
    private RequestInfoUtilService requestInfoUtilService;
    @InjectMocks
    private DefaultTenantInitServiceImpl tenantInitService;

    @Before
    public void setupRequestInfo() {
        DpsHeaders dpsHeaders = mock(DpsHeaders.class);
        when(dpsHeaders.getPartitionId()).thenReturn("dp");
        when(requestInfo.getHeaders()).thenReturn(dpsHeaders);
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setServiceAccount("service_principal_username");
        when(requestInfo.getTenantInfo()).thenReturn(tenantInfo);
        when(requestInfoUtilService.getUserId(dpsHeaders)).thenReturn("desId");
        when(requestInfoUtilService.getDomain(dpsHeaders.getPartitionId())).thenReturn("dp.domain.com");
        final List<String> list = new ArrayList<>();
        list.add("/provisioning/groups/datalake_user_groups.json");
        list.add("/provisioning/groups/datalake_service_groups.json");
        list.add("/provisioning/groups/data_groups.json");
        final List<String> fileNames = new ArrayList<>();
        fileNames.add("groups_of_service_principal.json");
        when(appProperties.getInitialGroups()).thenReturn(list);
        when(appProperties.getGroupsOfInitialUsers()).thenReturn(fileNames);
    }

    @Test
    public void shouldSuccessfullyLoadInitialGroups() {
        prepareFileReaderForGroupsTesting(GROUPS_WITH_NO_MEMBERS);
        EntityNode groupNode1 = prepareGroupNode("Id1");
        EntityNode groupNode2 = prepareGroupNode("Id2");
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                .requesterId("desId")
                .partitionDomain("dp.domain.com")
                .partitionId("dp")
                .build();
        when(createGroupService.run(groupNode1, createGroupServiceDto)).thenReturn(groupNode1);
        when(createGroupService.run(groupNode2, createGroupServiceDto)).thenReturn(groupNode2);

        tenantInitService.createDefaultGroups();

        verify(createGroupService, times(3)).run(groupNode1, createGroupServiceDto);
        verify(createGroupService, times(3)).run(groupNode2, createGroupServiceDto);
        verifyNoMoreInteractions(log, addMemberService);
    }

    @Test
    public void shouldLoadInitialGroupsWithFailure() {
        prepareFileReaderForGroupsTesting(GROUPS_WITH_NO_MEMBERS);
        EntityNode groupNode1 = prepareGroupNode("Id1");
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                .requesterId("desId")
                .partitionDomain("dp.domain.com")
                .partitionId("dp")
                .build();
        Exception exception = new RuntimeException("error");
        when(createGroupService.run(groupNode1, createGroupServiceDto)).thenThrow(exception);
        try {
            tenantInitService.createDefaultGroups();
            fail("Exception should take place");
        } catch (final AppException e) {
            assertEquals(500, e.getError().getCode());
            assertEquals("Internal Server Error", e.getError().getReason());
            assertEquals("Cannot create new group in DB", e.getError().getMessage());
        }
        verify(log).error("Error creating a group: groupid1 in partition dp", exception);
    }

    @Test
    public void shouldLoadInitialGroupsWithConflictFailure() {
        prepareFileReaderForGroupsTesting(GROUPS_WITH_MEMBERS);
        EntityNode groupNode1 = prepareGroupNode("Id1");
        EntityNode groupNode2 = prepareGroupNode("Id2");
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder().requesterId("desId")
                .partitionDomain("dp.domain.com").partitionId("dp").build();
        AddMemberDto addMemberDto1 = AddMemberDto.builder().email("member1@dp.domain.com").role(Role.MEMBER).build();
        AddMemberDto addMemberDto2 = AddMemberDto.builder().email("member2@dp.domain.com").role(Role.MEMBER).build();
        AddMemberServiceDto addMemberServiceDto1 = AddMemberServiceDto.builder().requesterId("desId").groupEmail("groupid1@dp.domain.com")
                .partitionId("dp").build();
        AddMemberServiceDto addMemberServiceDto2 = AddMemberServiceDto.builder().requesterId("desId").groupEmail("groupid2@dp.domain.com")
                .partitionId("dp").build();
        when(createGroupService.run(groupNode1, createGroupServiceDto))
                .thenThrow(new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), "This group already exists"));
        when(createGroupService.run(groupNode2, createGroupServiceDto))
                .thenThrow(new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), "This group already exists"));

        doThrow(new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), String.format("%s is already a member of group %s", addMemberDto1.getEmail(), addMemberServiceDto1.getGroupEmail())))
                .when(addMemberService).run(addMemberDto1, addMemberServiceDto1);
        doThrow(new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), String.format("%s is already a member of group %s", addMemberDto2.getEmail(), addMemberServiceDto2.getGroupEmail())))
                .when(addMemberService).run(addMemberDto2, addMemberServiceDto2);
        tenantInitService.createDefaultGroups();

        verify(createGroupService, times(3)).run(groupNode1, createGroupServiceDto);
        verify(createGroupService, times(3)).run(groupNode2, createGroupServiceDto);
        verify(addMemberService, times(3)).run(addMemberDto1, addMemberServiceDto1);
        verify(addMemberService, times(3)).run(addMemberDto2, addMemberServiceDto2);
        verifyNoMoreInteractions(log, addMemberService);
    }

    @Test
    public void shouldSuccessfullyLoadInitialGroupsWithMembers() {
        prepareFileReaderForGroupsTesting(GROUPS_WITH_MEMBERS);
        EntityNode groupNode1 = prepareGroupNode("Id1");
        EntityNode groupNode2 = prepareGroupNode("Id2");
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder().requesterId("desId")
                .partitionDomain("dp.domain.com").partitionId("dp").build();
        AddMemberDto addMemberDto1 = AddMemberDto.builder().email("member1@dp.domain.com").role(Role.MEMBER).build();
        AddMemberDto addMemberDto2 = AddMemberDto.builder().email("member2@dp.domain.com").role(Role.MEMBER).build();
        AddMemberServiceDto addMemberServiceDto1 = AddMemberServiceDto.builder().groupEmail("groupid1@dp.domain.com").requesterId("desId")
                .partitionId("dp").build();
        AddMemberServiceDto addMemberServiceDto2 = AddMemberServiceDto.builder().groupEmail("groupid2@dp.domain.com").requesterId("desId")
                .partitionId("dp").build();
        when(createGroupService.run(groupNode1, createGroupServiceDto)).thenReturn(groupNode1);
        when(createGroupService.run(groupNode2, createGroupServiceDto)).thenReturn(groupNode2);

        tenantInitService.createDefaultGroups();

        verify(createGroupService, times(3)).run(groupNode1, createGroupServiceDto);
        verify(createGroupService, times(3)).run(groupNode2, createGroupServiceDto);
        verify(addMemberService, times(3)).run(addMemberDto1, addMemberServiceDto1);
        verify(addMemberService, times(3)).run(addMemberDto2, addMemberServiceDto2);
        verifyNoMoreInteractions(log, addMemberService);
    }

    @Test
    public void shouldSuccessfullyLoadServicePrincipal() {
        prepareFileReaderForUsersTesting();
        AddMemberDto addMemberDto = AddMemberDto.builder().email("service_principal_username").role(Role.OWNER).build();
        AddMemberServiceDto addMemberServiceDto1 = AddMemberServiceDto.builder().groupEmail("groupid1@dp.domain.com").requesterId("desId")
                .partitionId("dp").build();
        AddMemberServiceDto addMemberServiceDto2 = AddMemberServiceDto.builder().groupEmail("groupid2@dp.domain.com").requesterId("desId")
                .partitionId("dp").build();

        tenantInitService.bootstrapInitialAccounts();

        verify(addMemberService).run(addMemberDto, addMemberServiceDto1);
        verify(addMemberService).run(addMemberDto, addMemberServiceDto2);
        verifyNoMoreInteractions(log);
    }

    @Test
    public void shouldSuccessfullyLoadInitialAccounts() {
        when(fileReaderService.readFile("groups_of_service_principal.json")).thenReturn(MEMBERS);
        AddMemberDto addMemberDto = AddMemberDto.builder().email("service_principal_username").role(Role.MEMBER).build();
        AddMemberServiceDto addMemberServiceDto1 = AddMemberServiceDto.builder().groupEmail("groupid1@dp.domain.com").requesterId("desId")
                .partitionId("dp").build();
        AddMemberServiceDto addMemberServiceDto2 = AddMemberServiceDto.builder().groupEmail("groupid2@dp.domain.com").requesterId("desId")
                .partitionId("dp").build();

        tenantInitService.bootstrapInitialAccounts();

        verify(addMemberService).run(addMemberDto, addMemberServiceDto1);
        verify(addMemberService).run(addMemberDto, addMemberServiceDto2);
        verifyNoMoreInteractions(log);
    }

    @Test
    public void shouldLoadInitialGroupsWithMembersWithFailure() {
        prepareFileReaderForGroupsTesting(GROUPS_WITH_MEMBERS);
        EntityNode groupNode1 = prepareGroupNode("Id1");
        EntityNode groupNode2 = prepareGroupNode("Id2");
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder().requesterId("desId")
                .partitionDomain("dp.domain.com").partitionId("dp").build();
        AddMemberDto addMemberDto1 = AddMemberDto.builder().email("member1@dp.domain.com").role(Role.MEMBER).build();
        AddMemberServiceDto addMemberServiceDto1 = AddMemberServiceDto.builder().groupEmail("groupid1@dp.domain.com").requesterId("desId")
                .partitionId("dp").build();
        when(createGroupService.run(groupNode1, createGroupServiceDto)).thenReturn(groupNode1);
        when(createGroupService.run(groupNode2, createGroupServiceDto)).thenReturn(groupNode2);
        Exception exception = new RuntimeException("error");
        doThrow(exception).when(addMemberService).run(addMemberDto1, addMemberServiceDto1);

        try {
            tenantInitService.createDefaultGroups();
            fail("Exception should take place");
        } catch (final AppException e) {
            assertEquals(500, e.getError().getCode());
            assertEquals("Internal Server Error", e.getError().getReason());
            assertEquals("Cannot add member to a group", e.getError().getMessage());
        }
        verify(log).error("Error at adding member (member1@dp.domain.com) to a group (groupid1@dp.domain.com) in partition dp", exception);
    }

    private EntityNode prepareGroupNode(String postfix) {
        DpsHeaders dpsHeaders = requestInfo.getHeaders();
        return CreateGroupDto.createGroupNode(new CreateGroupDto(("group" + postfix).toLowerCase(), "desc"),
                requestInfoUtilService.getDomain(dpsHeaders.getPartitionId()), dpsHeaders.getPartitionId());
    }

    private void prepareFileReaderForGroupsTesting(final String expectedJson) {
        when(fileReaderService.readFile("/provisioning/groups/datalake_user_groups.json")).thenReturn(expectedJson);
        when(fileReaderService.readFile("/provisioning/groups/datalake_service_groups.json")).thenReturn(expectedJson);
        when(fileReaderService.readFile("/provisioning/groups/data_groups.json")).thenReturn(expectedJson);
    }

    private void prepareFileReaderForUsersTesting() {
        when(fileReaderService.readFile("groups_of_service_principal.json")).thenReturn(OWNERS);
    }
}
