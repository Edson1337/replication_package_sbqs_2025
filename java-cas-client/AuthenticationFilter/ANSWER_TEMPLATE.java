package org.apereo.cas.client.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apereo.cas.client.authentication.AuthenticationFilter;
import org.apereo.cas.client.util.AbstractCasFilter;
import org.apereo.cas.client.authentication.ContainsPatternUrlPatternMatcherStrategy;
import org.apereo.cas.client.validation.Assertion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationFilterTests {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private Assertion assertion;
    @Mock
    private jakarta.servlet.FilterConfig filterConfig;
    @Mock
    private org.apereo.cas.client.authentication.GatewayResolver gatewayStorage;
    @Mock
    private org.apereo.cas.client.authentication.AuthenticationRedirectStrategy redirectStrategy;

    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new AuthenticationFilter();
        filter.setCasServerLoginUrl("https://cas.example.org/login");
        filter.setGatewayStorage(gatewayStorage);
        // injeta estratégia de redirect mockada
        filter.authenticationRedirectStrategy = redirectStrategy;
    }

    @Test
    void doFilter_withExistingAssertion_shouldProceedFilterChain() throws Exception {
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION)).thenReturn(assertion);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void doFilter_withTicketPresent_shouldProceedFilterChain() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getParameter("ticket")).thenReturn("ST-123");
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void doFilter_noTicketNoAssertion_shouldRedirectToCasLogin() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://client/app"));
        when(request.getQueryString()).thenReturn(null);
        when(gatewayStorage.hasGatewayedAlready(request, "http://client/app")).thenReturn(false);

        filter.setGateway(false);
        filter.doFilter(request, response, chain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).redirect(eq(request), eq(response), captor.capture());
        String redirectUrl = captor.getValue();
        assertTrue(redirectUrl.startsWith("https://cas.example.org/login"));
    }

    @Test
    void doFilter_ignorePattern_shouldProceedFilterChain() throws Exception {
        UrlPatternMatcherStrategy strategy = new ContainsPatternUrlPatternMatcherStrategy();
        strategy.setPattern("ignore");
        filter.setIgnoreUrlPatternMatcherStrategyClass(strategy);

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://client/app/ignore"));
        when(request.getQueryString()).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }
}
