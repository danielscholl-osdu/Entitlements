package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.GetMembersCountTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class GetMembersCountAzureTest extends GetMembersCountTest {
    public GetMembersCountAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}
