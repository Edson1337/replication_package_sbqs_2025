// AuthenticationFilterTest.java
package org.apereo.cas.client.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class AuthenticationFilterTest {
    private AuthenticationFilter filter;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;
    @Mock private HttpSession session;
    @Mock private GatewayResolver gatewayResolver;
    @Mock private AuthenticationRedirectStrategy redirectStrategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Subclasse para injetar a estratégia de redirect
        filter = new AuthenticationFilter() {
            void setRedirectStrategy(AuthenticationRedirectStrategy s) {
                this.authenticationRedirectStrategy = s;
            }
        };
        filter.setCasServerLoginUrl("https://cas.example.org/login");
        filter.setGateway(false);
        filter.setRenew(false);
        filter.setGatewayStorage(gatewayResolver);
        ((AuthenticationFilter) filter).setRedirectStrategy(redirectStrategy);
    }

    @Test
    void doFilter_comAssertion_naSessao_continuaChain() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("CONST_CAS_ASSERTION")).thenReturn(mock(Assertion.class));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void doFilter_urlIgnorada_continuaChain() throws IOException, ServletException {
        filter.setIgnoreUrlPatternMatcherStrategyClass(new ContainsPatternUrlPatternMatcherStrategy());
        ((ContainsPatternUrlPatternMatcherStrategy) filter.ignoreUrlPatternMatcherStrategyClass)
                .setPattern("/skip");

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://app/skip"));
        when(request.getQueryString()).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    void doFilter_semTicket_nemAssertion_redirecionaAoCAS() throws IOException, ServletException {
        when(request.getSession(false)).thenReturn(null);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://app/home"));
        when(request.getQueryString()).thenReturn(null);
        // sem param ticket
        when(request.getParameter("ticket")).thenReturn(null);

        filter.doFilter(request, response, chain);

        String expectedService = "http://app/home";
        String target = "https://cas.example.org/login?service=" + expectedService + "&renew=false&gateway=false";
        verify(redirectStrategy).redirect(request, response, target);
        verifyNoMoreInteractions(chain);
    }
}
