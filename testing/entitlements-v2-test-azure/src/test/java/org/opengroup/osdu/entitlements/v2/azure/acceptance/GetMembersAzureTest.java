package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.GetMembersTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class GetMembersAzureTest extends GetMembersTest {

    public GetMembersAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}
