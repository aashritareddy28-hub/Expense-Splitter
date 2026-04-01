package com.expense.backend.entity;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String paidBy;

    @Column(nullable = false)
    private String participants; // comma-separated strings

    @Column(nullable = false)
    private String category; // Food, Transportation, Entertainment, etc.

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Expense() {
    }

    public Expense(String description, Double amount, String paidBy, String participants, String category, LocalDate date, User user) {
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.participants = participants;
        this.category = category;
        this.date = date;
        this.user = user;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount != null ? amount : 0.0;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(String paidBy) {
        this.paidBy = paidBy;
    }

    public String getParticipants() {
        return participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", paidBy='" + paidBy + '\'' +
                ", participants='" + participants + '\'' +
                ", category='" + category + '\'' +
                ", date=" + date +
                '}';
    }
}
