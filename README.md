# Task Me Please 💻

**Personal task manager project currently in active development.**

Backend application for team task management with users, teams, invitations, tasks and comments.

The project is focused on building a production-style REST architecture with Spring Boot, jOOQ, PostgreSQL and server-side rendering.

#### Current progress
- user management
- teams and team members
- invitations
- tasks and comments domain models
- Flyway migrations
- jOOQ repositories
- service layer
- Dockerized PostgreSQL environment

#### Backend Stack
- Java 21
- Spring Boot
- Spring Security
- PostgreSQL
- jOOQ
- Flyway
- Docker
- JTE

#### Frontend Stack
- HTML
- Tailwind CSS
- htmx
- Alpine.js

---

### Project launch

Create a local environment file and set a real Brevo API key only when email delivery needs to be tested:

```bash
cp .env.example .env
docker compose up -d
set -a
source .env
set +a
./gradlew bootRun
```

Transactional emails are sent through the Brevo API over HTTPS. The required production values are:

- `BREVO_API_KEY` — Brevo API key; never commit the real value;
- `MAIL_FROM` — verified Brevo sender, currently `no-reply@taskmeplease.online`;
- `APP_BASE_URL` — public application URL used in verification, invitation and password reset links.

Optional settings are `BREVO_API_URL`, `BREVO_CONNECT_TIMEOUT` and `BREVO_READ_TIMEOUT`. SMTP variables are no
longer used.

Automated tests use a mock HTTP server and never call Brevo. To verify real delivery locally, start the application
with the environment above and complete registration, email verification and password reset flows with an address
you can access.
