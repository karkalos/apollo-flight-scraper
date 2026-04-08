# Apollo Flight Scraper

Flight deal tracker that scrapes [Apollo.se](https://www.apollo.se) for flight-only tickets. Built with Spring Boot, Selenium headless Chrome, and H2 file-based persistence.

Apollo.se has no public API — content loads via JavaScript, so the app uses Selenium to render pages and extract flight data from the DOM.

## Architecture

```
[Scheduler] ──> [FlightSearchService] ──> [ApolloScraperService (Selenium)]
                       │                          │
[REST API] ────────────┘                          │
                       │                          ▼
                 [H2 File DB] <──────── [Parse & Deduplicate (SHA-256)]
```

## Tech Stack

- **Java 21**, **Spring Boot 3.4.1**
- **Spring Data JPA** + **Flyway** migrations
- **Selenium** + **WebDriverManager** (auto-downloads ChromeDriver)
- **H2** file-based database (persists across restarts)
- **SpringDoc OpenAPI** (Swagger UI)

## Quick Start

### Prerequisites

- Java 21 (e.g. `brew install --cask corretto@21`)
- Google Chrome installed

### Run Locally

```bash
# Set Java 21 if not default
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home

# Build
mvn clean package -DskipTests

# Run with local profile (H2 file DB, non-headless Chrome)
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Verify

| URL | Description |
|-----|-------------|
| http://localhost:8080 | Root page |
| http://localhost:8080/swagger-ui.html | Swagger UI — interactive API docs |
| http://localhost:8080/h2-console | H2 database browser |

**H2 Console connection details:**
- JDBC URL: `jdbc:h2:file:./data/apollo-flights`
- Username: `sa`
- Password: *(empty)*

## REST API

### Flight Results

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/flights?fromDate=&toDate=&minPrice=&maxPrice=&tripType=&sortBy=&page=&size=` | Filtered, paginated results |
| `GET` | `/api/flights/{id}` | Single flight result |
| `GET` | `/api/flights/cheapest?tripType=&limit=10` | Cheapest flights |
| `DELETE` | `/api/flights/old?olderThanDays=30` | Cleanup old results |

### Search Configuration

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/config` | List all search configs |
| `GET` | `/api/config/{id}` | Get one config |
| `POST` | `/api/config` | Create new config |
| `PUT` | `/api/config/{id}` | Update config |
| `PATCH` | `/api/config/{id}/toggle` | Enable/disable config |
| `DELETE` | `/api/config/{id}` | Delete config |

**Example — create a new search config:**
```json
POST /api/config
{
  "name": "ARN to ATH Summer",
  "originAirport": "ARN",
  "destinationAirport": "ATH",
  "searchFromDate": "2026-06-01",
  "searchToDate": "2026-08-31",
  "maxPrice": 5000,
  "tripType": "RETURN",
  "pollIntervalMinutes": 720
}
```

### Scrape Control

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/scrape/trigger?configId=1` | Manual trigger (async, returns 202) |
| `GET` | `/api/scrape/status?limit=10` | Recent scrape logs |

## Configuration

All scraper settings live in `application.properties`. Key properties:

### Scraper

| Property | Default | Description |
|----------|---------|-------------|
| `apollo.scraper.base-url` | `https://www.apollo.se/resor/flygresor` | Apollo flight search URL |
| `apollo.scraper.chrome.headless` | `true` (`false` in local) | Run Chrome in headless mode |
| `apollo.scraper.timeout.page-load` | `30` | Page load timeout (seconds) |
| `apollo.scraper.timeout.flight-cards` | `20` | Wait for flight cards (seconds) |

### CSS Selectors

CSS selectors are **placeholders** that need tuning after inspecting Apollo's actual DOM. Update these in `application.properties` without recompiling:

| Property | Purpose |
|----------|---------|
| `apollo.scraper.selector.cookie-button` | Cookie consent dismiss button |
| `apollo.scraper.selector.flight-card` | Flight result card container |
| `apollo.scraper.selector.price` | Price element within a card |
| `apollo.scraper.selector.departure-date` | Departure date element |
| `apollo.scraper.selector.return-date` | Return date element |
| `apollo.scraper.selector.airline` | Airline name element |
| `apollo.scraper.selector.booking-link` | Booking link element |

Each selector supports comma-separated fallbacks (e.g. `.price, [data-testid='price']`).

**How to find the right selectors:**
1. Run with `apollo.scraper.chrome.headless=false` (default in local profile)
2. Chrome will open visibly — inspect the flight results page
3. Use browser DevTools to find the correct CSS selectors
4. Update `application.properties` and restart

### Scheduling

| Property | Default | Description |
|----------|---------|-------------|
| `apollo.scraper.scheduling.enabled` | `true` | Enable/disable auto-scraping |
| `apollo.scraper.scheduling.check-interval-ms` | `300000` (5 min) | How often to check if configs are due |
| Each config's `pollIntervalMinutes` | `360` (6 hours) | Per-config scrape frequency |

## Database Schema

Three tables managed by Flyway:

- **`search_config`** — Search parameters (airports, dates, price filters, trip type, poll interval)
- **`flight_result`** — Scraped flights (price, dates, times, airline, booking URL, SHA-256 checksum for dedup)
- **`scrape_log`** — Execution history (status, flights found, new flights saved, errors)

A default config row (ARN → IOA, 2-month window) is inserted by the V2 migration.

## Deduplication

Each flight result gets a SHA-256 checksum computed from `price|departureDate|returnDate|airline|tripType`. If the same flight appears in a later scrape with the same price, it's skipped. If the price changes, a new row is created — this lets you track price changes over time.

## Project Structure

```
src/main/java/com/example/template/
├── TemplateApplication.java          # Entry point, @EnableScheduling
├── ProbeController.java              # GET /db-check health probe
├── config/
│   └── SchedulingConfig.java         # Thread pool for async scraping
├── controller/
│   ├── FlightController.java         # /api/flights endpoints
│   ├── SearchConfigController.java   # /api/config endpoints
│   └── ScrapeController.java         # /api/scrape endpoints
├── entity/
│   ├── SearchConfig.java             # Search parameters entity
│   ├── FlightResult.java             # Scraped flight data entity
│   └── ScrapeLog.java                # Scrape execution log entity
├── enums/
│   ├── TripType.java                 # ONE_WAY, RETURN, BOTH
│   └── ScrapeStatus.java             # RUNNING, SUCCESS, FAILED
├── repository/
│   ├── SearchConfigRepository.java
│   ├── FlightResultRepository.java   # Filtered search, cheapest, cleanup
│   └── ScrapeLogRepository.java
└── service/
    ├── ApolloScraperService.java     # Selenium scraping engine
    ├── FlightSearchService.java      # Orchestration, checksum, dedup
    ├── ScheduledScrapeService.java   # @Scheduled polling
    └── SearchConfigService.java      # Config CRUD

src/main/resources/
├── application.properties            # Base config + all scraper settings
├── application-local.properties      # Local: H2 file DB, non-headless Chrome
└── db/migration/
    ├── V1__initial_schema.sql
    └── V2__flight_scraper_schema.sql
```

## Docker

The Dockerfile installs Google Chrome for containerized Selenium:

```bash
docker build -t apollo-flight-scraper .
docker run -p 8080:8080 -v $(pwd)/data:/app/data apollo-flight-scraper
```

The `-v` flag mounts the data directory so the H2 database persists across container restarts.

## Cloud Deployment (GCP)

The app retains its Cloud Run compatibility. For cloud deployment:

```bash
# Build and push
gcloud builds submit --tag gcr.io/[PROJECT_ID]/apollo-flight-scraper .

# Deploy
gcloud run deploy apollo-flight-scraper \
  --image gcr.io/[PROJECT_ID]/apollo-flight-scraper \
  --allow-unauthenticated \
  --memory 1Gi
```

Note: Cloud Run instances are stateless, so the H2 file DB won't persist across cold starts. For production use, switch to Cloud SQL PostgreSQL.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | Server port |
| `CLOUD_SQL_CONNECTION_NAME` | — | Cloud SQL instance (for cloud deploy) |
| `CLOUD_SQL_DATABASE_NAME` | `sample_db` | Database name (for cloud deploy) |
| `DB_USER` | `postgres` | Database user (for cloud deploy) |
| `DB_PASS` | `password123` | Database password (for cloud deploy) |
