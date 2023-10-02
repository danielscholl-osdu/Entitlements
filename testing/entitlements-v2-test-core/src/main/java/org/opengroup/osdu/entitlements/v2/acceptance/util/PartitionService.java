package org.opengroup.osdu.entitlements.v2.acceptance.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.Assert;
import org.opengroup.osdu.core.common.partition.Property;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;

@RequiredArgsConstructor
public class PartitionService {

  private final Gson gson = new Gson();
  private final ConfigurationService configurationService;
  private final HttpClientService httpClientService;

  public PartitionService(ConfigurationService configurationService) {
    this.configurationService = configurationService;
    this.httpClientService = new HttpClientService(configurationService);
  }

  public String getPartitionProperty(String property) throws Exception {
    String partitionApi = System.getProperty("PARTITION_API", System.getenv("PARTITION_API"));

    RequestData requestData = RequestData.builder()
        .url(partitionApi)
        .relativePath("/partitions/" + configurationService.getTenantId())
        .method("GET")
        .build();

    CloseableHttpResponse httpResponse = httpClientService.send(requestData);
    InputStream content = httpResponse.getEntity().getContent();

    Type parametrizedType = TypeToken.getParameterized(Map.class,
        new Class[]{String.class, Property.class}).getType();
    Map<String, Property> properties = (Map) gson.fromJson(new InputStreamReader(content), parametrizedType);

    Assert.assertNotNull(properties.get(property));
    return properties.get(property).getValue().toString();
  }
}
