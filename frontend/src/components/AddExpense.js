import React, { useState } from "react";
import ExpenseService from "../services/ExpenseService";

function AddExpense(){

const [description,setDescription] = useState("");
const [amount,setAmount] = useState("");
const [paidBy,setPaidBy] = useState("");
const [participants,setParticipants] = useState("");
const [category,setCategory] = useState("Food");
const [date,setDate] = useState(new Date().toISOString().split('T')[0]); // Default to today

const saveExpense = (e) => {

e.preventDefault();

if(!description || !amount || !paidBy || !participants || !category){
alert("Please fill all fields");
return;
}

// Validate amount
if(parseFloat(amount) <= 0){
alert("Amount must be greater than 0");
return;
}

// Trim whitespace from names
const paidByTrimmed = paidBy.trim();
const participantsTrimmed = participants.split(',').map(p => p.trim()).filter(p => p).join(', ');

if(!participantsTrimmed){
alert("Please enter at least one participant");
return;
}

const expense = {
description: description.trim(),
amount: parseFloat(amount),
paidBy: paidByTrimmed,
participants: participantsTrimmed,
category: category.trim(),
date: date
};

ExpenseService.addExpense(expense)
.then((response)=>{
console.log("Expense added:", response.data);
alert("Expense Added Successfully");
setDescription("");
setAmount("");
setPaidBy("");
setParticipants("");
setCategory("Food");
setDate(new Date().toISOString().split('T')[0]);
window.location.reload();
})
.catch((error)=>{
console.error("Error details:", error);
const errorMessage = error.response?.data?.message || error.message || "Unknown error occurred";
alert("Error adding expense: " + errorMessage);
});

};

return(
    <div>
        <h2>Add New Expense</h2>

        <form className="expense-form">
            <div className="form-group">
                <label>Description</label>
                <input
                    type="text"
                    placeholder="What was this expense for?"
                    value={description}
                    onChange={(e)=>setDescription(e.target.value)}
                />
            </div>

            <div className="form-group">
                <label>Amount (₹)</label>
                <input
                    type="number"
                    placeholder="0.00"
                    value={amount}
                    onChange={(e)=>setAmount(e.target.value)}
                    min="0"
                    step="0.01"
                />
            </div>

            <div className="form-group">
                <label>Who Paid?</label>
                <input
                    type="text"
                    placeholder="Enter the person's name"
                    value={paidBy}
                    onChange={(e)=>setPaidBy(e.target.value)}
                />
            </div>

            <div className="form-group">
                <label>Participants</label>
                <input
                    type="text"
                    placeholder="Enter names separated by commas"
                    value={participants}
                    onChange={(e)=>setParticipants(e.target.value)}
                />
            </div>

            <div className="form-group">
                <label>Category</label>
                <select
                    value={category}
                    onChange={(e)=>setCategory(e.target.value)}
                >
                    <option value="Food">🍽️ Food & Dining</option>
                    <option value="Transportation">🚗 Transportation</option>
                    <option value="Entertainment">🎬 Entertainment</option>
                    <option value="Shopping">🛍️ Shopping</option>
                    <option value="Bills & Utilities">💡 Bills & Utilities</option>
                    <option value="Healthcare">🏥 Healthcare</option>
                    <option value="Education">📚 Education</option>
                    <option value="Travel">✈️ Travel</option>
                    <option value="Other">📦 Other</option>
                </select>
            </div>

            <div className="form-group">
                <label>Date</label>
                <input
                    type="date"
                    value={date}
                    onChange={(e)=>setDate(e.target.value)}
                />
            </div>

            <button
                type="button"
                onClick={saveExpense}
                className="btn-primary"
            >
                ➕ Add Expense
            </button>
        </form>
    </div>
);
}

export default AddExpense;