## 1. Page Object Model (POM) Modernization

- [x] 1.1 Implement tab navigation (`switchTab`) and tab state synchronization in `LedgerPage.java`, verifying tab selection and active class stabilization
- [x] 1.2 Adapt `submitSmartQuickInput` and `submitStructuredInput` in `LedgerPage.java` to assert automatic transition to Tab 02 (Ledger History)
- [x] 1.3 Refactor financial summary extractors (`getTotalExpenseText`, `getTotalIncomeText`, `getNetBalanceText`) in `LedgerPage.java` to ensure Tab 03 (Analytics) is active before locator querying, preventing `v-show` hidden element timeouts
- [x] 1.4 Add auxiliary helper methods in `LedgerPage.java` for Tab 02 multi-dimensional filtering and Tab 04 category creation to support test expansions

## 2. UI E2E Test Suite Adaptation & Verification

- [x] 2.1 Refactor `AccountingFlowUiE2ETest.java` to validate the full tabbed journey (Tab 01 entry -> Tab 02 auto-transition & table verification -> Tab 03 metrics calculation -> logout) and verify execution with `.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AccountingFlowUiE2ETest"`
- [x] 2.2 Verify `AuthFlowUiE2ETest.java` passes cleanly with `.\mvnw.cmd test-compile failsafe:integration-test "-Dit.test=AuthFlowUiE2ETest"`
- [x] 2.3 Run full project verification (`.\mvnw.cmd verify`) ensuring unit tests, API integration tests, and UI tests all execute green without failure

## 3. Operation Manual & Documentation Governance

- [x] 3.1 Upgrade `docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md` to v2.0 including Four-Tab Workbench POM contracts, PowerShell CLI double-quote rules (`"-Dit.test=..."`), dynamic headed debugging flags, and the 13-case test catalog
- [x] 3.2 Verify document cross-references in `docs/README.md` and ensure document navigation consistency
