package com.expense.backend.controller;

import com.expense.backend.entity.Expense;
import com.expense.backend.entity.User;
import com.expense.backend.entity.Settlement;
import com.expense.backend.repository.SettlementRepository;
import com.expense.backend.repository.ExpenseRepository;
import com.expense.backend.repository.UserRepository;
import com.expense.backend.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private SettlementRepository settlementRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public List<Expense> getAllExpenses() {
        User currentUser = getCurrentUser();
        return expenseRepository.findByUser(currentUser);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public Expense addExpense(@RequestBody Expense expense) {
        try {
            // Validate input
            if (expense.getDescription() == null || expense.getDescription().trim().isEmpty()) {
                throw new IllegalArgumentException("Description cannot be empty");
            }
            double amount = expense.getAmount();
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }
            if (expense.getPaidBy() == null || expense.getPaidBy().trim().isEmpty()) {
                throw new IllegalArgumentException("PaidBy cannot be empty");
            }
            if (expense.getParticipants() == null || expense.getParticipants().trim().isEmpty()) {
                throw new IllegalArgumentException("Participants cannot be empty");
            }
            if (expense.getCategory() == null || expense.getCategory().trim().isEmpty()) {
                throw new IllegalArgumentException("Category cannot be empty");
            }
            if (expense.getDate() == null) {
                expense.setDate(LocalDate.now()); // Default to today if not provided
            }

            // Associate expense with current user
            User currentUser = getCurrentUser();
            expense.setUser(currentUser);

            System.out.println("Received expense: " + expense);
            return expenseRepository.save(expense);
        } catch (Exception e) {
            System.err.println("Error saving expense: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/balance")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public Map<String, Object> calculateBalances() {
        User currentUser = getCurrentUser();
        List<Expense> expenses = expenseRepository.findByUser(currentUser);
        Map<String, Double> balances = new HashMap<>();

        // Calculate balances
        for (Expense expense : expenses) {
            String paidBy = expense.getPaidBy();
            double amount = expense.getAmount();
            String[] participantsArray = expense.getParticipants().split(",");
            List<String> participants = new ArrayList<>();
            for (String p : participantsArray) {
                if (!p.trim().isEmpty()) {
                    participants.add(p.trim());
                }
            }

            if (participants.isEmpty()) continue;

            double share = amount / participants.size();

            // Payer gets back the amount they paid
            balances.put(paidBy, balances.getOrDefault(paidBy, 0.0) + amount);

            // Everyone who participated owes their share
            for (String participant : participants) {
                balances.put(participant, balances.getOrDefault(participant, 0.0) - share);
            }
        }

        // Calculate Settlements (Who owes whom)
        List<String> settlements = new ArrayList<>();
        
        List<Map.Entry<String, Double>> debtors = new ArrayList<>();
        List<Map.Entry<String, Double>> creditors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            // using a small epsilon to avoid floating point precision issues
            if (entry.getValue() < -0.01) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            } else if (entry.getValue() > 0.01) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            }
        }

        // Sort debtors by most debt (most negative to least negative) -> sort ascending
        debtors.sort(Map.Entry.comparingByValue());
        // Sort creditors by most credit (highest to lowest) -> sort descending
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int i = 0; // debtor index
        int j = 0; // creditor index

        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<String, Double> debtor = debtors.get(i);
            Map.Entry<String, Double> creditor = creditors.get(j);

            double debt = Math.abs(debtor.getValue());
            double credit = creditor.getValue();

            double amountToSettle = Math.min(debt, credit);

            // Format amount to 2 decimal places
            settlements.add(String.format("%s \u2192 %s : \u20B9%.2f", debtor.getKey(), creditor.getKey(), amountToSettle));

            debtor.setValue(debtor.getValue() + amountToSettle);
            creditor.setValue(creditor.getValue() - amountToSettle);

            if (Math.abs(debtor.getValue()) < 0.01) {
                i++;
            }
            if (creditor.getValue() < 0.01) {
                j++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("balances", balances);
        result.put("settlements", settlements);
        return result;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public Expense getExpenseById(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Expense expense = expenseRepository.findById(id).orElse(null);
        if (expense != null && expense.getUser().getId().equals(currentUser.getId())) {
            return expense;
        }
        return null;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public Map<String, String> deleteExpense(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Expense expense = expenseRepository.findById(id).orElse(null);
            if (expense != null && expense.getUser().getId().equals(currentUser.getId())) {
                expenseRepository.deleteById(id);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Expense deleted successfully");
                return response;
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Expense not found or access denied");
                return response;
            }
        } catch (Exception e) {
            System.err.println("Error deleting expense: " + e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return response;
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public Expense updateExpense(@PathVariable Long id, @RequestBody Expense updatedExpense) {
        User currentUser = getCurrentUser();
        Expense existing = expenseRepository.findById(id).orElse(null);
        if (existing == null || !existing.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Expense not found or access denied");
        }

        if (updatedExpense.getDescription() == null || updatedExpense.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        if (updatedExpense.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (updatedExpense.getPaidBy() == null || updatedExpense.getPaidBy().trim().isEmpty()) {
            throw new IllegalArgumentException("PaidBy cannot be empty");
        }
        if (updatedExpense.getParticipants() == null || updatedExpense.getParticipants().trim().isEmpty()) {
            throw new IllegalArgumentException("Participants cannot be empty");
        }
        if (updatedExpense.getCategory() == null || updatedExpense.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty");
        }

        existing.setDescription(updatedExpense.getDescription());
        existing.setAmount(updatedExpense.getAmount());
        existing.setPaidBy(updatedExpense.getPaidBy());
        existing.setParticipants(updatedExpense.getParticipants());
        existing.setCategory(updatedExpense.getCategory());
        existing.setDate(updatedExpense.getDate() != null ? updatedExpense.getDate() : existing.getDate());

        return expenseRepository.save(existing);
    }

    @GetMapping("/analytics/insights")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public Map<String, Object> getSpendingInsights() {
        User currentUser = getCurrentUser();
        return analyticsService.getSpendingInsights(currentUser);
    }

    @GetMapping("/analytics/budgets")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public Map<String, Object> getBudgetRecommendations() {
        User currentUser = getCurrentUser();
        return analyticsService.getBudgetRecommendations(currentUser);
    }

    @GetMapping("/settlements")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public List<Settlement> getSettlements() {
        User currentUser = getCurrentUser();
        return settlementRepository.findByUserAndPaid(currentUser, false);
    }

    @PostMapping("/settlements/generate")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public List<Settlement> generateSettlements() {
        User currentUser = getCurrentUser();
        List<Expense> expenses = expenseRepository.findByUser(currentUser);

        // Delete existing unpaid settlements
        List<Settlement> existing = settlementRepository.findByUserAndPaid(currentUser, false);
        settlementRepository.deleteAll(existing);

        Map<String, Double> balances = new HashMap<>();

        // Calculate balances
        for (Expense expense : expenses) {
            String paidBy = expense.getPaidBy();
            double amount = expense.getAmount();
            String[] participantsArray = expense.getParticipants().split(",");
            List<String> participants = new ArrayList<>();
            for (String p : participantsArray) {
                if (!p.trim().isEmpty()) {
                    participants.add(p.trim());
                }
            }

            if (participants.isEmpty()) continue;

            double share = amount / participants.size();

            // Payer gets back the amount they paid
            balances.put(paidBy, balances.getOrDefault(paidBy, 0.0) + amount);

            // Everyone who participated owes their share
            for (String participant : participants) {
                balances.put(participant, balances.getOrDefault(participant, 0.0) - share);
            }
        }

        // Calculate Settlements
        List<Settlement> settlements = new ArrayList<>();
        
        List<Map.Entry<String, Double>> debtors = new ArrayList<>();
        List<Map.Entry<String, Double>> creditors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            if (entry.getValue() < -0.01) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            } else if (entry.getValue() > 0.01) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            }
        }

        debtors.sort(Map.Entry.comparingByValue());
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int i = 0;
        int j = 0;

        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<String, Double> debtor = debtors.get(i);
            Map.Entry<String, Double> creditor = creditors.get(j);

            double debt = Math.abs(debtor.getValue());
            double credit = creditor.getValue();

            double amountToSettle = Math.min(debt, credit);

            Settlement settlement = new Settlement(debtor.getKey(), creditor.getKey(), amountToSettle, LocalDate.now(), currentUser);
            settlements.add(settlementRepository.save(settlement));

            debtor.setValue(debtor.getValue() + amountToSettle);
            creditor.setValue(creditor.getValue() - amountToSettle);

            if (Math.abs(debtor.getValue()) < 0.01) {
                i++;
            }
            if (creditor.getValue() < 0.01) {
                j++;
            }
        }

        return settlements;
    }

    @PutMapping("/settlements/{id}/paid")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public Settlement markAsPaid(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Settlement settlement = settlementRepository.findById(id).orElse(null);
        if (settlement != null && settlement.getUser().getId().equals(currentUser.getId())) {
            settlement.setPaid(true);
            return settlementRepository.save(settlement);
        }
        return null;
    }
}
