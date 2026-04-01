package com.expense.backend.repository;

import com.expense.backend.entity.Person;
import com.expense.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    List<Person> findByUser(User user);
}
