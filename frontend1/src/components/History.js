import React, { useState, useEffect } from "react";
import ExpenseService from "../services/ExpenseService";

function History({ refreshTrigger }) {
    const [expenses, setExpenses] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadHistory();
    }, [refreshTrigger]);

    const loadHistory = () => {
        ExpenseService.getHistory()
            .then((response) => {
                console.log("History response:", response.data);
                setExpenses(response.data);
                setLoading(false);
            })
            .catch((error) => {
                console.error("Error fetching history:", error);
                console.error("Error response:", error.response);
                setLoading(false);
            });
    };

    if (loading) {
        return <div className="loading">Loading expense history...</div>;
    }

    return (
        <div style={{ padding: "20px" }}>
            <h2>📜 Expense History</h2>
            {expenses.length === 0 ? (
                <div style={{ textAlign: "center", padding: "40px", color: "#666" }}>
                    <h3>No expenses found</h3>
                    <p>Add some expenses to see your history.</p>
                </div>
            ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "15px" }}>
                    {expenses.map((expense) => (
                        <div key={`${expense.isHistorical ? 'historical' : 'current'}-${expense.id}`} style={{
                            border: "1px solid #ddd",
                            borderRadius: "8px",
                            padding: "15px",
                            backgroundColor: expense.isHistorical ? "#f0f8ff" : "#f9f9f9",
                            position: "relative"
                        }}>
                            {expense.isHistorical && (
                                <div style={{
                                    position: "absolute",
                                    top: "10px",
                                    right: "10px",
                                    backgroundColor: "#ff9800",
                                    color: "white",
                                    padding: "2px 8px",
                                    borderRadius: "12px",
                                    fontSize: "12px",
                                    fontWeight: "bold"
                                }}>
                                    ARCHIVED
                                </div>
                            )}
                            <div style={{
                                display: "flex",
                                justifyContent: "space-between",
                                alignItems: "center",
                                marginBottom: "10px"
                            }}>
                                <h3 style={{ margin: "0", color: "#333" }}>{expense.description}</h3>
                                <span style={{
                                    color: "#666",
                                    fontSize: "14px",
                                    fontWeight: "normal"
                                }}>
                                    {expense.date ? new Date(expense.date).toLocaleDateString('en-US', {
                                        year: 'numeric',
                                        month: 'long',
                                        day: 'numeric'
                                    }) : 'No date'}
                                    {expense.isHistorical && expense.archivedAt && (
                                        <div style={{ fontSize: "12px", color: "#888" }}>
                                            Archived: {new Date(expense.archivedAt).toLocaleDateString()}
                                        </div>
                                    )}
                                </span>
                            </div>
                            <div style={{
                                display: "grid",
                                gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
                                gap: "10px"
                            }}>
                                <div>
                                    <strong>Amount:</strong> <span style={{ color: "#4CAF50", fontSize: "18px" }}>₹{expense.amount.toFixed(2)}</span>
                                </div>
                                <div>
                                    <strong>Paid by:</strong> {expense.paidBy}
                                </div>
                                <div>
                                    <strong>Participants:</strong> {expense.participants}
                                </div>
                                <div>
                                    <strong>Category:</strong> {expense.category}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default History;