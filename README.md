# 📄 Stock Sync Service – Coding Challenge

A Spring Boot microservice that periodically synchronizes product stock from **multiple vendors**, persists into an **H2** relational DB, detects **stock-out transitions**, and exposes **`GET /products`**.

## 🧠 Overview
- **Vendors** are configured in YAML, not hardcoded. Each vendor entry has an enabled flag (default: true). Add a vendor by editing YAML only. Two purposeful client types:
  - **RestVendorClient** – fetches from a REST endpoint (with retry).
  - **CsvVendorClient** – reads a local CSV file (simulating FTP drop).
- **DB**: H2 in-memory (JPA/Hibernate).
- **Scheduler**: runs every minute by default (`@Scheduled`), configurable via `sync.cron`.
- **Stock-out detection**: creates a row in `stock_out_events` when stock transitions from `>0` to `0`.
- **OpenAPI**: Swagger UI at `/swagger-ui.html`.

## 🛠 Tech Stack
- Java 21, Spring Boot 3
- Spring Web, Data JPA (H2), Scheduling
- Retry via RetryTemplate (exponential backoff)
- Apache Commons CSV
- springdoc-openapi (Swagger UI)
- JUnit 5, MockMvc, MockWebServer (REST client tests)
- Docker (multi-stage)

## 🧩 Architecture
    Controller
        └── ProductController  → GET /products

    Service
        ├── SyncService        → orchestrates sync across all configured VendorClient(s)
        └── VendorClient       → interface (strategy)
            ├── RestVendorClient   (config: name, url, enabled)
            └── CsvVendorClient    (config: name, path, enabled)

    Config
        ├── VendorsProperties  → binds vendors.rest[] / vendors.csv[]
        ├── VendorClientsConfig→ builds List<VendorClient> from properties
        └── RestClientConfig   → RestTemplate bean with timeouts

    Persistence (H2)
        ├── Product (sku, name, stockQuantity, vendor, updatedAt)
        └── StockOutEvent (sku, vendor, previousQuantity, occurredAt)

    Scheduler
        └── SyncScheduler (@Scheduled) → calls SyncService.syncAll()

## 📚 How to add a vendor
- Vendors are config-driven. 
- (Optionally) toggle enabled: false to disable any vendor.
- For fetching vendor's products from a REST endpoint:

`src/main/resources/application.yml`
```yaml
vendors:
  rest:
    - name: VENDOR_A
      url: http://localhost:8080/mock/vendor-a/products
      enabled: true
```
- For fetching vendor's products from a local CSV file:

`src/main/resources/application.yml`
```yaml
vendors:
  csv:
    - name: VENDOR_B
      path: /tmp/vendor-b/stock.csv
      enabled: true
```
- Restart — no code changes required.

## Env var overrides (Docker/K8s)

Spring relaxed binding lets you set:
```
VENDORS_REST_0_NAME=VENDOR_A
VENDORS_REST_0_URL=http://host.docker.internal:8080/mock/vendor-a/products
VENDORS_REST_0_ENABLED=true

VENDORS_CSV_0_NAME=VENDOR_B
VENDORS_CSV_0_PATH=/tmp/vendor-b/stock.csv
VENDORS_CSV_0_ENABLED=false
```

## ▶️ Run locally
**1) (Optional) Prepare Vendor B CSV**
```bash
mkdir -p /tmp/vendor-b
cat > /tmp/vendor-b/stock.csv <<'CSV'
sku,name,stockQuantity
ABC123,Product A,10
XYZ456,Product B,0
CSV
```

**2) Start the app**
```bash
mvn spring-boot:run
```

**3) Try endpoints**
- Products: http://localhost:8080/products
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console (JDBC URL jdbc:h2:mem:stocks)


## 🔧 Schedule Configuration
> The scheduler runs every minute. It pulls vendor's data, updates products, and records stock-out events for transitions **(>0 → 0)**.

`src/main/resources/application.yml`
```yaml
sync:
  enabled: true
  cron: "0 */1 * * * *"  # every minute
```


## 🧪 Tests
```bash
mvn test
```
Included tests (examples):
- RestVendorClientTest – Uses MockWebServer to verify:
  - success parsing
  - retry-then-success
  - retries exhausted → empty list
- CsvVendorClientTest – Valid/missing/malformed CSV cases
- ProductControllerTest – GET /products response shape
- SyncServiceIntegrationTest – stock-out transition persisted

## 🧾 Data model

- ```Product```
  - id (PK), sku, name, stockQuantity, vendor, updatedAt
  - Unique constraint on (sku, vendor)
- ```StockOutEvent```
  - id, sku, vendor, previousQuantity, occurredAt
  - Inserted only when existing product transitions >0 → 0

## 🧱 Assumptions & decisions
- Full sync: each run fetches full product lists from every enabled vendor.
- First insert with zero stock does not emit a stock-out event (transition only).
- Resilience:
  - REST vendors: timeouts + 3 retries (1s → 2s → 4s).
  - CSV vendors: missing or malformed files log errors and return partial/empty results; the job continues.
- DB: H2 in-memory for simplicity; unique (sku, vendor) ensures correct normalization.

## 🔮 Improvements (if given more time)
- Publish stock-out events to Kafka/SQS (outbox pattern).
- Vendor health metrics (Micrometer) and dashboards.
- Per-vendor validation & schema contracts.
- Flyway migrations for real DBs.
- AuthN/AuthZ and rate limiting.
- Delta-based/changed-only upserts.

## 🐳 Docker
**Build the image**

```bash
docker build -t stock-sync-service:local .
```

**Run the container (mount CSV folder if using Vendor B)**
```bash
mkdir -p /tmp/vendor-b
# ensure CSV exists
docker run --rm -p 8080:8080 \
  -v /tmp/vendor-b:/tmp/vendor-b \
  -e VENDORS_CSV_0_NAME=VENDOR_B \
  -e VENDORS_CSV_0_PATH=/tmp/vendor-b/stock.csv \
  -e VENDORS_CSV_0_ENABLED=true \
  stock-sync-service:local
```

---

**Endpoints**:
- `GET /products` – list latest products.
- `GET /mock/vendor-a/products` – mock vendor A payload.
- Swagger UI: `/swagger-ui.html`
