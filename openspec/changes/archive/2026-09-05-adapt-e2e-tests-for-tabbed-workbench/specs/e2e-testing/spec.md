## MODIFIED Requirements

### Requirement: Page Object Model Web Interaction
The browser testing architecture SHALL encapsulate DOM selector logic, navigation, and asynchronous UI event assertions within Page Object Model (POM) representations, shielding test specifications from cosmetic frontend template modifications and multi-tab layout shifts.

#### Scenario: Encapsulated login and authentication actions
- **WHEN** a UI test interacts with the login page
- **THEN** the test MUST invoke encapsulated `LoginPage` methods to submit credentials, toggle registration forms, and assert danger alert banners without directly coupling tests to raw CSS classes or element paths

#### Scenario: Async-aware ledger input and feedback assertion
- **WHEN** a UI test performs smart ledger recording
- **THEN** the test MUST invoke `LedgerPage` methods that automatically await SweetAlert2 dialog popups, verify transaction table rows, and extract dashboard balance cards without explicit fixed thread sleeps

#### Scenario: Four-tab navigation and visibility-aware element inspection
- **WHEN** a UI test queries elements contained in conditionally visible tab panels (such as Financial Analytics or Category Management)
- **THEN** the `LedgerPage` Page Object MUST explicitly switch to the target tab and await active class stabilization before querying locators, avoiding Playwright element visibility timeout errors caused by `v-show` (`display: none`) states

#### Scenario: Automated tab transition and ledger entry assertion
- **WHEN** a UI test submits a smart natural language input or structured ledger entry on Tab 01 (Quick Entry)
- **THEN** the `LedgerPage` Page Object MUST assert smooth automatic tab transition to Tab 02 (Ledger History), verify the toast notification, and confirm the newly inserted transaction row is rendered in the transaction table

### Requirement: End-to-End Golden Path Verification
The test suite SHALL systematically validate critical security boundaries and user golden journeys across API and UI layers.

#### Scenario: Cross-tenant horizontal privilege escalation prevention
- **WHEN** User B attempts to access, update, or delete ledger records or custom categories owned by User A via direct HTTP API calls
- **THEN** the security and service layers MUST reject the request with HTTP 403 Forbidden or HTTP 404 Not Found, prohibiting unauthorized data access

#### Scenario: Full user journey from authentication to ledger accounting and logout
- **WHEN** an unauthenticated user opens the application in a headless browser, logs in with valid credentials, inputs smart ledger text ("午餐 120") on Tab 01, observes auto-transition to Tab 02 with transaction rendering, inspects updated expense cards on Tab 03, and clicks logout
- **THEN** the browser MUST maintain JWT Bearer token in `localStorage` across page interactions, accurately reflect calculated monthly totals on Tab 03, and upon logout purge all local authentication tokens and redirect to `/login`

### Requirement: End-to-End Operational Manual and Diagnostics Governance
The project documentation system SHALL provide a unified, authoritative operation and troubleshooting manual for the entire E2E testing ecosystem, cataloging CLI execution commands, Page Object Model design rules, cataloged test cases, and diagnostic remediation paths.

#### Scenario: Standardized CLI execution guide availability
- **WHEN** a developer consults the E2E testing operation manual
- **THEN** the manual MUST provide copy-paste executable commands for full verification (`mvn verify`), targeted integration runs (`failsafe:integration-test`), single-class/single-method filtering with mandatory Windows PowerShell double-quoted parameters (`"-Dit.test=..."`), and dynamic headed debugging flags

#### Scenario: Comprehensive test case matrix and troubleshooting catalog
- **WHEN** a developer diagnoses a test failure or onboards into test maintenance
- **THEN** the documentation MUST provide an exhaustive matrix of 13 API and UI E2E test scenarios (covering auth, accounting, multi-tenant isolation, structured entry, multi-dimensional filtering, and category lifecycle) and structured FAQ remediation procedures for PowerShell CLI parsing, hidden element timeouts, auto-transitions, driver installation, and tenant isolation
