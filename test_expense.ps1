$body = @{
    description = "Test Expense"
    amount = 100.00
    paidBy = "Alice"
    participants = "Alice,Bob,Charlie"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:9090/api/expenses/add" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body `
  -UseBasicParsing

Write-Host "Status Code: $($response.StatusCode)"
Write-Host "Response: $($response.Content)"
