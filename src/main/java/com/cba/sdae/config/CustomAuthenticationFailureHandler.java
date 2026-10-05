package com.cba.sdae.config;

import com.cba.sdae.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public CustomAuthenticationFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("email");
        String clientIP = getClientIP(request);
        String blockKey = clientIP + ":" + (username != null ? username : "unknown");
        
        loginAttemptService.loginFailed(blockKey);
        
        response.sendRedirect("/login?erro=true");
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
