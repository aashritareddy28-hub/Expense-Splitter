package com.expense.backend.controller;

import com.expense.backend.entity.Person;
import com.expense.backend.entity.User;
import com.expense.backend.repository.PersonRepository;
import com.expense.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PersonController {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public List<Person> getAllPersons() {
        User currentUser = getCurrentUser();
        return personRepository.findByUser(currentUser);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public Person addPerson(@RequestBody Person person) {
        User currentUser = getCurrentUser();
        person.setUser(currentUser);
        return personRepository.save(person);
    }
}
