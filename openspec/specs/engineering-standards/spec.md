## Purpose

Defines repository-wide engineering standards, IDE workspace automation guardrails, backend code cleanliness conventions, and the zero-warning quality gate (Zero-Warning DoD) for all software deliverables.

## Requirements

### Requirement: IDE Workspace Automation
The workspace environment SHALL enforce automated code formatting, unused import cleanup, and tool warning suppression across all supported IDEs.

#### Scenario: Automatic organize imports on save
- **WHEN** a developer saves a Java source file within the workspace
- **THEN** the IDE MUST automatically trigger import organization (`source.organizeImports: always`), eliminating unused import statements and sorting existing ones without manual intervention

#### Scenario: Suppression of tool lifecycle noise
- **WHEN** the IDE Java language server and Spring Boot tooling inspect project dependencies
- **THEN** the workspace configuration MUST suppress non-blocking framework lifecycle end-of-support notices (`boot-java.validation.java.version-validation: OFF`) to ensure the Problems panel displays only genuine compilation errors and actionable warnings

### Requirement: Backend Code Cleanliness Conventions
The codebase SHALL adhere to strict clean code conventions eliminating redundant annotations, duplicate high-overhead object allocations, dead fields, and unnecessary access modifier exposures.

#### Scenario: JpaRepository interface annotation hygiene
- **WHEN** a data access interface extends `org.springframework.data.jpa.repository.JpaRepository`
- **THEN** the interface MUST NOT declare redundant `@Repository` annotations, relying on Spring Data JPA auto-proxying and built-in exception translation to avoid language server redundant annotation warnings

#### Scenario: Regex pattern compilation and caching
- **WHEN** a service or utility component utilizes regular expression matching
- **THEN** the `java.util.regex.Pattern` instance MUST be pre-compiled and cached as a `private static final` class constant, prohibiting repeated `Pattern.compile(...)` inside method bodies and prohibiting unreferenced orphan fields

#### Scenario: JUnit 5 test class and method visibility
- **WHEN** authoring unit test suites under JUnit 5 (Jupiter)
- **THEN** test classes and test methods MUST use package-private visibility without explicit `public` modifiers to comply with modern testing idioms

### Requirement: IDE Troubleshooting Protocol
The project SHALL maintain an authoritative, step-by-step troubleshooting protocol for recovering IDE language servers from phantom errors and cache desynchronization.

#### Scenario: Language server phantom error recovery
- **WHEN** the IDE environment encounters phantom errors, stale classpath indexing, or syntax highlighting failures
- **THEN** developers MUST be able to execute the 4-step recovery workflow (Reload Projects -> Clean LS Workspace -> Reload Window -> Maven recompile) to restore clean state deterministically

### Requirement: Zero-Warning Definition of Done (DoD)
All code changes, pull requests, and OpenSpec archive workflows SHALL satisfy a mandatory Zero-Warning quality gate before completion, including both fast unit test validation and full integration verification runs.

#### Scenario: Quality gate verification before delivery
- **WHEN** a feature, refactoring, or bug fix is prepared for commit or OpenSpec archive
- **THEN** the build MUST verify: `mvnw clean test-compile` succeeds, all unit tests pass (100% green) via `mvn test`, zero unused imports exist, zero orphan dead fields exist, and the IDE Problems panel contains zero errors and zero warnings

#### Scenario: Integration and E2E verification gate
- **WHEN** a pull request, release candidate, or architectural milestone verification is executed
- **THEN** the build MUST execute `mvnw verify` to run the Failsafe integration and end-to-end test suites against the in-memory test environment, requiring 100% test pass rate

#### Scenario: Module level conformance declaration
- **WHEN** a business module defines its acceptance criteria in quality assurance documentation
- **THEN** the module MUST link directly to the central engineering standards document as its single source of truth (SSOT) rather than duplicating individual technical rules

### Requirement: Automated Configuration Governance and Test Gates
The test infrastructure SHALL provide automated verification test gates in the Maven test phase to statically lint YAML configuration files against escaping violations and ensure configuration metadata generation for strongly-typed properties.

#### Scenario: Static linting of YAML Map keys for escaping violations
- **WHEN** the test suite executes `YamlConfigurationLintTest` during `mvn test`
- **THEN** all target YAML configuration files (including `application.yml` and `application-test.yml`) MUST be parsed and scanned, asserting that any Map key containing dots or underscores under hierarchical property prefixes (such as `logging.level` or `properties.hibernate`) is escaped with bracket notation `["..."]`, failing the test build if violations are detected

#### Scenario: Compile-time configuration metadata existence and prefix validation
- **WHEN** the test suite executes `ConfigurationMetadataTest` during or after compilation
- **THEN** the test MUST verify the presence of `target/classes/META-INF/spring-configuration-metadata.json` and validate that custom configuration properties (such as `jwt` and `crypto.password.policy`) are registered, failing the test build if metadata is missing or incomplete

#### Scenario: Enforcement of strongly-typed configuration injection
- **WHEN** business services or components require custom application configurations
- **THEN** components MUST inject strongly-typed `@ConfigurationProperties` classes instead of ad-hoc `@Value` annotations for hierarchical properties, adhering to the repository's configuration boundary rules

