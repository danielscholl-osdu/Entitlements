package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeAction;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeType;
import org.opengroup.osdu.entitlements.v2.validation.BootstrapGroupsConfigurationService;
import org.opengroup.osdu.entitlements.v2.validation.ServiceAccountsConfigurationService;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.removemember.RemoveMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RemoveMemberService {
    private final RemoveMemberRepo removeMemberRepo;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final GroupCacheService groupCacheService;
    private final JaxRsDpsLog log;
    private final ServiceAccountsConfigurationService serviceAccountsConfigurationService;
    private final BootstrapGroupsConfigurationService bootstrapGroupsConfigurationService;
    private final PermissionService permissionService;
    private final RequestInfo requestInfo;
    private final IEventPublisher eventPublisher;
    @Value("${event-publishing.enabled:false}")
    private Boolean eventPublishingEnabled;

    /**
     * @return a set of ids of impacted users
     */
    public Set<String> removeMember(RemoveMemberServiceDto removeMemberServiceDto) {
        log.info(String.format("requested by %s", removeMemberServiceDto.getRequesterId()));
        String groupEmail = removeMemberServiceDto.getGroupEmail();
        String memberEmail = removeMemberServiceDto.getMemberEmail();
        String partitionId = removeMemberServiceDto.getPartitionId();
        EntityNode existingGroupEntityNode = retrieveGroupRepo.groupExistenceValidation(groupEmail, partitionId);
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester(removeMemberServiceDto.getRequesterId(), partitionId);
        permissionService.verifyCanManageMembers(requesterNode, existingGroupEntityNode);
        EntityNode memberNode = retrieveGroupRepo.getMemberNodeForRemovalFromGroup(memberEmail, partitionId);
        removeMemberServiceDto.setChildrenReference(memberNode.getDirectChildReference(retrieveGroupRepo, existingGroupEntityNode).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(), String.format("Group %s does not have"
                    + " %s as a direct child/member. Please check the group hierarchy for an "
                    + "explicit member declaration.", groupEmail, memberEmail))
        ));

        if (serviceAccountsConfigurationService.isMemberProtectedServiceAccount(memberNode, existingGroupEntityNode)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    String.format("Key service accounts hierarchy is enforced, %s cannot be removed from group %s", memberEmail, groupEmail));
        }

        if (memberNode.isUsersDataRootGroup() && existingGroupEntityNode.isDataGroup()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "Users data root group hierarchy is enforced, member users.data.root cannot be removed");
        }

        if (bootstrapGroupsConfigurationService.isMemberProtectedFromRemoval(memberNode, existingGroupEntityNode)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    String.format("Bootstrap group hierarchy is enforced, member %s cannot be removed from group %s",
                            memberNode.getName(), existingGroupEntityNode.getName()));
        }

        Set<String> impactedUsers = removeMemberRepo.removeMember(existingGroupEntityNode, memberNode, removeMemberServiceDto);
        groupCacheService.refreshListGroupCache(impactedUsers, removeMemberServiceDto.getPartitionId());
        publishRemoveMemberEntitlementsChangeEvent(removeMemberServiceDto);
        return impactedUsers;
    }

    private void publishRemoveMemberEntitlementsChangeEvent(RemoveMemberServiceDto removeMemberServiceDto) {
        if (eventPublishingEnabled) {
            EntitlementsChangeEvent[] event = {EntitlementsChangeEvent.builder()
                    .kind(EntitlementsChangeType.groupChanged)
                    .group(removeMemberServiceDto.getGroupEmail())
                    .user(removeMemberServiceDto.getMemberEmail())
                    .action(EntitlementsChangeAction.remove)
                    .modifiedBy(removeMemberServiceDto.getRequesterId())
                    .modifiedOn(System.currentTimeMillis()).build()};
            eventPublisher.publish(event, requestInfo.getHeaders().getHeaders());
        }
    }
}
