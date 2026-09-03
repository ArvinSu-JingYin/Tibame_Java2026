## Purpose

Defines automated end-to-end (E2E) and integration testing specifications for the Daily Ledger System, ensuring system-level correctness across HTTP security filters, tenant boundary isolation, and browser user interactions.

## Requirements

### Requirement: Tiered Testing Execution and Separation
The system SHALL provide a structured test suite distinguishing fast, mock-driven unit tests from integration and browser automation tests, ensuring developers obtain millisecond-level feedback during active development while preserving comprehensive end-to-end validation.

#### Scenario: Fast local development test cycle
- **WHEN** a developer executes `mvn test`
- **THEN** the build MUST execute only isolated unit tests (`*Test.java`) without starting full Spring Boot application contexts or launching browser processes, completing within seconds

#### Scenario: Integration and verification build execution
- **WHEN** a verification run or continuous integration build executes `mvn verify`
- **THEN** the build MUST run all unit tests followed by Maven Failsafe integration tests (`*IT.java`, `*E2ETest.java`), spinning up an application instance on a random available port and executing end-to-end scenarios

### Requirement: Isolated Test Database and Security Context
The testing framework SHALL guarantee complete state isolation between test executions, preventing cross-test data pollution, concurrent account collisions, and browser credential leakage.

#### Scenario: Dedicated test database environment
- **WHEN** integration or E2E tests are initiated
- **THEN** the application MUST boot with the test profile (`application-test.yml`), initializing an independent in-memory H2 database populated from seed schemas, and automatically tearing down all data upon context shutdown

#### Scenario: Dynamic test user account generation
- **WHEN** a test scenario requires an authenticated user
- **THEN** the test suite MUST generate unique user accounts with dynamic UUID-suffixed usernames (`test_user_<UUID>`), preventing `409 Conflict` duplicate username errors across repeated or concurrent runs

#### Scenario: Sandboxed browser context isolation
- **WHEN** browser-driven UI E2E tests execute
- **THEN** each test method MUST instantiate a fresh, isolated `BrowserContext` sandbox before execution and dispose of it immediately after, ensuring cookies, sessions, and `localStorage` Bearer tokens never leak into subsequent tests

### Requirement: Page Object Model Web Interaction
The browser testing architecture SHALL encapsulate DOM selector logic, navigation, and asynchronous UI event assertions within Page Object Model (POM) representations, shielding test specifications from cosmetic frontend template modifications.

#### Scenario: Encapsulated login and authentication actions
- **WHEN** a UI test interacts with the login page
- **THEN** the test MUST invoke encapsulated `LoginPage` methods to submit credentials, toggle registration forms, and assert danger alert banners without directly coupling tests to raw CSS classes or element paths

#### Scenario: Async-aware ledger input and feedback assertion
- **WHEN** a UI test performs smart ledger recording
- **THEN** the test MUST invoke `LedgerPage` methods that automatically await SweetAlert2 dialog popups, verify transaction table rows, and extract dashboard balance cards without explicit fixed thread sleeps

### Requirement: End-to-End Golden Path Verification
The test suite SHALL systematically validate critical security boundaries and user golden journeys across API and UI layers.

#### Scenario: Cross-tenant horizontal privilege escalation prevention
- **WHEN** User B attempts to access, update, or delete ledger records or custom categories owned by User A via direct HTTP API calls
- **THEN** the security and service layers MUST reject the request with HTTP 403 Forbidden or HTTP 404 Not Found, prohibiting unauthorized data access

#### Scenario: Full user journey from authentication to ledger accounting and logout
- **WHEN** an unauthenticated user opens the application in a headless browser, logs in with valid credentials, inputs smart ledger text ("午餐 120"), observes the success notification, and clicks logout
- **THEN** the browser MUST redirect to `/ledger`, store the JWT Bearer token in `localStorage`, update the transaction table and monthly expense cards, and upon logout clear all local authentication tokens and redirect to `/login`
