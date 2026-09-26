package com.expensetracker.ai;

import com.expensetracker.dto.AnalyticsDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds a structured prompt from numbers that Java has ALREADY calculated
 * (monthly summary, category breakdown, budget utilization) and asks Gemini
 * only to turn those numbers into natural-language advice. Gemini never
 * computes totals, percentages, or budget math itself — that's deliberate:
 * LLM arithmetic isn't reliable enough to be the source of truth for a
 * user's financial figures.
 */
@Service
@RequiredArgsConstructor
public class GeminiFinancialAdvisorService {

    private final GeminiClient geminiClient;

    public String generateInsights(AnalyticsDtos.MonthlySummary summary,
                                    AnalyticsDtos.CategoryBreakdown breakdown,
                                    AnalyticsDtos.BudgetAnalytics budgetAnalytics) {

        String prompt = buildPrompt(summary, breakdown, budgetAnalytics);
        return geminiClient.generateInsight(prompt);
    }

    private String buildPrompt(AnalyticsDtos.MonthlySummary summary,
                                AnalyticsDtos.CategoryBreakdown breakdown,
                                AnalyticsDtos.BudgetAnalytics budgetAnalytics) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a friendly personal finance advisor. Based ONLY on the structured data below, ")
          .append("write 3-4 short, specific, encouraging recommendations for the user. ")
          .append("Do not invent numbers that are not given. Use plain, direct language. Use INR (₹).\n\n");

        sb.append("Monthly summary:\n");
        sb.append("- Total spending this month: ₹").append(summary.totalExpenses()).append("\n");
        sb.append("- Total spending last month: ₹").append(summary.previousMonthExpenses()).append("\n");
        sb.append("- Change vs last month: ").append(String.format("%.1f", summary.changePercent())).append("%\n");
        sb.append("- Transaction count: ").append(summary.transactionCount()).append("\n\n");

        sb.append("Category breakdown:\n");
        breakdown.categories().forEach(c ->
                sb.append("- ").append(c.categoryName()).append(": ₹").append(c.totalAmount())
                  .append(" (").append(String.format("%.1f", c.percentage())).append("% of spending)\n"));
        sb.append("Highest spending category: ").append(breakdown.highestSpendingCategory()).append("\n\n");

        sb.append("Budget utilization:\n");
        sb.append("- Overall budget: ₹").append(budgetAnalytics.totalBudget()).append("\n");
        sb.append("- Overall spent: ₹").append(budgetAnalytics.totalSpent()).append("\n");
        sb.append("- Overall utilization: ").append(String.format("%.1f", budgetAnalytics.overallUtilization())).append("%\n");
        budgetAnalytics.categoryBudgets().forEach(b ->
                sb.append("- ").append(b.categoryName() == null ? "Overall" : b.categoryName())
                  .append(": ₹").append(b.spent()).append(" / ₹").append(b.limitAmount())
                  .append(" (").append(b.status()).append(")\n"));

        return sb.toString();
    }
}
