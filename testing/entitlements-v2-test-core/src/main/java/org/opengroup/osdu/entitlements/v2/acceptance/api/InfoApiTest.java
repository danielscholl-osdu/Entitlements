package org.opengroup.osdu.entitlements.v2.acceptance.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.AcceptanceBaseTest;
import org.opengroup.osdu.entitlements.v2.acceptance.model.VersionInfo;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.VersionInfoUtils;

@Slf4j
public abstract class InfoApiTest extends AcceptanceBaseTest {

  private static final String DATA_PARTITION_ID = "data-partition-id";
  private static final String HTTP_GET = "GET";
  private static final String INFO_API_PATH = "info";

  private final VersionInfoUtils versionInfoUtils = new VersionInfoUtils();

  protected InfoApiTest(ConfigurationService configurationService, TokenService tokenService) {
    super(configurationService, tokenService);
  }

  @Test
  public void should_returnInfo() throws Exception {
    RequestData request =
        RequestData.builder()
            .relativePath(getApi())
            .method(getHttpMethod())
            .body(null)
            .token(tokenService.getToken().getValue())
            .dataPartitionId(DATA_PARTITION_ID)
            .build();
    ClientResponse response = httpClientService.send(request);

    assertEquals(200, response.getStatus());
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
