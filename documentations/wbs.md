
# Work breakdown

## DB Setup
- Design DB schema with the following table:
1. user
2. bank_account
3. bank_account_transaction

- Prepare liquibase migration script
- Prepare entity classes
- Prepare PSQL db docker
- make sure the migration scripts works upon running

## Create kafka tool
- create kafka docker
- add spring config
- create java event listener

## Redis setup
- pom
- yml config
- handle rate for testing

## Java service classes creation 
- Create the following classes under config package:
  - HttpSecurityConfig
- Create the following classes under controller package:
    - TransactionController
- Create the following classes under service package:
    - TransactionService
    - RateService
- Create the following classes under repository package:
    - TransactionRepository
- Create the following classes under event package:
    - HttpSecurityConfig
- Create the following classes under config package:
    - TransactionEventListener

## API related
- Define request & response format
- Create the end point
- build business logic


## Security
- Handle HTTP security config to allow extraction of User ID from JWT



