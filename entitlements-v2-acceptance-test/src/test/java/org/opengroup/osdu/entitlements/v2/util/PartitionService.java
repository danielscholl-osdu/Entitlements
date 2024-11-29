package org.opengroup.osdu.entitlements.v2.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;

import org.opengroup.osdu.core.common.partition.Property;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiredArgsConstructor
public class PartitionService {

  private final Gson gson = new Gson();
  private final ConfigurationService configurationService;
  private final HttpClientService httpClientService;
  private TestUtils testUtils = null;

  public PartitionService(ConfigurationService configurationService) {
    this.configurationService = configurationService;
    this.httpClientService = new HttpClientService(configurationService);
    this.testUtils = new TokenTestUtils();
  }

  public String getPartitionProperty(String property) throws Exception {
    String partitionApi = System.getProperty("PARTITION_URL", System.getenv("PARTITION_URL"));

    RequestData requestData = RequestData.builder()
        .url(partitionApi)
        .token(testUtils.getDataRootToken())
        .relativePath("partitions/" + configurationService.getTenantId())
        .method("GET")
        .build();

    CloseableHttpResponse httpResponse = httpClientService.send(requestData);
    InputStream content = httpResponse.getEntity().getContent();

    Type parametrizedType = TypeToken.getParameterized(Map.class,
        new Class[]{String.class, Property.class}).getType();
    Map<String, Property> properties = gson.fromJson(new InputStreamReader(content), parametrizedType);

    assertNotNull(properties.get(property));
    testUtils = null;
    return properties.get(property).getValue().toString();
  }
}
