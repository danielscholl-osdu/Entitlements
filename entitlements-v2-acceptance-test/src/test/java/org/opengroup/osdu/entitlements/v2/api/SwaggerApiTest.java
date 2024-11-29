package org.opengroup.osdu.entitlements.v2.api;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.AcceptanceBaseTest;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.util.ConfigurationService;

import static org.junit.Assert.assertEquals;

@Slf4j
public abstract class SwaggerApiTest extends AcceptanceBaseTest {

  private static final String HTTP_GET = "GET";
  private static final String SWAGGER_API_PATH = "swagger";
  private static final String SWAGGER_API_DOCS_PATH = "api-docs";

  private SwaggerApiTest(ConfigurationService configurationService) {
    super(configurationService);
  }

  @Test
  public void shouldReturn200_whenSwaggerApiIsCalled() throws Exception {
    RequestData request =
            RequestData.builder()
                    .relativePath(SWAGGER_API_PATH)
                    .method(HTTP_GET)
                    .body(null)
                    .token(testUtils.getToken())
                    .build();

    CloseableHttpResponse response = httpClientService.send(request);

    assertEquals(HttpStatus.SC_OK, response.getCode());
  }

  @Test
  public void shouldReturn200_whenSwaggerApiDocsIsCalled() throws Exception {
    RequestData request =
            RequestData.builder()
                    .relativePath(SWAGGER_API_DOCS_PATH)
                    .method(HTTP_GET)
                    .body(null)
                    .token(testUtils.getToken())
                    .build();

    CloseableHttpResponse response = httpClientService.send(request);
    
    assertEquals(HttpStatus.SC_OK, response.getCode());
  }

  @Override
  public void shouldReturn401WhenMakingHttpRequestWithoutToken() {
    // not actual for this case
  }

  @Override
  protected RequestData getRequestDataForNoTokenTest() {
    return null;
  }
}
