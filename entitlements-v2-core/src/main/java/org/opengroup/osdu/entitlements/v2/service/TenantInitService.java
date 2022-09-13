package org.opengroup.osdu.entitlements.v2.service;


public interface TenantInitService {

    void createDefaultGroups();

    void bootstrapInitialAccounts();
}
