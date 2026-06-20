package com.delivery.config;

import com.delivery.repository.AgentRepository;
import com.delivery.repository.AgentActivityLogRepository;
import com.delivery.model.AgentActivityLog;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Redirects the user to their specific dashboard based on their role after successful login.
 */
@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    private final AgentRepository agentRepository;
    private final AgentActivityLogRepository agentActivityLogRepository;

    public CustomSuccessHandler(AgentRepository agentRepository, AgentActivityLogRepository agentActivityLogRepository) {
        this.agentRepository = agentRepository;
        this.agentActivityLogRepository = agentActivityLogRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String redirectUrl = "/login?error=true"; // default fallback

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                redirectUrl = "/admin/dashboard";
                break;
            } else if (role.equals("ROLE_CUSTOMER")) {
                redirectUrl = "/customer/dashboard";
                break;
            } else if (role.equals("ROLE_AGENT")) {
                redirectUrl = "/agent/dashboard";
                // Update last login timestamp for the agent
                agentRepository.findByUserUsername(authentication.getName()).ifPresent(agent -> {
                    agent.setLastLogin(LocalDateTime.now());
                    agentRepository.save(agent);

                    // Log activity
                    String ipAddress = request.getHeader("X-Forwarded-For");
                    if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                        ipAddress = request.getRemoteAddr();
                    }
                    AgentActivityLog log = new AgentActivityLog(agent.getId(), "Login Success", ipAddress);
                    agentActivityLogRepository.save(log);
                });
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}
