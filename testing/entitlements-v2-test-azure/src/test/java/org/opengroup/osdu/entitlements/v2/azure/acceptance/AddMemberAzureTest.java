package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.AddMemberTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class AddMemberAzureTest extends AddMemberTest {

    public AddMemberAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}
