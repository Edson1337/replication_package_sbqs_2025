package org.apereo.cas.client.authentication;

import org.apereo.cas.client.Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterTests {

    private AuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private AuthenticationRedirectStrategy redirectStrategy;
    private GatewayResolver gatewayResolver;

    @BeforeEach
    void setup() throws Exception {
        filter = new AuthenticationFilter(Protocol.CAS2);
        filter.setCasServerLoginUrl("https://cas.example.com/login");

        // Inject AuthenticationRedirectStrategy via reflection
        Field redirectField = AuthenticationFilter.class
                .getDeclaredField("authenticationRedirectStrategy");
        redirectField.setAccessible(true);
        redirectStrategy = mock(AuthenticationRedirectStrategy.class);
        redirectField.set(filter, redirectStrategy);

        // Inject GatewayResolver via reflection
        Field gatewayField = AuthenticationFilter.class
                .getDeclaredField("gatewayStorage");
        gatewayField.setAccessible(true);
        gatewayResolver = mock(GatewayResolver.class);
        gatewayField.set(filter, gatewayResolver);

        // Create mocks for request/response/chain
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);

        // Spy to override service URL and ticket extraction methods
//        filter = Mockito.spy(filter);
//        doReturn("https://app/service")
//                .when(filter).constructServiceUrl(request, response);
//        doReturn("").when(filter).retrieveTicketFromRequest(request);
    }

    @Test
    void shouldIgnoreUrlWhenMatcherReturnsTrue() throws IOException, ServletException {
        UrlPatternMatcherStrategy ignoreStrategy = mock(UrlPatternMatcherStrategy.class);
        filter.setIgnoreUrlPatternMatcherStrategyClass(ignoreStrategy);
        when(ignoreStrategy.matches("https://app/service")).thenReturn(true);
        when(request.getRequestURL()).thenReturn(new StringBuffer("https://app/service"));
        when(request.getQueryString()).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldRedirectWhenNoTicketAndNotGatewayed() throws IOException, ServletException {
        when(gatewayResolver.hasGatewayedAlready(request, "https://app/service"))
                .thenReturn(false);
        filter.setGateway(false);
        when(request.getRequestURL()).thenReturn(new StringBuffer("ignored"));
        when(request.getQueryString()).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(redirectStrategy).redirect(
                eq(request),
                eq(response),
                contains("https://cas.example.com/login?service=https%3A%2F%2Fapp%2Fservice")
        );
        verifyNoInteractions(chain);
    }
}
