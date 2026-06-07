# Banking Service Repository Guide

## Project Overview

- Java 21 and Spring Boot 4 application built with Maven Wrapper.
- Main package: `com.kenc921.dxsp.simple_banking_service`.
- Persistence: Spring Data JPA, Hibernate, PostgreSQL, and Liquibase.
- Messaging: Spring Kafka with a transaction-specific dead-letter topic.
- Mapping: MapStruct. Boilerplate reduction: Lombok.
- Local infrastructure is defined in `docker-compose.yml`.

## Source Layout

- JPA entities and enums: `src/main/java/com/kenc921/dxsp/simple_banking_service/data`
- Feature code: packages such as `customer`, `transactions`, and `quote`
- Runtime configuration: `src/main/resources/application.yml`
- Local profile configuration: `src/main/resources/application-local.yml`
- Liquibase master: `src/main/resources/db/changelog/db.changelog-master.xml`
- Production schema changes: `src/main/resources/db/changelog/main`
- Local/test seed data: `src/main/resources/db/changelog/test`
- Tests mirror production packages under `src/test/java`
- Developer documentation: `documentations`

## Java Conventions

- Follow the existing package-by-feature structure. Keep shared persistence models in `data`.
- Use `OffsetDateTime` for database timestamps. Hibernate is configured to use UTC.
- Use `BigDecimal` for monetary values.
- Keep database table names singular and snake_case.
- Keep entity, column, relationship, and repository names aligned with the Liquibase schema.
- Use constructor injection. Lombok `@RequiredArgsConstructor` is the established pattern.
- Use MapStruct for non-trivial DTO/entity mapping rather than duplicating manual mapping logic.
- Put transaction boundaries on service methods that perform persistence work.
- Add focused tests for changed behavior. Prefer lightweight unit tests where a Spring context is unnecessary.
- Format Java with the configured fmt Maven plugin.

## Entity and Schema Synchronization

When changing an entity, inspect and update every affected layer:

1. JPA annotations, Java field types, enums, and relationships.
2. Spring Data repositories and custom query field names.
3. MapStruct mappers, DTOs, services, and tests.
4. Liquibase schema changelogs.
5. Test seed changelogs and developer documentation.

Do not rely on Hibernate to mutate the schema. `spring.jpa.hibernate.ddl-auto` is `validate`; Liquibase owns schema creation and migration.

## Liquibase Conventions

- Prefer standard Liquibase XML elements such as `createTable`, `column`, `addColumn`, `insert`, and constraint tags. Use raw `sql` only when XML cannot express the operation clearly.
- Main changelog filename format:
  `yyyyMMddHHmmss-00X-migration-name.xml`
- Test seed filename format:
  `yyyyMMddHHmmss-test-seed-00X-migration-name.xml`
- Files are executed lexicographically by `includeAll`; choose prefixes that preserve the required order.
- Main changelogs execute with context `main`.
- Test seed changelogs execute with context `test`; the local profile enables both `main` and `test`.
- For a file containing multiple changeSets, use ordered IDs such as:
  - `003-1-description`
  - `003-2-description`
  - `test-seed-001-1-description`
- Keep each changeSet ID unique together with its author and logical file path.
- Use preconditions where rerunning against a partially initialized disposable database should be tolerated.
- Use deterministic UUIDs and values in test seeds.
- For PostgreSQL timestamp seed values, use an explicitly typed expression, for example:
  `valueComputed="TIMESTAMPTZ '2026-01-01 00:00:00+00'"`
- For PostgreSQL array seed values, use a typed expression such as:
  `valueComputed="ARRAY['USD', 'EUR']::varchar(3)[]"`

### Checksum Safety

- Assume any committed main changeSet may already have run in another environment.
- Do not edit an applied changeSet merely to evolve the schema; add a new migration.
- Renaming a changelog changes its physical identity unless a stable `logicalFilePath` is retained.
- Editing an applied changeSet body causes a checksum mismatch unless Liquibase-specific remediation is deliberately applied.
- Test seed files are also tracked in `DATABASECHANGELOG`. Adding a new seed changeSet is safe; editing an already executed seed changeSet can cause a checksum mismatch.
- Only rewrite existing migrations when the user explicitly requires it and the affected databases are known to be disposable or will be reset.

## Test Transaction Seed Rules

- Keep transaction `value_date` values in ascending order.
- Use sequential descriptions: `Testing Transaction 1`, `Testing Transaction 2`, and so on.
- `CREDIT` transactions must have a strictly positive amount.
- `DEBIT` transactions must have a strictly negative amount.
- Zero-value transactions are invalid.
- Keep IDs deterministic and ensure seed preconditions cover all inserted IDs.

## Kafka Transaction Ingestion

- Source topic property: `banking-service.transactions.kafka.topic`.
- Dead-letter topic property: `banking-service.transactions.kafka.dead-letter-topic`.
- Consume raw string records so failures can log and republish the original payload.
- Deserialize into `CustomerBankTransactionIncomingMessageDto`, then run Jakarta Bean Validation.
- Resolve the account by `accountIban`; derive the customer from the account relationship.
- Map amount, currency, value date, description, and transaction ID through the existing mapper.
- Set `createdAt` at ingestion time using `OffsetDateTime.now()`.
- Derive direction from amount: positive is `CREDIT`, negative is `DEBIT`, and zero is rejected.
- Preserve the original key and raw payload when publishing to the dead-letter topic.
- Keep the transaction error handler scoped through
  `bankTransactionKafkaListenerContainerFactory`; do not make it the default handler for unrelated listeners.
- Error logs must include source topic, partition, offset, key, raw payload, and exception.
- Update `documentations/kafka_transaction_ingestion.md` when the payload or publishing workflow changes.

## Configuration Rules

- Default configuration enables only the Liquibase `main` context.
- The `local` profile enables both `main` and `test` contexts.
- Keep Kafka disabled by default unless the requested workflow requires listeners to start.
- Prefer environment-variable overrides for local connection settings.
- Do not commit new real credentials, tokens, private keys, or production connection details.

## Verification

Run the narrowest relevant checks first, then broaden when the change affects shared behavior:

```bash
./mvnw fmt:check
./mvnw test
./mvnw verify
```

Useful focused test form:

```bash
./mvnw -Dtest=CustomerBankTransactionIncomingMessageIngestionServiceTest test
```

For local integration verification:

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Use the destructive reset below only when explicitly requested because it deletes local database and pgAdmin volumes:

```bash
docker compose down --volumes --remove-orphans
docker compose up -d
```

When reporting verification, distinguish code failures from environment restrictions such as unavailable Docker access, blocked Maven cache writes, occupied ports, or JVM agent attachment limitations.

## Change Discipline

- Keep changes scoped to the requested behavior.
- Do not revert unrelated working-tree modifications.
- Do not modify generated files under `target`.
- Update documentation when developer-facing configuration, payloads, or local workflows change.
- Inspect existing implementations and tests before introducing a new abstraction or configuration pattern.
