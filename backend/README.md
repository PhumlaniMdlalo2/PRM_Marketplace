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
| `MAIL_HOST` | no | `smtp-relay.brevo.com` | Brevo SMTP relay host. |
| `MAIL_PORT` | no | `587` | SMTP submission port. |
| `MAIL_USERNAME` | no | `your-smtp-login` | Brevo SMTP login (usually the login or API key user, not necessarily the same as the From address). |
| `MAIL_PASSWORD` | no | none | Brevo SMTP key or password for the SMTP relay. |
| `MAIL_FROM` | no | `noreply@prm-marketplace.local` | Actual sender address on verification and password-reset emails. |
| `FRONTEND_URL` | no | `http://localhost:5173` | Frontend origin used for order tracking links in customer emails. Set this to the deployed frontend origin in production. |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:5173,http://localhost:3000` | Comma separated, explicit origins only. A `*` is refused at startup, and so is an empty list — see below. |
| `ADMIN_EMAIL` | no | `admin.vendra@gmail.com` | Admin account provisioned at startup. |

Orders can be placed for delivery or meetup. Delivery uses the buyer's saved address and includes an
estimated date five business days after checkout (weekends excluded); meetup arrangements are made
between buyer and seller through marketplace messages. Order confirmation and status emails contain
the marketplace order reference and a link to the in-app progress page. Progress reflects seller
updates in the marketplace; it is not live carrier or GPS tracking. Email delivery requires valid
SMTP configuration. A failed order email is logged and does not undo checkout or an order update.
| `ADMIN_PASSWORD` | no | generated | Initial password for the admin account. Keep it in local environment configuration, never in source control. |
| `PAYMENT_SIMULATION_ENABLED` | no | `false` | Enables buyer-selected sandbox success/failure results. Development only, never enable in production. |
| `JWT_EXPIRATION_MS` | no | `86400000` | 24 hours. |

## Signup and selling

Signup offers three account types:

- **Student:** must use an `.ac.za` or `.edu.za` email address and complete the emailed verification
  code. This supports academic institutions beyond CPUT.
- **Vendor:** requires a business/store name and may include a business registration number. Email
  verification is followed by admin review of the seller profile.
- **Community member:** uses any valid email address and is stored as a `RESIDENT` account.

Students keep the `STUDENT` role when they decide to sell. They apply for a seller profile in
Settings using the same business details as a vendor account. Seller profiles begin unverified, and
admin approval is required before a vendor or student seller can publish or sell listings. The
backend enforces approval for listing creation, public catalogue/detail reads, reactivation, and
checkout; it is not just a frontend badge.

Students can set a campus in their profile. The public catalogue accepts an optional `campus` search
filter to find sellers whose student profile matches that campus. Student discussion groups are
campus-scoped: students can create, join, and post only in groups for their current campus. Group
membership and campus are checked on the server for posts, comments, and likes; group threads do
not appear in the public bulletin. Campus is self-selected and is not independently verified by
the application.

### CORS

The application allows credentialed requests, so a wildcard origin would let any site on the
internet make authenticated calls as a signed-in user. `*` is therefore rejected at startup. An empty
or blank list is rejected too, for a less obvious reason: it produces a configuration that permits
nothing, which starts cleanly and then fails every cross-origin request once traffic arrives, with an
error that points at the frontend rather than at configuration.

## Admin accounts

Admin supervises orders, refunds payments, resolves reports and verifies sellers. None of that is
reachable by self-registration. The application provisions the configured admin address at startup
and never stores an initial password in source control.

The frontend provides a separate sign-in at `/admin/login` and an admin console at `/admin`.
The console includes report and seller review, plus account listing and deletion. Order status
changes and payment settlement remain API-only for now.

### Sandbox payments

There is no payment gateway connected. In local development, enable the explicit sandbox simulator:

```powershell
$env:PAYMENT_SIMULATION_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

Checkout records a pending `SANDBOX` payment and never asks for card details. On the order detail
page, the buyer can explicitly simulate success or failure and retry a failed attempt. This records
test state only; it does not transfer money. Keep `PAYMENT_SIMULATION_ENABLED=false` in production.
With simulation disabled, checkout still records a pending payment for manual admin verification.

The application provisions `admin.vendra@gmail.com` at startup by default. Set the initial
password through the environment before starting the backend:

```bash
ADMIN_PASSWORD='choose-a-strong-password' ./mvnw spring-boot:run
```

Set `ADMIN_EMAIL` only if you want a different admin address. A non-empty `ADMIN_PASSWORD` is
applied at startup to the configured admin account, including an account that is already admin.
Leave it unset to keep the existing password. Keep the setting in local environment configuration,
not source control.

An account that already exists is promoted; if it does not exist, it is created with the configured
initial password or, when none is supplied, a random password that is never logged. To use the
random-password fallback, claim the account through the password-reset flow:

```bash
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin.vendra@gmail.com"}'
```

> **This depends on mail working.** The reset email is the only way in. Reset-mail delivery failures
> are logged without changing the privacy-preserving response; if mail is unreachable, an admin
> account using the random-password fallback cannot be claimed until delivery works. Verification mail failures
> return 503, and the registration/resend request reports that delivery failed. Configure the Brevo
> SMTP credentials above and check `GET /actuator/health/readiness`, which includes the mail
> indicator and answers 503 while mail is down. Liveness deliberately does not, since restarting the
> process would not fix mail.

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