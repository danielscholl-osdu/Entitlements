package org.opengroup.osdu.entitlements.v2.logging;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.http.ResponseHeaders;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.Request;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ResponseLogFilter implements Filter {
    private final RequestInfo requestInfo;
    private final AppProperties appProperties;
    private final JaxRsDpsLog logger;

    @Override
    public void init(FilterConfig filterConfig) {
        //do nothing
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        String uri = requestInfo.getUri();
        if (isNotHealthCheckRequest(uri)) {
            logger.info(String.format("ResponseLogFilter#doFilter start timestamp: %d", System.currentTimeMillis()));
        }
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        requestInfo.getHeaders().addCorrelationIdIfMissing();
        setResponseHeaders(httpServletResponse);

        long startTime;
        Object property = httpServletRequest.getServletContext().getAttribute("starttime");
        if (property == null) {
            startTime = System.currentTimeMillis();
        } else {
            startTime = (long) property;
        }

        try {
            if (shouldRequestBeRedirectedToHttps(uri)) {
                redirectRequestToHttps(uri, httpServletResponse);
            } else {
                if (isOptionsRequest(httpServletRequest)) {
                    httpServletResponse.setStatus(200);
                } else {
                    filterChain.doFilter(servletRequest, servletResponse);
                }
            }
        } finally {
            if (isNotHealthCheckRequest(uri)) {
                logRequest(uri, httpServletRequest, httpServletResponse, startTime);
                logger.info(String.format("ResponseLogFilter#doFilter done timestamp: %d", System.currentTimeMillis()));
            }
        }
    }

    private void setResponseHeaders(HttpServletResponse httpServletResponse) {
        for (Map.Entry<String, List<Object>> header : ResponseHeaders.STANDARD_RESPONSE_HEADERS.entrySet()) {
            StringBuilder builder = new StringBuilder();
            header.getValue().forEach(builder::append);
            httpServletResponse.addHeader(header.getKey(), builder.toString());
        }
        httpServletResponse.addHeader(DpsHeaders.CORRELATION_ID, requestInfo.getHeaders().getCorrelationId());
    }

    private boolean shouldRequestBeRedirectedToHttps(String uri) {
        return (isNotLocalHost(uri)
                && isNotSwaggerRequest(uri)
                && isNotHealthCheckRequest(uri)
                && isNotHttps() && isNotHttpAccepted());
    }

    private void redirectRequestToHttps(String uri, HttpServletResponse httpServletResponse) {
        httpServletResponse.setStatus(307);
        String location = uri.replaceFirst("http", "https");
        httpServletResponse.addHeader("location", location);
    }

    private boolean isNotHttps() {
        return (!requestInfo.isHttps() && !requestInfo.isCronRequest());
    }

    private boolean isNotLocalHost(String uri) {
        return (!uri.contains("//localhost") && !uri.contains("//127.0.0.1"));
    }

    private boolean isNotSwaggerRequest(String uri) {
        return (!uri.contains("/swagger")
                && !uri.contains("/v2/api-docs")
                && !uri.contains("/configuration/ui")
                && !uri.contains("/webjars/"));
    }

    private boolean isNotHealthCheckRequest(String uri) {
        return (!uri.endsWith("/liveness_check") && !uri.endsWith("/readiness_check"));
    }

    private boolean isNotHttpAccepted() {
        return !appProperties.isHttpAccepted();
    }

    private boolean isOptionsRequest(HttpServletRequest request) {
        return request.getMethod().equalsIgnoreCase("OPTIONS");
    }

    private void logRequest(String uri, HttpServletRequest request, HttpServletResponse response, long startTime) {
        logger.request(Request.builder()
                .requestMethod(request.getMethod())
                .latency(Duration.ofMillis(System.currentTimeMillis() - startTime))
                .requestUrl(uri)
                .Status(response.getStatus())
                .ip(requestInfo.getUserIp())
                .build());
    }
}
