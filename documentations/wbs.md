
# Work breakdown

## DB Setup
- Design DB schema with the following table:
1. customer
2. customer_bank_account
3. customer_bank_account_transaction

- Prepare liquibase migration script
- Prepare entity classes
- Prepare PSQL db docker
- make sure the migration scripts works upon running

## Create kafka tool
- create kafka docker
- add spring config
- create java event listener

## Java service classes creation 
- Create the following classes under config package:
  - HttpSecurityConfig
- Create the following classes under controller package:
    - CustomerBankAccountTransactionController
- Create the following classes under service package:
    - CustomerBankAccountTransactionService
    - QuoteService
- Create the following classes under repository package:
    - CustomerBankAccountTransactionRepository
- Create the following classes under event package:
    - HttpSecurityConfig
- Create the following classes under config package:
    - BankTransactionKafkaListener

## API related
- Define request & response format
- Create the end point
- build business logic


## Security
- Handle HTTP security config to allow extraction of Customer ID from JWT



