package org.opengroup.osdu.entitlements.v2.service;


import org.opengroup.osdu.entitlements.v2.model.init.InitServiceDto;

public interface TenantInitService {

    void createDefaultGroups();

    void bootstrapInitialAccounts(InitServiceDto initServiceDto);
}
