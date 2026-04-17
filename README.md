# Database-Backed Sequence Generator

A small Spring Boot + JPA service that hands out unique sequence numbers per item type (`ORDER`, `USER`, `INVOICE`, …), safe under concurrent callers across threads, JVMs, and restarts.

## Requirements

- Java 21+
- Maven 3.8+

H2 is used out of the box — no database to install.

## Build & test

```bash
mvn test
```

This runs:

- `DbSequenceGeneratorTest` — happy path, per-type independence, unknown-type error.
- `ConcurrencyTest` — 16 threads × 500 calls = 8 000 concurrent `nextValue("ORDER")` invocations; asserts all returned values are unique and contiguous. This is the load-bearing test for the locking design.

## Usage

The entire public API is one method:

```java
public interface SequenceGenerator {
    long nextValue(String sequenceType);
}
```

From any Spring-managed bean:

```java
@Service
public class OrderService {
    private final SequenceGenerator sequences;

    public OrderService(SequenceGenerator sequences) {
        this.sequences = sequences;
    }

    public Order createOrder(...) {
        long orderId = sequences.nextValue("ORDER");
        // ...
    }
}
```

Registered types (seeded in `data.sql`): `ORDER`, `USER`, `INVOICE`. Adding a new type = one row in `sequence_registry`.

## Design

### Schema

A single table:

```sql
CREATE TABLE sequence_registry (
    sequence_type VARCHAR(64) PRIMARY KEY,
    current_value BIGINT      NOT NULL,
    increment_by  BIGINT      NOT NULL DEFAULT 1
);
```

### How `nextValue(type)` works

1. Open a new transaction (`REQUIRES_NEW`, `READ_COMMITTED`).
2. `SELECT ... FROM sequence_registry WHERE sequence_type = ? FOR UPDATE`
   (Spring Data JPA `@Lock(PESSIMISTIC_WRITE)`).
3. `current_value += increment_by`, flush, return the new value.
4. Commit releases the row lock.

### Why pessimistic locking?

- **Correctness is obvious.** While transaction A holds the row, transaction B waits. No two callers ever read the same `current_value`.
- **No retries, no compensating logic.** Optimistic locking would need a retry loop on `OptimisticLockException`; not worth the code for a generator where throughput is rarely the bottleneck.
- **Per-type fan-out.** Locks are scoped to a single row, so `ORDER` and `USER` callers never block each other.

Trade-off: callers on the *same* type serialise through the DB. For very high-throughput needs, a batch-allocation ("hi/lo") variant would pull a block of N ids per DB round-trip — not needed here.

## Switching to MySQL

No Java code changes. In `src/main/resources/application.properties`, comment the H2 block and uncomment the MySQL block. Add the runtime dependency `com.mysql:mysql-connector-j` to `pom.xml`. Run `schema.sql` + `data.sql` against your MySQL instance. That's it — `FOR UPDATE` works identically on InnoDB.

## Project layout

```
src/
├── main/java/assignment/
│   ├── SeqGenApplication.java
│   ├── domain/SequenceRegistry.java
│   ├── repository/SequenceRegistryRepository.java
│   └── service/
│       ├── SequenceGenerator.java            (interface)
│       ├── DbSequenceGenerator.java          (impl, @Transactional)
│       └── UnknownSequenceTypeException.java
├── main/resources/
│   ├── application.properties
│   ├── schema.sql
│   └── data.sql
└── test/java/assignment/
    ├── DbSequenceGeneratorTest.java
    └── ConcurrencyTest.java
```
