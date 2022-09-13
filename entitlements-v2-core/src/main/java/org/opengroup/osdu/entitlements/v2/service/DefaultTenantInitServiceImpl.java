package org.opengroup.osdu.entitlements.v2.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class DefaultTenantInitServiceImpl implements TenantInitService {

    private final JaxRsDpsLog log;
    private final RequestInfo requestInfo;
    private final AppProperties appProperties;
    private final AddMemberService addMemberService;
    private final FileReaderService fileReaderService;
    private final CreateGroupService createGroupService;
    private final RequestInfoUtilService requestInfoUtilService;

    @Override
    public void createDefaultGroups() {
        appProperties.getInitialGroups().forEach(this::bootstrapGroups);
    }

    @Override
    public void bootstrapInitialAccounts() {
        final Map<String, String> userEmails = createUserEmails();
        List<String> fileNames = appProperties.getGroupsOfInitialUsers();
        for (String fileName : fileNames) {
            final String fileContent = fileReaderService.readFile(fileName);
            final JsonObject userElement = getUserJsonObject(fileContent);
            final String emailKey = userElement.get("email").getAsString();
            final String role = userElement.get("role").getAsString();
            final List<String> groupNames;
            if ("OWNER".equalsIgnoreCase(role)) {
                groupNames = getGroupNamesForOwner(fileContent);
            } else {
                groupNames = getGroupNamesForMember(fileContent);
            }
            final AddMemberDto addMemberDto = AddMemberDto.builder()
                    .email(userEmails.get(emailKey))
                    .role(Role.valueOf(role.toUpperCase()))
                    .build();
            String partitionId = requestInfo.getHeaders().getPartitionId();
            String partitionDomain = requestInfoUtilService.getDomain(partitionId);
            final String requesterId = requestInfoUtilService.getUserId(requestInfo.getHeaders());
            groupNames.stream()
                    .map(name -> createEmail(name, partitionDomain))
                    .forEach(groupId -> {
                        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                                .groupEmail(groupId)
                                .partitionId(partitionId)
                                .requesterId(requesterId)
                                .build();
                        addMemberToGroup(addMemberDto, addMemberServiceDto);
                    });
        }
    }

    private void bootstrapGroups(final String fileName) {
        final JsonArray array = getGroupsFromJson(fileName);
        List<EntityNode> result = new ArrayList<>();
        array.forEach(element -> result.add(createGroup(element)));
        final String requesterId = requestInfoUtilService.getUserId(requestInfo.getHeaders());
        final String partitionId = requestInfo.getHeaders().getPartitionId();
        final String partitionDomain = requestInfoUtilService.getDomain(partitionId);
        final CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                .requesterId(requesterId)
                .partitionId(partitionId)
                .partitionDomain(partitionDomain)
                .build();
        final Map<String, String> groupIdsByName = new HashMap<>();
        result.forEach(group -> groupIdsByName.put(group.getName().toLowerCase(), createGroup(group, createGroupServiceDto)));
        getMembersPerGroup(array).forEach((key, value) -> {
            AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                    .groupEmail(groupIdsByName.get(key.toLowerCase()))
                    .partitionId(partitionId)
                    .requesterId(requesterId)
                    .build();
            value.forEach(member -> {
                AddMemberDto addMemberDto = AddMemberDto.builder()
                        .email(createEmail(member, partitionDomain))
                        .role(Role.MEMBER)
                        .build();
                addMemberToGroup(addMemberDto, addMemberServiceDto);
            });
        });
    }

    private JsonArray getGroupsFromJson(final String fileName) {
        String fileContent = fileReaderService.readFile(fileName);
        return JsonParser.parseString(fileContent)
                .getAsJsonObject()
                .get("groups")
                .getAsJsonArray();
    }

    private EntityNode createGroup(final JsonElement element) {
        final JsonObject jsonObject = element.getAsJsonObject();
        final String name = jsonObject.get("name").getAsString();
        final String desc = jsonObject.get("description").getAsString();
        String partitionIdHeader = requestInfo.getHeaders().getPartitionId();
        String partitionDomain = requestInfoUtilService.getDomain(partitionIdHeader);
        return CreateGroupDto.createGroupNode(new CreateGroupDto(name, desc), partitionDomain, partitionIdHeader);
    }

    private Map<String, List<String>> getMembersPerGroup(final JsonArray array) {
        Map<String, List<String>> membersPerGroup = new LinkedHashMap<>();
        array.forEach(element -> {
            List<String> members = getMembers(element);
            if (!members.isEmpty()) {
                membersPerGroup.put(getGroupName(element), members);
            }
        });
        return membersPerGroup;
    }

    private List<String> getMembers(final JsonElement element) {
        final List<String> members = new ArrayList<>();
        JsonElement membersElement = element.getAsJsonObject().get("members");
        if (membersElement == null) {
            return members;
        }
        membersElement.getAsJsonArray()
                .forEach(jsonElement -> members.add(jsonElement.getAsJsonObject().get("name").getAsString()));
        return members;
    }

    private String getGroupName(final JsonElement element) {
        return element.getAsJsonObject().get("name").getAsString();
    }

    private void addMemberToGroup(final AddMemberDto addMemberDto, final AddMemberServiceDto addMemberServiceDto) {
        try {
            addMemberService.run(addMemberDto, addMemberServiceDto);
        } catch (Exception e) {
            if (isNotConflictException(e, "is already a member of group")) {
                log.error(String.format("Error at adding member (%s) to a group (%s) in partition %s", addMemberDto.getEmail(),
                        addMemberServiceDto.getGroupEmail(), addMemberServiceDto.getPartitionId()), e);
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Cannot add member to a group");
            }
        }
    }

    private boolean isNotConflictException(Exception e, final String expectedErrorMessage) {
        return !isConflictException(e, expectedErrorMessage);
    }

    private boolean isConflictException(Exception e, final String expectedErrorMessage) {
        return e instanceof AppException
                && ((AppException) e).getError().getCode() == HttpStatus.CONFLICT.value()
                && ((AppException) e).getError().getMessage().contains(expectedErrorMessage);
    }

    private String createEmail(String name, String partitionDomain) {
        return String.format("%s@%s", name.toLowerCase(), partitionDomain.toLowerCase());
    }

    private String createGroup(final EntityNode group, final CreateGroupServiceDto createGroupServiceDto) {
        try {
            return createGroupService.run(group, createGroupServiceDto).getNodeId();
        } catch (final Exception e) {
            if (isNotConflictException(e, "This group already exists")) {
                log.error(String.format("Error creating a group: %s in partition %s", group.getName(), createGroupServiceDto.getPartitionId()), e);
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Cannot create new group in DB");
            }
            return group.getNodeId();
        }
    }

    private Map<String, String> createUserEmails() {
        Map<String, String> userEmails = new HashMap<>();
        userEmails.put("SERVICE_PRINCIPAL", requestInfo.getTenantInfo().getServiceAccount());
        return userEmails;
    }

    private JsonObject getUserJsonObject(String fileContent) {
        return JsonParser.parseString(fileContent)
                .getAsJsonObject()
                .get("users")
                .getAsJsonArray()
                .iterator()
                .next()
                .getAsJsonObject();
    }

    private List<String> getGroupNamesForOwner(final String fileContent) {
        final List<String> groupNames = new ArrayList<>();
        final JsonArray array = JsonParser.parseString(fileContent)
                .getAsJsonObject()
                .get("ownersOf")
                .getAsJsonArray();
        array.forEach(element -> groupNames.add(element.getAsJsonObject().get("groupName").getAsString()));
        return groupNames;
    }

    private List<String> getGroupNamesForMember(final String fileContent) {
        final List<String> groupNames = new ArrayList<>();
        final JsonArray array = JsonParser.parseString(fileContent)
                .getAsJsonObject()
                .get("membersOf")
                .getAsJsonArray();
        array.forEach(element -> groupNames.add(element.getAsJsonObject().get("groupName").getAsString()));
        return groupNames;
    }
}
