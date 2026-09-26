package com.expensetracker.controller;

import com.expensetracker.ai.GeminiFinancialAdvisorService;
import com.expensetracker.security.CurrentUserResolver;
import com.expensetracker.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Advisor")
public class AiController {

    private final GeminiFinancialAdvisorService advisorService;
    private final AnalyticsService analyticsService;
    private final CurrentUserResolver currentUser;

    @GetMapping("/insights")
    public Map<String, String> insights() {
        Long userId = currentUser.userId();
        LocalDate now = LocalDate.now();

        var summary = analyticsService.getMonthlySummary(userId, now.getYear(), now.getMonthValue());
        var breakdown = analyticsService.getCategoryBreakdown(userId, now.getYear(), now.getMonthValue());
        var budgetAnalytics = analyticsService.getBudgetAnalytics(userId, now.getYear(), now.getMonthValue());

        // Note: if this throws ApiExceptions.AiServiceException, GlobalExceptionHandler
        // turns it into a 503 with a friendly message — the frontend shows that, not a crash.
        String insight = advisorService.generateInsights(summary, breakdown, budgetAnalytics);
        return Map.of("insight", insight);
    }
}
