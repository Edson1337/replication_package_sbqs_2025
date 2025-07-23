package org.apereo.cas.client.authentication;

import org.apereo.cas.client.Protocol;
import org.apereo.cas.client.authentication.AuthenticationFilter;
import org.apereo.cas.client.authentication.GatewayResolver;
import org.apereo.cas.client.authentication.AuthenticationRedirectStrategy;
import org.apereo.cas.client.util.CommonUtils;
import org.apereo.cas.client.validation.Assertion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterTests {

    private AuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private HttpSession session;
    private GatewayResolver gatewayResolver;
    private AuthenticationRedirectStrategy redirectStrategy;

    @BeforeEach
    void setUp() throws Exception {
        // Subclasse para fixar constructServiceUrl
        filter = new AuthenticationFilter(Protocol.CAS2) {
            @Override
            protected String constructServiceUrl(HttpServletRequest req, HttpServletResponse res) {
                return "https://app/service";
            }
        };
        filter.setCasServerLoginUrl("https://cas.server/login");
        filter.init();

        // mocks
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        session = mock(HttpSession.class);
        gatewayResolver = mock(GatewayResolver.class);
        redirectStrategy = mock(AuthenticationRedirectStrategy.class);

        // injetar via reflection
        Field f1 = AuthenticationFilter.class.getDeclaredField("gatewayStorage");
        f1.setAccessible(true);
        f1.set(filter, gatewayResolver);
        Field f2 = AuthenticationFilter.class.getDeclaredField("authenticationRedirectStrategy");
        f2.setAccessible(true);
        f2.set(filter, redirectStrategy);
    }

    @Test
    void shouldIgnoreExcludedUrl() throws ServletException, IOException {
        filter.setIgnoreUrlPatternMatcherStrategyClass(new org.apereo.cas.client.authentication.ContainsPatternUrlPatternMatcherStrategy());
        filter.getIgnoreUrlPatternMatcherStrategyClass().setPattern("ignore");
        when(request.getRequestURL()).thenReturn(new StringBuffer("https://app/ignore"));
        when(request.getQueryString()).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldPassThroughWhenAssertionInSession() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(CommonUtils.CONST_CAS_ASSERTION)).thenReturn(mock(Assertion.class));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldPassThroughWhenTicketPresent() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(null);
        when(request.getParameter("ticket")).thenReturn("ST-123");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldPassThroughWhenAlreadyGatewayed() throws ServletException, IOException {
        filter.setGateway(true);
        when(request.getSession(false)).thenReturn(null);
        when(request.getParameter("ticket")).thenReturn(null);
        when(gatewayResolver.hasGatewayedAlready(request, "https://app/service")).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldRedirectToCasWhenNoTicketNoAssertion() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(null);
        when(request.getParameter("ticket")).thenReturn(null);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).redirect(eq(request), eq(response), urlCaptor.capture());

        String expected = CommonUtils.constructRedirectUrl(
                "https://cas.server/login",
                filter.getProtocol().getServiceParameterName(),
                "https://app/service",
                false, false, null
        );
        assertEquals(expected, urlCaptor.getValue());
    }
}
