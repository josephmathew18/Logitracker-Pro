package com.delivery.controller;

import com.delivery.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String role = auth.getAuthorities().iterator().next().getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            } else if (role.equals("ROLE_CUSTOMER")) {
                return "redirect:/customer/dashboard";
            } else if (role.equals("ROLE_AGENT")) {
                return "redirect:/agent/dashboard";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        jakarta.servlet.http.HttpServletRequest request,
                        Model model) {
        if (error != null) {
            String errorMessage = "Invalid Username or Password!";
            if ("deactivated".equals(error)) {
                errorMessage = "Your account has been deactivated. Please contact the administrator.";
            } else {
                jakarta.servlet.http.HttpSession session = request.getSession(false);
                if (session != null) {
                    Exception lastException = (Exception) session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
                    if (lastException != null && lastException.getMessage() != null && 
                            (lastException.getMessage().toLowerCase().contains("deactivated") || 
                             lastException.getClass().getSimpleName().equals("DisabledException"))) {
                        errorMessage = "Your account has been deactivated. Please contact the administrator.";
                    }
                }
            }
            model.addAttribute("errorMessage", errorMessage);
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (request.getParameter("passwordChanged") != null) {
            model.addAttribute("successMessage", "Password updated successfully. Please log in again.");
        }
        if (request.getParameter("passwordReset") != null) {
            model.addAttribute("successMessage", "Password reset successfully. Please log in again.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               @RequestParam("name") String name,
                               @RequestParam("phone") String phone,
                               @RequestParam("email") String email,
                               @RequestParam("address") String address,
                               Model model) {
        if (userService.usernameExists(username)) {
            model.addAttribute("errorMessage", "Username is already taken.");
            return "register";
        }
        if (userService.emailExists(email)) {
            model.addAttribute("errorMessage", "Email is already registered.");
            return "register";
        }

        try {
            userService.registerCustomer(username, password, name, phone, email, address);
            model.addAttribute("successMessage", "Registration successful! Please login.");
            return "login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "register";
        }
    }
}
