import axios from "axios";

const BASE_URL = "https://expense-splitter-production-93dc.up.railway.app/";
const API_URL = BASE_URL+"api/expenses";
const PERSONS_API_URL = BASE_URL+"api/persons";
const AUTH_API_URL = BASE_URL+"api/auth";

class ExpenseService{

    // Authentication methods
    login(credentials) {
        return axios.post(AUTH_API_URL + "/signin", credentials);
    }

    register(user) {
        return axios.post(AUTH_API_URL + "/signup", user);
    }

    logout() {
        localStorage.removeItem("user");
    }

    getCurrentUser() {
        return JSON.parse(localStorage.getItem("user"));
    }

    // Helper method to get auth header
    getAuthHeader() {
        const user = this.getCurrentUser();
        if (user && user.accessToken) {
            return { Authorization: 'Bearer ' + user.accessToken };
        } else {
            return {};
        }
    }

    addExpense(expense){
        return axios.post(API_URL + "/add", expense, { headers: this.getAuthHeader() });
    }

    getAllExpenses(){
        return axios.get(API_URL, { headers: this.getAuthHeader() });
    }

    getExpenseById(id){
        return axios.get(API_URL + "/" + id, { headers: this.getAuthHeader() });
    }

    deleteExpense(id){
        return axios.delete(API_URL + "/" + id, { headers: this.getAuthHeader() });
    }

    updateExpense(id, expense){
        return axios.put(API_URL + "/" + id, expense, { headers: this.getAuthHeader() });
    }

    getSpendingInsights(){
        return axios.get(API_URL + "/analytics/insights", { headers: this.getAuthHeader() });
    }

    getBudgetRecommendations(){
        return axios.get(API_URL + "/analytics/budgets", { headers: this.getAuthHeader() });
    }

    getBalances(){
        return axios.get(API_URL + "/balance", { headers: this.getAuthHeader() });
    }

    getSettlements(){
        return axios.get(API_URL + "/settlements", { headers: this.getAuthHeader() });
    }

    generateSettlements(){
        return axios.post(API_URL + "/settlements/generate", {}, { headers: this.getAuthHeader() });
    }

    markSettlementAsPaid(id){
        return axios.put(API_URL + "/settlements/" + id + "/paid", {}, { headers: this.getAuthHeader() });
    }

    finalizeSettlements(){
        return axios.post(API_URL + "/settlements/finalize", {}, { headers: this.getAuthHeader() });
    }

    getHistory(){
        return axios.get(API_URL + "/history", { headers: this.getAuthHeader() });
    }

    getAllPersons(){
        return axios.get(PERSONS_API_URL, { headers: this.getAuthHeader() });
    }

    addPerson(person){
        return axios.post(PERSONS_API_URL, person, { headers: this.getAuthHeader() });
    }

}

const expenseService = new ExpenseService();
export default expenseService;