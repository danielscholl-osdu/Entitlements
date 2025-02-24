package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.DeleteGroupTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class DeleteGroupAzureTest extends DeleteGroupTest {

    public DeleteGroupAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}
