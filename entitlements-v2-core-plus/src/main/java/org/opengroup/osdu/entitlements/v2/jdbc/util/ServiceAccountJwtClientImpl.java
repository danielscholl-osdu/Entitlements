package org.opengroup.osdu.entitlements.v2.jdbc.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceAccountJwtClientImpl implements IServiceAccountJwtClient {

  @Override
  public String getIdToken(String tenantName) {
    return "";
  }
}
