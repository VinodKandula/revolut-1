## Revolut Money Transfer

Implemention a RESTful API (including data model and the backing implementation) for money transfers between accounts.

Project is based on:
- com.sparkjava:spark-core
- com.google.code.gson:gson
- org.jooq:jooq
- com.h2database:h2

Design assumptions and limitations:
- No REST API versioning
- No timezones for database timestamps
- No results pagination
- No "self" or "cross" references in REST API responses
- Data type for money: `DECIMAL(19,2)`
- Failed transfers are not restarted
- Correctness of concurrent transfers is based on H2 MVCC engine and exclusive row locks, see [h2 mvcc](http://h2database.com/html/advanced.html#mvcc) and [h2 tx isolation](http://h2database.com/html/advanced.html#transaction_isolation)

## How to Build
    mvn clean package

## How to Run
    java -jar ./target/money-transfers-with-deps-1.0.jar

## End Points

### Accounts
    GET /accounts
    GET /accounts/:id
    GET /accounts/:id/transfers
    POST /accounts

### Transfers
    GET /transfers
    GET /trsnfers/:id
    POST /transfers




