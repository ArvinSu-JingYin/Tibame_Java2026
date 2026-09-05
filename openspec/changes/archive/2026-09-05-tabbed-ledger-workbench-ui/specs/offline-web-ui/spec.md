## ADDED Requirements

### Requirement: Tabbed Workbench Navigation and Category Management
The ledger workbench SHALL provide a Swiss-style geometric tab navigation system supporting four distinct workspaces (`01 記帳錄入`, `02 交易明細`, `03 財務概覽`, `04 分類管理`), with category management presented as a full-width dedicated tab view instead of a modal dialog.

#### Scenario: Switching between workbench tabs
- **WHEN** an authenticated user clicks on any tab button (`01 記帳錄入`, `02 交易明細`, `03 財務概覽`, `04 分類管理`) in the navigation bar
- **THEN** the Vue application MUST immediately switch the active workspace view without reloading the page, applying high-contrast active state styling to the selected tab

#### Scenario: Managing categories in full-width dedicated tab
- **WHEN** an authenticated user switches to the `04 分類管理` tab
- **THEN** the application MUST render the flat table listing system-protected and user-defined categories with inline creation form and delete actions for user-defined categories

## MODIFIED Requirements

### Requirement: Central Quick-Entry Search Bar Experience
The ledger workbench SHALL feature a dedicated Focus Quick-Entry workspace (`01 記帳錄入`) as the default post-login landing view, enabling rapid, distraction-free transaction logging with automatic autofocus and seamless auto-transition to transaction history upon submission.

#### Scenario: Logging a transaction via central quick-entry bar
- **WHEN** a user enters transaction details (type, amount, category, date, description) in the `01 記帳錄入` view and submits
- **THEN** the Vue application MUST send the creation request, display a geometric Swiss Toast confirmation, reset the input form, reload transaction records and summary metrics, and automatically transition the active tab to `02 交易明細`

#### Scenario: Default focus landing after authentication
- **WHEN** an authenticated user opens or reloads `/ledger`
- **THEN** the application MUST default the active workspace to `01 記帳錄入`, rendering only the clean transaction entry form and focusing the amount input field

#### Scenario: Toggling between structured and NLP entry modes
- **WHEN** a user switches between `結構化錄入` and `自然語言解析 (NLP)` modes within `01 記帳錄入`
- **THEN** the view MUST toggle the respective input interface while preserving common transaction state and Swiss geometric styling

### Requirement: Real-Time Financial Summary and Transaction History
The ledger workbench SHALL provide isolated dedicated workspaces for transaction history (`02 交易明細`) and monthly summary analytics (`03 財務概覽`), maintaining reactive state synchronization across tabs.

#### Scenario: Rendering monthly summary cards
- **WHEN** the user views `03 財務概覽` or a new record is posted
- **THEN** the summary cards for Total Expense, Total Income, and Net Balance in `03 財務概覽` MUST update immediately with formatted currency figures

#### Scenario: Rendering transaction history in dedicated ledger tab
- **WHEN** the user views `02 交易明細`
- **THEN** the view MUST display the paginated transaction table with multi-criteria filters (type, category, date range, keyword) and edit/delete actions

#### Scenario: Deleting a transaction with confirmation
- **WHEN** a user clicks the delete button for a transaction item in `02 交易明細`
- **THEN** a Swiss-styled SweetAlert2 confirmation dialog MUST appear, and upon confirmation, send the DELETE request, remove the row from the table, and refresh the financial summary metrics
