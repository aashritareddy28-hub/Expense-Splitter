package com.expense.backend.repository;

import com.expense.backend.entity.Settlement;
import com.expense.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByUser(User user);
    List<Settlement> findByUserAndPaid(User user, Boolean paid);
}