package org.opengroup.osdu.entitlements.v2.aws.acceptance.api;

import org.opengroup.osdu.entitlements.v2.acceptance.api.InfoApiTest;
import org.opengroup.osdu.entitlements.v2.util.AwsConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AwsTokenService;

public class InfoApiAwsTest extends InfoApiTest {
  public InfoApiAwsTest() {
    super(new AwsConfigurationService(), new AwsTokenService());
  }
}
