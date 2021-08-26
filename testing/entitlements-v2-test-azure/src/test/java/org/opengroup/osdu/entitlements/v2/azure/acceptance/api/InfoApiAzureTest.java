package org.opengroup.osdu.entitlements.v2.azure.acceptance.api;

import org.opengroup.osdu.entitlements.v2.acceptance.api.InfoApiTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class InfoApiAzureTest extends InfoApiTest {
  public InfoApiAzureTest() {
    super(new AzureConfigurationService(), new AzureTokenService());
  }
}
