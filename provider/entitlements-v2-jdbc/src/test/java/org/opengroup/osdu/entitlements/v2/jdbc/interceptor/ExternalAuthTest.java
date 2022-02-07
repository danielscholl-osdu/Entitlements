package org.opengroup.osdu.entitlements.v2.jdbc.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.jdbc.interceptor.AuthTestConfig.CORRECT_TOKEN;
import static org.opengroup.osdu.entitlements.v2.jdbc.interceptor.AuthTestConfig.MATCHING_USER_EMAIL;
import static org.opengroup.osdu.entitlements.v2.jdbc.interceptor.AuthTestConfig.NOT_MATCHING_USER_EMAIL;
import static org.opengroup.osdu.entitlements.v2.jdbc.interceptor.AuthTestConfig.TOKEN_WITH_NOT_CORRECT_SECRET;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.config.EntOpenIDProviderConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntitlementsConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    properties = {
      "gcp-authentication-mode=EXTERNAL",
      "openid.provider.url=http://test",
      "openid.provider.user-id-claim-name=email"
    },
    classes = AuthTestConfig.class)
@RunWith(SpringRunner.class)
public class ExternalAuthTest {

    @Mock
    private Object handler;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @MockBean
    private JaxRsDpsLog jaxRsDpsLog;
    @MockBean
    private EntOpenIDProviderConfig entOpenIDProviderConfig;
    @Autowired
    private EntitlementsConfigurationProperties entitlementsConfigurationProperties;
    @Autowired
    public RequestHeaderInterceptor interceptor;
    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setUp() {
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("");
    }

    @Test
    public void shouldAuthenticateWhenOnlyUserIdHeaderPresent() {
        when(request.getHeader(entitlementsConfigurationProperties.getGcpXUserIdentityHeaderName())).thenReturn(MATCHING_USER_EMAIL);
        assertTrue(interceptor.preHandle(request, response, handler));
        DpsHeaders headers = applicationContext.getBean(DpsHeaders.class);
        assertEquals(MATCHING_USER_EMAIL, headers.getUserId());
    }

    @Test
    public void shouldAuthenticateWhenOnlyOpenIDTokenPresent() {
        when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(CORRECT_TOKEN);
        assertTrue(interceptor.preHandle(request, response, handler));
        DpsHeaders headers = applicationContext.getBean(DpsHeaders.class);
        assertEquals(MATCHING_USER_EMAIL, headers.getUserId());
    }

    @Test
    public void shouldAuthenticateWhenOpenIDTokenAndUserIdHeadersPresentAndMatching() {
        when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(CORRECT_TOKEN);
        when(request.getHeader(entitlementsConfigurationProperties.getGcpXUserIdentityHeaderName())).thenReturn(MATCHING_USER_EMAIL);
        assertTrue(interceptor.preHandle(request, response, handler));
        DpsHeaders headers = applicationContext.getBean(DpsHeaders.class);
        assertEquals(MATCHING_USER_EMAIL, headers.getUserId());
    }

    @Test(expected = AppException.class)
    public void shouldNotAuthenticateWhenNoHeadersPresent() {
        interceptor.preHandle(request, response, handler);
    }

    @Test(expected = AppException.class)
    public void shouldNotAuthenticateWhenOpenIDTokenAndUserIdHeadersPresentAndNotMatching() {
        when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(CORRECT_TOKEN);
        when(request.getHeader(entitlementsConfigurationProperties.getGcpXUserIdentityHeaderName())).thenReturn(NOT_MATCHING_USER_EMAIL);
        interceptor.preHandle(request, response, handler);
    }

    @Test(expected = AppException.class)
    public void shouldNotAuthenticateWhenOpenIDTokenAndUserIdHeadersPresentAndTokenNotCorrect() {
        when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(TOKEN_WITH_NOT_CORRECT_SECRET);
        when(request.getHeader(entitlementsConfigurationProperties.getGcpXUserIdentityHeaderName())).thenReturn(MATCHING_USER_EMAIL);
        interceptor.preHandle(request, response, handler);
    }
}
