package org.opengroup.osdu.entitlements.v2.acceptance.util;

import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.model.VersionInfo;

public class VersionInfoUtils {

  public VersionInfo getVersionInfoFromResponse(ClientResponse response) {
    assertTrue(response.getType().toString().contains("application/json"));
    String json = response.getEntity(String.class);
    Gson gson = new Gson();
    return gson.fromJson(json, VersionInfo.class);
  }
}
