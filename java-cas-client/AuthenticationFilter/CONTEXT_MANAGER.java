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
        filter.setIgnoreUrlPatternMatcherStrategyClass(uri -> true);

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
        doReturn("ST-123").when(filter).retrieveTicketFromRequest(request);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldSkipWhenAlreadyGatewayed() throws IOException, ServletException {
        filter.setGateway(true);
        when(request.getSession(false)).thenReturn(null);
        when(gatewayStorage.hasGatewayedAlready(request, SERVICE_URL)).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void shouldRedirectWithGatewayStorage() throws IOException, ServletException {
        filter.setGateway(true);
        when(request.getSession(false)).thenReturn(null);
        when(gatewayStorage.hasGatewayedAlready(request, SERVICE_URL)).thenReturn(false);
        when(gatewayStorage.storeGatewayInformation(request, SERVICE_URL))
                .thenReturn(SERVICE_URL + "/gw");

        filter.doFilter(request, response, chain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).redirect(eq(request), eq(response), captor.capture());
        String url = captor.getValue();

        assertTrue(url.startsWith("https://cas.test/login"));
        assertTrue(url.contains("gateway=true"));
        assertTrue(url.contains("service=" + SERVICE_URL + "/gw"));
    }

    @Test
    void shouldRedirectWithRenewParameter() throws IOException, ServletException {
        filter.setRenew(true);
        when(request.getSession(false)).thenReturn(null);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).redirect(eq(request), eq(response), captor.capture());
        String url = captor.getValue();

        assertTrue(url.contains("renew=true"));
    }

    @Test
    void shouldRedirectWithCustomMethod() throws IOException, ServletException {
        filter.setMethod("POST");
        when(request.getSession(false)).thenReturn(null);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).redirect(eq(request), eq(response), captor.capture());
        String url = captor.getValue();

        assertTrue(url.contains("method=POST"));
    }
}
