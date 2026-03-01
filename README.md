# GCP Spring Boot Template

A production-ready Spring Boot template designed for fast deployment to Google Cloud Platform (Cloud Run & Cloud SQL). Includes a lightweight JDBC persistence example.

## Features
- **Spring Boot 3.4.1** & **Java 21**.
- **PostgreSQL Persistence**: Uses `JdbcTemplate` for lightweight, direct SQL interaction.
- **Database Migrations (Flyway)**: Automated, versioned schema management out of the box.
- **Cloud SQL Integration**: Native support via Spring Cloud GCP.
- **Zero-Conf Deployment**: Optimized `Dockerfile` for Cloud Run.
- **Local Dev Support**: Pre-configured H2 database profile with Flyway support.

## Quick Start (Cloud)

### 1. Build and Push
```bash
gcloud builds submit --tag gcr.io/[PROJECT_ID]/gcp-template .
```

### 2. Deploy to Cloud Run
```bash
gcloud run deploy gcp-service \
  --image gcr.io/[PROJECT_ID]/gcp-template \
  --add-cloudsql-instances [PROJECT_ID]:[REGION]:[INSTANCE_NAME] \
  --allow-unauthenticated
```

## Local Development

Run the application locally using the `local` profile (uses in-memory H2):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Database Migrations
This project uses **Flyway** for database migrations. 
- Place all migration scripts in: `src/main/resources/db/migration/`
- Use the versioning format: `V1__description.sql`, `V2__another_change.sql`.

### API Endpoints
- `GET /`: Health check.
- `POST /messages`: Save a message (send raw text in body).
- `GET /messages`: List all saved messages.

## Project Structure
- `src/main/resources/db/migration/`: All SQL schema and data migrations.
- `application-local.properties`: Development overrides for local H2 use.
- `Dockerfile`: Multi-stage build for minimal container size.

## Cloud SQL Setup

Before deploying, you need to create a PostgreSQL instance on Google Cloud.

### 1. Enable APIs
```bash
gcloud services enable sqladmin.googleapis.com
```

### 2. Create Instance
```bash
gcloud sql instances create [INSTANCE_NAME] \
    --database-version=POSTGRES_15 \
    --tier=db-f1-micro \
    --region=[REGION]
```

### 3. Create Database
```bash
gcloud sql databases create [DATABASE_NAME] --instance=[INSTANCE_NAME]
```

### 4. Create User
```bash
gcloud sql users create [DB_USER] --instance=[INSTANCE_NAME] --password=[DB_PASS]
```

> [!NOTE]
> The default configuration in `application.properties` uses `sample_db` as the database name and `postgres` as the user. Ensure your Cloud Run service account has the **Cloud SQL Client** role.

## Configuration
The following environment variables are supported for Cloud SQL:
- `CLOUD_SQL_CONNECTION_NAME`: The full connection string (`project:region:instance`).
- `CLOUD_SQL_DATABASE_NAME`: Name of the database.
- `DB_USER`: Database username.
- `DB_PASS`: Database password.
