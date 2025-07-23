package org.apereo.cas.client.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apereo.cas.client.validation.Assertion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterTest {

    private AuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private HttpSession session;

    @BeforeEach
    void setUp() throws ServletException {
        filter = new AuthenticationFilter();
        filter.setCasServerLoginUrl("https://cas.login/");
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        session = mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(session);
    }

//    @Test
//    void shouldIgnoreWhenUrlExcluded() throws IOException, ServletException {
//        filter.setIgnoreUrlPatternMatcherStrategyClass(uri -> true);
//        when(request.getRequestURL()).thenReturn(new StringBuffer("http://app/test"));
//        when(request.getQueryString()).thenReturn("a=b");
//
//        filter.doFilter(request, response, chain);
//
//        verify(chain).doFilter(request, response);
//        verifyNoInteractions(response);
//    }

    @Test
    void shouldProceedWhenAssertionInSession() throws IOException, ServletException {
        when(session.getAttribute(AuthenticationFilter.CONST_CAS_ASSERTION))
                .thenReturn(mock(Assertion.class));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldProceedWhenTicketPresent() throws IOException, ServletException {
        when(session.getAttribute(AuthenticationFilter.CONST_CAS_ASSERTION)).thenReturn(null);
        when(request.getParameter("ticket")).thenReturn("ST-123");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldProceedWhenGatewayAlreadyApplied() throws IOException, ServletException {
        filter.setGateway(true);
        GatewayResolver gw = mock(GatewayResolver.class);
        when(gw.hasGatewayedAlready(any(), anyString())).thenReturn(true);
        filter.setGatewayStorage(gw);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldRedirectWhenNoTicketOrGateway() throws IOException, ServletException {
        filter.setRenew(true);
        filter.setGateway(true);
        GatewayResolver gw = mock(GatewayResolver.class);
        when(gw.hasGatewayedAlready(any(), anyString())).thenReturn(false);
        when(gw.storeGatewayInformation(any(), anyString())).thenReturn("serviceUrl?gateway");
        filter.setGatewayStorage(gw);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).sendRedirect(captor.capture());
        String redirectUrl = captor.getValue();
        assertTrue(redirectUrl.startsWith("https://cas.login/?"));
        assertTrue(redirectUrl.contains("renew=true"));
        assertTrue(redirectUrl.contains("gateway=true"));
    }
}
