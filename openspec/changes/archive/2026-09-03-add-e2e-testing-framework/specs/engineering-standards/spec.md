## MODIFIED Requirements

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
