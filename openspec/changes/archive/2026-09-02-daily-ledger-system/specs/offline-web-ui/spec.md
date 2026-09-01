## Purpose

Renders a self-hosted, strictly offline (No-CDN) single-page web interface following Swiss Design Style aesthetics for personal financial management.

## ADDED Requirements

### Requirement: Strict Offline Asset Delivery
The system SHALL serve all UI dependencies, styles, scripts, and fonts locally from the application server without making any external CDN network requests.

#### Scenario: Application loading in an offline or air-gapped network environment
- **WHEN** a browser client opens the web application without an active internet connection
- **THEN** all JavaScript libraries (Vue 3, Bootstrap 5.3, Axios, SweetAlert2) and CSS styles MUST load successfully from local static directories (`/lib/`) with zero network failures

### Requirement: Swiss International Typographic Style Aesthetics
The user interface SHALL strictly implement Swiss Design Style principles featuring structured grid layouts, bold sans-serif typography, high-contrast monochrome tones with signature Swiss Red (`#DC2626`) accents, and geometric sharp borders.

#### Scenario: Visual presentation of components and cards
- **WHEN** any page or UI component is rendered
- **THEN** the elements MUST display with solid geometric borders (1px/2px solid), crisp contrast, clear typographic hierarchy, and uppercase section indexing (e.g., `SYS-LEDGER // 01`)

### Requirement: Authentication Page and State Management
The web UI SHALL provide an interactive login and registration interface with client-side validation, JWT persistence in localStorage, and automatic Axios authorization header injection.

#### Scenario: User submits login form on authentication view
- **WHEN** a user enters valid login credentials and submits
- **THEN** the Vue application MUST call the login API via Axios, store the returned JWT token, and seamlessly transition the view to the Ledger Workbench

#### Scenario: Handling unauthenticated or expired session
- **WHEN** an API call returns a 401 Unauthorized status via Axios response interceptor
- **THEN** the client MUST clear stored credentials, alert the user via SweetAlert2, and redirect to the login screen

### Requirement: Central Quick-Entry Search Bar Experience
The ledger workbench SHALL feature a prominent central Google-style input bar enabling rapid structured transaction logging with minimal keystrokes.

#### Scenario: Logging a transaction via central quick-entry bar
- **WHEN** a user chooses type (Expense/Income), fills amount, selects category, types note, and presses Enter or clicks submit
- **THEN** the Vue application MUST instantly send a creation request, trigger a clean geometric SweetAlert2 toast notification, reset the input bar, and reload both statistics and the history table

### Requirement: Real-Time Financial Summary and Transaction History
The ledger workbench SHALL display monthly summary metric cards and a real-time reactive transaction table with filtering, pagination, editing, and deletion.

#### Scenario: Rendering monthly summary cards
- **WHEN** the ledger workbench is loaded or a new record is posted
- **THEN** the summary cards for Total Expense, Total Income, and Net Balance MUST update immediately with formatted currency figures

#### Scenario: Deleting a transaction with confirmation
- **WHEN** a user clicks the delete button for a transaction item
- **THEN** a Swiss-styled SweetAlert2 confirmation dialog MUST appear, and upon confirmation, send the DELETE request and remove the row from the table
