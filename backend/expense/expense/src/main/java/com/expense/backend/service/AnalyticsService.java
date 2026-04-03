package com.expense.backend.service;

import com.expense.backend.entity.Expense;
import com.expense.backend.entity.ExpenseHistory;
import com.expense.backend.entity.SettlementHistory;
import com.expense.backend.entity.User;
import com.expense.backend.repository.ExpenseHistoryRepository;
import com.expense.backend.repository.ExpenseRepository;
import com.expense.backend.repository.SettlementHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private SettlementHistoryRepository settlementHistoryRepository;

    @Autowired
    private ExpenseHistoryRepository expenseHistoryRepository;

    public Map<String, Object> getSpendingInsights(User user) {
        Map<String, Object> insights = new HashMap<>();

        // Get all current expenses for the user
        List<Expense> currentExpenses = expenseRepository.findByUser(user);

        // Get all historical expenses for the user
        List<ExpenseHistory> historicalExpenses = expenseHistoryRepository.findByUser(user);

        // Combine current and historical expenses for analysis
        List<Expense> allExpenses = new ArrayList<>(currentExpenses);
        // Convert historical expenses to expense format for analysis
        for (ExpenseHistory hist : historicalExpenses) {
            // Create a temporary expense object for analysis
            Expense tempExpense = new Expense();
            tempExpense.setId(hist.getId());
            tempExpense.setDescription(hist.getDescription());
            tempExpense.setAmount(hist.getAmount());
            tempExpense.setPaidBy(hist.getPaidBy());
            tempExpense.setParticipants(hist.getParticipants());
            tempExpense.setCategory(hist.getCategory());
            tempExpense.setDate(hist.getDate());
            tempExpense.setUser(hist.getUser());
            allExpenses.add(tempExpense);
        }

        // Get settlement history for fallback analytics when no expenses exist at all
        List<SettlementHistory> settlementHistories = settlementHistoryRepository.findByUser(user);

        if (allExpenses.isEmpty()) {
            if (settlementHistories.isEmpty()) {
                // Provide default insights for new users
                insights.put("currentMonthTotal", 0.0);
                insights.put("previousMonthTotal", 0.0);
                insights.put("allTimeTotal", 0.0);
                insights.put("totalExpenses", 0);
                insights.put("categoryBreakdown", Collections.emptyList());
                insights.put("monthlyTrend", Collections.emptyList());

                List<String> insightsList = new ArrayList<>();
                insightsList.add("Welcome to Expense Splitter! Start by adding your first expense to see personalized insights.");
                insightsList.add("Track shared expenses with friends and family to split costs fairly.");
                insightsList.add("Use categories to organize your spending and identify patterns.");
                insightsList.add("Regular settlements help maintain balanced accounts with your group.");
                insights.put("insights", insightsList);
                insights.put("message", "No expense data available yet. Add some expenses to unlock detailed insights!");

                return insights;
            }

            double fallbackSettledAmount = calculateTotalSettledAmount(settlementHistories);
            int fallbackFinalizations = settlementHistories.size();

            insights.put("currentMonthTotal", 0.0);
            insights.put("previousMonthTotal", 0.0);
            insights.put("allTimeTotal", fallbackSettledAmount);
            insights.put("totalExpenses", 0);
            insights.put("categoryBreakdown", Collections.emptyList());
            insights.put("monthlyTrend", Collections.emptyList());

            List<String> insightsList = new ArrayList<>();
            insightsList.add("No active expenses available; insights based on your settlement history.");
            insightsList.add(String.format("You have finalized settlements %d time(s), settling a total of ₹%.2f", fallbackFinalizations, fallbackSettledAmount));
            if (fallbackFinalizations > 0) {
                double avgSettlement = fallbackSettledAmount / fallbackFinalizations;
                insightsList.add(String.format("Your average settlement amount is ₹%.2f", avgSettlement));
            }
            insightsList.add("Add new expenses to see current spending patterns and trends.");
            insights.put("insights", insightsList);
            insights.put("message", "Historical insights from your settlement data.");

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

        // Monthly trend (last 6 months) - includes both current and historical expenses
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            List<Expense> monthExpenses = expenseRepository.findByUserAndMonth(user, month.getYear(), month.getMonthValue());

            // Add historical expenses for this month
            List<ExpenseHistory> monthHistoricalExpenses = historicalExpenses.stream()
                .filter(h -> h.getDate() != null &&
                           YearMonth.from(h.getDate()).equals(month))
                .collect(Collectors.toList());

            // Convert historical to expense format and add to month expenses
            for (ExpenseHistory hist : monthHistoricalExpenses) {
                Expense tempExpense = new Expense();
                tempExpense.setId(hist.getId());
                tempExpense.setDescription(hist.getDescription());
                tempExpense.setAmount(hist.getAmount());
                tempExpense.setPaidBy(hist.getPaidBy());
                tempExpense.setParticipants(hist.getParticipants());
                tempExpense.setCategory(hist.getCategory());
                tempExpense.setDate(hist.getDate());
                tempExpense.setUser(hist.getUser());
                monthExpenses.add(tempExpense);
            }

            double monthTotal = monthExpenses.stream().mapToDouble(Expense::getAmount).sum();

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month.getMonth().name() + " " + month.getYear());
            monthData.put("total", monthTotal);
            monthData.put("count", monthExpenses.size());
            monthlyTrend.add(monthData);
        }

        // Use existing settlement history for additional insights
        int totalFinalizations = settlementHistories.size();
        double totalSettledAmount = calculateTotalSettledAmount(settlementHistories);

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

        // Settlement insights
        if (totalFinalizations > 0) {
            insightsList.add(String.format("You have finalized settlements %d time(s), settling a total of ₹%.2f", totalFinalizations, totalSettledAmount));
            double avgSettlementAmount = totalSettledAmount / totalFinalizations;
            insightsList.add(String.format("Your average settlement amount per finalization is ₹%.2f", avgSettlementAmount));
        }

        // Monthly settlement frequency
        if (totalFinalizations > 0 && !allExpenses.isEmpty()) {
            LocalDate firstExpense = allExpenses.stream().map(Expense::getDate).min(LocalDate::compareTo).orElse(LocalDate.now());
            LocalDate lastExpense = allExpenses.stream().map(Expense::getDate).max(LocalDate::compareTo).orElse(LocalDate.now());
            long totalMonths = Math.max(1, java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(firstExpense), YearMonth.from(lastExpense)) + 1);
            double settlementsPerMonth = (double) totalFinalizations / totalMonths;
            insightsList.add(String.format("You finalize settlements approximately %.1f time(s) per month", settlementsPerMonth));
        }

        // Historical spending patterns
        if (allExpenses.size() > 1) {
            // Calculate spending consistency
            List<Double> monthlyTotals = monthlyTrend.stream()
                .map(m -> (Double) m.get("total"))
                .filter(total -> total > 0)
                .collect(Collectors.toList());

            if (monthlyTotals.size() > 1) {
                double avgMonthly = monthlyTotals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double variance = monthlyTotals.stream()
                    .mapToDouble(total -> Math.pow(total - avgMonthly, 2))
                    .average().orElse(0.0);
                double stdDev = Math.sqrt(variance);

                if (stdDev < avgMonthly * 0.2) {
                    insightsList.add("Your monthly spending is quite consistent over time");
                } else if (stdDev > avgMonthly * 0.5) {
                    insightsList.add("Your monthly spending varies significantly - consider budgeting for irregular expenses");
                }
            }

            // Longest period without expenses
            allExpenses.sort(Comparator.comparing(Expense::getDate));
            long maxGapDays = 0;
            for (int i = 1; i < allExpenses.size(); i++) {
                long gap = java.time.temporal.ChronoUnit.DAYS.between(
                    allExpenses.get(i-1).getDate(), allExpenses.get(i).getDate());
                maxGapDays = Math.max(maxGapDays, gap);
            }
            if (maxGapDays > 30) {
                insightsList.add(String.format("Your longest period without expenses was %d days", maxGapDays));
            }
        }

        // Category diversity
        if (sortedCategories.size() > 1) {
            insightsList.add(String.format("You spend across %d different categories", sortedCategories.size()));
        }

        // Recent activity
        LocalDate weekAgo = LocalDate.now().minusWeeks(1);
        long recentExpenses = allExpenses.stream()
            .filter(e -> e.getDate().isAfter(weekAgo))
            .count();
        if (recentExpenses == 0) {
            insightsList.add("No expenses in the last week - time to add some activity!");
        } else {
            insightsList.add(String.format("You added %d expense(s) in the last week", recentExpenses));
        }

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
        insights.put("includesHistoricalData", !historicalExpenses.isEmpty());

        return insights;
    }

    public Map<String, Object> getBudgetRecommendations(User user) {
        Map<String, Object> recommendations = new HashMap<>();

        // Get all current expenses
        List<Expense> currentExpenses = expenseRepository.findByUser(user);

        // Get all historical expenses
        List<ExpenseHistory> historicalExpenses = expenseHistoryRepository.findByUser(user);

        // Combine current and historical expenses for analysis
        List<Expense> allExpenses = new ArrayList<>(currentExpenses);
        // Convert historical expenses to expense format for analysis
        for (ExpenseHistory hist : historicalExpenses) {
            Expense tempExpense = new Expense();
            tempExpense.setId(hist.getId());
            tempExpense.setDescription(hist.getDescription());
            tempExpense.setAmount(hist.getAmount());
            tempExpense.setPaidBy(hist.getPaidBy());
            tempExpense.setParticipants(hist.getParticipants());
            tempExpense.setCategory(hist.getCategory());
            tempExpense.setDate(hist.getDate());
            tempExpense.setUser(hist.getUser());
            allExpenses.add(tempExpense);
        }

        if (allExpenses.isEmpty()) {
            List<SettlementHistory> settlementHistories = settlementHistoryRepository.findByUser(user);
            if (settlementHistories.isEmpty()) {
                recommendations.put("message", "No expenses found. Add some expenses to get budget recommendations!");
                return recommendations;
            }

            double totalSettledAmount = calculateTotalSettledAmount(settlementHistories);
            long monthsSinceFirstFinalization = Math.max(1, java.time.temporal.ChronoUnit.MONTHS.between(
                    YearMonth.from(settlementHistories.stream().map(SettlementHistory::getCreatedAt).min(LocalDateTime::compareTo).orElse(LocalDateTime.now()).toLocalDate()),
                    YearMonth.from(LocalDate.now())) + 1);

            double suggestedBudget = (totalSettledAmount / monthsSinceFirstFinalization) * 1.1;

            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("category", "History-based estimate");
            suggestion.put("monthlyAverage", totalSettledAmount / monthsSinceFirstFinalization);
            suggestion.put("suggestedBudget", suggestedBudget);
            suggestion.put("expenseCount", 0);
            suggestion.put("totalSpent", totalSettledAmount);
            suggestion.put("monthsAnalyzed", monthsSinceFirstFinalization);
            suggestion.put("settlementAdjusted", true);

            recommendations.put("budgetSuggestions", Collections.singletonList(suggestion));
            recommendations.put("totalRecommendedBudget", suggestedBudget);
            recommendations.put("dataPeriod", "Based on settlement history");
            recommendations.put("monthsAnalyzed", monthsSinceFirstFinalization);
            recommendations.put("avgMonthlySettlements", totalSettledAmount / monthsSinceFirstFinalization);
            recommendations.put("settlementHistoryCount", settlementHistories.size());
            recommendations.put("message", "No current expense entries; using history to estimate budgets.");

            return recommendations;
        }

        // For budget analysis, focus on recent data (last 6 months) to provide relevant recommendations
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);

        // Get current expenses from the last 6 months
        List<Expense> recentCurrentExpenses = expenseRepository.findByUserAndDateBetween(user, sixMonthsAgo, LocalDate.now());

        // Add historical expenses from the last 6 months
        List<ExpenseHistory> recentHistoricalExpenses = historicalExpenses.stream()
            .filter(h -> h.getDate() != null && !h.getDate().isBefore(sixMonthsAgo))
            .collect(Collectors.toList());

        // Combine recent expenses for budget analysis
        List<Expense> expensesForAnalysis = new ArrayList<>(recentCurrentExpenses);
        for (ExpenseHistory hist : recentHistoricalExpenses) {
            Expense tempExpense = new Expense();
            tempExpense.setId(hist.getId());
            tempExpense.setDescription(hist.getDescription());
            tempExpense.setAmount(hist.getAmount());
            tempExpense.setPaidBy(hist.getPaidBy());
            tempExpense.setParticipants(hist.getParticipants());
            tempExpense.setCategory(hist.getCategory());
            tempExpense.setDate(hist.getDate());
            tempExpense.setUser(hist.getUser());
            expensesForAnalysis.add(tempExpense);
        }

        if (expensesForAnalysis.isEmpty()) {
            recommendations.put("message", "No expenses found in the last 6 months for budget analysis");
            return recommendations;
        }

        // Calculate date range for recent expenses
        LocalDate earliestRecentDate = expensesForAnalysis.stream()
                .map(Expense::getDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now().minusMonths(1));

        LocalDate latestRecentDate = expensesForAnalysis.stream()
                .map(Expense::getDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        // Calculate available months (at least 1)
        long monthsBetween = Math.max(1, java.time.temporal.ChronoUnit.MONTHS.between(
                YearMonth.from(earliestRecentDate), YearMonth.from(latestRecentDate)) + 1);

        // Calculate average monthly spending by category
        Map<String, Double> monthlyCategoryAverages = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (Expense expense : expensesForAnalysis) {
            String category = expense.getCategory();
            monthlyCategoryAverages.put(category, monthlyCategoryAverages.getOrDefault(category, 0.0) + expense.getAmount());
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
        }

        // Get settlement history to factor in settlement patterns
        List<SettlementHistory> settlementHistories = settlementHistoryRepository.findByUser(user);
        double settledAmount = 0.0;
        long settlementsMonthsSpan = 1;

        if (!settlementHistories.isEmpty()) {
            // Calculate average monthly settlement amount
            double totalSettledAmount = 0.0;
            LocalDateTime earliestSettlement = settlementHistories.stream()
                    .map(SettlementHistory::getCreatedAt)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            LocalDateTime latestSettlement = settlementHistories.stream()
                    .map(SettlementHistory::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            // Calculate months span (inclusive of both start and end months)
            settlementsMonthsSpan = Math.max(1, java.time.temporal.ChronoUnit.MONTHS.between(
                    YearMonth.from(earliestSettlement.toLocalDate()),
                    YearMonth.from(latestSettlement.toLocalDate())) + 1);

            // Parse settlement amounts from history - look specifically for "Paid Settlements:" section
            for (SettlementHistory history : settlementHistories) {
                String summary = history.getSummary();
                String[] lines = summary.split("\n");
                boolean inSettlementsSection = false;

                for (String line : lines) {
                    line = line.trim();
                    if (line.equals("Paid Settlements:")) {
                        inSettlementsSection = true;
                        continue;
                    }
                    if (inSettlementsSection && line.contains("->") && line.contains(":")) {
                        try {
                            // Extract amount after the last colon
                            int lastColonIndex = line.lastIndexOf(":");
                            if (lastColonIndex > 0) {
                                String amountStr = line.substring(lastColonIndex + 1).trim();
                                // Remove any non-numeric characters except decimal point
                                amountStr = amountStr.replaceAll("[^0-9.]", "");
                                if (!amountStr.isEmpty()) {
                                    totalSettledAmount += Double.parseDouble(amountStr);
                                }
                            }
                        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                            // Skip invalid lines
                        }
                    }
                    // Stop parsing when we hit another section
                    if (inSettlementsSection && line.startsWith("Spending Insights:")) {
                        break;
                    }
                }
            }

            settledAmount = totalSettledAmount;
        }

        final double avgMonthlySettlements = settledAmount / settlementsMonthsSpan;

        // Calculate total monthly spending to compare with settlements
        double totalMonthlySpending = monthlyCategoryAverages.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum() / monthsBetween;

        // Adjust budget suggestions based on settlement patterns
        List<Map<String, Object>> budgetSuggestions = monthlyCategoryAverages.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    double totalSpent = entry.getValue();
                    double monthlyAverage = totalSpent / monthsBetween;
                    int expenseCount = categoryCounts.get(category);

                    // Suggest budget as 110% of average (10% buffer)
                    double suggestedBudget = monthlyAverage * 1.1;

                    // If user has high settlement frequency (settlements > 50% of monthly spending), suggest more conservative budgets
                    if (avgMonthlySettlements > totalMonthlySpending * 0.5) {
                        suggestedBudget = monthlyAverage * 1.05; // Reduce buffer for frequent settlers
                    }

                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("category", category);
                    suggestion.put("monthlyAverage", monthlyAverage);
                    suggestion.put("suggestedBudget", suggestedBudget);
                    suggestion.put("expenseCount", expenseCount);
                    suggestion.put("totalSpent", totalSpent);
                    suggestion.put("monthsAnalyzed", monthsBetween);
                    suggestion.put("settlementAdjusted", avgMonthlySettlements > totalMonthlySpending * 0.5);

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
        recommendations.put("dataPeriod", "Based on " + monthsBetween + " months of data");
        recommendations.put("monthsAnalyzed", monthsBetween);
        recommendations.put("avgMonthlySettlements", avgMonthlySettlements);
        recommendations.put("settlementHistoryCount", settlementHistories.size());
        recommendations.put("includesHistoricalData", !historicalExpenses.isEmpty());

        return recommendations;
    }

    private double calculateTotalSettledAmount(List<SettlementHistory> histories) {
        double totalSettledAmount = 0.0;

        for (SettlementHistory history : histories) {
            String summary = history.getSummary();
            if (summary == null || summary.isEmpty()) {
                continue;
            }

            String[] lines = summary.split("\n");
            boolean inSettlementsSection = false;

            for (String line : lines) {
                line = line.trim();
                if (line.equals("Paid Settlements:")) {
                    inSettlementsSection = true;
                    continue;
                }
                if (inSettlementsSection && line.contains("->") && line.contains(":")) {
                    try {
                        int lastColonIndex = line.lastIndexOf(":");
                        if (lastColonIndex > 0) {
                            String amountStr = line.substring(lastColonIndex + 1).trim();
                            amountStr = amountStr.replaceAll("[^0-9.]", "");
                            if (!amountStr.isEmpty()) {
                                totalSettledAmount += Double.parseDouble(amountStr);
                            }
                        }
                    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                        // Skip invalid line
                    }
                }
                if (inSettlementsSection && (line.startsWith("Spending Insights:") || line.startsWith("Budget Recommendations:"))) {
                    break;
                }
            }
        }

        return totalSettledAmount;
    }
}