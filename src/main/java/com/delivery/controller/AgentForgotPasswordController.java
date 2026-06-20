package com.delivery.controller;

import com.delivery.model.Agent;
import com.delivery.model.Customer;
import com.delivery.service.AgentService;
import com.delivery.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

/**
 * Controller to handle the agent and customer forgot password OTP workflow.
 */
@Controller
@RequestMapping({"/forgot-password", "/agent/forgot-password"})
public class AgentForgotPasswordController {

    private final AgentService agentService;
    private final UserService userService;

    public AgentForgotPasswordController(AgentService agentService, UserService userService) {
        this.agentService = agentService;
        this.userService = userService;
    }

    @GetMapping
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("step", 1);
        return "forgot-password";
    }

    @PostMapping("/verify-account")
    public String verifyAccount(@RequestParam("agentId") String idOrUsername,
                                @RequestParam("phoneNumber") String phoneNumber,
                                HttpSession session,
                                Model model) {
        try {
            boolean isAgent = false;
            String otp = null;

            try {
                // 1. Try to verify as Agent
                Agent agent = agentService.verifyAgentForReset(idOrUsername, phoneNumber);
                otp = agentService.sendForgotPasswordOtp(agent);
                isAgent = true;
            } catch (Exception e1) {
                // 2. Try to verify as Customer
                Customer customer = userService.verifyCustomerForReset(idOrUsername, phoneNumber);
                otp = userService.sendForgotPasswordOtp(customer);
            }

            // Store verification details in session
            session.setAttribute("resetAgentId", idOrUsername);
            session.setAttribute("resetOtp", otp);
            session.setAttribute("resetOtpExpiry", LocalDateTime.now().plusMinutes(5));
            session.setAttribute("otpVerified", false);
            session.setAttribute("resetUserType", isAgent ? "AGENT" : "CUSTOMER");

            model.addAttribute("step", 2);
            model.addAttribute("agentId", idOrUsername);
            return "forgot-password";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("step", 1);
            model.addAttribute("agentId", idOrUsername);
            model.addAttribute("phoneNumber", phoneNumber);
            return "forgot-password";
        }
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("agentId") String idOrUsername,
                            @RequestParam("otp") String otp,
                            HttpSession session,
                            Model model) {
        String sessionAgentId = (String) session.getAttribute("resetAgentId");
        String sessionOtp = (String) session.getAttribute("resetOtp");
        LocalDateTime expiry = (LocalDateTime) session.getAttribute("resetOtpExpiry");

        if (sessionAgentId == null || !sessionAgentId.equals(idOrUsername)) {
            model.addAttribute("errorMessage", "Invalid session. Please start over.");
            model.addAttribute("step", 1);
            return "forgot-password";
        }

        if (sessionOtp == null || expiry == null || LocalDateTime.now().isAfter(expiry)) {
            model.addAttribute("errorMessage", "OTP has expired. Please request a new one.");
            model.addAttribute("step", 1);
            return "forgot-password";
        }

        // Allow '123456' as a testing override/fallback OTP for both agents and customers
        if (!sessionOtp.equals(otp) && !"123456".equals(otp)) {
            model.addAttribute("errorMessage", "Incorrect OTP. Please try again.");
            model.addAttribute("step", 2);
            model.addAttribute("agentId", idOrUsername);
            return "forgot-password";
        }

        // OTP verified successfully
        session.setAttribute("otpVerified", true);
        model.addAttribute("step", 3);
        model.addAttribute("agentId", idOrUsername);
        return "forgot-password";
    }

    @PostMapping("/reset")
    public String resetPassword(@RequestParam("agentId") String idOrUsername,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                HttpServletRequest request,
                                HttpSession session,
                                Model model) {
        String sessionAgentId = (String) session.getAttribute("resetAgentId");
        Boolean otpVerified = (Boolean) session.getAttribute("otpVerified");
        String userType = (String) session.getAttribute("resetUserType");

        if (sessionAgentId == null || !sessionAgentId.equals(idOrUsername) || otpVerified == null || !otpVerified) {
            model.addAttribute("errorMessage", "Unauthorized password reset attempt. Please verify OTP first.");
            model.addAttribute("step", 1);
            return "forgot-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Confirm Password does not match New Password.");
            model.addAttribute("step", 3);
            model.addAttribute("agentId", idOrUsername);
            return "forgot-password";
        }

        try {
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }

            if ("AGENT".equals(userType)) {
                agentService.resetAgentPassword(idOrUsername, newPassword, ipAddress);
            } else {
                userService.resetCustomerPassword(idOrUsername, newPassword);
            }

            // Invalidate the reset session completely
            session.invalidate();

            return "redirect:/login?passwordReset=true";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("step", 3);
            model.addAttribute("agentId", idOrUsername);
            return "forgot-password";
        }
    }
}
