package com.expensetracker.controller;

import com.expensetracker.dto.BudgetDtos.BudgetRequest;
import com.expensetracker.dto.BudgetDtos.BudgetResponse;
import com.expensetracker.security.CurrentUserResolver;
import com.expensetracker.service.BudgetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final CurrentUserResolver currentUser;

    @PostMapping
    public ResponseEntity<BudgetResponse> createOrUpdate(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.createOrUpdate(currentUser.userId(), currentUser.entity(), request));
    }

    @GetMapping
    public List<BudgetResponse> getForMonth(@RequestParam int year, @RequestParam int month) {
        return budgetService.getForMonth(currentUser.userId(), year, month);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
