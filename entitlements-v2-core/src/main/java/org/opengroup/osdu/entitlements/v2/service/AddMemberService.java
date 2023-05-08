package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberDto;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeAction;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeType;
import org.opengroup.osdu.entitlements.v2.provider.interfaces.IMessageBus;
import org.opengroup.osdu.entitlements.v2.spi.addmember.AddMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddMemberService {

    private final RetrieveGroupRepo retrieveGroupRepo;
    private final AddMemberRepo addMemberRepo;
    private final AppProperties config;
    private final JaxRsDpsLog log;
    private final PermissionService permissionService;
    private final GroupCacheService groupCacheService;
    private final IMessageBus messageBus;
    private final RequestInfo requestInfo;

    /**
     * Add Member only allows to create a member node for a new user (first time add a user to a data partition), but not for a group.
     * For a group, the group must exist before it can be added into another group, otherwise, it will mess up the data.
     * The problem is that request body only ask for member email, so we still need to check the member email to decide if it
     * is a valid request.
     */

    public void run(AddMemberDto addMemberDto, AddMemberServiceDto addMemberServiceDto) {
        log.info(String.format("requested by %s", addMemberServiceDto.getRequesterId()));
        String memberDesId = addMemberDto.getEmail();
        EntityNode memberNode = retrieveGroupRepo.getEntityNode(memberDesId, addMemberServiceDto.getPartitionId()).orElseGet(
                () -> createNewMemberNode(addMemberDto.getEmail(), memberDesId, addMemberServiceDto.getPartitionId()));
        EntityNode existingGroupEntityNode = retrieveGroupRepo.groupExistenceValidation(addMemberServiceDto.getGroupEmail(), addMemberServiceDto.getPartitionId());
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester(addMemberServiceDto.getRequesterId(), addMemberServiceDto.getPartitionId());
        permissionService.verifyCanManageMembers(requesterNode, existingGroupEntityNode);
        if (memberNode.isGroup() && Role.OWNER.equals(addMemberDto.getRole())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Group can only be MEMBER of another group");
        }
        if (memberNode.getDirectChildReference(retrieveGroupRepo, existingGroupEntityNode).isPresent()) {
            throw new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), String.format("%s is already a member of group %s", addMemberDto.getEmail(), addMemberServiceDto.getGroupEmail()));
        }
        Set<ParentReference> allExistingParents = retrieveGroupRepo.loadAllParents(memberNode).getParentReferences();
        Set<ParentReference> allExistingParentsFilteredByPartition =
                allExistingParents.stream().filter(ref -> ref.getDataPartitionId().equalsIgnoreCase(addMemberServiceDto.getPartitionId())).collect(Collectors.toSet());
        if (allExistingParentsFilteredByPartition.size() >= EntityNode.MAX_PARENTS) {
            log.error(String.format("Identity %s already belong to %d groups", addMemberDto.getEmail(), allExistingParents.size()));
            throw new AppException(HttpStatus.PRECONDITION_FAILED.value(), HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), String.format("%s's group quota hit. Identity can't belong to more than %d groups", addMemberDto.getEmail(), EntityNode.MAX_PARENTS));
        }
        Set<ParentReference> allParentsOfGroup = retrieveGroupRepo.loadAllParents(existingGroupEntityNode).getParentReferences();
        Set<String> allParentsOfGroupEmails = allParentsOfGroup.stream().map(ref -> ref.getId()).collect(Collectors.toSet());
        if (allParentsOfGroupEmails.contains(addMemberDto.getEmail()) || existingGroupEntityNode.getNodeId().equalsIgnoreCase(addMemberDto.getEmail())) {            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Cyclic membership is not allowed");
        }
        AddMemberRepoDto addMemberRepoDto = AddMemberRepoDto.builder().memberNode(memberNode).role(addMemberDto.getRole()).
                partitionId(addMemberServiceDto.getPartitionId()).existingParents(allExistingParents).build();
        Set<String> impactedUsers = addMemberRepo.addMember(existingGroupEntityNode, addMemberRepoDto);
        groupCacheService.refreshListGroupCache(impactedUsers, addMemberServiceDto.getPartitionId());
        publishAddMemberEntitlementsChangeEvent(addMemberDto, addMemberServiceDto);
    }

    private EntityNode createNewMemberNode(String memberPrimaryId, String memberDesId, String partitionId) {
        if (!memberPrimaryId.endsWith(String.format("@%s.%s", partitionId, config.getDomain()))) {
            return EntityNode.createMemberNodeForNewUser(memberDesId, partitionId);
        } else {
            throw new AppException(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), String.format("Member group %s not found", memberPrimaryId));
        }
    }

    private void publishAddMemberEntitlementsChangeEvent(AddMemberDto addMemberDto, AddMemberServiceDto addMemberServiceDto) {
        DpsHeaders headers = requestInfo.getHeaders();
        EntitlementsChangeEvent event = EntitlementsChangeEvent.builder()
                .kind(EntitlementsChangeType.groupChanged)
                .group(addMemberServiceDto.getGroupEmail())
                .user(addMemberDto.getEmail())
                .action(EntitlementsChangeAction.add)
                .modifiedBy(addMemberServiceDto.getRequesterId())
                .modifiedOn(System.currentTimeMillis()).build();
        messageBus.publishMessage(headers, event);
    }
}
