package com.expensetracker.service;

import com.expensetracker.cache.AnalyticsCacheService;
import com.expensetracker.entity.Budget;
import com.expensetracker.entity.Category;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock BudgetRepository budgetRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock AnalyticsCacheService cacheService;
    @InjectMocks BudgetService budgetService;

    @Test
    void toResponse_statusIsNormal_whenUtilizationBelow80Percent() {
        Category category = Category.builder().id(1L).name("Food").build();
        Budget budget = Budget.builder().id(1L).category(category).year(2026).month(9)
                .limitAmount(BigDecimal.valueOf(7000)).build();
        when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(1L, 1L,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(BigDecimal.valueOf(3000));

        var response = budgetService.toResponse(1L, budget);

        assertThat(response.status()).isEqualTo("NORMAL");
        assertThat(response.utilizationPercent()).isCloseTo(42.86, org.assertj.core.data.Offset.offset(0.01));
        assertThat(response.remaining()).isEqualByComparingTo("4000");
    }

    @Test
    void toResponse_statusIsApproaching_whenUtilizationBetween80And100Percent() {
        Category category = Category.builder().id(1L).name("Food").build();
        Budget budget = Budget.builder().id(1L).category(category).year(2026).month(9)
                .limitAmount(BigDecimal.valueOf(7000)).build();
        when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(1L, 1L,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(BigDecimal.valueOf(6000));

        var response = budgetService.toResponse(1L, budget);

        assertThat(response.status()).isEqualTo("APPROACHING");
    }

    @Test
    void toResponse_statusIsExceeded_whenSpentPastLimit() {
        Category category = Category.builder().id(1L).name("Food").build();
        Budget budget = Budget.builder().id(1L).category(category).year(2026).month(9)
                .limitAmount(BigDecimal.valueOf(7000)).build();
        when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(1L, 1L,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(BigDecimal.valueOf(8450));

        var response = budgetService.toResponse(1L, budget);

        assertThat(response.status()).isEqualTo("EXCEEDED");
        assertThat(response.remaining()).isEqualByComparingTo("-1450");
    }

    @Test
    void toResponse_usesOverallExpenseSum_whenBudgetHasNoCategory() {
        Budget budget = Budget.builder().id(2L).category(null).year(2026).month(9)
                .limitAmount(BigDecimal.valueOf(30000)).build();
        when(expenseRepository.sumAmountByUserAndDateRange(1L,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(BigDecimal.valueOf(15000));

        var response = budgetService.toResponse(1L, budget);

        assertThat(response.categoryName()).isNull();
        assertThat(response.utilizationPercent()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(0.01));
    }
}
