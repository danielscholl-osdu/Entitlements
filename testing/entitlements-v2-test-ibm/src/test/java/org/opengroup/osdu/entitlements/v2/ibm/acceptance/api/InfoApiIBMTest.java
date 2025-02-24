package org.opengroup.osdu.entitlements.v2.ibm.acceptance.api;

import org.opengroup.osdu.entitlements.v2.acceptance.api.InfoApiTest;
import org.opengroup.osdu.entitlements.v2.util.IBMConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.IBMTokenService;

public class InfoApiIBMTest extends InfoApiTest {
  public InfoApiIBMTest() {
    super(new IBMConfigurationService(), new IBMTokenService());
  }
}
