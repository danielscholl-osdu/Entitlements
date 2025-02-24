package org.opengroup.osdu.entitlements.v2.azure.acceptance;

import org.opengroup.osdu.entitlements.v2.acceptance.DeleteMemberTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class DeleteMemberAzureTest extends DeleteMemberTest {

    public DeleteMemberAzureTest() {
        super(new AzureConfigurationService(), new AzureTokenService());
    }
}
