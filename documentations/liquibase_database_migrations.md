# Liquibase Database Migrations

## Overview

Liquibase is the database schema migration tool used by Banking Service. It describes database
changes as version-controlled changelogs and applies only changesets that have not already run
against a target database.

The application enables Liquibase during Spring Boot startup. Hibernate is configured with
`spring.jpa.hibernate.ddl-auto: validate`, so Hibernate verifies that the mapped entities match the
database but does not create or alter the schema. Liquibase remains the single owner of schema
evolution.

This separation is deliberate:

- JPA entities define the application's persistence model.
- Liquibase changelogs define how an existing database moves safely between schema versions.
- Hibernate validation detects drift between the two models during startup.

## Why Liquibase Is Used

Liquibase provides several advantages over ad hoc SQL execution or automatic Hibernate schema
updates:

- **Repeatable environment setup:** the same ordered changelogs can initialize local, test, and
  deployed databases.
- **Incremental upgrades:** an existing database receives only changesets that have not previously
  executed.
- **Version control:** schema history is reviewed and released with application source code.
- **Auditability:** `DATABASECHANGELOG` records the changeset ID, author, file path, execution time,
  execution type, context, Liquibase version, and checksum.
- **Concurrency control:** `DATABASECHANGELOGLOCK` prevents multiple application instances from
  changing the schema concurrently.
- **Change integrity:** stored checksums detect unexpected modification of changesets that have
  already run.
- **Environment-specific data:** contexts allow local test seeds to be excluded from normal
  deployments.
- **Database-aware change types:** XML elements such as `createTable`, `addColumn`, and
  `addForeignKeyConstraint` express intent while Liquibase generates the appropriate SQL.

Liquibase automatically creates `DATABASECHANGELOG` and `DATABASECHANGELOGLOCK` when they do not
exist.

## Changelog Structure

```text
src/main/resources/db/changelog
├── db.changelog-master.xml
├── main
│   └── yyyyMMddHHmmss-00X-migration-name.xml
└── test
    └── yyyyMMddHHmmss-test-seed-00X-migration-name.xml
```

Spring Boot loads the root changelog configured by:

```yaml
spring:
  liquibase:
    change-log: db/changelog/db.changelog-master.xml
```

The master changelog uses `includeAll`:

```xml
<includeAll
        path="main"
        relativeToChangelogFile="true"
        contextFilter="main"/>

<includeAll
        path="test"
        relativeToChangelogFile="true"
        contextFilter="test"
        endsWithFilter=".xml"/>
```

Liquibase executes files selected by `includeAll` in alphabetical order. The timestamp and
sequence components in this repository's filename convention therefore control migration order.

## Contexts

The default application configuration enables only:

```yaml
spring:
  liquibase:
    contexts:
      - main
```

The `local` profile enables:

```yaml
spring:
  liquibase:
    contexts:
      - main
      - test
```

Consequently:

- Normal deployments execute schema and reference-data changes under `main`.
- Local development executes `main` first and then deterministic test seeds under `test`.
- Test seed data is not inserted into environments that enable only `main`.

The context must be explicitly configured. Running Liquibase without a context filter can make
context-tagged changes eligible for execution.

## Migration Naming Conventions

The repository conventions are defined in [AGENTS.md](../AGENTS.md#liquibase-conventions) and must
be followed for new changelogs.

### Main Migration Files

```text
yyyyMMddHHmmss-00X-migration-name.xml
```

Example:

```text
20260606234636-001-create-data-entities.xml
```

### Test Seed Files

```text
yyyyMMddHHmmss-test-seed-00X-migration-name.xml
```

Example:

```text
20260606234636-test-seed-001-local-test-data.xml
```

Use a current creation timestamp and increment `00X` when multiple files share that timestamp.
Names must remain lexicographically sortable in the intended execution order.

### Changeset IDs

For a main changelog with one changeset:

```text
001-create-data-entities
```

For a main changelog with multiple changesets:

```text
003-1-create-customer-authorization-tables
003-2-customer-view-only-role-setup
```

For a test seed changelog with multiple changesets:

```text
test-seed-001-1-customer
test-seed-001-2-account
test-seed-001-3-transactions
```

A changeset is identified by its `id`, `author`, and changelog file path. Keep this combination
unique and stable.

## Authoring Guidelines

Prefer standard Liquibase XML change types:

```xml
<changeSet id="004-add-example-column" author="developer-name">
    <addColumn tableName="customer">
        <column name="example_value" type="varchar(100)">
            <constraints nullable="true"/>
        </column>
    </addColumn>
</changeSet>
```

Use `createTable`, `column`, `addColumn`, `insert`, index, and constraint elements when they can
express the operation clearly. Use raw SQL only when the XML change types cannot represent the
required PostgreSQL behavior without making the migration harder to understand.

Additional repository rules:

- Keep table and column names singular and `snake_case`.
- Keep Liquibase definitions synchronized with JPA entities and repository queries.
- Use `OffsetDateTime` in Java and `timestamp with time zone` in PostgreSQL.
- Use deterministic UUIDs and values in test seeds.
- Use explicit PostgreSQL expressions for seed timestamps, for example:
  `valueComputed="TIMESTAMPTZ '2026-01-01 00:00:00+00'"`.
- Use typed expressions for PostgreSQL arrays, for example:
  `valueComputed="ARRAY['USD', 'EUR']::varchar(3)[]"`.
- Keep transaction seed value dates in ascending order.
- Keep credit amounts positive, debit amounts negative, and reject zero-value transactions.
- Add preconditions where they meaningfully protect disposable databases from partial seed setup.

## Applied Changesets and Checksums

Treat an applied changeset as immutable.

When Liquibase executes a changeset, it stores its checksum in `DATABASECHANGELOG`. On later
starts, Liquibase compares the current changeset checksum with the stored value. A material change
to an applied changeset can stop startup with a checksum validation error.

For normal schema evolution:

1. Do not rewrite an applied changeset.
2. Add a new migration that transforms the existing schema.
3. Keep the new migration backward and operationally safe for environments being upgraded.

Adding a new changeset does not invalidate old checksums. Liquibase recognizes it as undeployed and
executes it during the next eligible update.

Renaming or moving a changelog can change the file-path component of changeset identity. Existing
files in this repository use `logicalFilePath` where migration identity needs to remain stable
independently of the physical timestamped filename.

Do not use `clear-checksums`, manually edit `DATABASECHANGELOG`, or add `validCheckSum` as a routine
fix. Those actions require an explicit migration decision and coordinated handling across every
affected environment.

Test seed changesets are also tracked in `DATABASECHANGELOG`. Adding a new seed changeset is safe;
editing an already executed seed can produce the same checksum problem as editing a main
changeset.

## Startup and Failure Behavior

At application startup, Liquibase:

1. Connects through the configured Spring datasource.
2. Acquires `DATABASECHANGELOGLOCK`.
3. Reads `DATABASECHANGELOG`.
4. Resolves changelogs and active contexts.
5. Validates applied changeset checksums.
6. Executes eligible undeployed changesets in order.
7. Records successful executions in `DATABASECHANGELOG`.
8. Releases the lock.

If a migration fails, application startup fails rather than allowing the service to run against an
unknown or incompatible schema. Investigate the migration and database state before restarting.
Do not bypass the failure by enabling Hibernate schema generation.

## Developer Review Checklist

Before submitting a database change:

1. Confirm the migration filename and changeset IDs follow `AGENTS.md`.
2. Confirm execution order relative to all existing files.
3. Prefer Liquibase XML change types over raw SQL.
4. Confirm JPA entities, mappings, repositories, and tests match the resulting schema.
5. Confirm the changeset is additive rather than an edit to an applied migration.
6. Consider existing data, nullability, defaults, indexes, constraints, locks, and rollback needs.
7. Verify `main` versus `test` context placement.
8. Start from a clean local database and confirm the full changelog executes.
9. Test an upgrade from the previous schema state when the change is intended for a persistent
   environment.

## External References

- [Liquibase: What is a changelog?](https://docs.liquibase.com/pro/user-guide/what-is-a-changelog)
- [Liquibase: What is a changeset?](https://docs.liquibase.com/concepts/basic/changeset.html)
- [Liquibase: `includeAll`](https://docs.liquibase.com/reference-guide/changelog-attributes/includeall)
- [Liquibase: Contexts](https://docs.liquibase.com/reference-guide/changelog-attributes/what-are-contexts)
- [Liquibase: `DATABASECHANGELOG`](https://docs.liquibase.com/concepts/basic/databasechangelog-table.html)
- [Liquibase: `DATABASECHANGELOGLOCK`](https://docs.liquibase.com/concepts/tracking-tables/databasechangeloglock-table.html)
- [Liquibase: Changeset checksums](https://docs.liquibase.com/concepts/changelogs/changeset-checksums.html)
