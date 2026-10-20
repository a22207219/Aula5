$ErrorActionPreference = "Stop"

$orderBase = "http://localhost:8083/api/v1/orders"
$productBase = "http://localhost:8081/api/v1/products"
$supplierBase = "http://localhost:8082/api/v1/suppliers"
$tag = [guid]::NewGuid().ToString("N").Substring(0, 10)

function Assert-Equal($actual, $expected, $message) {
  if ($actual -ne $expected) {
    throw "$message (expected '$expected', got '$actual')"
  }
}
function Get-Stock([string] $productId) {
  return [int](Invoke-RestMethod -Method Get -Uri "$productBase/$productId/stock").stock
}

Write-Host "1. Creating an active supplier..."
$supplierBody = @{ name = "Lab 5 Supplier $tag"; email = "lab5-$tag@example.com"; phone = "210000000"; address = "Lisbon"; active = $true } | ConvertTo-Json
$supplier = Invoke-RestMethod -Method Post -Uri $supplierBase -ContentType "application/json" -Body $supplierBody

Write-Host "2. Creating a product..."
$productBody = @{ sku = "LAB5-$tag"; name = "Lab 5 Product $tag"; description = "Order-service smoke test"; price = 9.99; supplierId = $supplier.id } | ConvertTo-Json
$product = Invoke-RestMethod -Method Post -Uri $productBase -ContentType "application/json" -Body $productBody
$productId = [string]$product.id

Write-Host "3. Seeding stock..."
$seedBody = @{ quantity = 5; operationId = [guid]::NewGuid().ToString() } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$productBase/$productId/stock/increment" -ContentType "application/json" -Body $seedBody | Out-Null
Assert-Equal (Get-Stock $productId) 5 "Stock seed did not set stock to 5"

Write-Host "4. Creating an order must not change stock..."
$orderBody = @{ productId = $productId; quantity = 2 } | ConvertTo-Json
$order = Invoke-RestMethod -Method Post -Uri $orderBase -ContentType "application/json" -Body $orderBody
$orderId = [string]$order.id
Assert-Equal $order.status "PENDING" "New order is not PENDING"
Assert-Equal (Get-Stock $productId) 5 "Creating an order unexpectedly changed stock"

Write-Host "5. Confirming the order decrements stock once..."
$confirmed = Invoke-RestMethod -Method Post -Uri "$orderBase/$orderId/confirm"
Assert-Equal $confirmed.status "CONFIRMED" "Order did not become CONFIRMED"
Assert-Equal (Get-Stock $productId) 3 "Confirm did not decrement stock by 2"

Write-Host "6. Repeating confirm must not decrement stock again..."
Invoke-RestMethod -Method Post -Uri "$orderBase/$orderId/confirm" | Out-Null
Assert-Equal (Get-Stock $productId) 3 "Repeated confirm changed stock"

Write-Host "7. Cancelling restores stock exactly once..."
$cancelled = Invoke-RestMethod -Method Post -Uri "$orderBase/$orderId/cancel"
Assert-Equal $cancelled.status "CANCELLED" "Order did not become CANCELLED"
Assert-Equal (Get-Stock $productId) 5 "Cancel did not restore stock"
Invoke-RestMethod -Method Post -Uri "$orderBase/$orderId/cancel" | Out-Null
Assert-Equal (Get-Stock $productId) 5 "Repeated cancel changed stock"

Write-Host "Lab 5 end-to-end smoke test passed. Supplier ID: $($supplier.id); Product ID: $productId; Order ID: $orderId"
