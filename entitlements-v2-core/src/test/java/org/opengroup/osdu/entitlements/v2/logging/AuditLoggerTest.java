package org.opengroup.osdu.entitlements.v2.logging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(PowerMockRunner.class)
public class AuditLoggerTest {
    @Mock
    private JaxRsDpsLog log;

    @Mock
    private RequestInfo requestInfo;

    @Mock
    private RequestInfoUtilService requestInfoUtilService;

    @InjectMocks
    private AuditLogger auditLogger;

    private AuditEvents auditEvents;

    @Before
    public void setup() {
        auditEvents = new AuditEvents("user", "partitionid");

        Map<String, String> headers = new HashMap<>();
        headers.put(DpsHeaders.DATA_PARTITION_ID, "partitionid");
        headers.put(DpsHeaders.CORRELATION_ID, "1234567890");
        DpsHeaders dpsHeaders = DpsHeaders.createFromMap(headers);
        when(requestInfo.getHeaders()).thenReturn(dpsHeaders);
        when(requestInfoUtilService.getUserId(dpsHeaders)).thenReturn("user");
    }

    @Test
    public void shouldLogRedisInstanceBackup() {
        auditLogger.redisInstanceBackup(AuditStatus.SUCCESS);
        verify(log).audit(auditEvents.getRedisBackupEvent(AuditStatus.SUCCESS));
    }

    @Test
    public void shouldLogRedisInstanceBackupVersions() {
        auditLogger.redisInstanceBackupVersions(AuditStatus.SUCCESS);
        verify(log).audit(auditEvents.getRedisBackupVersionEvent(AuditStatus.SUCCESS));
    }

    @Test
    public void shouldLogRedisInstanceRestore() {
        auditLogger.redisInstanceRestore(AuditStatus.SUCCESS, "version");
        verify(log).audit(auditEvents.getRedisRestoreEvent(AuditStatus.SUCCESS, "version"));
    }

    @Test
    public void shouldLogCreateGroup() {
        auditLogger.createGroup(AuditStatus.SUCCESS, "groupid");
        verify(log).audit(auditEvents.getCreateGroupEvent(AuditStatus.SUCCESS, "groupid"));
    }

    @Test
    public void shouldLogDeleteGroup() {
        auditLogger.deleteGroup(AuditStatus.SUCCESS, "groupid");
        verify(log).audit(auditEvents.getDeleteGroupEvent(AuditStatus.SUCCESS, "groupid"));
    }

    @Test
    public void shouldLogAddMember() {
        auditLogger.addMember(AuditStatus.SUCCESS, "groupid", "memberid", Role.MEMBER);
        verify(log).audit(auditEvents.getAddMemberEvent(AuditStatus.SUCCESS, "groupid", "memberid", Role.MEMBER));
    }

    @Test
    public void shouldLogRemoveMember() {
        auditLogger.removeMember(AuditStatus.SUCCESS, "groupid", "memberid", "requestid");
        verify(log).audit(auditEvents.getRemoveMemberEvent(AuditStatus.SUCCESS, "groupid", "memberid", "requestid"));
    }

    @Test
    public void shouldLogListGroup() {
        auditLogger.listGroup(AuditStatus.SUCCESS, new ArrayList<>());
        verify(log).audit(auditEvents.getListGroupEvent(AuditStatus.SUCCESS, new ArrayList<>()));
    }

    @Test
    public void shouldLogListMember() {
        auditLogger.listMember(AuditStatus.SUCCESS, "groupid");
        verify(log).audit(auditEvents.getListMemberEvent(AuditStatus.SUCCESS, "groupid"));
    }

    @Test
    public void shouldLogUpdateAppIds() {
        Set<String> appIds = new HashSet<>();
        appIds.add("appid1");
        auditLogger.updateAppIds(AuditStatus.SUCCESS, "groupid", appIds);
        verify(log).audit(auditEvents.getUpdateAppIdsEvent(AuditStatus.SUCCESS, "groupid", appIds));
    }
}
