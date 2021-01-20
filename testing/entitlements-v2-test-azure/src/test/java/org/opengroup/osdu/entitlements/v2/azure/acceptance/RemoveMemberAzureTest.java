package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.RemoveMemberTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class RemoveMemberAzureTest extends RemoveMemberTest {

    public RemoveMemberAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}
