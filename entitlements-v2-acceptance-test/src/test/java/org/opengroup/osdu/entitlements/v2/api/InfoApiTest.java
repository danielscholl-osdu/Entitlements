package org.opengroup.osdu.entitlements.v2.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.AcceptanceBaseTest;
import org.opengroup.osdu.entitlements.v2.model.VersionInfo;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.util.*;


@Slf4j
public class InfoApiTest extends AcceptanceBaseTest {

  private static final String DATA_PARTITION_ID = "data-partition-id";
  private static final String HTTP_GET = "GET";
  private static final String INFO_API_PATH = "info";

  private final VersionInfoUtils versionInfoUtils = new VersionInfoUtils();

  public InfoApiTest() {
    super(new AnthosConfigurationService(), new OpenIDTokenProvider());
  }

  @Test
  public void should_returnInfo() throws Exception {
    RequestData request =
        RequestData.builder()
            .relativePath(getApi())
            .method(getHttpMethod())
            .body(null)
            .token(getTokenService().getToken().getValue())
            .dataPartitionId(DATA_PARTITION_ID)
            .build();
    CloseableHttpResponse response = getHttpClientService().send(request);

    assertEquals(200, response.getCode());
    VersionInfo responseObject = versionInfoUtils.getVersionInfoFromResponse(response);

    assertNotNull(responseObject.getGroupId());
    assertNotNull(responseObject.getArtifactId());
    assertNotNull(responseObject.getVersion());
    assertNotNull(responseObject.getBuildTime());
    assertNotNull(responseObject.getBranch());
    assertNotNull(responseObject.getCommitId());
    assertNotNull(responseObject.getCommitMessage());
  }

  @Test
  public void should_returnInfo_withTrailingSlash() throws Exception {
    RequestData request =
        RequestData.builder()
            .relativePath(getApi()+"/")
            .method(getHttpMethod())
            .body(null)
            .token(getTokenService().getToken().getValue())
            .dataPartitionId(DATA_PARTITION_ID)
            .build();
    CloseableHttpResponse response = getHttpClientService().send(request);

    assertEquals(200, response.getCode());
    VersionInfo responseObject = versionInfoUtils.getVersionInfoFromResponse(response);

    assertNotNull(responseObject.getGroupId());
    assertNotNull(responseObject.getArtifactId());
    assertNotNull(responseObject.getVersion());
    assertNotNull(responseObject.getBuildTime());
    assertNotNull(responseObject.getBranch());
    assertNotNull(responseObject.getCommitId());
    assertNotNull(responseObject.getCommitMessage());
  }

  @Override
  public void shouldReturn401WhenMakingHttpRequestWithoutToken() {
    // not actual for this case
  }

  private String getApi() {
    return INFO_API_PATH;
  }

  private String getHttpMethod() {
    return HTTP_GET;
  }

  @Override
  protected RequestData getRequestDataForNoTokenTest() {
    return null;
  }
}
