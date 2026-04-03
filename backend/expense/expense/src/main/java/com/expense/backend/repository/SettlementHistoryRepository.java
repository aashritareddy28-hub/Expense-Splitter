package com.expense.backend.repository;

import com.expense.backend.entity.SettlementHistory;
import com.expense.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementHistoryRepository extends JpaRepository<SettlementHistory, Long> {
    List<SettlementHistory> findByUser(User user);
}
