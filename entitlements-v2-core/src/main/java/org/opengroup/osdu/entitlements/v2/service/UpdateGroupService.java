package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeAction;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeType;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.renamegroup.RenameGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.updateappids.UpdateAppIdsRepo;
import org.opengroup.osdu.entitlements.v2.util.GroupCreationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UpdateGroupService {

    private final RetrieveGroupRepo retrieveGroupRepo;
    private final GroupCacheService groupCacheService;
    private final RenameGroupRepo renameGroupRepo;
    private final UpdateAppIdsRepo updateAppIdsRepo;
    private final DefaultGroupsService defaultGroupsService;
    private final PermissionService permissionService;
    private final RequestInfo requestInfo;
    @Autowired
    private IEventPublisher eventPublisher;
    @Value("${event-publishing.enabled:false}")
    private Boolean eventPublishingEnabled;

    public UpdateGroupResponseDto updateGroup(UpdateGroupServiceDto updateGroupServiceDto) {
        String existingGroupEmail = updateGroupServiceDto.getExistingGroupEmail();
        String existingGroupName = existingGroupEmail.split("@")[0];
        String partitionId = updateGroupServiceDto.getPartitionId();
        String partitionDomain = updateGroupServiceDto.getPartitionDomain();

        validateGroupName(existingGroupName);
        EntityNode existingGroupEntityNode = retrieveGroupRepo.groupExistenceValidation(existingGroupEmail, partitionId);
        validateGroupOwnerPermission(updateGroupServiceDto.getRequesterId(), partitionId, existingGroupEntityNode);

        List<String> existingAppIds = new ArrayList<>();
        existingAppIds.addAll(existingGroupEntityNode.getAppIds());
        UpdateGroupResponseDto result = new UpdateGroupResponseDto(existingGroupName, existingGroupEmail, existingAppIds);

        Set<String> impactedUsers = new HashSet<>();
        if (updateGroupServiceDto.getRenameOperation() != null) {
            validateIfGroupIsNotDataGroup(existingGroupEmail, existingGroupEntityNode);
            String newGroupName = updateGroupServiceDto.getRenameOperation().getValue().get(0).toLowerCase();
            validateGroupName(newGroupName);

            String newGroupEmail = GroupCreationUtil.createGroupEmail(newGroupName, partitionDomain);
            validateIfNewGroupNameDoesNotExist(newGroupName, partitionId, newGroupEmail);

            impactedUsers.addAll(renameGroupRepo.run(existingGroupEntityNode, newGroupName));
            result.setEmail(newGroupEmail);
            result.setName(newGroupName);
        }

        if (updateGroupServiceDto.getAppIdsOperation() != null) {
            List<String> allowedAppIdsList = updateGroupServiceDto.getAppIdsOperation().getValue();
            impactedUsers.addAll(updateAppIdsRepo.updateAppIds(existingGroupEntityNode, new HashSet<>(allowedAppIdsList)));
            result.setAppIds(allowedAppIdsList);
        }
        groupCacheService.refreshListGroupCache(new HashSet<>(impactedUsers), updateGroupServiceDto.getPartitionId());
        publishRenameGroupEntitlementsChangeEvent(updateGroupServiceDto);
        return result;
    }

    private void validateIfNewGroupNameDoesNotExist(String newGroupName, String partitionId, String newGroupEmail) {
        if (retrieveGroupRepo.getEntityNode(newGroupEmail, partitionId).isPresent()) {
            String errorMessage = String.format("Invalid group name : \"%s\", it already exists", newGroupName);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), errorMessage);
        }
    }

    private void validateIfGroupIsNotDataGroup(String groupEmail, EntityNode existingGroupEntityNode) {
        if (existingGroupEntityNode.isDataGroup()) {
            String errorMessage = String.format("Invalid group, given group email : \"%s\" is a data group", groupEmail);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), errorMessage);
        }
    }

    private void validateGroupName(String groupName) {
        if (defaultGroupsService.isDefaultGroupName(groupName)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Invalid group, group update API cannot work with bootstrapped groups");
        }
    }

    private void validateGroupOwnerPermission(String requesterId, String partitionId, EntityNode existingGroupEntityNode) {
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester(requesterId, partitionId);
        permissionService.verifyCanManageMembers(requesterNode, existingGroupEntityNode);
    }

    private void publishRenameGroupEntitlementsChangeEvent(UpdateGroupServiceDto updateGroupServiceDto) {
        if(eventPublishingEnabled) {
            if (updateGroupServiceDto.getRenameOperation() != null) {
                String newGroupName = updateGroupServiceDto.getRenameOperation().getValue().get(0).toLowerCase();
                String newGroupEmail = GroupCreationUtil.createGroupEmail(newGroupName, updateGroupServiceDto.getPartitionDomain());

                EntitlementsChangeEvent[] event = {
                        EntitlementsChangeEvent.builder()
                                .kind(EntitlementsChangeType.groupChanged)
                                .group(updateGroupServiceDto.getExistingGroupEmail())
                                .updatedGroupEmail(newGroupEmail)
                                .action(EntitlementsChangeAction.replace)
                                .modifiedBy(updateGroupServiceDto.getRequesterId())
                                .modifiedOn(System.currentTimeMillis()).build()
                };
                eventPublisher.publish(event, requestInfo.getHeaders().getHeaders());
            }
        }
    }
}
