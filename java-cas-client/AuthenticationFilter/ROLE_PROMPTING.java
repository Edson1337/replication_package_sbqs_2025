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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Mock
    private GatewayResolver gatewayStorage;
    @Mock
    private AuthenticationRedirectStrategy redirectStrategy;

    @InjectMocks
    private AuthenticationFilter filter;

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    @BeforeEach
    void setup() {
        filter = new AuthenticationFilter();
        filter.setCasServerLoginUrl("https://cas.example.org/login");
        filter.setGatewayStorage(gatewayStorage);
//        filter.authenticationRedirectStrategy = redirectStrategy;
    }

//    @Test
//    void doFilter_shouldSkipWhenUrlIgnored() throws IOException, ServletException {
//        // ignora tudo
//        filter.setIgnoreUrlPatternMatcherStrategyClass(uri -> true);
//
//        filter.doFilter(request, response, chain);
//
//        verify(chain).doFilter(request, response);
//        verifyNoInteractions(redirectStrategy);
//    }

    @Test
    void doFilter_shouldSkipWhenSessionHasAssertion() throws IOException, ServletException {
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("CONST_CAS_ASSERTION")).thenReturn(mock(Assertion.class));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void doFilter_shouldSkipWhenTicketPresent() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(null);
        when(request.getParameter("ticket")).thenReturn("ST-1");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void doFilter_shouldSkipWhenGatewayAppliedAlready() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(null);
        when(request.getParameter("ticket")).thenReturn("");
        when(gatewayStorage.hasGatewayedAlready(any(), anyString())).thenReturn(true);

        filter.setGateway(true);
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoMoreInteractions(redirectStrategy);
    }

    @Test
    void doFilter_shouldRedirectToCasWhenNoTicketOrAssertion() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(null);
        when(request.getParameter("ticket")).thenReturn(null);

        filter.setRenew(true);
        filter.setGateway(false);

        filter.doFilter(request, response, chain);

        verify(redirectStrategy).redirect(eq(request), eq(response), urlCaptor.capture());
        String redirectUrl = urlCaptor.getValue();
        assertTrue(redirectUrl.startsWith("https://cas.example.org/login"));
        assertTrue(redirectUrl.contains("renew=true"));
    }
}
