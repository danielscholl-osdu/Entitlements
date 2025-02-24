package org.opengroup.osdu.entitlements.v2.util;

import com.google.gson.Gson;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.opengroup.osdu.entitlements.v2.model.VersionInfo;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class VersionInfoUtils {

  public VersionInfo getVersionInfoFromResponse(CloseableHttpResponse response) {
    String json = "";
    try {
      assertTrue(response.getHeader("Content-Type").getValue().contains("application/json"));
      json = EntityUtils.toString(response.getEntity());
    } catch (ProtocolException | IOException e) {
      throw new RuntimeException(e);
    }
    Gson gson = new Gson();
    return gson.fromJson(json, VersionInfo.class);
  }
}
