package com.delivery.service;

import com.delivery.model.Agent;
import com.delivery.model.AgentStatus;
import com.delivery.model.User;
import com.delivery.repository.AgentRepository;
import com.delivery.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

/**
 * Custom service for loading user details for Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;

    public CustomUserDetailsService(UserRepository userRepository, AgentRepository agentRepository) {
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            throw new DisabledException("Your account has been deactivated. Please contact the administrator.");
        }

        if ("AGENT".equals(user.getRole())) {
            Optional<Agent> agentOpt = agentRepository.findById(user.getUsername());
            if (agentOpt.isPresent()) {
                Agent agent = agentOpt.get();
                if (agent.getStatus() != AgentStatus.ACTIVE) {
                    throw new DisabledException("Your account has been deactivated. Please contact the administrator.");
                }
            }
        }

        // Add ROLE_ prefix as Spring Security expects
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }
}
