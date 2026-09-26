package com.expensetracker.repository;

import com.expensetracker.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    Page<Expense> findAll(org.springframework.data.jpa.domain.Specification<Expense> spec, Pageable pageable);

    @Query("select coalesce(sum(e.amount),0) from Expense e where e.user.id = :userId " +
            "and e.date between :start and :end")
    BigDecimal sumAmountByUserAndDateRange(@Param("userId") Long userId,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end);

    @Query("select coalesce(sum(e.amount),0) from Expense e where e.user.id = :userId " +
            "and e.category.id = :categoryId and e.date between :start and :end")
    BigDecimal sumAmountByUserAndCategoryAndDateRange(@Param("userId") Long userId,
                                                       @Param("categoryId") Long categoryId,
                                                       @Param("start") LocalDate start,
                                                       @Param("end") LocalDate end);

    @Query("select e.category.id, e.category.name, coalesce(sum(e.amount),0), count(e) " +
            "from Expense e where e.user.id = :userId and e.date between :start and :end " +
            "group by e.category.id, e.category.name")
    List<Object[]> categoryTotals(@Param("userId") Long userId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    @Query("select e.date, coalesce(sum(e.amount),0) from Expense e " +
            "where e.user.id = :userId and e.date between :start and :end " +
            "group by e.date order by e.date")
    List<Object[]> dailyTotals(@Param("userId") Long userId,
                                @Param("start") LocalDate start,
                                @Param("end") LocalDate end);

    long countByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);
}
