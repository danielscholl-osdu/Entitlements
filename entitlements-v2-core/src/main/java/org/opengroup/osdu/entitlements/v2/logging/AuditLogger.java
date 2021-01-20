package org.opengroup.osdu.entitlements.v2.logging;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.Set;

@Component
@RequestScope
public class AuditLogger {
    @Autowired
    private JaxRsDpsLog logger;

    @Autowired
    private RequestInfo requestInfo;

    @Autowired
    private RequestInfoUtilService requestInfoUtilService;

    private AuditEvents events = null;

    private AuditEvents getEvents() {
        if (this.events == null) {
            DpsHeaders headers = this.requestInfo.getHeaders();
            String userId = requestInfoUtilService.getUserId(headers);
            this.events = new AuditEvents(userId, headers.getPartitionId());
        }
        return this.events;
    }

    public void redisInstanceBackup(AuditStatus auditStatus) {
        this.writeLog(this.getEvents().getRedisBackupEvent(auditStatus));
    }

    public void redisInstanceBackupVersions(AuditStatus auditStatus) {
        this.writeLog(this.getEvents().getRedisBackupVersionEvent(auditStatus));
    }

    public void redisInstanceRestore(AuditStatus auditStatus, String version) {
        this.writeLog(this.getEvents().getRedisRestoreEvent(auditStatus, version));
    }

    public void createGroup(AuditStatus auditStatus, String groupId) {
        this.writeLog(this.getEvents().getCreateGroupEvent(auditStatus, groupId));
    }

    public void updateGroup(AuditStatus auditStatus, String groupId) {
        this.writeLog(this.getEvents().getUpdateGroupEvent(auditStatus, groupId));
    }

    public void deleteGroup(AuditStatus auditStatus, String groupId) {
        this.writeLog(this.getEvents().getDeleteGroupEvent(auditStatus, groupId));
    }

    public void addMember(AuditStatus auditStatus, String groupId, String memberId, Role role) {
        this.writeLog(this.getEvents().getAddMemberEvent(auditStatus, groupId, memberId, role));
    }

    public void removeMember(AuditStatus auditStatus, String groupId, String memberId, String requesterId) {
        this.writeLog(this.getEvents().getRemoveMemberEvent(auditStatus, groupId, memberId, requesterId));
    }

    public void listGroup(AuditStatus auditStatus, List<String> groupIds) {
        this.writeLog(this.getEvents().getListGroupEvent(auditStatus, groupIds));
    }

    public void listMember(AuditStatus auditStatus, String groupId) {
        this.writeLog(this.getEvents().getListMemberEvent(auditStatus, groupId));
    }

    public void updateAppIds(AuditStatus auditStatus, String groupId, Set<String> appIds) {
        this.writeLog(this.getEvents().getUpdateAppIdsEvent(auditStatus, groupId, appIds));
    }

    public void updateGroupsInCacheForKeys(AuditStatus auditStatus, Set<String> keys) {
        this.writeLog(this.getEvents().getUpdateGroupsInCacheForKeysEvent(auditStatus, keys));
    }

    private void writeLog(AuditPayload log) {
        this.logger.audit(log);
    }
}
