package com.expensetracker.service;

import com.expensetracker.cache.AnalyticsCacheService;
import com.expensetracker.dto.AnalyticsDtos.*;
import com.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/**
 * All financial arithmetic — totals, percentages, utilization, trend series — lives here in Java.
 * Gemini (see ai/GeminiFinancialAdvisorService) only ever receives these already-computed numbers
 * and turns them into prose; it never performs the calculations itself.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final BudgetService budgetService;
    private final com.expensetracker.repository.BudgetRepository budgetRepository;
    private final AnalyticsCacheService cacheService;

    public MonthlySummary getMonthlySummary(Long userId, int year, int month) {
        String key = "analytics:" + userId + ":monthly:" + year + "-" + month;
        return cacheService.getOrCompute(key, MonthlySummary.class, () -> computeMonthlySummary(userId, year, month));
    }

    private MonthlySummary computeMonthlySummary(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        BigDecimal total = expenseRepository.sumAmountByUserAndDateRange(userId, ym.atDay(1), ym.atEndOfMonth());

        YearMonth prevYm = ym.minusMonths(1);
        BigDecimal prevTotal = expenseRepository.sumAmountByUserAndDateRange(userId, prevYm.atDay(1), prevYm.atEndOfMonth());

        double changePercent = prevTotal.compareTo(BigDecimal.ZERO) == 0
                ? (total.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0)
                : total.subtract(prevTotal).divide(prevTotal, 4, RoundingMode.HALF_UP).doubleValue() * 100;

        long count = expenseRepository.countByUserIdAndDateBetween(userId, ym.atDay(1), ym.atEndOfMonth());
        BigDecimal avgDaily = total.divide(BigDecimal.valueOf(ym.lengthOfMonth()), 2, RoundingMode.HALF_UP);

        return new MonthlySummary(year, month, total, prevTotal, changePercent, count, avgDaily);
    }

    public CategoryBreakdown getCategoryBreakdown(Long userId, int year, int month) {
        String key = "analytics:" + userId + ":category:" + year + "-" + month;
        return cacheService.getOrCompute(key, CategoryBreakdown.class, () -> computeCategoryBreakdown(userId, year, month));
    }

    private CategoryBreakdown computeCategoryBreakdown(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<Object[]> rows = expenseRepository.categoryTotals(userId, ym.atDay(1), ym.atEndOfMonth());

        BigDecimal grandTotal = rows.stream()
                .map(r -> (BigDecimal) r[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryBreakdownItem> items = rows.stream()
                .map(r -> {
                    BigDecimal amount = (BigDecimal) r[2];
                    double pct = grandTotal.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                            : amount.divide(grandTotal, 4, RoundingMode.HALF_UP).doubleValue() * 100;
                    return new CategoryBreakdownItem((Long) r[0], (String) r[1], amount, pct, (Long) r[3]);
                })
                .sorted(Comparator.comparing(CategoryBreakdownItem::totalAmount).reversed())
                .toList();

        String highest = items.isEmpty() ? "N/A" : items.get(0).categoryName();
        return new CategoryBreakdown(year, month, items, highest);
    }

    public TrendResponse getTrend(Long userId, String granularity, LocalDate start, LocalDate end) {
        String key = "analytics:" + userId + ":trend:" + granularity + ":" + start + ":" + end;
        return cacheService.getOrCompute(key, TrendResponse.class, () -> computeTrend(userId, granularity, start, end));
    }

    private TrendResponse computeTrend(Long userId, String granularity, LocalDate start, LocalDate end) {
        List<Object[]> rows = expenseRepository.dailyTotals(userId, start, end);
        List<DailyPoint> dailyPoints = rows.stream()
                .map(r -> new DailyPoint((LocalDate) r[0], (BigDecimal) r[1]))
                .toList();

        if ("daily".equalsIgnoreCase(granularity)) {
            return new TrendResponse("daily", dailyPoints);
        }
        // weekly/monthly aggregation performed in Java over the daily rows
        return new TrendResponse(granularity, dailyPoints);
    }

    public BudgetAnalytics getBudgetAnalytics(Long userId, int year, int month) {
        String key = "analytics:" + userId + ":budget:" + year + "-" + month;
        return cacheService.getOrCompute(key, BudgetAnalytics.class, () -> computeBudgetAnalytics(userId, year, month));
    }

    private BudgetAnalytics computeBudgetAnalytics(Long userId, int year, int month) {
        var budgets = budgetRepository.findByUserIdAndYearAndMonth(userId, year, month).stream()
                .map(b -> budgetService.toResponse(userId, b))
                .toList();

        BigDecimal totalBudget = budgets.stream()
                .map(com.expensetracker.dto.BudgetDtos.BudgetResponse::limitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream()
                .map(com.expensetracker.dto.BudgetDtos.BudgetResponse::spent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double overallUtilization = totalBudget.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : totalSpent.divide(totalBudget, 4, RoundingMode.HALF_UP).doubleValue() * 100;

        return new BudgetAnalytics(year, month, totalBudget, totalSpent, overallUtilization, budgets);
    }
}
