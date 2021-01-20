package org.opengroup.osdu.entitlements.v2.logging;

import com.google.common.base.Strings;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.util.JsonConverter;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

class AuditEvents {

    private static final String REDIS_BACKUP_ACTION_ID = "ET201";
    private static final String REDIS_BACKUP_MESSAGE = "Back up redis instance";
    private static final String REDIS_LIST_BACKUP_VERSIONS_ACTION_ID = "ET202";
    private static final String REDIS_LIST_BACKUP_VERSIONS_MESSAGE = "List backup version of the redis instance";
    private static final String REDIS_RESTORE_ACTION_ID = "ET203";
    private static final String REDIS_RESTORE_MESSAGE = "Restore redis instance from version %s";
    private static final String REDIS_CREATE_GROUP_TRANSACTION_ACTION_ID = "ET210";
    private static final String REDIS_CREATE_GROUP_TRANSACTION_MESSAGE = "Create group %s";
    private static final String REDIS_DELETE_GROUP_TRANSACTION_ACTION_ID = "ET211";
    private static final String REDIS_DELETE_GROUP_TRANSACTION_MESSAGE = "Delete group %s";
    private static final String REDIS_LIST_GROUP_TRANSACTION_ACTION_ID = "ET212";
    private static final String REDIS_LIST_GROUP_TRANSACTION_MESSAGE = "Return all groups the caller belongs to of the tenant";
    private static final String REDIS_ADD_MEMBER_TRANSACTION_ACTION_ID = "ET213";
    private static final String REDIS_ADD_MEMBER_TRANSACTION_MESSAGE = "Add entity %s to group %s as %s";
    private static final String REDIS_REMOVE_MEMBER_TRANSACTION_ACTION_ID = "ET214";
    private static final String REDIS_REMOVE_MEMBER_TRANSACTION_MESSAGE = "Remove entity %s to group %s as requested by %s";
    private static final String REDIS_LIST_MEMBER_TRANSACTION_ACTION_ID = "ET215";
    private static final String REDIS_LIST_MEMBER_TRANSACTION_MESSAGE = "Return all direct members of group %s";
    private static final String REDIS_UPDATE_APP_IDS_TRANSACTION_ACTION_ID = "ET216";
    private static final String REDIS_UPDATE_APP_IDS_TRANSACTION_MESSAGE = "Update appId for group %s to %s";
    private static final String REDIS_UPDATE_GROUP_TRANSACTION_ACTION_ID = "ET217";
    private static final String REDIS_UPDATE_GROUP_TRANSACTION_MESSAGE = "Update group %s";
    private static final String REDIS_UPDATE_GROUPS_IN_CACHE_FOR_KEYS_ACTION_ID = "ET219";
    private static final String REDIS_UPDATE_GROUPS_IN_CACHE_FOR_KEYS_MESSAGE = "Update groups in cache for given keys";

    private final String user;
    private final String dataPartitionId;

    AuditEvents(String user, String dataPartitionId) {
        if (Strings.isNullOrEmpty(user)) {
            throw new IllegalArgumentException("User not supplied for audit events");
        }

        this.user = user;
        this.dataPartitionId = dataPartitionId;
    }

    AuditPayload getRedisBackupEvent(AuditStatus auditStatus) {
        return AuditPayload.builder()
                .action(AuditAction.JOB_RUN)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_BACKUP_ACTION_ID)
                .message(REDIS_BACKUP_MESSAGE)
                .resources(singletonList(dataPartitionId))
                .build();
    }

    AuditPayload getRedisBackupVersionEvent(AuditStatus auditStatus) {
        return AuditPayload.builder()
                .action(AuditAction.READ)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_LIST_BACKUP_VERSIONS_ACTION_ID)
                .message(REDIS_LIST_BACKUP_VERSIONS_MESSAGE)
                .resources(singletonList(dataPartitionId))
                .build();
    }

    AuditPayload getRedisRestoreEvent(AuditStatus auditStatus, String version) {
        return AuditPayload.builder()
                .action(AuditAction.UPDATE)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_RESTORE_ACTION_ID)
                .message(String.format(REDIS_RESTORE_MESSAGE, version))
                .resources(singletonList(dataPartitionId))
                .build();
    }

    AuditPayload getCreateGroupEvent(AuditStatus auditStatus, String groupId) {
        return AuditPayload.builder()
                .action(AuditAction.CREATE)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_CREATE_GROUP_TRANSACTION_ACTION_ID)
                .message(String.format(REDIS_CREATE_GROUP_TRANSACTION_MESSAGE, groupId))
                .resources(Arrays.asList(dataPartitionId, groupId))
                .build();
    }

    AuditPayload getUpdateGroupEvent(AuditStatus auditStatus, String groupId) {
        return AuditPayload.builder()
                .action(AuditAction.UPDATE)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_UPDATE_GROUP_TRANSACTION_ACTION_ID)
                .message(String.format(REDIS_UPDATE_GROUP_TRANSACTION_MESSAGE, groupId))
                .resources(Arrays.asList(dataPartitionId, groupId))
                .build();
    }

    AuditPayload getDeleteGroupEvent(AuditStatus auditStatus, String groupId) {
        return AuditPayload.builder()
                .action(AuditAction.DELETE)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_DELETE_GROUP_TRANSACTION_ACTION_ID)
                .message(String.format(REDIS_DELETE_GROUP_TRANSACTION_MESSAGE, groupId))
                .resources(Arrays.asList(dataPartitionId, groupId))
                .build();
    }

    AuditPayload getAddMemberEvent(AuditStatus auditStatus, String groupId, String memberId, Role role) {
        return AuditPayload.builder()
                .action(AuditAction.UPDATE)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_ADD_MEMBER_TRANSACTION_ACTION_ID)
                .message(String.format(REDIS_ADD_MEMBER_TRANSACTION_MESSAGE, memberId, groupId, role))
                .resources(Arrays.asList(dataPartitionId, groupId, memberId, role.toString()))
                .build();
    }

    AuditPayload getRemoveMemberEvent(AuditStatus auditStatus, String groupId, String memberId, String requesterId) {
        return AuditPayload.builder()
                .action(AuditAction.DELETE)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_REMOVE_MEMBER_TRANSACTION_ACTION_ID)
                .message(String.format(REDIS_REMOVE_MEMBER_TRANSACTION_MESSAGE, memberId, groupId, requesterId))
                .resources(Arrays.asList(dataPartitionId, groupId, memberId, requesterId))
                .build();
    }

    AuditPayload getListGroupEvent(AuditStatus auditStatus, List<String> groupIds) {
        return AuditPayload.builder()
                .action(AuditAction.READ)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_LIST_GROUP_TRANSACTION_ACTION_ID)
                .message(REDIS_LIST_GROUP_TRANSACTION_MESSAGE)
                .resources(Stream.concat(Collections.singletonList(dataPartitionId).stream(), groupIds.stream()).collect(Collectors.toList()))
                .build();
    }

    AuditPayload getListMemberEvent(AuditStatus auditStatus, String groupId) {
        return AuditPayload.builder()
                .action(AuditAction.READ)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_LIST_MEMBER_TRANSACTION_ACTION_ID)
                .message(String.format(REDIS_LIST_MEMBER_TRANSACTION_MESSAGE, groupId))
                .resources(Arrays.asList(dataPartitionId, groupId))
                .build();
    }

    AuditPayload getUpdateAppIdsEvent(AuditStatus auditStatus, String groupId, Set<String> appIds) {
        return AuditPayload.builder()
                .action(AuditAction.UPDATE)
                .status(auditStatus)
                .user(this.user)
                .actionId(REDIS_UPDATE_APP_IDS_TRANSACTION_ACTION_ID)
                .message(String.format(REDIS_UPDATE_APP_IDS_TRANSACTION_MESSAGE, groupId, JsonConverter.toJson(appIds)))
                .resources(Arrays.asList(dataPartitionId, groupId))
                .build();
    }

    AuditPayload getUpdateGroupsInCacheForKeysEvent(AuditStatus auditStatus, Set<String> keys) {
        return AuditPayload.builder()
                .action(AuditAction.UPDATE)
                .status(auditStatus)
                .user(user)
                .actionId(REDIS_UPDATE_GROUPS_IN_CACHE_FOR_KEYS_ACTION_ID)
                .message(REDIS_UPDATE_GROUPS_IN_CACHE_FOR_KEYS_MESSAGE)
                .resources(new ArrayList<>(keys))
                .build();
    }
}
