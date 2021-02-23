package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.TenantInitTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class TenantInitAzureTest extends TenantInitTest {

    public TenantInitAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}
