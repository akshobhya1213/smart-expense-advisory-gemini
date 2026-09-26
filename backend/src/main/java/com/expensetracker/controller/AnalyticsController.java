package com.expensetracker.controller;

import com.expensetracker.dto.AnalyticsDtos.*;
import com.expensetracker.security.CurrentUserResolver;
import com.expensetracker.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserResolver currentUser;

    @GetMapping("/monthly")
    public MonthlySummary monthly(@RequestParam int year, @RequestParam int month) {
        return analyticsService.getMonthlySummary(currentUser.userId(), year, month);
    }

    @GetMapping("/category")
    public CategoryBreakdown category(@RequestParam int year, @RequestParam int month) {
        return analyticsService.getCategoryBreakdown(currentUser.userId(), year, month);
    }

    @GetMapping("/trends")
    public TrendResponse trends(@RequestParam(defaultValue = "daily") String granularity,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return analyticsService.getTrend(currentUser.userId(), granularity, startDate, endDate);
    }

    @GetMapping("/budget")
    public BudgetAnalytics budget(@RequestParam int year, @RequestParam int month) {
        return analyticsService.getBudgetAnalytics(currentUser.userId(), year, month);
    }
}
