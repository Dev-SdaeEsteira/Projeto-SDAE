package com.cba.sdae.filter;

import com.cba.sdae.service.LoginAttemptService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BruteForceProtectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BruteForceProtectionFilter.class);

    private final LoginAttemptService loginAttemptService;

    public BruteForceProtectionFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Apply brute force protection only to login endpoints
        if (requestURI.equals("/login") || requestURI.equals("/api/auth/login")) {
            String clientIP = getClientIP(request);
            String loginIdentifier = getLoginIdentifier(request);

            // Use both IP and login identifier for blocking
            String blockKey = clientIP + ":" + loginIdentifier;

            if (loginAttemptService.isBlocked(blockKey)) {
                log.warn("Tentativa de login bloqueada para IP: {}, Identificador: {}", clientIP, loginIdentifier);
                // CORRIGIDO: HttpServletResponse.SC_TOO_MANY_REQUESTS não existe.
                // Usamos o HttpStatus do Spring (429).
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"erro\":\"Muitas tentativas de login. Tente novamente em 15 minutos.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private String getLoginIdentifier(HttpServletRequest request) {
        // For form login, get from parameters
        String username = request.getParameter("email");
        if (username != null && !username.isEmpty()) {
            return username;
        }

        // For REST API login, we'll handle this in the controller
        return "unknown";
    }
}