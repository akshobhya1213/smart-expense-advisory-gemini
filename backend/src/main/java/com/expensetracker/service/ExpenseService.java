package com.expensetracker.service;

import com.expensetracker.cache.AnalyticsCacheService;
import com.expensetracker.dto.ExpenseDtos.ExpenseRequest;
import com.expensetracker.dto.ExpenseDtos.ExpenseResponse;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.Expense;
import com.expensetracker.exception.ApiExceptions;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.ExpenseRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final AnalyticsCacheService cacheService;

    @Transactional
    public ExpenseResponse create(Long userId, com.expensetracker.entity.User user, ExpenseRequest req) {
        Category category = resolveCategory(req.categoryId());

        Expense expense = Expense.builder()
                .amount(req.amount())
                .description(req.description())
                .category(category)
                .date(req.date())
                .paymentMethod(req.paymentMethod())
                .notes(req.notes())
                .user(user)
                .build();

        expense = expenseRepository.save(expense);
        cacheService.evict("analytics:" + userId);
        log.info("Expense created: id={} userId={} category={}", expense.getId(), userId, category.getName());
        return ExpenseResponse.from(expense);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> search(Long userId, String search, Long categoryId,
                                         LocalDate startDate, LocalDate endDate,
                                         Expense.PaymentMethod paymentMethod, Pageable pageable) {
        Specification<Expense> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (search != null && !search.isBlank()) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("notes")), "%" + search.toLowerCase() + "%")
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
            if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
            if (paymentMethod != null) predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return expenseRepository.findAll(spec, pageable).map(ExpenseResponse::from);
    }

    public ExpenseResponse getById(Long userId, Long id) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Expense not found"));
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse update(Long userId, Long id, ExpenseRequest req) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Expense not found"));

        Category category = resolveCategory(req.categoryId());

        expense.setAmount(req.amount());
        expense.setDescription(req.description());
        expense.setCategory(category);
        expense.setDate(req.date());
        expense.setPaymentMethod(req.paymentMethod());
        expense.setNotes(req.notes());

        expense = expenseRepository.save(expense);
        cacheService.evict("analytics:" + userId);
        log.info("Expense updated: id={} userId={} category={}", id, userId, category.getName());
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Expense not found"));
        expenseRepository.delete(expense);
        cacheService.evict("analytics:" + userId);
        log.info("Expense deleted: id={} userId={}", id, userId);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return categoryService.getMiscellaneousCategory();
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Category not found"));
    }
}
