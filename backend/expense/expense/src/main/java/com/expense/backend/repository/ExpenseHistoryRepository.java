package com.expense.backend.repository;

import com.expense.backend.entity.ExpenseHistory;
import com.expense.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseHistoryRepository extends JpaRepository<ExpenseHistory, Long> {
    List<ExpenseHistory> findByUser(User user);
}