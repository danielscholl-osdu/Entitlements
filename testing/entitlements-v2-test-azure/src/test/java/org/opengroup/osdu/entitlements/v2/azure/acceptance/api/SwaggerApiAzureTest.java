package org.opengroup.osdu.entitlements.v2.azure.acceptance.api;

import org.opengroup.osdu.entitlements.v2.acceptance.api.SwaggerApiTest;
import org.opengroup.osdu.entitlements.v2.util.AzureConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AzureTokenService;

public class SwaggerApiAzureTest extends SwaggerApiTest {
  public SwaggerApiAzureTest() {
    super(new AzureConfigurationService(), new AzureTokenService());
  }
}