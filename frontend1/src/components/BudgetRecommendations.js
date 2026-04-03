
/*eslint-disable react-hooks/exhaustive-deps */
import React, { useState, useEffect } from "react";
import ExpenseService from "../services/ExpenseService";

function BudgetRecommendations() {
    const [recommendations, setRecommendations] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadRecommendations();
    }, []);

    const loadRecommendations = () => {
        setLoading(true);
        ExpenseService.getBudgetRecommendations()
            .then((response) => {
                setRecommendations(response.data);
                setLoading(false);
            })
            .catch((error) => {
                console.error("Error loading recommendations:", error);
                setError("Failed to load budget recommendations");
                setLoading(false);
            });
    };

    if (loading) {
        return (
            <div style={{ padding: "20px", textAlign: "center" }}>
                <p>Loading budget recommendations...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div style={{ padding: "20px", textAlign: "center", color: "red" }}>
                <p>{error}</p>
            </div>
        );
    }

    if (
        !recommendations || 
        !recommendations.budgetSuggestions || 
        recommendations.budgetSuggestions.length === 0
    ){
        return (
            <div style={{ padding: "20px", textAlign: "center" }}>
                <p>{recommendations?.message || 
                "No budget recommendations available. Add more expenses to get personalized recommendations."}</p>
            </div>
        );
    }

    return (
        <div style={{ padding: "20px" }}>
            <h2>Budget Recommendations</h2>
            <p style={{ color: "#666", marginBottom: "20px" }}>
                Based on your spending patterns over the {recommendations.dataPeriod}
            </p>

            {/* Total Recommended Budget */}
            <div style={{ marginBottom: "30px", padding: "20px", border: "2px solid #4CAF50", borderRadius: "8px", backgroundColor: "#f0f8f0", textAlign: "center" }}>
                <h3>Total Recommended Monthly Budget</h3>
                <p style={{ fontSize: "36px", fontWeight: "bold", color: "#4CAF50", margin: "10px 0" }}>
                    ₹{recommendations?.totalRecommendedBudget
                    ? parseFloat(recommendations.totalRecommendedBudget).toFixed(2) 
                    : "0.00"}
                </p>
                <p style={{ color: "#666" }}>Suggested budget across all categories</p>
            </div>

            {/* Category Recommendations */}
            {recommendations.budgetSuggestions && recommendations.budgetSuggestions.length > 0 && (
                <div>
                    <h3>Category-Specific Budgets</h3>
                    <div style={{ display: "grid", gap: "15px" }}>
                        {recommendations.budgetSuggestions.map((suggestion, index) => (
                            <div key={index} style={{ padding: "20px", border: "1px solid #ddd", borderRadius: "8px", backgroundColor: "#fafafa" }}>
                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "10px" }}>
                                    <h4 style={{ margin: "0", color: "#333" }}>{suggestion.category}</h4>
                                    <div style={{ textAlign: "right" }}>
                                        <div style={{ fontSize: "20px", fontWeight: "bold", color: "#4CAF50" }}>
                                            ₹{(suggestion.suggestedBudget ?? 0).toFixed(2)}
                                        </div>
                                        <div style={{ fontSize: "12px", color: "#666" }}>recommended</div>
                                    </div>
                                </div>

                                <div style={{ display: "flex", gap: "20px", fontSize: "14px", color: "#666" }}>
                                    <div>
                                        <strong>Average:</strong> ₹{(suggestion.monthlyAverage ?? 0).toFixed(2)}/month
                                    </div>
                                    <div>
                                        <strong>Total Spent:</strong> ₹{(suggestion.totalSpent ?? 0).toFixed(2)} ({recommendations.dataPeriod})
                                    </div>
                                    <div>
                                        <strong>Transactions:</strong> {suggestion.expenseCount}
                                    </div>
                                </div>

                                <div style={{ marginTop: "10px", fontSize: "13px", color: "#666" }}>
                                    💡 <em>This budget includes a 10% buffer above your average spending</em>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Tips */}
            <div style={{ marginTop: "30px", padding: "20px", border: "1px solid #2196F3", borderRadius: "8px", backgroundColor: "#f0f8ff" }}>
                <h4>💡 Budget Tips</h4>
                <ul style={{ margin: "10px 0", paddingLeft: "20px" }}>
                    <li>Review your budget monthly and adjust based on actual spending</li>
                    <li>Set up automatic savings transfers when you receive income</li>
                    <li>Use the 50/30/20 rule: 50% needs, 30% wants, 20% savings/debt</li>
                    <li>Track expenses daily for better awareness</li>
                </ul>
            </div>
        </div>
    );
}

export default BudgetRecommendations;