# Kafka Transaction Ingestion

## Topics

The local Docker Compose environment creates these topics:

- Source topic: `account-transactions`
- Dead-letter topic: `account-transactions-dlq`

The application consumes source messages as raw JSON strings. A message that cannot be parsed,
validated, or persisted is published unchanged to the dead-letter topic.

## Payload Format

Each message value must be a JSON object with this structure:

```json
{
  "transactionId": "30000000-0000-0000-0000-000000000001",
  "accountIban": "GB29NWBK60161331926819",
  "amount": 250.75,
  "currency": "USD",
  "valueDate": "2026-02-01T09:30:00Z",
  "description": "Testing Kafka Transaction"
}
```

Field requirements:

| Field | Requirement |
| --- | --- |
| `transactionId` | Required UUID. It must not already exist in the database. |
| `accountIban` | Required nonblank IBAN that already exists in `customer_bank_account`. |
| `amount` | Required non-zero decimal. Positive is credit; negative is debit. |
| `currency` | Required three-letter uppercase currency code. |
| `valueDate` | Required ISO-8601 timestamp with an offset, such as `Z` or `+08:00`. |
| `description` | Optional text with a maximum length of 255 characters. |

Use the transaction UUID as both the Kafka message key and the payload `transactionId`.

## Credit Payload

```json
{
  "transactionId": "30000000-0000-0000-0000-000000000001",
  "accountIban": "GB29NWBK60161331926819",
  "amount": 250.75,
  "currency": "USD",
  "valueDate": "2026-02-01T09:30:00Z",
  "description": "Kafka credit transaction"
}
```

The application stores this transaction with direction `CREDIT`.

## Debit Payload

```json
{
  "transactionId": "30000000-0000-0000-0000-000000000002",
  "accountIban": "GB29NWBK60161331926819",
  "amount": -75.25,
  "currency": "USD",
  "valueDate": "2026-02-02T09:30:00Z",
  "description": "Kafka debit transaction"
}
```

The application stores this transaction with direction `DEBIT`.

## Publish With Kafka UI

1. Start the local infrastructure with `docker compose up -d`.
2. Start the application with the `local` Spring profile.
3. Open [http://localhost:8081](http://localhost:8081).
4. Select the `local` cluster.
5. Open **Topics**, then select `account-transactions`.
6. Select **Produce Message**.
7. Enter the payload transaction UUID in the message key field.
8. Paste one of the JSON payloads above into the message value field.
9. Publish the message.
10. Verify the row in `customer_bank_account_transaction`.

To inspect failures, open `account-transactions-dlq` in Kafka UI. The dead-letter record retains the
original key and raw JSON value, with Kafka headers describing the source record and exception.

## Dead-Letter Conditions

The message is sent to the dead-letter topic without retries when:

- The JSON is malformed or cannot be deserialized.
- A required field is absent or invalid.
- The amount is zero.
- The transaction UUID already exists.
- The account IBAN does not exist.
- Database persistence fails.

The application logs the exception together with the complete raw message before publishing it to
the dead-letter topic. If dead-letter publication itself fails, the source message is not silently
treated as successfully recovered.
