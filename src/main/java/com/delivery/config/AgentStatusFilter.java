package com.delivery.config;

import com.delivery.model.Agent;
import com.delivery.model.AgentStatus;
import com.delivery.repository.AgentRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Optional;

/**
 * Filter to check if the authenticated user is an agent and their status is ACTIVE.
 * If not ACTIVE, invalidates the active session, clears security context, and blocks access.
 */
@Component
public class AgentStatusFilter extends OncePerRequestFilter {

    private final AgentRepository agentRepository;

    public AgentStatusFilter(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            String username = auth.getName();
            boolean isAgent = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));
            if (isAgent && username != null) {
                Optional<Agent> agentOpt = agentRepository.findById(username);
                if (agentOpt.isPresent()) {
                    Agent agent = agentOpt.get();
                    if (agent.getStatus() != AgentStatus.ACTIVE) {
                        // Clear security context and invalidate session
                        SecurityContextHolder.clearContext();
                        if (request.getSession(false) != null) {
                            request.getSession().invalidate();
                        }

                        String requestURI = request.getRequestURI();
                        if (requestURI.startsWith("/api/")) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Your account has been deactivated. Please contact the administrator.\"}");
                            return;
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login?error=deactivated");
                            return;
                        }
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
