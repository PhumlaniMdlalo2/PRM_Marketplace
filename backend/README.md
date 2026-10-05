# PRM Marketplace backend

Spring Boot 4 REST API for the student resale marketplace. MySQL owns the data, Flyway owns the
schema, and authentication is a stateless JWT.

## Requirements

- JDK 21 or newer
- Maven is not needed separately; use the wrapper (`./mvnw`, `mvnw.cmd` on Windows)
- MySQL 8, running locally on port 3306 — only to *run* the application. The test suite does not need
  it, apart from the opt-in migration test described below.

## Running it

```bash
# The application refuses to start without a signing key, so this is not optional.
export JWT_SECRET="$(openssl rand -base64 48)"
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
.\mvnw.cmd spring-boot:run
```

The API is then on `http://localhost:8080` and Swagger UI on
`http://localhost:8080/swagger-ui.html`.

## Configuration

Everything below is read from the environment. The defaults are chosen so that a fresh clone starts
on a laptop with a local MySQL; none of them are safe for a real deployment.

| Variable | Required | Default | Notes |
| --- | --- | --- | --- |
| `JWT_SECRET` | **yes** | none | At least 32 bytes. Startup fails on a missing, short, or previously-committed placeholder value rather than signing tokens with a key anyone can read from the repository. |
| `DB_URL` | no | `jdbc:mysql://localhost:3306/prm_marketplace?createDatabaseIfNotExist=true` | Note the default *creates* the database if it is absent, using whatever credentials below are set. |
| `DB_USERNAME` | no | `root` | |
| `DB_PASSWORD` | no | `password` | |
| `MAIL_HOST` | no | `localhost` | Defaults to port 1025, the Mailpit/MailHog convention. |
| `MAIL_PORT` | no | `1025` | |
| `MAIL_USERNAME` | no | `noreply@prm-marketplace.local` | Doubles as the From address, so setting it is what makes mail deliverable. |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:5173,http://localhost:3000` | Comma separated, explicit origins only. A `*` is refused at startup, and so is an empty list — see below. |
| `FACULTY_EMAIL` | no | none | See the next section. |
| `JWT_EXPIRATION_MS` | no | `86400000` | 24 hours. |

### CORS

The application allows credentialed requests, so a wildcard origin would let any site on the
internet make authenticated calls as a signed-in user. `*` is therefore rejected at startup. An empty
or blank list is rejected too, for a less obvious reason: it produces a configuration that permits
nothing, which starts cleanly and then fails every cross-origin request once traffic arrives, with an
error that points at the frontend rather than at configuration.

## Faculty accounts

Faculty supervises orders, refunds payments, resolves reports and verifies sellers. None of that is
reachable by self-registration — a self-chosen role would be an escalation — so **the application
ships with no faculty account at all**. That is the correct state for a fresh clone: the privileged
routes stay unreachable rather than being served by a login published in version control.

To promote an address at startup:

```bash
FACULTY_EMAIL=dean@example.ac.za ./mvnw spring-boot:run
```

An account that already exists is promoted; one that does not is created with a random password that
is deliberately never logged. Either way the account is claimed through the password-reset flow:

```bash
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H 'Content-Type: application/json' \
  -d '{"email":"dean@example.ac.za"}'
```

> **This depends on mail working.** The reset email is the only way in, and mail failures are logged
> and swallowed rather than failing the request. If the mail server is unreachable, a freshly
> provisioned faculty account cannot be claimed at all. `GET /actuator/health/readiness` reports this:
> it includes the mail indicator and answers 503 while mail is down. Liveness deliberately does not,
> since restarting the process would not fix mail.

## Health

| Endpoint | Purpose |
| --- | --- |
| `GET /actuator/health` | Aggregate. 503 if anything at all is down, including mail. |
| `GET /actuator/health/liveness` | Is this process working. **This is the path to probe.** Reports `{"status":"UP"}` and nothing else. |
| `GET /actuator/health/readiness` | Can this instance actually serve. Includes the database and mail. |

All three are public, because an orchestrator asking whether the service is up should not have to
hold a credential. Nothing else under `/actuator` is exposed; `/actuator/env` and `/actuator/beans`
answer 404 even to an authenticated caller.

SQL logging is off. `spring.jpa.show-sql` prints every statement, which is fine while debugging and
a steady stream of the whole schema — column names included — in production logs that are usually
shipped somewhere shared. It also ignores `logging.level.org.hibernate.SQL=warn`, so the level that
looks like it turns SQL logging off does nothing while it is enabled. Turn it on for a single run
when you need it.

One leftover: four `@DataJpaTest` slices (`CommentRepositoryTest`, `MessageRepositoryTest`,
`ProductSearchRepositoryTest`, `VendorProfileRepositoryTest`) still print their statements during a
test run. That is not this setting leaking — it survives `spring.jpa.show-sql=false` in the real
configuration, in `application-test.properties`, and as a JVM system property, so it comes from a
Hibernate logging channel that none of them reaches. Nothing else in the suite prints SQL.

## Tests

```bash
./mvnw test
```

Runs against an in-memory H2 database with the schema built from the entities, so the whole suite
runs in about a minute without MySQL. `application-test.properties` is layered on top of the real
configuration and redirects only the datasource; the profile is activated by a surefire system
property in `pom.xml`.

### The MySQL migration test

The suite above never executes a migration script, because V1 uses `enum(...)` columns and
`information_schema`, which H2 cannot parse. One opt-in test covers the real thing:

```bash
RUN_MYSQL_INTEGRATION_TESTS=true ./mvnw verify
```

> **Read this before running it.** The test creates a database called `prm_marketplace_flywaytest` on
> `localhost:3306` and **drops it** in teardown. The host is hardcoded; the credentials come from
> `DB_USERNAME`/`DB_PASSWORD`, which default to `root`/`password`. Do not point it at a shared or
> production-adjacent server. It is the only thing standing between a broken migration and production,
> so run it before any release that ships one.

## Schema

Flyway owns the schema and Hibernate only validates its mappings against it
(`ddl-auto=validate`), so the application can never quietly alter a table behind the migrations'
back. Migrations live in `src/main/resources/db/migration`.

`baseline-on-migrate` is enabled at version 1, which adopts an existing non-empty database that has no
`flyway_schema_history` table and skips V1. That is what lets the original development database keep
working. It also means pointing `DB_URL` at any pre-existing database whose tables happen to be
called `users`, `products` or `orders` will silently adopt it at version 1 and then apply V2 onward to
it. Check the target before starting the application against anything you did not create.