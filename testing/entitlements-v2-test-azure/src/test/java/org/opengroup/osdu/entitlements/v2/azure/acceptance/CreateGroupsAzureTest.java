package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.CreateGroupTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class CreateGroupsAzureTest extends CreateGroupTest {

    public CreateGroupsAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}
