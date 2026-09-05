## Context

The Daily Ledger System migrated from a single-page vertical scroll layout to a Swiss-style Four-Tab Workbench:
- **Tab 01**: 記帳錄入 (Quick Entry) - default tab, features Structured Input & Natural Language Parsing (NLP) modes. Automatically transitions to Tab 02 upon successful submission.
- **Tab 02**: 交易明細 (Transaction Ledger) - displays paginated transaction records and multi-dimensional filter bar.
- **Tab 03**: 財務概覽 (Financial Analytics) - contains monthly summary cards (`.swiss-stat-income`, `.swiss-stat-expense`, `.swiss-stat-balance`). Hidden under `v-show="activeTab === 'analytics'"`.
- **Tab 04**: 分類管理 (Category Management) - two-tier category maintenance. Hidden under `v-show="activeTab === 'categories'"`.

Because Vue's `v-show` directives apply `display: none` to inactive tab containers, Playwright's `waitForSelector(..., { state: VISIBLE })` timed out after 30 seconds when querying summary cards directly from Tab 01. Furthermore, Windows PowerShell CLI parameter parsing breaks Maven `-Dit.test` arguments unless enclosed in double quotes.

## Goals / Non-Goals

**Goals:**
- Modernize `LedgerPage.java` with tab-switching mechanics (`switchTab`) and visibility-aware locators.
- Adapt `submitSmartQuickInput` and `submitStructuredInput` to assert the automatic workflow transition to Tab 02.
- Refactor `getTotalExpenseText()`, `getTotalIncomeText()`, and `getNetBalanceText()` to ensure Tab 03 is active before querying metrics.
- Update `AccountingFlowUiE2ETest.java` to validate the complete tabbed user flow without timeouts or flaky sleeps.
- Provide helper methods in `LedgerPage.java` for structured entry, ledger filtering, and category creation to support extended test suites (`StructuredEntryUiTest`, `LedgerFilterUiTest`, `CategoryLifecycleUiTest`).
- Upgrade `docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md` to v2.0 incorporating PowerShell quotation rules, dynamic headed debugging, tabbed POM patterns, and the 13-case test catalog.

**Non-Goals:**
- Modifying backend APIs or database schemas (all API tests and security filters remain unchanged).
- Redesigning the frontend Vue components or CSS stylesheets.
- Altering the Maven Surefire / Failsafe dual-track lifecycle configuration in `pom.xml`.

## Decisions

### Decision 1: Encapsulate Tab Switching and Locators in `LedgerPage`
- **Choice**: Implement `switchTab(String tabName)` inside `LedgerPage.java` accepting canonical keys (`entry`, `history`, `analytics`, `categories`), clicking `.swiss-tab-btn:has-text(...)` and awaiting `.swiss-tab-btn.active:has-text(...)`.
- **Rationale**: Keeps tests decoupled from DOM indexing and CSS structure. Ensures Playwright waits for CSS class transitions and Vue reactivity cycles to settle before interacting with elements inside panels.
- **Alternative considered**: Requiring individual test methods to invoke raw Playwright `page.click(".swiss-tab-btn:nth-child(3)")`. Rejected because it leaks raw DOM selectors into test specifications and violates POM design principles.

### Decision 2: Self-Healing Metric Queries in Tab 03
- **Choice**: Within `getTotalExpenseText()`, `getTotalIncomeText()`, and `getNetBalanceText()`, verify if Tab 03 is already active; if not, invoke `switchTab("analytics")` before locator resolution.
- **Rationale**: Eliminates hidden-element visibility timeouts (`TimeoutError: 30000ms exceeded`) while preserving concise assertions in test methods (e.g. `assertThat(ledgerPage.getTotalExpenseText()).contains("120.00")`).
- **Alternative considered**: Requiring every test to manually call `ledgerPage.switchTab("analytics")` prior to calling `getTotalExpenseText()`. Rejected because forgetting this step causes hard-to-diagnose timeouts for developers unfamiliar with `v-show` internals.

### Decision 3: Assert Auto-Transition to Tab 02 in Entry Actions
- **Choice**: In `submitSmartQuickInput` and `submitStructuredInput`, after dispatching the submission button click, assert `.swiss-tab-btn.active:has-text('交易明細')` or `.swiss-tab-btn.active:has-text('02')`.
- **Rationale**: The UI's core UX feature is "Fast Entry to History Auto-Transition" (`activeTab = 'history'`). Explicitly asserting this state in the POM guarantees that tests fail fast if the UI fails to transition.
- **Alternative considered**: Leaving tab assertion to the test class. Rejected because the auto-transition is an intrinsic contract of the submission action in the Swiss Tabbed Workbench.

### Decision 4: PowerShell CLI Quotation Governance in Documentation
- **Choice**: All CLI commands in `10_e2e_testing_guide_and_operation_manual.md` MUST specify `-Dit.test` parameters wrapped in double quotes (e.g. `"-Dit.test=AccountingFlowUiE2ETest"`).
- **Rationale**: Windows PowerShell parses unquoted `-D` flags as PowerShell parameters or flags, generating `Unknown lifecycle phase` errors in Maven execution.
- **Alternative considered**: Documenting Bash / cmd syntax only. Rejected because Windows PowerShell is the primary terminal environment on developer workstations.

## Risks / Trade-offs

- **[Risk]**: Tab transition animation or network latency causing race conditions when switching tabs rapidly.
  - **Mitigation**: Use Playwright's built-in `waitForSelector(".swiss-tab-btn.active...")` and locator auto-waiting rather than arbitrary `Thread.sleep()`.
- **[Risk]**: Tests querying Tab 01 immediately after metric assertions in Tab 03 failing due to hidden form fields.
  - **Mitigation**: Input methods (`submitSmartQuickInput`, `submitStructuredInput`) explicitly ensure `switchTab("entry")` is executed before interacting with input fields.
- **[Risk]**: Divergence between test catalog documentation and implemented tests.
  - **Mitigation**: The 13-case test matrix will clearly distinguish implemented tests (#1 - #10) from extension test skeletons (#11 - #13).
