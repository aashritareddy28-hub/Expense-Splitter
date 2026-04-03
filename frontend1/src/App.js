import React, { useState, useEffect } from "react";
import AddExpense from "./components/AddExpense";
import ExpenseList from "./components/ExpenseList";
import Login from "./components/Login";
import SpendingInsights from "./components/SpendingInsights";
import BudgetRecommendations from "./components/BudgetRecommendations";
import History from "./components/History";
import ExpenseService from "./services/ExpenseService";
import "./App.css";

function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("expenses");
  const [refreshKey, setRefreshKey] = useState(0);

  const handleRefresh = () => {
    setRefreshKey((prev) => prev + 1);
  };

  useEffect(() => {
    const user = ExpenseService.getCurrentUser();
    if (user) {
      setCurrentUser(user);
    }
    setLoading(false);
  }, []);

  const handleLogin = (userData) => {
    setCurrentUser(userData);
  };

  const handleLogout = () => {
    ExpenseService.logout();
    setCurrentUser(null);
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  if (!currentUser) {
    return <Login onLogin={handleLogin} />;
  }

  if (!currentUser) {
    return <Login onLogin={handleLogin} />;
  }

  return (
    <div className="app-container">
      <header className="header">
        <div className="user-info">
          <h1>Group Expense Management Dashboard</h1>
          <div>
            <span className="welcome-text">Welcome, {currentUser.username}!</span>
            <button
              className="logout-btn"
              onClick={handleLogout}
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <div className="dashboard">
        {/* Navigation Tabs */}
        <div className="tabs">
          <button
            className={`tab-button ${activeTab === "expenses" ? "active" : ""}`}
            onClick={() => setActiveTab("expenses")}
          >
            💰 Expenses
          </button>
          <button
            className={`tab-button ${activeTab === "insights" ? "active" : ""}`}
            onClick={() => setActiveTab("insights")}
          >
            📊 Insights
          </button>
          <button
            className={`tab-button ${activeTab === "budgets" ? "active" : ""}`}
            onClick={() => setActiveTab("budgets")}
          >
            🎯 Budgets
          </button>
          <button
            className={`tab-button ${activeTab === "history" ? "active" : ""}`}
            onClick={() => setActiveTab("history")}
          >
            📜 History
          </button>
        </div>

        {/* Tab Content */}
        <div className="tab-content">
          {activeTab === "expenses" && (
            <div className="expenses-grid">
              <div className="card">
                <AddExpense onDataChange={handleRefresh} />
              </div>
              <div className="card">
                <ExpenseList refreshTrigger={refreshKey} />
              </div>
            </div>
          )}

          {activeTab === "insights" && (
            <div className="card">
              <SpendingInsights />
            </div>
          )}

          {activeTab === "budgets" && (
            <div className="card">
              <BudgetRecommendations />
            </div>
          )}

          {activeTab === "history" && (
            <div className="card">
              <History refreshTrigger={refreshKey} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;