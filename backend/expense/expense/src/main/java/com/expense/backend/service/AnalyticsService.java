package com.expense.backend.service;

import com.expense.backend.entity.Expense;
import com.expense.backend.entity.User;
import com.expense.backend.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public Map<String, Object> getSpendingInsights(User user) {
        Map<String, Object> insights = new HashMap<>();

        // Get all expenses for the user
        List<Expense> allExpenses = expenseRepository.findByUser(user);

        if (allExpenses.isEmpty()) {
            insights.put("message", "No expenses found. Start adding expenses to see insights!");
            return insights;
        }

        // Current month data
        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(now);
        List<Expense> currentMonthExpenses = expenseRepository.findByUserAndMonth(user, currentMonth.getYear(), currentMonth.getMonthValue());

        // Previous month data for comparison
        YearMonth previousMonth = currentMonth.minusMonths(1);
        List<Expense> previousMonthExpenses = expenseRepository.findByUserAndMonth(user, previousMonth.getYear(), previousMonth.getMonthValue());

        // Calculate totals
        double currentMonthTotal = currentMonthExpenses.stream().mapToDouble(Expense::getAmount).sum();
        double previousMonthTotal = previousMonthExpenses.stream().mapToDouble(Expense::getAmount).sum();
        double allTimeTotal = allExpenses.stream().mapToDouble(Expense::getAmount).sum();

        // Category breakdown
        Map<String, Double> categoryTotals = allExpenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory, Collectors.summingDouble(Expense::getAmount)));

        // Sort categories by total amount (descending)
        List<Map.Entry<String, Double>> sortedCategories = categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Monthly trend (last 6 months)
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            List<Expense> monthExpenses = expenseRepository.findByUserAndMonth(user, month.getYear(), month.getMonthValue());
            double monthTotal = monthExpenses.stream().mapToDouble(Expense::getAmount).sum();

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month.getMonth().name() + " " + month.getYear());
            monthData.put("total", monthTotal);
            monthData.put("count", monthExpenses.size());
            monthlyTrend.add(monthData);
        }

        // Insights
        List<String> insightsList = new ArrayList<>();

        // Compare with previous month
        if (previousMonthTotal > 0) {
            double changePercent = ((currentMonthTotal - previousMonthTotal) / previousMonthTotal) * 100;
            if (changePercent > 10) {
                insightsList.add(String.format("Your spending increased by %.1f%% compared to last month", changePercent));
            } else if (changePercent < -10) {
                insightsList.add(String.format("Great! Your spending decreased by %.1f%% compared to last month", Math.abs(changePercent)));
            }
        }

        // Top spending category
        if (!sortedCategories.isEmpty()) {
            String topCategory = sortedCategories.get(0).getKey();
            double topCategoryAmount = sortedCategories.get(0).getValue();
            double topCategoryPercent = (topCategoryAmount / allTimeTotal) * 100;
            insightsList.add(String.format("Your top spending category is '%s' (%.1f%% of total expenses)", topCategory, topCategoryPercent));
        }

        // Average expense
        double avgExpense = allTimeTotal / allExpenses.size();
        insightsList.add(String.format("Your average expense amount is ₹%.2f", avgExpense));

        // Build response
        insights.put("currentMonthTotal", currentMonthTotal);
        insights.put("previousMonthTotal", previousMonthTotal);
        insights.put("allTimeTotal", allTimeTotal);
        insights.put("totalExpenses", allExpenses.size());
        insights.put("categoryBreakdown", sortedCategories.stream()
                .map(entry -> {
                    Map<String, Object> cat = new HashMap<>();
                    cat.put("category", entry.getKey());
                    cat.put("total", entry.getValue());
                    cat.put("percentage", (entry.getValue() / allTimeTotal) * 100);
                    return cat;
                })
                .collect(Collectors.toList()));
        insights.put("monthlyTrend", monthlyTrend);
        insights.put("insights", insightsList);

        return insights;
    }

    public Map<String, Object> getBudgetRecommendations(User user) {
        Map<String, Object> recommendations = new HashMap<>();

        // Get expenses from the last 3 months for better recommendations
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        List<Expense> recentExpenses = expenseRepository.findByUserAndDateBetween(user, threeMonthsAgo, LocalDate.now());

        if (recentExpenses.isEmpty()) {
            recommendations.put("message", "Need at least 3 months of expense data for budget recommendations");
            return recommendations;
        }

        // Calculate average monthly spending by category
        Map<String, Double> monthlyCategoryAverages = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (Expense expense : recentExpenses) {
            String category = expense.getCategory();
            monthlyCategoryAverages.put(category, monthlyCategoryAverages.getOrDefault(category, 0.0) + expense.getAmount());
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
        }

        // Convert to monthly averages (divide by 3 months)
        List<Map<String, Object>> budgetSuggestions = monthlyCategoryAverages.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    double totalSpent = entry.getValue();
                    double monthlyAverage = totalSpent / 3.0;
                    int expenseCount = categoryCounts.get(category);

                    // Suggest budget as 110% of average (10% buffer)
                    double suggestedBudget = monthlyAverage * 1.1;

                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("category", category);
                    suggestion.put("monthlyAverage", monthlyAverage);
                    suggestion.put("suggestedBudget", suggestedBudget);
                    suggestion.put("expenseCount", expenseCount);
                    suggestion.put("totalSpent3Months", totalSpent);

                    return suggestion;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("monthlyAverage"), (Double) a.get("monthlyAverage")))
                .collect(Collectors.toList());

        // Calculate total recommended budget
        double totalRecommendedBudget = budgetSuggestions.stream()
                .mapToDouble(s -> (Double) s.get("suggestedBudget"))
                .sum();

        recommendations.put("budgetSuggestions", budgetSuggestions);
        recommendations.put("totalRecommendedBudget", totalRecommendedBudget);
        recommendations.put("dataPeriod", "Last 3 months");

        return recommendations;
    }
}