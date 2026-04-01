$response = Invoke-WebRequest -Uri "http://localhost:9090/api/expenses" `
  -Method GET `
  -UseBasicParsing

Write-Host "Status Code: $($response.StatusCode)"
$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
