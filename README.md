# 📄 Stock Sync Service – Coding Challenge

A Spring Boot microservice that periodically synchronizes product stock from **two vendors**, persists into an **H2** relational DB, detects **stock-out transitions**, and exposes **`GET /products`**.

## 🧠 Overview
- **Vendor A**: mocked REST API at `/mock/vendor-a/products` (serves `src/main/resources/vendor-a-sample.json`).
- **Vendor B**: CSV file located at `/tmp/vendor-b/stock.csv` (simulating an FTP drop).
- **DB**: H2 in-memory (JPA/Hibernate).
- **Scheduler**: runs every minute by default (`@Scheduled`), configurable via `sync.cron`.
- **Stock-out detection**: creates a row in `stock_out_events` when stock transitions from `>0` to `0`.
- **OpenAPI**: Swagger UI at `/swagger-ui.html`.

## 🛠 Tech Stack
- Java 21, Spring Boot 3.3.x
- Spring Web, Data JPA, H2
- Apache Commons CSV
- Spring Retry + AOP (exponential backoff for Vendor A API)
- springdoc-openapi
- JUnit 5, MockMvc

## ▶️ How to Run (local)
```bash
# 1) Ensure Java 21 and Maven are installed
mvn -v

# 2) (Optional) Prepare Vendor B CSV
sudo mkdir -p /tmp/vendor-b
cat > /tmp/vendor-b/stock.csv <<'CSV'
sku,name,stockQuantity
ABC123,Product A,10
XYZ456,Product B,0
CSV

# 3) Build & run
mvn spring-boot:run

# 4) Visit endpoints
# List products
curl http://localhost:8080/products

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

> The scheduler runs every minute. It pulls Vendor A JSON and Vendor B CSV, updates products, and records stock-out events for transitions **(>0 → 0)**.

## 🔧 Configuration
`src/main/resources/application.yml`
```yaml
sync:
  enabled: true
  cron: "0 */1 * * * *"  # every minute

vendorA:
  url: http://localhost:8080/mock/vendor-a/products

vendorB:
  csvPath: /tmp/vendor-b/stock.csv
```

You can override via environment variables or `--vendorB.csvPath=...` and `--vendorA.url=...`.

## 🧪 Tests
```bash
mvn test
```
- `SyncServiceIntegrationTest` verifies stock-out detection.
- `ProductControllerTest` checks `GET /products`.
- `VendorAClientTest` validates `VendorAClient`.
- `VendorBReaderTest` validates `VendorBReader`.

## 🧩 Design & Decisions
- **Uniqueness**: `(sku, vendor)` is enforced by a unique constraint; SKUs are **not** globally unique.
- **Stock-out**: Only transitions **from positive** to **zero** create `StockOutEvent`. First-time insert with zero **does not** trigger an event (assumption).
- **Resilience**: Vendor A API fetch uses **Spring Retry** (3 attempts, exponential backoff).
- **Separation of concerns**: Controllers are thin; `SyncService` contains core orchestration; vendor readers isolated (`VendorAClient`, `VendorBReader`).
- **CSV parsing**: Uses Apache Commons CSV with headers.
- **OpenAPI**: Out-of-the-box via springdoc, no extra config.
- **H2 console**: available at `/h2-console` for convenience.

## 🔮 Improvements (if given more time)
- Incremental sync (deltas) & hash-based change detection.
- Outbox pattern + event publisher (Kafka) when stock-out occurs.
- Observability: Micrometer metrics & structured logs (JSON) + tracing.
- Idempotent sync runs with job metadata.
- Validation & quarantine for malformed vendor data.
- Authentication/authorization for REST endpoints.
- Flyway migrations instead of `ddl-auto`.
- More extensive tests (error paths, retries, CSV edge cases).

## 🐳 Docker
A simple Dockerfile is included.

```bash
# Build the jar
mvn -DskipTests package

# Build the image
docker build -t stock-sync-service:local .

# Run the container (mount CSV folder if using Vendor B)
docker run --rm -p 8080:8080 \
  -v /tmp/vendor-b:/tmp/vendor-b \
  -e VENDORB_CSVPATH=/tmp/vendor-b/stock.csv \
  stock-sync-service:local
```

## 📚 FAQ Mapping
- **CSV format**: `sku,name,stockQuantity` UTF-8 with commas. Path defaults to `/tmp/vendor-b/stock.csv`.
- **Vendor A**: simulated via `/mock/vendor-a/products` serving `vendor-a-sample.json`.
- **SKU uniqueness**: enforced as `(sku, vendor)` in DB schema.
- **Zero stock**: transition detection persisted in `stock_out_events` (and logged). 
- **Full sync**: each run is full; deltas noted as future work.

---

**Endpoints**:
- `GET /products` – list latest products.
- `GET /mock/vendor-a/products` – mock vendor A payload.
- Swagger UI: `/swagger-ui.html`
