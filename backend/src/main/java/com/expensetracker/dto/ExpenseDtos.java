package com.expensetracker.dto;

import com.expensetracker.entity.Expense;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExpenseDtos {

    public record ExpenseRequest(
            @NotNull @Positive BigDecimal amount,
            @NotBlank String description,
            @NotNull Long categoryId,
            @NotNull LocalDate date,
            @NotNull Expense.PaymentMethod paymentMethod,
            String notes
    ) {}

    public record ExpenseResponse(
            Long id,
            BigDecimal amount,
            String description,
            Long categoryId,
            String categoryName,
            LocalDate date,
            Expense.PaymentMethod paymentMethod,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static ExpenseResponse from(Expense e) {
            return new ExpenseResponse(
                    e.getId(), e.getAmount(), e.getDescription(),
                    e.getCategory().getId(), e.getCategory().getName(),
                    e.getDate(), e.getPaymentMethod(), e.getNotes(),
                    e.getCreatedAt(), e.getUpdatedAt()
            );
        }
    }
}
