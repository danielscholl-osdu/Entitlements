package org.opengroup.osdu.entitlements.v2.service;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.renamegroup.RenameGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.updateappids.UpdateAppIdsRepo;
import org.opengroup.osdu.entitlements.v2.util.GroupCreationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class UpdateGroupService {

    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;
    @Autowired
    private RenameGroupRepo renameGroupRepo;
    @Autowired
    private UpdateAppIdsRepo updateAppIdsRepo;
    @Autowired
    private DefaultGroupsService defaultGroupsService;
    @Autowired
    private PermissionService permissionService;

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

        if (updateGroupServiceDto.getRenameOperation() != null) {
            validateIfGroupIsNotDataGroup(existingGroupEmail, existingGroupEntityNode);
            String newGroupName = updateGroupServiceDto.getRenameOperation().getValue().get(0).toLowerCase();
            validateGroupName(newGroupName);

            String newGroupEmail = GroupCreationUtil.createGroupEmail(newGroupName, partitionDomain);
            validateIfNewGroupNameDoesNotExist(newGroupName, partitionId, newGroupEmail);

            renameGroupRepo.run(existingGroupEntityNode, newGroupName);
            result.setEmail(newGroupEmail);
            result.setName(newGroupName);
        }

        if (updateGroupServiceDto.getAppIdsOperation()!= null) {
            List<String> allowedAppIdsList = updateGroupServiceDto.getAppIdsOperation().getValue();
            updateAppIdsRepo.updateAppIds(existingGroupEntityNode, new HashSet<>(allowedAppIdsList));
            result.setAppIds(allowedAppIdsList);
        }

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
        if (!permissionService.hasOwnerPermissionOf(requesterNode, existingGroupEntityNode)) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Not authorized to manage members");
        }
    }
}
