package org.opengroup.osdu.entitlements.v2.jdbc.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.entitlements.v2.jdbc.interceptor.AuthTestConfig.CORRECT_TOKEN;
import static org.opengroup.osdu.entitlements.v2.jdbc.interceptor.AuthTestConfig.IAP_EMAIL_PREFIX;
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
import org.opengroup.osdu.entitlements.v2.jdbc.config.OpenIDProviderConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.IapConfigurationProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.userinfo.impl.IapUserInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(properties = "gcp-authentication-mode=IAP", classes = AuthTestConfig.class)
@RunWith(SpringRunner.class)
public class IapAuthTest {

    @Mock
    private Object handler;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @MockBean
    private JaxRsDpsLog jaxRsDpsLog;
    @Autowired
    private OpenIDProviderConfig openIDProviderConfig;
    @Autowired
    private IapUserInfoProvider iapUserInfoProvider;
    @Autowired
    private IapConfigurationProperties iapConfigurationProperties;
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
    public void testShouldAuthenticateRequestWithCorrectIapTokenAndUserHeader() {
        when(request.getHeader(iapConfigurationProperties.getJwtHeader())).thenReturn(CORRECT_TOKEN);
        when(request.getHeader(iapConfigurationProperties.getUserIdHeader())).thenReturn(IAP_EMAIL_PREFIX + MATCHING_USER_EMAIL);
        assertTrue(interceptor.preHandle(request, response, handler));
        DpsHeaders headers = applicationContext.getBean(DpsHeaders.class);
        assertEquals(MATCHING_USER_EMAIL, headers.getUserId());
    }

    @Test
    public void testShouldAuthenticateRequestWithAuthorizationHeaderOnly() {
        when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(CORRECT_TOKEN);
        assertTrue(interceptor.preHandle(request, response, handler));
        DpsHeaders headers = applicationContext.getBean(DpsHeaders.class);
        assertEquals(MATCHING_USER_EMAIL, headers.getUserId());
    }

    @Test(expected = AppException.class)
    public void testShouldNotAuthenticateRequestWithCorrectIapTokenAndNotMatchingUserHeader() {
        when(request.getHeader(iapConfigurationProperties.getJwtHeader())).thenReturn(CORRECT_TOKEN);
        when(request.getHeader(iapConfigurationProperties.getUserIdHeader())).thenReturn(IAP_EMAIL_PREFIX + NOT_MATCHING_USER_EMAIL);
        interceptor.preHandle(request, response, handler);
    }

    @Test(expected = AppException.class)
    public void testShouldNotAuthenticateRequestWithMissingAuthHeaders() {
        when(request.getHeader(iapConfigurationProperties.getJwtHeader())).thenReturn(null);
        when(request.getHeader(iapConfigurationProperties.getUserIdHeader())).thenReturn(null);
        interceptor.preHandle(request, response, handler);
    }

    @Test(expected = AppException.class)
    public void testShouldNotAuthenticateWithMalformedUserEmailHeader() {
        when(request.getHeader(iapConfigurationProperties.getJwtHeader())).thenReturn(CORRECT_TOKEN);
        when(request.getHeader(iapConfigurationProperties.getUserIdHeader())).thenReturn(NOT_MATCHING_USER_EMAIL);
        interceptor.preHandle(request, response, handler);
    }

    @Test(expected = AppException.class)
    public void testShouldNotAuthenticateWithNotCorrectOpenIDToken() {
        when(request.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn(TOKEN_WITH_NOT_CORRECT_SECRET);
        interceptor.preHandle(request, response, handler);
    }

    @Test(expected = AppException.class)
    public void testShouldNotAuthenticateWithNotCorrectIAPToken() {
        when(request.getHeader(iapConfigurationProperties.getJwtHeader())).thenReturn(TOKEN_WITH_NOT_CORRECT_SECRET);
        when(request.getHeader(iapConfigurationProperties.getUserIdHeader())).thenReturn(NOT_MATCHING_USER_EMAIL);
        interceptor.preHandle(request, response, handler);
    }


}
