## Revolut Money Transfer

Implementation a RESTful API (including data model and the backing implementation) for money transfers between accounts.

Project is based on:
- com.sparkjava:spark-core
- com.google.code.gson:gson
- org.jooq:jooq
- org.hsqldb:hsqldb

Design assumptions and limitations:
- No REST API versioning
- No timezones for database timestamps
- No results pagination
- No "self" or "cross" references in REST API responses
- Data type for money: `DECIMAL(19,2)`
- Failed transfers are not restarted
- Correctness of concurrent transfers is based on READ COMMITTED isolation level, which is default and `keeps write locks on tables until commit, but releases the read locks after each operation`, see [HSQLDB locking](http://hsqldb.org/doc/2.0/guide/sessions-chapt.html#snc_tx_2pl)

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




