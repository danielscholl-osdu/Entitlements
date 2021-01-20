package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.UpdateGroupTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class UpdateGroupAzureTest extends UpdateGroupTest {

    public UpdateGroupAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}
