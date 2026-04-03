import React, {useState, useCallback} from "react";
import ExpenseService from "../services/ExpenseService";

function ExpenseList({ refreshTrigger }){

const [expenses,setExpenses] = useState([]);
const [total,setTotal] = useState(0);
const [settlements,setSettlements] = useState([]);
const [showSettlements,setShowSettlements] = useState(true);
const [showExpenses,setShowExpenses] = useState(true);
const [editingExpense, setEditingExpense] = useState(null);
const [editingValues, setEditingValues] = useState({});

const calculateShare = (amount, participants) => {
if(!participants) return 0;
const participantList = participants.split(",").filter(p => p.trim() !== "");
return (amount / participantList.length).toFixed(2);
};

const loadExpenses = useCallback(() => {
ExpenseService.getAllExpenses().then((response)=>{
setExpenses(response.data);
let sum = 0;
response.data.forEach(e=>{
sum += e.amount;
});
setTotal(sum.toFixed(2));
// Use generateSettlements to keep settlements in sync with current expense data and paid flags
generateSettlements();
})
.catch((error)=>{
console.error("Error fetching expenses:", error);
});
}, []);

const toggleExpenses = () => {
if (showExpenses) {
setShowExpenses(false);
setShowSettlements(false);
} else {
loadExpenses();
setShowExpenses(true);
}
};

// Load expenses whenever data changes (new expense added/updated)
React.useEffect(() => {
  if (showExpenses) {
    loadExpenses();
  }
}, [refreshTrigger, showExpenses, loadExpenses]);

const generateSettlements = () => {
ExpenseService.generateSettlements().then((response)=>{
setSettlements(response.data);
})
.catch((error)=>{
console.error("Error generating settlements:", error);
});
};

const markAsPaid = (id) => {
ExpenseService.markSettlementAsPaid(id).then(()=>{
generateSettlements(); // Recompute after payment so same page updates
})
.catch((error)=>{
console.error("Error marking as paid:", error);
});
};

const finalizeSettlements = () => {
ExpenseService.finalizeSettlements().then((response)=>{
alert(response.data.message || "Finalization completed");
loadExpenses();
}).catch((error)=>{
console.error("Error finalizing settlements:", error);
alert("Error finalizing settlements: " + (error.response?.data?.error || error.message));
});
};

const startEdit = (expense) => {
setEditingExpense(expense.id);
setEditingValues({
description: (expense.description || ""),
category: (expense.category || ""),
date: (expense.date ? new Date(expense.date).toISOString().split("T")[0] : ""),
amount: (expense.amount || 0),
paidBy: (expense.paidBy || ""),
participants: (expense.participants || "")
});
};

const cancelEdit = () => {
setEditingExpense(null);
setEditingValues({});
};

const handleEditChange = (field, value) => {
setEditingValues((prev) => ({ ...prev, [field]: value }));
};

const saveEdit = (id) => {
ExpenseService.updateExpense(id, {
...editingValues,
amount: parseFloat(editingValues.amount),
}).then(() => {
alert("Expense updated successfully");
setEditingExpense(null);
setEditingValues({});
loadExpenses();
}).catch((error)=>{
console.error("Error updating expense:", error);
alert("Error updating expense: " + (error.response?.data?.message || error.message));
});
};

const deleteExpense = (id) => {
if(window.confirm("Are you sure you want to delete this expense?")){
ExpenseService.deleteExpense(id)
.then(()=>{
alert("Expense deleted successfully");
loadExpenses();
})
.catch((error)=>{
console.error("Error deleting expense:", error);
alert("Error deleting expense: " + error.message);
});
}
};

return(

<div>
    <h2>Expense Management</h2>

    <button
        onClick={toggleExpenses}
        className={`btn-primary ${showExpenses ? 'btn-danger' : ''}`}
    >
        {showExpenses ? "👁️ Hide Expenses" : "📋 Load Expenses"}
    </button>

    {showExpenses && (
        <>
            {expenses.length === 0 ? (
                <div className="empty-state">
                    <h3>No expenses added yet</h3>
                    <p>Start by adding your first expense above!</p>
                </div>
            ) : (
                <div className="responsive-table">
                    <table>
                        <thead>
                            <tr>
                                <th>Description</th>
                                <th>Category</th>
                                <th>Date</th>
                                <th>Amount</th>
                                <th>Paid By</th>
                                <th>Participants</th>
                                <th>Per Person</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {expenses.map(expense => (
                                <tr key={expense.id}>
                                    {editingExpense === expense.id ? (
                                        <>
                                            <td>
                                                <input
                                                    type="text"
                                                    value={editingValues.description}
                                                    onChange={(e) => handleEditChange("description", e.target.value)}
                                                />
                                            </td>
                                            <td>
                                                <input
                                                    type="text"
                                                    value={editingValues.category}
                                                    onChange={(e) => handleEditChange("category", e.target.value)}
                                                />
                                            </td>
                                            <td>
                                                <input
                                                    type="date"
                                                    value={editingValues.date}
                                                    onChange={(e) => handleEditChange("date", e.target.value)}
                                                />
                                            </td>
                                            <td>
                                                <input
                                                    type="number"
                                                    min="0"
                                                    step="0.01"
                                                    value={editingValues.amount}
                                                    onChange={(e) => handleEditChange("amount", e.target.value)}
                                                />
                                            </td>
                                            <td>
                                                <input
                                                    type="text"
                                                    value={editingValues.paidBy}
                                                    onChange={(e) => handleEditChange("paidBy", e.target.value)}
                                                />
                                            </td>
                                            <td>
                                                <input
                                                    type="text"
                                                    value={editingValues.participants}
                                                    onChange={(e) => handleEditChange("participants", e.target.value)}
                                                />
                                            </td>
                                            <td>₹ {calculateShare(editingValues.amount || 0, editingValues.participants)}</td>
                                            <td>
                                                <button
                                                    onClick={() => saveEdit(expense.id)}
                                                    className="btn-success btn-small"
                                                >
                                                    💾 Save
                                                </button>
                                                <button
                                                    onClick={cancelEdit}
                                                    className="btn-secondary btn-small"
                                                >
                                                    ✖️ Cancel
                                                </button>
                                            </td>
                                        </>
                                    ) : (
                                        <>
                                            <td>{expense.description}</td>
                                            <td>{(expense.category || "Uncategorized")}</td>
                                            <td>{(expense.date ? new Date(expense.date).toLocaleDateString() : "N/A")}</td>
                                            <td>₹ {expense.amount}</td>
                                            <td>{expense.paidBy}</td>
                                            <td>{expense.participants}</td>
                                            <td>₹ {calculateShare(expense.amount, expense.participants)}</td>
                                            <td>
                                                <button
                                                    onClick={() => startEdit(expense)}
                                                    className="btn-info btn-small"
                                                >
                                                    ✏️ Edit
                                                </button>
                                                <button
                                                    onClick={() => deleteExpense(expense.id)}
                                                    className="btn-danger btn-small"
                                                >
                                                    🗑️ Delete
                                                </button>
                                            </td>
                                        </>
                                    )}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </>
    )}

    {showExpenses && (
        <>
            <div className="expense-summary">
                <h3>Total Expenses: ₹ {total}</h3>
            </div>

            <div className="settlement-controls">
                <button
                    onClick={() => setShowSettlements(!showSettlements)}
                    className="btn-info"
                >
                    {showSettlements ? "👁️ Hide Settlements" : "💰 Show Settlements"}
                </button>
            </div>

            {showSettlements && settlements.length > 0 && (
                <div className="settlements-container">
                    <h3>💰 Settlement Details</h3>
                    <p className="settlement-subtitle">Who should pay whom:</p>
                    <ul className="settlements-list">
                        {settlements.map((settlement) => (
                            <li key={settlement.id} className="settlement-item">
                                <span className="settlement-text">
                                    {settlement.fromPerson} → {settlement.toPerson} : ₹{settlement.amount.toFixed(2)}
                                </span>
                                <button
                                    onClick={() => markAsPaid(settlement.id)}
                                    className="btn-secondary btn-small"
                                >
                                    ✅ Mark as Paid
                                </button>
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            {showSettlements && settlements.length === 0 && (
                <div className="settlements-container settled">
                    <h3>✅ All Settled!</h3>
                    <p>Everyone is even. No payments needed.</p>
                    <div className="settlement-actions">
                        {/* Hide generate button when there are no pending settlements (all paid) */}
                        {expenses.length === 0 ? (
                            <p>Add an expense to create settlements again.</p>
                        ) : (
                            <p style={{ color: "#555", fontSize: "14px" }}>
                                New or updated expenses will automatically produce new settlement suggestions.
                            </p>
                        )}
                        <button
                            onClick={finalizeSettlements}
                            className="btn-success"
                        >
                            🧾 Finalize and Clear Data
                        </button>
                    </div>
                </div>
            )}
        </>
    )}

</div>

);
}

export default ExpenseList;