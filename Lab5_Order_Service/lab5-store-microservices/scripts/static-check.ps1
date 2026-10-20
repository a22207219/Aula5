$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

$required = @(
  "pom.xml", "mvnw", "mvnw.cmd", ".mvn/wrapper/maven-wrapper.properties",
  "docker-compose.yml", ".env.example", ".gitignore",
  "product-service/pom.xml", "supplier-service/pom.xml", "order-service/pom.xml",
  "product-service/src/main/resources/db/migration/V1__create_product.sql",
  "product-service/src/main/resources/db/migration/V3__stock_operations.sql",
  "supplier-service/src/main/resources/db/migration/V1__create_supplier.sql",
  "order-service/src/main/resources/db/migration/V1__create_orders.sql",
  "order-service/src/main/java/pt/ulusofona/cd/order/controller/OrderController.java",
  "order-service/src/main/java/pt/ulusofona/cd/order/service/OrderService.java",
  "order-service/src/main/java/pt/ulusofona/cd/order/service/OrderRecovery.java",
  "product-service/src/main/java/pt/ulusofona/cd/product/controller/StockController.java",
  "scripts/init-databases.sh", "scripts/lab5-smoke-test.ps1"
)

foreach ($path in $required) {
  if (-not (Test-Path (Join-Path $root $path))) {
    throw "Missing required Lab 5 file: $path"
  }
}

if (Test-Path (Join-Path $root ".env")) {
  throw ".env must not be committed in the project archive"
}

$files = Get-ChildItem -Path $root -Recurse -File -Include *.java,*.xml,*.yml,*.yaml,*.sql,*.md,*.sh
$source = ($files | ForEach-Object { Get-Content -Path $_.FullName -Raw }) -join "`n"
$compose = Get-Content (Join-Path $root "docker-compose.yml") -Raw
$envExample = Get-Content (Join-Path $root ".env.example") -Raw
$dbInit = Get-Content (Join-Path $root "scripts/init-databases.sh") -Raw
$windowsWrapper = Get-Content (Join-Path $root "mvnw.cmd") -Raw
if ($windowsWrapper -notmatch "tmp-apache-maven" -or $windowsWrapper -notmatch "Copy-Item") {
  throw "Windows Maven wrapper must extract to a temporary directory before installing Maven"
}

foreach ($needle in @(
  "@HttpExchange", "RestClientAdapter", "HttpServiceProxyFactory",
  "@Retry", "@CircuitBreaker", "ProblemDetail",
  "stock/decrement", "stock/increment", "operationId", "CONFIRMING",
  "CANCELLING", "restoreRequired", "findTop100ByStatusOrderByUpdatedAtAsc"
)) {
  if ($source -notmatch [regex]::Escape($needle)) {
    throw "Missing required Lab 5 feature: $needle"
  }
}

foreach ($forbidden in @(
  "spring-cloud-dependencies", "spring-cloud-starter-openfeign", "@EnableFeignClients"
)) {
  if ($source -match [regex]::Escape($forbidden)) {
    throw "Forbidden Spring Cloud/OpenFeign feature detected: $forbidden"
  }
}

if ($compose -match "(?m)^version\s*:") {
  throw "Compose must use v2 syntax without a version key"
}
foreach ($name in @("PRODUCT_DB_USER", "PRODUCT_DB_PASSWORD", "SUPPLIER_DB_USER", "SUPPLIER_DB_PASSWORD", "ORDER_DB_USER", "ORDER_DB_PASSWORD")) {
  if ($compose -notmatch [regex]::Escape($name) -or $envExample -notmatch [regex]::Escape($name) -or $dbInit -notmatch [regex]::Escape($name)) {
    throw "Database initialization is not configured consistently for $name"
  }
}

# product_id must deliberately remain an ordinary UUID, not a cross-service FK.
$orderMigration = Get-Content (Join-Path $root "order-service/src/main/resources/db/migration/V1__create_orders.sql") -Raw
if ($orderMigration -match "(?i)FOREIGN\s+KEY|REFERENCES\s+product") {
  throw "orders.product_id must not have a foreign key to Product's database"
}

Write-Host "Lab 5 static checks passed."
Write-Host "These checks are structural only; run .\mvnw.cmd clean verify and Docker smoke tests before submission."
