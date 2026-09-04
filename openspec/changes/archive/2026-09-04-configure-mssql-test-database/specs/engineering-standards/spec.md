## ADDED Requirements

### Requirement: Dedicated SQL Server Testing Profile and Database Isolation
The test environment SHALL provide a dedicated, isolated Spring profile (`test-mssql`) targeting an independent test database (`tibame_account_test`), ensuring zero data cross-contamination with the development or production databases.

#### Scenario: Automatic schema and data initialization for isolated test database
- **WHEN** the application test suite or runner launches with active profile `test-mssql`
- **THEN** the initialization engine MUST execute `schema.sql` and `data.sql` with idempotent continuation (`continue-on-error: true`), provisioning required tables and seed categories automatically without manual DDL operations

#### Scenario: Dynamic database credential inheritance and overrides
- **WHEN** connecting to the dedicated test database under profile `test-mssql`
- **THEN** the configuration MUST inherit default or process environment variables (`DB_TEST_PASSWORD` falling back to `DB_PASSWORD` or local default `1111`) without exposing personal credentials in tracked files

### Requirement: Transactional Integration Test Base and State Cleanliness
The integration testing infrastructure SHALL provide an abstract base test class enforcing transactional rollback and MockMvc configuration across integration test suites.

#### Scenario: Automatic rollback after test execution
- **WHEN** an integration test extending the base integration class executes write operations against the test database
- **THEN** the test transaction MUST automatically roll back upon completion, leaving the target database state pristine and unpolluted

#### Scenario: Uniform MockMvc and test context configuration
- **WHEN** an integration test suite inherits from the base class
- **THEN** the test context MUST provide pre-configured MockMvc instances under MOCK web environment, avoiding repetitive context bootstrap boilerplate

### Requirement: SQL Server IDENTITY Dynamic Assertion Standards
Integration test suites interacting with SQL Server instances SHALL account for `IDENTITY(1,1)` non-consecutive jumping behavior by utilizing dynamic ID assertions rather than hardcoded identifiers.

#### Scenario: Dynamic primary key assertion against identity jump
- **WHEN** an entity is persisted and queried within a test transaction in SQL Server
- **THEN** test assertions MUST evaluate ID presence and positivity (`isNotNull()`, `isPositive()`) rather than asserting static or deterministic sequence numbers (such as `1L`)

### Requirement: Configuration Governance Inclusion for Test Profiles
Automated configuration linting SHALL enforce YAML key escaping standards across all test configuration profiles.

#### Scenario: Verification of test configuration profile key escaping
- **WHEN** `YamlConfigurationLintTest` scans repository YAML configurations
- **THEN** `application-test-mssql.yml` MUST be included in the test verification scope, asserting that all map keys with dots or underscores comply with bracket escaping rules
