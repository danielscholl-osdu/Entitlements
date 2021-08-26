package org.opengroup.osdu.entitlements.v2.jdbc.acceptance.api;

import org.opengroup.osdu.entitlements.v2.acceptance.api.InfoApiTest;
import org.opengroup.osdu.entitlements.v2.util.JdbcConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.JdbcTokenService;

public class InfoApiJdbcTest extends InfoApiTest {
  public InfoApiJdbcTest() {
    super(new JdbcConfigurationService(), new JdbcTokenService());
  }
}
