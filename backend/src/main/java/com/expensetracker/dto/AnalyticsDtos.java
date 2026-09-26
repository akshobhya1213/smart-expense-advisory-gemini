package com.expensetracker.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AnalyticsDtos {

    public record MonthlySummary(
            int year, int month,
            BigDecimal totalExpenses,
            BigDecimal previousMonthExpenses,
            double changePercent,
            long transactionCount,
            BigDecimal averageDailySpend
    ) implements Serializable {}

    public record CategoryBreakdownItem(
            Long categoryId, String categoryName,
            BigDecimal totalAmount, double percentage, long transactionCount
    ) implements Serializable {}

    public record CategoryBreakdown(
            int year, int month,
            List<CategoryBreakdownItem> categories,
            String highestSpendingCategory
    ) implements Serializable {}

    public record DailyPoint(LocalDate date, BigDecimal amount) implements Serializable {}

    public record TrendResponse(
            String granularity, // daily | weekly | monthly
            List<DailyPoint> points
    ) implements Serializable {}

    public record BudgetAnalytics(
            int year, int month,
            BigDecimal totalBudget,
            BigDecimal totalSpent,
            double overallUtilization,
            List<BudgetDtos.BudgetResponse> categoryBudgets
    ) implements Serializable {}
}
