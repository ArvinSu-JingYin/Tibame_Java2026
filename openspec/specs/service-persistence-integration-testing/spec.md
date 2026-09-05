## Purpose

Defines automated service-layer persistence integration testing specifications for the Daily Ledger System, ensuring transactional rollback, real JPA/Hibernate interaction, dynamic specification querying, accurate JPQL aggregations, and relational foreign key integrity across both H2 and Microsoft SQL Server database engines.

## Requirements

### Requirement: Service-Layer Real Database Persistence and Transaction Isolation
The testing framework SHALL execute service-layer operations against a real database connection without launching an HTTP Web container, guaranteeing complete transactional rollback and zero test data pollution upon test completion.

#### Scenario: Automatic rollback with zero database pollution
- **WHEN** a service-layer integration test executes database modifications (create, update, delete) within an active `@Transactional` boundary
- **THEN** the test framework MUST commit zero dirty data to the underlying database and roll back all transactional mutations immediately upon test completion

#### Scenario: Dynamic test user generation and cryptographic hash persistence
- **WHEN** a test user is provisioned via the integration test base helper
- **THEN** the system MUST persist a unique user entity with a BCrypt hashed password (`$2a$10$`) and a dynamic UUID-suffixed username, ensuring zero unique constraint collision across test runs

### Requirement: Financial Calculation and Monthly Ledger Aggregation Accuracy
The system SHALL execute real SQL/JPQL aggregation functions and dynamic Spring Data JPA Specifications against physical table structures, validating mathematical precision and null-safe financial summaries.

#### Scenario: Accurate monthly summary aggregation with real JPQL functions
- **WHEN** multiple income and expense records are persisted across different dates and the service calculates a target month's summary
- **THEN** the system MUST execute `COALESCE(SUM(r.amount), 0)` JPQL queries, accurately computing total income, total expense, and net balance while strictly excluding transactions from other months

#### Scenario: Zero-record monthly boundary defense without null pointer exception
- **WHEN** the monthly summary service is queried for a month containing zero transaction records
- **THEN** the system MUST return `BigDecimal.ZERO` for income, expense, and balance without throwing `NullPointerException`

#### Scenario: Multi-dimensional Specification dynamic query execution
- **WHEN** ledger records are queried using compound filtering criteria (category ID, date range, transaction type, and keyword)
- **THEN** the system MUST dynamically generate valid JPA criteria SQL with proper `AND` conjunctions and descending date sorting, returning exact matching records and pagination metadata

### Requirement: Tenant Boundary and Security Defense in Service Persistence
The service layer SHALL enforce tenant boundary isolation and foreign key validity at the database query level, rejecting unauthorized cross-tenant operations.

#### Scenario: Cross-tenant horizontal privilege escalation prevention in record retrieval and deletion
- **WHEN** User A attempts to retrieve, update, or delete a ledger record owned by User B via service-layer methods
- **THEN** the service layer MUST reject the operation with a resource not found or access forbidden exception, and User B's record MUST remain unmodified in the database

#### Scenario: Non-existent category foreign key association rejection
- **WHEN** a ledger record creation is attempted with a non-existent category ID
- **THEN** the service layer MUST reject the transaction before persistence, preventing orphaned records and database integrity violations

### Requirement: Category Management Lifecycle and Relational Constraints
The system SHALL enforce system seed category immutability, tenant-scoped category visibility, unique naming rules, and foreign key deletion prevention.

#### Scenario: System seed category immutability protection
- **WHEN** an update or deletion operation is attempted on a system seed category (`isSystem = true`)
- **THEN** the service layer MUST reject the modification with a business exception, preserving the seed category name and sort order intact

#### Scenario: Multi-tenant custom category visibility isolation
- **WHEN** User A creates a custom category and User B queries available categories
- **THEN** User B MUST see system categories and User B's own categories, but MUST NOT see User A's custom category

#### Scenario: Deletion prevention for categories with associated transaction records
- **WHEN** a user attempts to delete a custom category that has one or more associated ledger records (`countByCategoryId > 0`)
- **THEN** the service layer MUST reject the deletion request with a business validation error, preserving referential integrity

#### Scenario: Safe deletion of unassociated custom categories
- **WHEN** a user deletes a custom category that has zero associated ledger records
- **THEN** the service layer MUST remove the category from the database, and subsequent queries MUST NOT return the deleted category

#### Scenario: Duplicate category name rejection within user scope
- **WHEN** a user attempts to create a category with a name and type identical to an existing category owned by that same user
- **THEN** the service layer MUST reject the creation with a conflict exception, maintaining single-tenant uniqueness

### Requirement: Dual Database Engine Compatibility
The service persistence integration test suite SHALL be capable of executing against both the in-memory H2 database engine and a physical Microsoft SQL Server 2022 instance without code modifications.

#### Scenario: High-speed verification using in-memory H2 database
- **WHEN** tests are executed under the default `test` Spring profile
- **THEN** all persistence integration tests MUST execute against an in-memory H2 database in MSSQLServer compatibility mode and pass within seconds

#### Scenario: Physical dialect verification using MS SQL Server 2022
- **WHEN** tests are executed with `-Dspring.profiles.active=test-mssql`
- **THEN** all persistence integration tests MUST connect to the physical SQL Server test database (`tibame_account_test`), validating real SQL Server dialect queries, identity sequence generation, and unicode collation
