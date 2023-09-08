package org.opengroup.osdu.entitlements.v2.azure.filters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.slf4j.MDC;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class AppIdFilterTest {

    @InjectMocks
    private AppIdFilter appIdFilter;

    @Mock
    private DpsHeaders dpsHeaders;

    @Test
    public void shouldPopulateAppIdSuccessfully() throws IOException, ServletException {

        try (MockedStatic<MDC> mock = Mockito.mockStatic(MDC.class)) {
            mock.when(() -> MDC.put("x-app-id", "x-app-id-value")).thenAnswer(Answers.RETURNS_DEFAULTS);

            HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
            HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
            FilterChain filterChain = Mockito.mock(FilterChain.class);
            Mockito.when(dpsHeaders.getAppId()).thenReturn("x-app-id-value");
            appIdFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

            mock.verify(() -> MDC.put("x-app-id", "x-app-id-value"));
        }
    }
}
