package com.expensetracker.service;

import com.expensetracker.cache.AnalyticsCacheService;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.Expense;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ApiExceptions;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock ExpenseRepository expenseRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock AnalyticsCacheService cacheService;
    @InjectMocks ExpenseService expenseService;

    /**
     * This is the core "User A cannot access User B's expense" guarantee.
     * The repository lookup is scoped to (id AND userId), so even a correct
     * expense id belonging to someone else returns empty -> 404, not the data.
     */
    @Test
    void getById_throwsNotFound_whenExpenseBelongsToAnotherUser() {
        when(expenseRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getById(1L, 99L))
                .isInstanceOf(ApiExceptions.ResourceNotFoundException.class);
    }

    @Test
    void getById_returnsExpense_whenOwnedByUser() {
        Category category = Category.builder().id(1L).name("Food").build();
        User user = User.builder().id(1L).email("matrixx@example.com").build();
        Expense expense = Expense.builder()
                .id(5L).amount(BigDecimal.valueOf(250)).description("Lunch")
                .category(category).date(LocalDate.now())
                .paymentMethod(Expense.PaymentMethod.UPI).user(user)
                .build();
        when(expenseRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(expense));

        var response = expenseService.getById(1L, 5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.categoryName()).isEqualTo("Food");
    }

    @Test
    void delete_evictsAnalyticsCache_afterDeletion() {
        Category category = Category.builder().id(1L).name("Food").build();
        Expense expense = Expense.builder().id(5L).category(category)
                .amount(BigDecimal.TEN).date(LocalDate.now())
                .paymentMethod(Expense.PaymentMethod.CASH).build();
        when(expenseRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(expense));

        expenseService.delete(1L, 5L);

        verify(expenseRepository).delete(expense);
        verify(cacheService).evict("analytics:1");
    }
}
