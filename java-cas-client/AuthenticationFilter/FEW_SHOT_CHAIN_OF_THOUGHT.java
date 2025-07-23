package org.apereo.cas.client.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterTest {

    private AuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter();
        filter.setCasServerLoginUrl("https://cas.server/login");

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        session = mock(HttpSession.class);

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://app/service"));
        when(request.getQueryString()).thenReturn(null);
    }

    @Test
    void doFilter_ignoresWhenRequestUrlIsExcluded() throws ServletException, IOException {
        UrlPatternMatcherStrategy matcher = mock(UrlPatternMatcherStrategy.class);
        when(matcher.matches("http://app/service")).thenReturn(true);
        filter.setIgnoreUrlPatternMatcherStrategyClass(matcher);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

//    @Test
//    void doFilter_allowsWhenAssertionInSession() throws ServletException, IOException {
//        when(request.getSession(false)).thenReturn(session);
//        when(session.getAttribute(anyString())).thenReturn(new Object());
//
//        filter.doFilter(request, response, chain);
//
//        verify(chain).doFilter(request, response);
//        verify(response, never()).sendRedirect(anyString());
//    }

//    @Test
//    void doFilter_allowsWhenTicketIsPresent() throws ServletException, IOException {
//        when(request.getSession(false)).thenReturn(null);
//        when(request.getParameter("ticket")).thenReturn("ST-123");
//
//        filter.doFilter(request, response, chain);
//
//        verify(chain).doFilter(request, response);
//        verify(response, never()).sendRedirect(anyString());
//    }

//    @Test
//    void doFilter_allowsWhenAlreadyGatewayed() throws ServletException, IOException {
//        GatewayResolver gatewayResolver = mock(GatewayResolver.class);
//        filter.setGatewayStorage(gatewayResolver);
//
//        when(request.getSession(false)).thenReturn(null);
//        when(request.getParameter("ticket")).thenReturn(null);
//        when(gatewayResolver.hasGatewayedAlready(request, anyString())).thenReturn(true);
//
//        filter.doFilter(request, response, chain);
//
//        verify(chain).doFilter(request, response);
//        verify(response, never()).sendRedirect(anyString());
//    }

//    @Test
//    void doFilter_redirectsToCasLoginWithoutGateway() throws ServletException, IOException {
//        when(request.getSession(false)).thenReturn(null);
//        when(request.getParameter("ticket")).thenReturn(null);
//        // default gateway is false
//
//        filter.doFilter(request, response, chain);
//
//        verify(response).sendRedirect(startsWith("https://cas.server/login"));
//    }

//    @Test
//    void doFilter_redirectsToCasLoginWithGateway() throws ServletException, IOException {
//        filter.setGateway(true);
//        GatewayResolver gatewayResolver = mock(GatewayResolver.class);
//        filter.setGatewayStorage(gatewayResolver);
//
//        when(request.getSession(false)).thenReturn(null);
//        when(request.getParameter("ticket")).thenReturn(null);
//        when(gatewayResolver.hasGatewayedAlready(request, anyString())).thenReturn(false);
//        when(gatewayResolver.storeGatewayInformation(request, anyString()))
//                .thenReturn("http://app/service?gatewayed=true");
//
//        filter.doFilter(request, response, chain);
//
//        verify(response).sendRedirect(argThat(url -> url.contains("gateway=true")));
//    }
}