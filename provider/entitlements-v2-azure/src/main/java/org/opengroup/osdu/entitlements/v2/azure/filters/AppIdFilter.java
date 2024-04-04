package org.opengroup.osdu.entitlements.v2.azure.filters;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

/**
 * This filter was created as an extension of
 * {@link org.opengroup.osdu.azure.filters.Slf4jMDCFilter}
 */
@Component
@ConditionalOnProperty(value = "logging.mdccontext.enabled", havingValue = "true", matchIfMissing = true)
public class AppIdFilter implements Filter {
    @Autowired
    private DpsHeaders dpsHeaders;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        MDC.put(DpsHeaders.APP_ID, dpsHeaders.getAppId());
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
