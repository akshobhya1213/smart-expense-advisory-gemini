package com.expensetracker.service;

import com.expensetracker.cache.AnalyticsCacheService;
import com.expensetracker.dto.BudgetDtos.BudgetRequest;
import com.expensetracker.dto.BudgetDtos.BudgetResponse;
import com.expensetracker.entity.Budget;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ApiExceptions;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final AnalyticsCacheService cacheService;

    @Transactional
    public BudgetResponse createOrUpdate(Long userId, User user, BudgetRequest req) {
        Category category = req.categoryId() != null
                ? categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Category not found"))
                : null;

        Budget budget = (category == null
                ? budgetRepository.findByUserIdAndYearAndMonthAndCategoryIsNull(userId, req.year(), req.month())
                : budgetRepository.findByUserIdAndYearAndMonthAndCategoryId(userId, req.year(), req.month(), category.getId()))
                .orElse(Budget.builder().user(user).category(category).year(req.year()).month(req.month()).build());

        budget.setLimitAmount(req.limitAmount());
        budget = budgetRepository.save(budget);
        cacheService.evict("analytics:" + userId);
        return toResponse(userId, budget);
    }

    public List<BudgetResponse> getForMonth(Long userId, int year, int month) {
        return budgetRepository.findByUserIdAndYearAndMonth(userId, year, month).stream()
                .map(b -> toResponse(userId, b))
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
        cacheService.evict("analytics:" + userId);
    }

    BudgetResponse toResponse(Long userId, Budget budget) {
        YearMonth ym = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal spent = budget.getCategory() == null
                ? expenseRepository.sumAmountByUserAndDateRange(userId, start, end)
                : expenseRepository.sumAmountByUserAndCategoryAndDateRange(userId, budget.getCategory().getId(), start, end);

        BigDecimal limit = budget.getLimitAmount();
        BigDecimal remaining = limit.subtract(spent);
        double utilization = limit.compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : spent.divide(limit, 4, RoundingMode.HALF_UP).doubleValue() * 100;

        String status = utilization >= 100 ? "EXCEEDED" : utilization >= 80 ? "APPROACHING" : "NORMAL";

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory() != null ? budget.getCategory().getId() : null,
                budget.getCategory() != null ? budget.getCategory().getName() : null,
                budget.getYear(), budget.getMonth(),
                limit, spent, remaining, utilization, status
        );
    }
}
