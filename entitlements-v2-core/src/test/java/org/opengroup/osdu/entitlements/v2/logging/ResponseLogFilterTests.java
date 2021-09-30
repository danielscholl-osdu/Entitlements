package org.opengroup.osdu.entitlements.v2.logging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.Request;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class ResponseLogFilterTests {
    @Mock
    private RequestInfo requestInfo;
    @Mock
    private DpsHeaders headers;
    @Mock
    private ServletContext context;
    @Mock
    private FilterChain filterChain;
    @Mock
    private JaxRsDpsLog logger;
    @Mock
    private HttpServletRequest servletRequest;
    @InjectMocks
    private ResponseLogFilter responseLogFilter;
    private final HttpServletResponse servletResponse = mockHttpServletResponse();

    @Before
    public void setup() {
        when(requestInfo.getHeaders()).thenReturn(headers);
        when(headers.getCorrelationId()).thenReturn("testcorrelationId");
        when(headers.getAuthorization()).thenReturn("testJwt");
        when(requestInfo.getUri()).thenReturn("http://google.com");
        when(requestInfo.getUserIp()).thenReturn("127.0.0.1");
        when(servletRequest.getMethod()).thenReturn("OPTIONS");
        when(servletRequest.getServletContext()).thenReturn(context);
        when(context.getAttribute("starttime")).thenReturn(null);
    }

    @Test
    public void shouldNotThrowAppExceptionWhenIsACron() throws Exception {
        when(requestInfo.isCronRequest()).thenReturn(true);
        when(requestInfo.isHttps()).thenReturn(false);
        responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
        assertEquals(200, servletResponse.getStatus());
    }

    @Test
    public void shouldNotThrowAppExceptionWhenIsAHttpsRequest() throws Exception {
        when(requestInfo.isHttps()).thenReturn(true);
        responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
        assertEquals(200, servletResponse.getStatus());
    }

    @Test
    public void shouldLogHttpRequestInfoWhenFilterIsCalled() throws Exception {
        when(requestInfo.isCronRequest()).thenReturn(true);
        when(requestInfo.isHttps()).thenReturn(true);
        responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
        assertEquals(200, servletResponse.getStatus());
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
        verify(logger).request(eq(ResponseLogFilter.class.getName()), argument.capture());
        Request result = argument.getValue();
        assertEquals("127.0.0.1", result.getIp());
        assertEquals(200, result.getStatus());
    }

    @Test
    public void shouldNotLogHttpRequestInfoWhenItsHealthCheckRequest() throws Exception {
        when(requestInfo.isCronRequest()).thenReturn(true);
        when(requestInfo.isHttps()).thenReturn(true);
        String[] healthCheckPaths = {"/liveness_check", "/readiness_check"};
        for (String path : healthCheckPaths) {
            when(requestInfo.getUri()).thenReturn("http:/" + path);
            responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
            assertEquals(200, servletResponse.getStatus());
            ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
            verify(logger, never()).request(argument.capture());
        }
    }

    @Test
    public void shouldReturn200WhenItsSwaggerRequest() throws Exception {
        String[] swaggerPaths = {"/swagger", "/v2/api-docs", "/configuration/ui", "/webjars/"};
        for (String path : swaggerPaths) {
            when(requestInfo.getUri()).thenReturn("http:/" + path);
            responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
            assertEquals(200, servletResponse.getStatus());
        }
    }

    @Test
    public void shouldReturn200WhenItsHealthCheckRequest() throws Exception {
        String[] healthCheckPaths = {"/liveness_check", "/readiness_check"};
        for (String path : healthCheckPaths) {
            when(requestInfo.getUri()).thenReturn("http:/" + path);
            responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
            assertEquals(200, servletResponse.getStatus());
        }
    }

    private HttpServletResponse mockHttpServletResponse() {
        return new HttpServletResponse() {
            int response;

            @Override
            public void addCookie(Cookie cookie) {
            }

            @Override
            public boolean containsHeader(String s) {
                return false;
            }

            @Override
            public String encodeURL(String s) {
                return null;
            }

            @Override
            public String encodeRedirectURL(String s) {
                return null;
            }

            @Override
            public String encodeUrl(String s) {
                return null;
            }

            @Override
            public String encodeRedirectUrl(String s) {
                return null;
            }

            @Override
            public void sendError(int i, String s) {
            }

            @Override
            public void sendError(int i) {
            }

            @Override
            public void sendRedirect(String s) {
            }

            @Override
            public void setDateHeader(String s, long l) {
            }

            @Override
            public void addDateHeader(String s, long l) {
            }

            @Override
            public void setHeader(String s, String s1) {
            }

            @Override
            public void addHeader(String s, String s1) {
            }

            @Override
            public void setIntHeader(String s, int i) {
            }

            @Override
            public void addIntHeader(String s, int i) {
            }

            @Override
            public void setStatus(int i) {
                response = i;
            }

            @Override
            public void setStatus(int i, String s) {
            }

            @Override
            public int getStatus() {
                return response;
            }

            @Override
            public String getHeader(String s) {
                return null;
            }

            @Override
            public Collection<String> getHeaders(String s) {
                return null;
            }

            @Override
            public Collection<String> getHeaderNames() {
                return null;
            }

            @Override
            public String getCharacterEncoding() {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public ServletOutputStream getOutputStream() {
                return null;
            }

            @Override
            public PrintWriter getWriter() {
                return null;
            }

            @Override
            public void setCharacterEncoding(String s) {
            }

            @Override
            public void setContentLength(int i) {
            }

            @Override
            public void setContentLengthLong(long l) {
            }

            @Override
            public void setContentType(String s) {
            }

            @Override
            public void setBufferSize(int i) {
            }

            @Override
            public int getBufferSize() {
                return 0;
            }

            @Override
            public void flushBuffer() {
            }

            @Override
            public void resetBuffer() {
            }

            @Override
            public boolean isCommitted() {
                return false;
            }

            @Override
            public void reset() {
            }

            @Override
            public void setLocale(Locale locale) {
            }

            @Override
            public Locale getLocale() {
                return null;
            }
        };
    }
}
