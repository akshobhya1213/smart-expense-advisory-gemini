package com.expensetracker.ai;

import com.expensetracker.dto.AnalyticsDtos.*;
import com.expensetracker.exception.ApiExceptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiFinancialAdvisorServiceTest {

    @Mock GeminiClient geminiClient;

    private MonthlySummary summary() {
        return new MonthlySummary(2026, 9, BigDecimal.valueOf(15000), BigDecimal.valueOf(12000), 25.0, 30, BigDecimal.valueOf(500));
    }

    private CategoryBreakdown breakdown() {
        var item = new CategoryBreakdownItem(1L, "Food", BigDecimal.valueOf(8450), 56.3, 12);
        return new CategoryBreakdown(2026, 9, List.of(item), "Food");
    }

    private BudgetAnalytics budgetAnalytics() {
        var budgetResponse = new com.expensetracker.dto.BudgetDtos.BudgetResponse(
                1L, 1L, "Food", 2026, 9,
                BigDecimal.valueOf(7000), BigDecimal.valueOf(8450), BigDecimal.valueOf(-1450), 120.7, "EXCEEDED");
        return new BudgetAnalytics(2026, 9, BigDecimal.valueOf(30000), BigDecimal.valueOf(15000), 50.0, List.of(budgetResponse));
    }

    @Test
    void generateInsights_returnsGeminiText_onSuccess() {
        when(geminiClient.generateInsight(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("Consider reducing Food spending this month.");

        GeminiFinancialAdvisorService service = new GeminiFinancialAdvisorService(geminiClient);
        String result = service.generateInsights(summary(), breakdown(), budgetAnalytics());

        assertThat(result).contains("Food spending");
    }

    @Test
    void generateInsights_propagatesAiServiceException_whenGeminiUnavailable() {
        // The prompt-builder itself never throws; failures come from the client (network/API key/etc.)
        // and must surface as ApiExceptions.AiServiceException so GlobalExceptionHandler can turn
        // them into a friendly 503 instead of a raw 500 or a crash.
        when(geminiClient.generateInsight(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new ApiExceptions.AiServiceException("Gemini API call failed", null));

        GeminiFinancialAdvisorService service = new GeminiFinancialAdvisorService(geminiClient);

        assertThatThrownBy(() -> service.generateInsights(summary(), breakdown(), budgetAnalytics()))
                .isInstanceOf(ApiExceptions.AiServiceException.class);
    }
}
