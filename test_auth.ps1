# Test authentication and expense addition
$registerBody = @{
    username = "testuser"
    email = "test@example.com"
    password = "password123"
} | ConvertTo-Json

Write-Host "Attempting to register user..."
try {
    $registerResponse = Invoke-WebRequest -Uri "http://localhost:9090/api/auth/signup" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $registerBody `
        -UseBasicParsing

    Write-Host "Register Status Code: $($registerResponse.StatusCode)"
    Write-Host "Register Response: $($registerResponse.Content)"
} catch {
    Write-Host "Register Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Register Response Body: $responseBody"
    }
}

$loginBody = @{
    username = "testuser"
    password = "password123"
} | ConvertTo-Json

Write-Host "Attempting to login..."
try {
    $loginResponse = Invoke-WebRequest -Uri "http://localhost:9090/api/auth/signin" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $loginBody `
        -UseBasicParsing

    Write-Host "Login Status Code: $($loginResponse.StatusCode)"
    $loginData = $loginResponse.Content | ConvertFrom-Json
    Write-Host "Login Response: $($loginData | ConvertTo-Json)"

    if ($loginData.accessToken) {
        Write-Host "Token received: $($loginData.accessToken)"

        # Now try to add expense with token
        $expenseBody = @{
            description = "Test Expense"
            amount = 50.00
            paidBy = "Alice"
            participants = "Alice,Bob"
        } | ConvertTo-Json

        $expenseResponse = Invoke-WebRequest -Uri "http://localhost:9090/api/expenses/add" `
            -Method POST `
            -Headers @{
                "Content-Type"="application/json"
                "Authorization"="Bearer $($loginData.accessToken)"
            } `
            -Body $expenseBody `
            -UseBasicParsing

        Write-Host "Expense Add Status Code: $($expenseResponse.StatusCode)"
        Write-Host "Expense Add Response: $($expenseResponse.Content)"
    } else {
        Write-Host "No accessToken received in login response"
    }
} catch {
    Write-Host "Login Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Login Response Body: $responseBody"
    }
}