package org.apereo.cas.client.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apereo.cas.client.Protocol;
import org.apereo.cas.client.validation.Assertion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTests {

    @Spy
    private AuthenticationFilter filter = new AuthenticationFilter(Protocol.CAS2);

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private HttpSession session;
    @Mock
    private UrlPatternMatcherStrategy matcherStrategy;
    @Mock
    private GatewayResolver gatewayStorage;
    @Mock
    private AuthenticationRedirectStrategy redirectStrategy;

    private static final String SERVICE_URL = "http://app/callback";

    @BeforeEach
    void setup() throws ServletException {
        filter.setCasServerLoginUrl("https://cas.test/login");
        filter.setGatewayStorage(gatewayStorage);
        filter.authenticationRedirectStrategy = redirectStrategy;
        doReturn(SERVICE_URL).when(filter).constructServiceUrl(request, response);
        doReturn("").when(filter).retrieveTicketFromRequest(request);
    }

    @Test
    void shouldSkipWhenUrlExcluded() throws IOException, ServletException {
        filter.setIgnoreUrlPatternMatcherStrategyClass(matcherStrategy);
        when(matcherStrategy.matches(anyString())).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldSkipWhenSessionHasAssertion() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("CONST_CAS_ASSERTION")).thenReturn(mock(Assertion.class));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldSkipWhenTicketPresent() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(null);
        doReturn("ST-12345").when(filter).retrieveTicketFromRequest(request);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldRedirectWhenNoTicketOrAssertion() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(null);
        doReturn("").when(filter).retrieveTicketFromRequest(request);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).redirect(eq(request), eq(response), captor.capture());

        String redirectUrl = captor.getValue();
        assertTrue(redirectUrl.startsWith("https://cas.test/login"));
        assertTrue(redirectUrl.contains("service=" + SERVICE_URL));
    }
}
