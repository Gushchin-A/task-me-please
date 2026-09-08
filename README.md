# Task Me Please

Task Me Please is a server-rendered task tracker for personal and team work. It supports teams, invitations,
tasks, comments, dynamic team tags, task roles and archived work.

This is an educational project developed through hands-on work with assistance from coding agents. Product
decisions, review and final integration remain human-directed.

## Features

- registration, email verification, login and password reset;
- personal and team task lists;
- teams, member roles, invitations and member removal or self-leave;
- task author and assignee, status, deadline and dynamic team tags;
- comments, archiving and restoring tasks;
- transactional email notifications for account, team and task events.

## Stack

- Java 21 and Spring Boot;
- Spring MVC and Spring Security;
- PostgreSQL, jOOQ and Flyway;
- JTE server-side templates;
- HTML, Tailwind CSS, htmx and Alpine.js where they fit the existing interface;
- Docker Compose for local PostgreSQL;
- Brevo Transactional Email API over HTTPS.

The visual language is based on [GitHub Primer Primitives](https://primer.style/). Primer assets are stored in
the repository and served locally; the application does not fetch styles from GitHub, Primer or a CDN at runtime.

## Public deployment

The application and PostgreSQL database are deployed on Render. The public instance uses Render's free tier, so
the first request after inactivity may take a little longer while the service wakes up.

## Run locally

Requirements:

- Java 21;
- Docker and Docker Compose.

Create a local environment file, start PostgreSQL and run the application:

```bash
cp .env.example .env
docker compose up -d
set -a
source .env
set +a
./gradlew bootRun
```

The application is available at `http://localhost:8181` by default.

To stop the local database:

```bash
docker compose down
```

## Environment variables

`.env` is ignored by Git and must never contain committed secrets. `.env.example` contains the complete set of
variables used by the application with safe development values or placeholders.

| Variable | Purpose |
| --- | --- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL. |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL user. |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password. |
| `APP_BASE_URL` | Public base URL used in verification, invitation and password-reset links. Set it to the Render URL in production. |
| `PORT` | HTTP port. Render provides it automatically; use `8181` locally. |
| `BREVO_API_KEY` | Brevo API key for transactional email. Keep it empty when email delivery is not needed locally. |
| `BREVO_API_URL` | Brevo Transactional Email API endpoint. |
| `BREVO_CONNECT_TIMEOUT` | Connection timeout for the Brevo API. |
| `BREVO_READ_TIMEOUT` | Response timeout for the Brevo API. |
| `MAIL_FROM` | Sender address verified in Brevo. |
| `REMEMBER_ME_KEY` | Long random secret for persistent login cookies. |
| `JTE_DEVELOPMENT_MODE` | Enables JTE development mode locally. |

For real local email testing, configure `BREVO_API_KEY`, `MAIL_FROM`, `APP_BASE_URL` and
`REMEMBER_ME_KEY` in your untracked `.env`. Use a mailbox you can access and complete the relevant confirmation
flow. Automated tests mock the HTTP boundary and do not send emails to Brevo.

## Verification

Run the full project checks:

```bash
./gradlew clean build
```

The build runs tests, Checkstyle, PMD and Spotless verification.
