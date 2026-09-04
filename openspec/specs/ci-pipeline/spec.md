## Purpose

Defines repository continuous integration (CI) workflows, automated quality gatekeepers, commit message convention enforcement with Traditional Chinese requirements, OpenSpec specification validation, fast unit testing gates, and deep integration testing with Playwright browser caching and artifact preservation.

## Requirements

### Requirement: Pull Request Compliance and Convention Gatekeeper
The CI system SHALL enforce pull request title, commit history, and specification consistency quality gates on all pull request events targeting the main branch.

#### Scenario: Valid pull request title and conventional commit messages
- **WHEN** a developer or automated agent opens or synchronizes a pull request targeting `main` with title and commit messages adhering to `<type>(<scope>): <Traditional Chinese text>`
- **THEN** the `pr-compliance` job MUST validate that the type matches one of `(feat|fix|refactor|perf|test|style|docs|chore|revert)`, the scope matches one of `(controller|service|repository|entity|dto|config|security|exception|view|common|build|specs)`, contains Traditional Chinese characters (`[\u4e00-\u9fa5]`), does not end with a period, and passes the check

#### Scenario: Rejection of non-compliant commit message or pull request title
- **WHEN** a pull request contains a title or commit message without Chinese characters (e.g. English-only summary), with an unauthorized scope, or violating the conventional commit format
- **THEN** the `pr-compliance` job MUST fail with descriptive error messages, blocking the pull request from merging

#### Scenario: OpenSpec specification validity verification
- **WHEN** a pull request is submitted or updated
- **THEN** the `pr-compliance` job MUST install OpenSpec tooling (`@fission-ai/openspec`) under Node.js 20 and execute `openspec validate --all`, failing the check if any syntax, schema, or incomplete artifact validation errors are detected

### Requirement: Fast PR Unit Testing Gate
The CI system SHALL execute a dedicated fast unit testing gate on pull requests to provide feedback within one minute without downloading heavy browser engines.

#### Scenario: Fast unit test execution under in-memory database
- **WHEN** code changes are submitted to a pull request
- **THEN** the `pr-unit-test` job MUST configure Java 21 with Maven dependency caching and execute `./mvnw clean test --no-transfer-progress`, executing unit tests against H2 in-memory mode while excluding Playwright browser tests

#### Scenario: Pull request merge protection status checks
- **WHEN** both `pr-compliance` and `pr-unit-test` jobs complete successfully
- **THEN** the pull request status checks SHALL report green, satisfying the required checks for branch protection

### Requirement: Main Deep Verification and Regression Pipeline
The CI system SHALL execute comprehensive integration testing, Playwright end-to-end verification, and application packaging upon push or merge events to the main branch.

#### Scenario: Playwright browser cache hit and verification execution
- **WHEN** changes are pushed to `main` and the Playwright browser cache key (`~/.cache/ms-playwright`) matches the runner OS and `pom.xml` hash
- **THEN** the CI job MUST restore the cached Chromium binary without re-downloading, execute `./mvnw clean verify --no-transfer-progress` across all unit, integration, and E2E suites, and package the Spring Boot executable JAR

#### Scenario: Playwright browser cache miss recovery
- **WHEN** changes are pushed to `main` and the Playwright browser cache is missing or invalidated
- **THEN** the CI job MUST trigger `./mvnw exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium"` to install the browser and its Linux OS dependencies before executing verification

### Requirement: CI Test Report and Artifact Archival
The CI system SHALL collect and preserve test reports and build outputs as build artifacts for auditing and post-failure diagnostics.

#### Scenario: Always upload test reports on success or failure
- **WHEN** the `main-verify` job completes execution (regardless of whether tests succeeded or failed)
- **THEN** the CI workflow MUST upload `target/surefire-reports/` and `target/failsafe-reports/` to GitHub Artifacts with a retention policy of at least 7 days
