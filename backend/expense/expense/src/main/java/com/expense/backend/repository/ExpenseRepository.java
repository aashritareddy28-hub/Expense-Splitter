package com.expense.backend.repository;

import com.expense.backend.entity.Expense;
import com.expense.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUser(User user);

    List<Expense> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);

    @Query("SELECT e FROM Expense e WHERE e.user = :user AND YEAR(e.date) = :year AND MONTH(e.date) = :month")
    List<Expense> findByUserAndMonth(@Param("user") User user, @Param("year") int year, @Param("month") int month);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user = :user GROUP BY e.category")
    List<Object[]> getTotalByCategory(@Param("user") User user);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user = :user AND e.date BETWEEN :startDate AND :endDate GROUP BY e.category")
    List<Object[]> getTotalByCategoryInDateRange(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user")
    Double getTotalAmount(@Param("user") User user);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user AND e.date BETWEEN :startDate AND :endDate")
    Double getTotalAmountInDateRange(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
