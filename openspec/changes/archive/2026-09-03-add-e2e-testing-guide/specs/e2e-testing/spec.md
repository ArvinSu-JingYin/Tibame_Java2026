## ADDED Requirements

### Requirement: Dynamic Headed Debugging Mode and Slow-Motion Support
The browser testing runtime SHALL support dynamic, non-intrusive activation of headed browser execution and operational pace throttling, allowing developers to visually inspect UI interactions, layout animations, and popup states without altering automated continuous integration (CI) defaults.

#### Scenario: Headless execution by default
- **WHEN** UI E2E tests are executed without explicit headed configuration flags
- **THEN** the test runtime MUST launch Chromium in headless mode (`headless: true`) with zero added artificial delay, ensuring fast and silent background execution suitable for CI pipelines

#### Scenario: Headed debugging triggered via system property
- **WHEN** UI E2E tests are launched with JVM argument `-Dplaywright.headed=true`
- **THEN** the test framework MUST launch visible browser windows (`headless: false`) and inject an automatic 400ms slow-motion step delay (`slowMo: 400`), enabling engineers to visually trace DOM actions and SweetAlert2 transitions in real time

#### Scenario: Headed debugging triggered via environment variable
- **WHEN** UI E2E tests are launched in an environment where `PLAYWRIGHT_HEADED` is set to `"true"` (case-insensitive)
- **THEN** the test framework MUST enable headed browser execution and throttle action playback identically to the system property configuration

### Requirement: End-to-End Operational Manual and Diagnostics Governance
The project documentation system SHALL provide a unified, authoritative operation and troubleshooting manual for the entire E2E testing ecosystem, cataloging CLI execution commands, Page Object Model design rules, cataloged test cases, and diagnostic remediation paths.

#### Scenario: Standardized CLI execution guide availability
- **WHEN** a developer consults the E2E testing operation manual
- **THEN** the manual MUST provide copy-paste executable commands for full verification (`mvn verify`), targeted integration runs (`failsafe:integration-test`), single-class/single-method filtering (`-Dit.test`), and headed debugging switches

#### Scenario: Comprehensive test case matrix and troubleshooting catalog
- **WHEN** a developer diagnoses a test failure or onboard into test maintenance
- **THEN** the documentation MUST provide an exhaustive matrix of all API and UI E2E test scenarios and structured FAQ remediation procedures for browser driver acquisition, port assignment, account collision avoidance, and assertion timings
