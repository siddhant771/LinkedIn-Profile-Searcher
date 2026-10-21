# Alumni LinkedIn Profile Searcher

A Java Spring Boot REST API designed to search alumni profiles through
PhantomBuster and store matching results in PostgreSQL.

## Technology Stack

- Java 17 and Spring Boot
- PostgreSQL and JdbcTemplate
- Flyway database migrations
- Swagger / OpenAPI
- JUnit and Mockito
- Docker Compose

## API Endpoints

### POST /api/alumni/search

Accepts a university, designation and optional graduation year.

Example request:

{
  "university": "VIT AP University",
  "designation": "Software Engineer",
  "passoutYear": 2026
}

### GET /api/alumni/all?page=0&size=10

Returns saved profiles with pagination.

## Local Setup

1. Install and start Docker Desktop.
2. Copy `.env.example` to `.env`.
3. Add your PhantomBuster credentials to `.env`.
4. From the project root, run:

    docker compose up --build -d

Local Swagger UI:

http://localhost:8081/swagger-ui.html

Local saved profiles API:

http://localhost:8081/api/alumni/all?page=0&size=10

View application logs:

    docker compose logs --tail=80 app

Stop the application:

    docker compose down

Real credentials must not be committed to Git.

## Design

- Controller handles HTTP requests and request validation.
- Service filters matching profiles and removes duplicate LinkedIn URLs.
- PhantomClient communicates with PhantomBuster.
- Repository uses PostgreSQL upsert queries to insert or update profiles.
- Flyway manages database schema migrations.

## Testing

The three AlumniServiceTest unit tests passed during the local Docker build.

The Dockerfile runs Maven verification:

    mvn -B verify

These unit tests do not verify the live provider integration or database
persistence.

## Deployment Status

A public deployment URL and public Swagger URL are not available
with this submission.

During deployment, Render requested payment-card details, which I
could not provide. I also attempted Railway deployment, but its
dashboard displayed a service incident stating that the dashboard
and API were unavailable and builds were paused.

The application runs locally using Docker Compose. The localhost
links above are for local testing and are not publicly accessible.

Public deployment remains incomplete.

## PhantomBuster Integration Status

Requests from the application reached PhantomBuster. Its execution
logs confirmed successful authentication with the connected LinkedIn
account, followed by:

"Monthly search limits reached for 1 account"

The Phantom finished with exit code 1.

Because of this limit, successful live profile retrieval, result-field
mapping and saving retrieved profiles into PostgreSQL remain unverified.
The configured search argument also requires verification against the
Phantom's actual input configuration.

No mock profiles are presented as successfully retrieved LinkedIn data.

## Submission Note

This submission includes the implementation, service unit tests,
Docker configuration and database migration.

Public deployment and successful end-to-end live search are outstanding
requirements. The limitations are documented here for reviewer clarity.
