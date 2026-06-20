package com.delivery.controller;

import com.delivery.model.Agent;
import com.delivery.service.AgentService;
import com.delivery.service.FeedbackService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/agent")
public class AgentFeedbackController {

    private final FeedbackService feedbackService;
    private final AgentService agentService;

    public AgentFeedbackController(FeedbackService feedbackService, AgentService agentService) {
        this.feedbackService = feedbackService;
        this.agentService = agentService;
    }

    /**
     * Display Agent Feedback Dashboard.
     */
    @GetMapping("/feedback")
    public String agentFeedbackDashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<Agent> agentOpt = agentService.getAgentByUsername(username);

        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            Map<String, Object> stats = feedbackService.getAgentFeedbackStatistics(agent.getId());

            model.addAttribute("agent", agent);
            model.addAttribute("stats", stats);
            model.addAttribute("feedbacks", stats.get("history"));
        }

        return "agent/feedback";
    }
}
