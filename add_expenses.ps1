# Script to add sample expenses for the current user
# Replace YOUR_TOKEN_HERE with the token from your browser's developer tools
# (Network tab -> login request -> Response headers -> Authorization)

$token = "YOUR_TOKEN_HERE"  # <-- Replace this with your actual token

$expenses = @(
    @{
        description = "Coffee at Cafe"
        amount = 25.00
        paidBy = "You"
        participants = "You,Friend"
        category = "Food"
    },
    @{
        description = "Bus Ticket"
        amount = 15.50
        paidBy = "Friend"
        participants = "You,Friend"
        category = "Transportation"
    },
    @{
        description = "Movie Night"
        amount = 40.00
        paidBy = "You"
        participants = "You,Friend,Partner"
        category = "Entertainment"
    },
    @{
        description = "Groceries"
        amount = 85.75
        paidBy = "Partner"
        participants = "You,Friend,Partner"
        category = "Food"
    }
)

Write-Host "Adding sample expenses..."
Write-Host "Note: Replace YOUR_TOKEN_HERE with your actual JWT token from the browser"
Write-Host ""

foreach ($expense in $expenses) {
    $body = $expense | ConvertTo-Json
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:9090/api/expenses/add" -Method POST -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $token"} -Body $body -UseBasicParsing
        Write-Host "✓ Added: $($expense.description) - ₹$($expense.amount)"
    } catch {
        Write-Host "✗ Failed to add: $($expense.description) - $($_.Exception.Message)"
    }
}

Write-Host ""
Write-Host "After adding expenses, refresh your History page to see them!"