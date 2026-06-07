# Simple Banking Service

## Introduction

Simple Banking Service is an experimental Java 21 + Spring Boot backend that supports two core e-banking workflows:

1. Receiving new bank account transactions from Kafka and persisting them.
2. Returning an authenticated customer's historical transactions through a paginated REST API.

The application was built to fulfill the business and technical deliverables defined in
[Business Requirements for e-Banking Portal](documentations/requirements.md). Those requirements
describe a reusable API for customers with multiple bank accounts and currencies, transaction
histories spanning arbitrary calendar months, Kafka-based transaction ingestion, JWT-secured
access, currency conversion, container packaging, and Kubernetes/OpenShift deployment preparation.

The implementation uses:

- Spring Web for REST API delivery.
- Spring Security and OAuth 2.0 Resource Server for JWT authentication.
- Spring Kafka for transaction ingestion and dead-letter handling.
- Spring Data JPA and PostgreSQL for transactional persistence and paginated queries.
- Liquibase for controlled database schema evolution and local test data.
- MapStruct for entity and API model mapping.
- Springdoc OpenAPI for API schema generation and local Swagger UI.
- Docker, Kubernetes, and OpenShift resources for packaging and deployment preparation.

The transaction domain is described in detail in the
[Transactions Module documentation](documentations/transactions_module.md).

## Documentation Index

| Document | Purpose |
| --- | --- |
| [Business Requirements](documentations/requirements.md) | Original business requirements and expected deliverables |
| [Database Entities](documentations/database_entities.md) | Shared JPA entities, relationships, and persistence usage |
| [Transactions Module](documentations/transactions_module.md) | Detailed ingestion and listing implementation |
| [Kafka Transaction Ingestion](documentations/kafka_transaction_ingestion.md) | Kafka payloads, publishing, and dead-letter behavior |
| [JWT Authentication](documentations/jwt_authentication.md) | Authentication, authorization, and mock tokens |
| [Liquibase Database Migrations](documentations/liquibase_database_migrations.md) | Schema ownership, migration structure, naming, contexts, and checksum rules |
| [Docker & Deployment Related](documentations/docker_and_deployment_related.md) | Docker, local verification, Kubernetes, OpenShift, and WIP considerations |
| [Container Deployment](documentations/container_deployment.md) | Concise container and deployment reference |
| [Work Breakdown Structure](documentations/wbs.md) | Project task and delivery breakdown |

## 1. Data Model

The banking service stores its core business state in a small set of shared JPA entities under
`src/main/java/com/kenc921/dxsp/simple_banking_service/data`.

- `Customer` is the root identity record and the owner of accounts, roles, and transaction
  history.
- `CustomerBankAccount` stores each IBAN, its alias, and the set of supported currencies.
- `CustomerBankAccountTransaction` stores the persisted transaction ledger rows used by both
  ingestion and reporting.
- `CustomerRole` and `CustomerPrivilege` hold the database-backed authorization model used by JWT
  authentication.

The relationships are:

- one customer owns many bank accounts
- one customer owns many transactions through account ownership
- one customer can have many roles
- one role can grant many privileges
- one account can have many transactions

For the full entity summary and relation diagram, see
[Database Entities](documentations/database_entities.md).

## 2. Authentication and Authorization

The service operates as an OAuth 2.0 resource server. Clients call protected APIs with an
RSA-signed JWT:

```text
Authorization: Bearer <token>
```

Spring Security validates the token signature and timestamps. `CustomerJwtConverter` then extracts
the required `customer_id` claim and loads the customer, roles, and privileges from PostgreSQL.
Authorization is therefore based on current database records rather than trusting roles supplied
directly by the token.

The transaction listing endpoint requires the following authority:

```text
transactions:view
```

The authenticated customer identity is obtained from the Spring Security principal. The REST API
does not accept a customer identifier as a request parameter, preventing a caller from selecting
another customer's transaction history.

The local profile provides development-only RSA keys and a mock token generator. These keys must
not be reused in a production environment.

For the complete authentication flow, token claims, mock JWT generation, and failure behavior, see
[JWT Authentication](documentations/jwt_authentication.md).

## 3. Transactions Module

The transactions module implements the application's primary business capability. It is divided
into two workflows:

- Kafka-based transaction ingestion.
- Authenticated, paginated transaction history retrieval.

The module follows package-by-feature organization:

```text
src/main/java/com/kenc921/dxsp/simple_banking_service/transactions
├── config
├── controller
├── listener
├── model
├── repository
└── service
```

Shared JPA entities are located under:

```text
src/main/java/com/kenc921/dxsp/simple_banking_service/data
```

See [Transactions Module](documentations/transactions_module.md) for service flows, sequence
diagrams, persistence behavior, currency conversion, and requirement traceability.

### 3.1 Transaction Ingestion

`BankTransactionKafkaListener` consumes raw JSON records from the configured Kafka source topic.
The raw value is retained so that invalid messages can be logged and republished unchanged to the
transaction dead-letter topic.

The ingestion workflow:

1. Receives a Kafka record containing the transaction key and JSON payload.
2. Deserializes the payload into `CustomerBankTransactionIncomingMessageDto`.
3. Applies Jakarta Bean Validation to the message.
4. Rejects duplicate transaction identifiers and zero-value transactions.
5. Looks up the bank account using `accountIban`.
6. Derives customer ownership from the persisted account relationship.
7. Maps amount, currency, value date, description, and account details into the transaction entity.
8. Sets the ingestion timestamp using `OffsetDateTime.now()`.
9. Derives `CREDIT` for a positive amount and `DEBIT` for a negative amount.
10. Persists the transaction within a database transaction.

Messages that cannot be deserialized, validated, resolved, or persisted are handled by a
transaction-specific Kafka error handler. The handler logs the source topic, partition, offset,
key, raw payload, and exception before publishing the original record to the dead-letter topic.

Default topic properties:

```text
Source:      account-transactions
Dead letter: account-transactions-dlq
```

Payload examples and Kafka UI publishing instructions are available in
[Kafka Transaction Ingestion](documentations/kafka_transaction_ingestion.md).

### 3.2 Transaction Listing API

The historical transaction API is exposed as:

```http
GET /api/v1/transactions
```

Required and optional query parameters:

| Parameter | Purpose |
| --- | --- |
| `year` | Calendar year to query |
| `month` | Calendar month from 1 through 12 |
| `page` | Zero-based page number; defaults to `0` |
| `size` | Number of records per page; defaults to `20` |
| `majorDisplayCurrency` | Three-letter currency used for converted values and page totals |

Example:

```bash
curl \
  -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/transactions?year=2026&month=1&page=0&size=20&majorDisplayCurrency=USD"
```

The service:

- Uses the authenticated customer's UUID to scope the database query.
- Converts the requested `YearMonth` into explicit UTC date boundaries.
- Retrieves only the requested page from PostgreSQL.
- Orders records deterministically by value date and transaction ID.
- Resolves exchange-rate quotes for currencies present on the page.
- Converts transaction values into the selected major display currency.
- Calculates separate credit and debit totals for the returned page.
- Returns transaction content together with pagination metadata.

The current `DummyQuoteService` supplies a fixed rate and represents the integration boundary for
an external exchange-rate provider. A production implementation must provide real rates and define
provider authentication, timeout, retry, caching, and failure-handling policies.

Detailed controller, service, repository, mapping, and aggregation behavior is documented in
[Paginated Transaction Listing](documentations/transactions_module.md#2-paginated-transaction-listing).

## 4. Data and Database Migrations

PostgreSQL stores customers, accounts, authorization data, and transaction history. Hibernate is
configured with `ddl-auto: validate`; it validates entity-to-schema compatibility but does not
create or modify the schema.

Liquibase is the project's database schema migration and version-control mechanism. It represents
each database change as an ordered changeset stored with the application source. During startup,
Liquibase compares the configured changelog with the target database and applies only eligible
changesets that have not already executed.

Liquibase owns schema evolution:

```text
src/main/resources/db/changelog
├── db.changelog-master.xml
├── main
└── test
```

- The `main` context contains application schema migrations.
- The `test` context contains deterministic local test data.
- The default application configuration runs only `main`.
- The `local` profile runs both `main` and `test`.

This approach provides repeatable database setup, incremental upgrades, reviewable schema history,
environment-specific test data, and detection of unexpected changes to applied migrations.
Liquibase records execution history and checksums in `DATABASECHANGELOG` and coordinates concurrent
migration attempts with `DATABASECHANGELOGLOCK`. Both tracking tables are created automatically
when Liquibase first runs against a database.

The context separation ensures that local test records are not inserted into normal deployed
environments. Applied changesets should be treated as immutable; schema evolution should normally
be implemented by adding a new migration rather than editing an existing one.

For migration naming conventions, execution order, XML authoring rules, checksum behavior, review
guidance, and official references, see
[Liquibase Database Migrations](documentations/liquibase_database_migrations.md). Repository-level
conventions are also defined in [AGENTS.md](AGENTS.md#liquibase-conventions).

## 5. Local Development

Reset the local Docker environment before starting development:

```bash
docker compose down --volumes --remove-orphans && docker compose up -d
```

This command is copied from `testing_tools/reset_docker_command.txt`. It deletes the local
PostgreSQL and pgAdmin volumes, including all locally persisted database state, and then recreates
the supporting services. Do not run it when local data must be retained.

After Docker reports the supporting services as healthy, start the application from IntelliJ IDEA:

1. Open **Run > Edit Configurations**.
2. Select the shared **BankServiceApplication (local)** Spring Boot configuration.
3. Run or debug the configuration.

The shared configuration is stored at
`.idea/runConfigurations/BankServiceApplication__local_.xml`. It starts
`BankingServiceApplication` with the `local` Spring profile, enabling the local datasource, Kafka
settings, Swagger UI, development JWT keys, and Liquibase `main` plus `test` contexts.

Useful local endpoints:

| Service | Address |
| --- | --- |
| Banking API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Kafka UI | `http://localhost:8081` |
| pgAdmin | `http://localhost:5050` |
| Liveness | `http://localhost:8080/actuator/health/liveness` |
| Readiness | `http://localhost:8080/actuator/health/readiness` |

For a complete Docker-based workflow, including image construction, Compose networking, mock JWT
generation, Postman testing, and environment variables, see
[Docker & Deployment Related](documentations/docker_and_deployment_related.md).

## 6. Build and Verification

For a fast local artifact build without running tests:

```bash
./mvnw clean install -DskipTests
```

This command compiles the application and test sources, packages the executable JAR, and installs
the artifact into the local Maven repository. Because `-DskipTests` suppresses test execution, it
is a build command rather than evidence that the application has been verified.

Before review, merge, or release, run:

```bash
./mvnw clean verify
```

`verify` executes the configured tests and Maven lifecycle checks. Skipping tests should be limited
to cases where a fast packaging result is explicitly required.

## 7. Deployment

> **Work in progress:** The deployment resources are an initial technical baseline and are not
> production-ready. They may require changes before they run in a specific Kubernetes or OpenShift
> environment because cluster networking, image registries, credentials, security policies,
> storage, Kafka, PostgreSQL, and platform conventions differ between environments.

The repository contains an initial deployment baseline:

```text
deploy
├── kubernetes
│   ├── configmap.yml
│   ├── deployment.yml
│   ├── secret.example.yml
│   └── service.yml
└── openshift
    └── route.yml
```

The manifests demonstrate environment-variable configuration, non-root execution, resource
requests and limits, rolling updates, graceful shutdown, health probes, a cluster-internal Service,
and an OpenShift edge-TLS Route.

These resources are a **work in progress** and require environment-specific production hardening.
Image registry policy, managed secrets, identity-provider integration, database migration
ownership, Kafka security, observability, scaling, resilience, and network controls must be
validated before production use.

See [Docker & Deployment Related](documentations/docker_and_deployment_related.md) for deployment
behavior, application commands, and the production-readiness checklist. A shorter container
reference is also available in [Container Deployment](documentations/container_deployment.md).
