## Why

The Daily Ledger web interface has evolved from a single vertical stacked page into a Swiss-style Four-Tab Workbench (Tab 01 Quick Entry, Tab 02 Ledger History, Tab 03 Financial Analytics, Tab 04 Category Management). This architectural change broke existing Playwright UI E2E tests:
1. Playwright waits on hidden summary statistics cards in Tab 03 (`v-show="activeTab === 'analytics'"`, evaluated as `display: none` by Vue) resulting in 30-second timeouts.
2. The UI automatically transitions from Tab 01 to Tab 02 upon successful transaction submission, but the legacy Page Object Model (POM) in `LedgerPage.java` assumes single-page layout without tab awareness.
3. Windows PowerShell commands in the operational guide cause lifecycle phase errors if `-Dit.test` parameters lack quotation marks.

Adapting the Page Object Model, updating UI test assertions, and revising the operational manual to v2.0 will restore full green-light verification across all testing tiers.

## What Changes

- **Page Object Model (POM) Modernization**:
  - Enhance `LedgerPage.java` with tab switching methods (`switchTab(tabName)`), awaiting active tab states.
  - Adapt `submitSmartQuickInput` and `submitStructuredInput` to assert auto-transition to Tab 02 (Ledger History).
  - Update `getTotalExpenseText()`, `getTotalIncomeText()`, and `getNetBalanceText()` to explicitly switch to Tab 03 (Financial Analytics) before querying metric locators.
  - Implement POM methods for Tab 02 filtering and Tab 04 category management interactions.
- **UI Test Suite Adaptation**:
  - Update `AccountingFlowUiE2ETest.java` to align with the tabbed workflow, verifying successful entry, tab auto-transition to Tab 02, and metrics extraction in Tab 03.
  - [Optional/Future-proof] Add expansion test skeletons for structured entry (`StructuredEntryUiTest`), multi-dimensional ledger filtering (`LedgerFilterUiTest`), and category lifecycle (`CategoryLifecycleUiTest`).
- **Operational Guide & Troubleshooting Revision**:
  - Update `docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md` to v2.0, incorporating the four-tab architecture, Windows PowerShell double-quote guidelines (`"-Dit.test=..."`), dynamic headed debugging flags, and updated troubleshooting matrices.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `e2e-testing`: Adapt Page Object Model (POM) to Swiss tabbed workbench, support tab switching, handle async hidden element visibility, assert auto-transitions, and synchronize operational documentation.

## Impact

- `src/test/java/com/tibame/e2e/pages/LedgerPage.java`: Modernized with tab switching and visibility-aware element access.
- `src/test/java/com/tibame/e2e/ui/AccountingFlowUiE2ETest.java`: Updated to adhere to tabbed POM contract and verify auto-transitions.
- `docs/specifications/daily_ledger_system/10_e2e_testing_guide_and_operation_manual.md`: Upgraded to v2.0.
- All existing API tests (`AuthApiE2ETest`, `LedgerApiE2ETest`, `TenantIsolationSecurityE2ETest`) remain untouched and continue passing.
