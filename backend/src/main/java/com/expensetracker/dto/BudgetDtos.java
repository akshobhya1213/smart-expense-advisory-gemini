package com.expensetracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class BudgetDtos {

    public record BudgetRequest(
            Long categoryId, // null = overall monthly budget
            @NotNull @Min(2000) @Max(3000) Integer year,
            @NotNull @Min(1) @Max(12) Integer month,
            @NotNull @Positive BigDecimal limitAmount
    ) {}

    public record BudgetResponse(
            Long id,
            Long categoryId,
            String categoryName, // null for overall budget
            int year,
            int month,
            BigDecimal limitAmount,
            BigDecimal spent,
            BigDecimal remaining,
            double utilizationPercent,
            String status // NORMAL, APPROACHING, EXCEEDED
    ) {}
}
