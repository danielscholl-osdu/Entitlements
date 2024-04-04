package org.opengroup.osdu.entitlements.v2.logging;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.http.ResponseHeaders;
import org.opengroup.osdu.core.common.http.ResponseHeadersFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.Request;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ResponseLogFilter implements Filter {
    private final RequestInfo requestInfo;
    private final JaxRsDpsLog logger;

    private ResponseHeadersFactory responseHeadersFactory = new ResponseHeadersFactory();

    // defaults to * for any front-end, string must be comma-delimited if more than one domain
    @Value("${ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS:*}")
    String ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS;

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
            logger.debug(String.format("ResponseLogFilter#doFilter start timestamp: %d", System.currentTimeMillis()));
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
            if (isOptionsRequest(httpServletRequest)) {
                httpServletResponse.setStatus(200);
            } else {
                filterChain.doFilter(servletRequest, servletResponse);
            }
        } finally {
            if (isNotHealthCheckRequest(uri)) {
                logRequest(uri, httpServletRequest, httpServletResponse, startTime);
                logger.debug(String.format("ResponseLogFilter#doFilter done timestamp: %d", System.currentTimeMillis()));
            }
        }
    }

    private void setResponseHeaders(HttpServletResponse httpServletResponse) {
            Map<String, String> responseHeaders = responseHeadersFactory.getResponseHeaders(ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS);
            for(Map.Entry<String, String> header : responseHeaders.entrySet()){
                httpServletResponse.addHeader(header.getKey(), header.getValue());
            }
        httpServletResponse.addHeader(DpsHeaders.CORRELATION_ID, requestInfo.getHeaders().getCorrelationId());
    }

    private boolean isNotHealthCheckRequest(String uri) {
        return (!uri.endsWith("/liveness_check") && !uri.endsWith("/readiness_check"));
    }

    private boolean isOptionsRequest(HttpServletRequest request) {
        return request.getMethod().equalsIgnoreCase("OPTIONS");
    }

    private void logRequest(String uri, HttpServletRequest request, HttpServletResponse response, long startTime) {
        logger.request(ResponseLogFilter.class.getName(), Request.builder()
                .requestMethod(request.getMethod())
                .latency(Duration.ofMillis(System.currentTimeMillis() - startTime))
                .requestUrl(uri)
                .Status(response.getStatus())
                .ip(requestInfo.getUserIp())
                .build());
    }
}
