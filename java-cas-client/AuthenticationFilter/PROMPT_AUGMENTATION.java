package org.apereo.cas.client.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apereo.cas.client.authentication.AuthenticationFilter;
import org.apereo.cas.client.authentication.UrlPatternMatcherStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTests {

    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    @Mock
    FilterChain chain;
    @Mock
    UrlPatternMatcherStrategy ignoreMatcher;
    @Mock
    org.apereo.cas.client.util.GatewayResolver gatewayStorage;
    @Mock
    org.apereo.cas.client.util.AuthenticationRedirectStrategy redirectStrategy;

    @InjectMocks
    AuthenticationFilter filter;

    @Captor
    ArgumentCaptor<String> redirectUrlCaptor;

    @BeforeEach
    void setup() {
        // Espiona o filtro para stubar métodos protegidos sem chamar initInternal
        filter = Mockito.spy(new AuthenticationFilter());
        filter.setCasServerLoginUrl("https://cas.test/login");
        filter.setGatewayStorage(gatewayStorage);
        // injeta estratégia de redirect personalizada
        filter.authenticationRedirectStrategy = redirectStrategy;
    }

    @Test
    void shouldIgnoreUrlAndContinueChain() throws IOException, ServletException {
        when(ignoreMatcher.matches(anyString())).thenReturn(true);
        filter.setIgnoreUrlPatternMatcherStrategyClass(ignoreMatcher);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://app/resource"));
        when(request.getQueryString()).thenReturn("a=1");

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldContinueWhenAssertionInSession() throws IOException, ServletException {
        var session = mock(javax.servlet.http.HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthenticationFilter.CONST_CAS_ASSERTION)).thenReturn(new Object());

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldContinueWhenTicketPresent() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(null);
        when(request.getParameter("ticket")).thenReturn("ST-12345");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldContinueWhenGatewayedAlready() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(null);
        doReturn(null).when(filter).retrieveTicketFromRequest(request);
        doReturn("serviceUrl").when(filter).constructServiceUrl(request, response);
        filter.setGateway(true);
        when(gatewayStorage.hasGatewayedAlready(request, "serviceUrl")).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldRedirectToCasWhenNoTicketOrAssertion() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(null);
        doReturn(null).when(filter).retrieveTicketFromRequest(request);
        doReturn("svc").when(filter).constructServiceUrl(request, response);
        filter.setRenew(false);
        filter.setGateway(false);
        filter.setMethod(null);

        filter.doFilter(request, response, chain);

        verify(redirectStrategy).redirect(eq(request), eq(response), redirectUrlCaptor.capture());
        String redirectUrl = redirectUrlCaptor.getValue();
        assertTrue(redirectUrl.startsWith("https://cas.test/login?service=svc"));
    }

    @Test
    void shouldRedirectWithGatewayParameterAndStoredService() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(null);
        doReturn(null).when(filter).retrieveTicketFromRequest(request);
        doReturn("origSvc").when(filter).constructServiceUrl(request, response);
        filter.setRenew(false);
        filter.setGateway(true);
        when(gatewayStorage.storeGatewayInformation(request, "origSvc")).thenReturn("modifiedSvc");

        filter.doFilter(request, response, chain);

        verify(redirectStrategy).redirect(eq(request), eq(response), redirectUrlCaptor.capture());
        String redirectUrl = redirectUrlCaptor.getValue();
        assertTrue(redirectUrl.contains("gateway=true"));
        assertTrue(redirectUrl.contains("service=modifiedSvc"));
    }
}
