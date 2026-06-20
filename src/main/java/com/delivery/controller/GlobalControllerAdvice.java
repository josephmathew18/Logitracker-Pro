package com.delivery.controller;

import com.delivery.model.Agent;
import com.delivery.model.Customer;
import com.delivery.service.AgentService;
import com.delivery.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;
    private final AgentService agentService;

    public GlobalControllerAdvice(UserService userService, AgentService agentService) {
        this.userService = userService;
        this.agentService = agentService;
    }

    @ModelAttribute("customer")
    public Customer addCustomerToModel(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isCustomer = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
            if (isCustomer) {
                return userService.getCustomerByUsername(authentication.getName()).orElse(null);
            }
        }
        return null;
    }

    @ModelAttribute("agent")
    public Agent addAgentToModel(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAgent = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));
            if (isAgent) {
                return agentService.getAgentByUsername(authentication.getName()).orElse(null);
            }
        }
        return null;
    }
}
