import React, { useState, useEffect } from "react";
import ExpenseService from "../services/ExpenseService";

const PIE_COLORS = ["#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#E91E63", "#03A9F4", "#8BC34A", "#FF5722"];

function getArcPath(startAngle, endAngle, radius, cx, cy) {
    const x1 = cx + radius * Math.cos(startAngle);
    const y1 = cy + radius * Math.sin(startAngle);
    const x2 = cx + radius * Math.cos(endAngle);
    const y2 = cy + radius * Math.sin(endAngle);
    const largeArcFlag = endAngle - startAngle > Math.PI ? 1 : 0;

    return `M ${cx} ${cy} L ${x1} ${y1} A ${radius} ${radius} 0 ${largeArcFlag} 1 ${x2} ${y2} Z`;
}

function SpendingInsights() {
    const [insights, setInsights] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadInsights();
    }, []);

    const loadInsights = () => {
        setLoading(true);
        ExpenseService.getSpendingInsights()
            .then((response) => {
                setInsights(response.data);
                setLoading(false);
            })
            .catch((error) => {
                console.error("Error loading insights:", error);
                setError("Failed to load spending insights");
                setLoading(false);
            });
    };

    if (loading) {
        return (
            <div style={{ padding: "20px", textAlign: "center" }}>
                <p>Loading insights...</p>
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

    if (!insights) {
        return (
            <div style={{ padding: "20px", textAlign: "center" }}>
                <p>No insights available</p>
            </div>
        );
    }

    return (
        <div style={{ padding: "20px" }}>
            <h2>Spending Insights</h2>

            {insights.message && (
                <div style={{ padding: "10px", marginBottom: "20px", border: "1px solid #ffa726", borderRadius: "4px", backgroundColor: "#fff3e0", color: "#e65100" }}>
                    <p><strong>Note:</strong> {insights.message}</p>
                </div>
            )}

            {/* Summary Cards */}
            <div style={{ display: "flex", gap: "20px", marginBottom: "30px", flexWrap: "wrap" }}>
                <div style={{ flex: "1", minWidth: "200px", padding: "20px", border: "1px solid #ddd", borderRadius: "8px", backgroundColor: "#f9f9f9" }}>
                    <h3>This Month</h3>
                    <p style={{ fontSize: "24px", fontWeight: "bold", color: "#4CAF50" }}>
                        ₹{insights.currentMonthTotal?.toFixed(2) || "0.00"}
                    </p>
                </div>
                <div style={{ flex: "1", minWidth: "200px", padding: "20px", border: "1px solid #ddd", borderRadius: "8px", backgroundColor: "#f9f9f9" }}>
                    <h3>Last Month</h3>
                    <p style={{ fontSize: "24px", fontWeight: "bold", color: "#2196F3" }}>
                        ₹{insights.previousMonthTotal?.toFixed(2) || "0.00"}
                    </p>
                </div>
                <div style={{ flex: "1", minWidth: "200px", padding: "20px", border: "1px solid #ddd", borderRadius: "8px", backgroundColor: "#f9f9f9" }}>
                    <h3>All Time</h3>
                    <p style={{ fontSize: "24px", fontWeight: "bold", color: "#FF9800" }}>
                        ₹{insights.allTimeTotal?.toFixed(2) || "0.00"}
                    </p>
                </div>
            </div>

            {/* AI Insights */}
            {insights.insights && insights.insights.length > 0 && (
                <div style={{ marginBottom: "30px", padding: "20px", border: "1px solid #4CAF50", borderRadius: "8px", backgroundColor: "#f0f8f0" }}>
                    <h3>🤖 AI Insights</h3>
                    <ul>
                        {insights.insights.map((insight, index) => (
                            <li key={index} style={{ marginBottom: "8px" }}>{insight}</li>
                        ))}
                    </ul>
                </div>
            )}

            {/* Category Breakdown */}
            {insights.categoryBreakdown && insights.categoryBreakdown.length > 0 && (
                <div style={{ marginBottom: "30px" }}>
                    <h3>Category Breakdown</h3>

                    {/* Pie chart */}
                    <div style={{ display: "flex", flexDirection: "row", gap: "30px", flexWrap: "wrap", alignItems: "center" }}>
                        <svg width="280" height="280" viewBox="0 0 280 280" style={{ border: "1px solid #ddd", borderRadius: "50%", backgroundColor: "#fff" }}>
                            {(() => {
                                let startAngle = -Math.PI / 2;
                                const cx = 140;
                                const cy = 140;
                                const radius = 110;
                                return insights.categoryBreakdown.map((category, index) => {
                                    const sliceAngle = (category.percentage / 100) * 2 * Math.PI;
                                    const endAngle = startAngle + sliceAngle;
                                    const path = getArcPath(startAngle, endAngle, radius, cx, cy);
                                    const color = PIE_COLORS[index % PIE_COLORS.length];
                                    startAngle = endAngle;
                                    return <path key={category.category} d={path} fill={color} stroke="#ffffff" strokeWidth="1" />;
                                });
                            })()}
                        </svg>

                        <div style={{ flex: 1, minWidth: "220px" }}>
                            {insights.categoryBreakdown.map((category, index) => (
                                <div key={category.category} style={{ display: "flex", alignItems: "center", marginBottom: "10px" }}>
                                    <span style={{ width: "14px", height: "14px", borderRadius: "50%", backgroundColor: PIE_COLORS[index % PIE_COLORS.length], display: "inline-block", marginRight: "10px" }}></span>
                                    <strong style={{ width: "90px" }}>{category.category}</strong>
                                    <span style={{ marginLeft: "8px", color: "#444" }}>₹{category.total.toFixed(2)}</span>
                                    <span style={{ marginLeft: "auto", color: "#666" }}>{category.percentage.toFixed(1)}%</span>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Bar breakdown fallback summary */}
                    <div style={{ marginTop: "20px", display: "flex", flexDirection: "column", gap: "10px" }}>
                        {insights.categoryBreakdown.map((category, index) => (
                            <div key={index} style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                                <span style={{ width: "16px", height: "16px", borderRadius: "50%", backgroundColor: PIE_COLORS[index % PIE_COLORS.length], display: "inline-block" }}></span>
                                <div style={{ width: "140px", fontWeight: "bold" }}>{category.category}</div>
                                <div style={{ flex: "1", height: "12px", backgroundColor: "#e0e0e0", borderRadius: "10px", overflow: "hidden" }}>
                                    <div style={{ width: `${category.percentage}%`, height: "100%", backgroundColor: PIE_COLORS[index % PIE_COLORS.length], borderRadius: "10px" }}></div>
                                </div>
                                <div style={{ width: "90px", textAlign: "right" }}>
                                    ₹{category.total.toFixed(2)}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Monthly Trend */}
            {insights.monthlyTrend && insights.monthlyTrend.length > 0 && (
                <div>
                    <h3>6-Month Spending Trend</h3>
                    <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                        {insights.monthlyTrend.map((month, index) => (
                            <div key={index} style={{ padding: "10px", border: "1px solid #ddd", borderRadius: "4px", textAlign: "center", minWidth: "120px" }}>
                                <div style={{ fontSize: "12px", color: "#666" }}>{month.month}</div>
                                <div style={{ fontSize: "18px", fontWeight: "bold" }}>₹{month.total.toFixed(2)}</div>
                                <div style={{ fontSize: "12px", color: "#666" }}>{month.count} expenses</div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}

export default SpendingInsights;