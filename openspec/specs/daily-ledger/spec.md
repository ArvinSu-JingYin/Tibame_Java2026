## Purpose

Processes financial ledger transactions (incomes and expenses), multi-criteria search queries, and real-time monthly financial summary calculations with strict per-user isolation.

## Requirements

### Requirement: Create Ledger Record
The system SHALL allow authenticated users to log financial transactions (expense or income) with amount, category, date, and description.

#### Scenario: Successfully creating an expense or income record
- **WHEN** an authenticated user submits a transaction with a valid positive amount, a valid accessible category ID, a valid date, and optional description
- **THEN** the system MUST persist the record tied to the user's ID and return the created record details

#### Scenario: Creating a record with an invalid or unauthorized category
- **WHEN** a user attempts to create a record referencing a category ID that does not exist or belongs to a different user
- **THEN** the system MUST reject the creation with a validation or not found error

#### Scenario: Creating a record with non-positive amount
- **WHEN** a user submits an amount less than or equal to 0
- **THEN** the system MUST reject the request with an invalid amount error

### Requirement: Query and Filter Ledger Records
The system SHALL provide paginated query capabilities for ledger records with optional multi-attribute filters (keyword, category, date range, record type).

#### Scenario: Paginated query for current user's records
- **WHEN** an authenticated user requests `/api/v1/records` with page and size parameters
- **THEN** the system MUST return only the records belonging to that user in reverse chronological order with pagination metadata

#### Scenario: Multi-condition filtering
- **WHEN** an authenticated user specifies filter parameters such as `startDate`, `endDate`, `categoryId`, `recordType`, or `keyword`
- **THEN** the system MUST return matching records belonging strictly to the user matching all provided criteria

#### Scenario: Strict user data isolation
- **WHEN** user A queries ledger records
- **THEN** the system MUST never return records belonging to user B

### Requirement: Update Ledger Record
The system SHALL allow authenticated users to update the details of an existing ledger record owned by them.

#### Scenario: Successfully updating an owned ledger record
- **WHEN** a user submits valid modified fields for a record they own
- **THEN** the system MUST persist changes and return the updated record representation

#### Scenario: Attempting to update another user's ledger record
- **WHEN** a user attempts to update a record ID belonging to another user
- **THEN** the system MUST return a 403 Forbidden or 404 Not Found response without modifying data

### Requirement: Delete Ledger Record
The system SHALL allow authenticated users to delete an existing ledger record owned by them.

#### Scenario: Successfully deleting an owned ledger record
- **WHEN** a user requests deletion of a record ID they own
- **THEN** the system MUST remove the record from the database and return a success confirmation

#### Scenario: Attempting to delete another user's ledger record
- **WHEN** a user attempts to delete a record ID belonging to another user
- **THEN** the system MUST prevent deletion and return a 403 Forbidden or 404 Not Found error

### Requirement: Calculate Monthly Financial Summary
The system SHALL calculate aggregate financial metrics (total income, total expense, and net balance) for a specified year and month.

#### Scenario: Retrieving monthly summary for given year and month
- **WHEN** an authenticated user requests `/api/v1/records/summary?year=YYYY&month=MM`
- **THEN** the system MUST calculate the exact sum of all EXPENSE amounts, sum of all INCOME amounts, and net balance (`totalIncome - totalExpense`) for that month and user
