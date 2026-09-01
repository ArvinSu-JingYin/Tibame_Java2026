## Purpose

Manages dual-tier financial categories (system-defined defaults and user-defined custom categories) for income and expense transactions.

## Requirements

### Requirement: Query Available Categories
The system SHALL provide an API to retrieve all available categories for the authenticated user, combining system-wide default categories and the user's custom categories.

#### Scenario: Retrieving full category list for authenticated user
- **WHEN** an authenticated user requests `/api/v1/categories`
- **THEN** the system MUST return all system-default categories (`is_system = true`) plus all custom categories created by that specific user (`user_id = current_user_id`)

#### Scenario: Filtering categories by transaction type
- **WHEN** an authenticated user queries categories with a `type=EXPENSE` or `type=INCOME` filter parameter
- **THEN** the system MUST return only categories matching the requested type, sorted by sort order

### Requirement: Create Custom Category
The system SHALL allow authenticated users to create new custom categories associated with their user account.

#### Scenario: Successfully creating a custom category
- **WHEN** an authenticated user submits a new category payload with valid name, type (`EXPENSE` or `INCOME`), and optional icon code
- **THEN** the system MUST save the category with `user_id` set to the authenticated user and `is_system = false`, returning the created category object

#### Scenario: Creating category with duplicate name under same type
- **WHEN** a user attempts to create a custom category with a name that already exists for their account under the same transaction type
- **THEN** the system MUST reject the creation with a descriptive conflict error message

### Requirement: Update Custom Category
The system SHALL allow users to modify the name, icon code, or sort order of their own custom categories.

#### Scenario: Successfully updating an owned custom category
- **WHEN** a user submits valid update fields for a custom category they created
- **THEN** the system MUST update the category details and return the updated entity

#### Scenario: Attempting to modify a system-default category or another user's category
- **WHEN** a user attempts to update a category where `is_system = true` or `user_id` belongs to another user
- **THEN** the system MUST reject the operation with a forbidden or not found error

### Requirement: Delete Custom Category
The system SHALL allow users to delete their own custom categories, provided the category is not currently linked to existing ledger records.

#### Scenario: Successfully deleting an unused custom category
- **WHEN** a user requests deletion of an owned custom category that has zero linked ledger records
- **THEN** the system MUST delete the category and return a success status

#### Scenario: Deleting a category currently referenced by ledger records
- **WHEN** a user attempts to delete a custom category that is referenced by one or more ledger records
- **THEN** the system MUST prevent deletion and return a conflict error indicating that linked records exist

#### Scenario: Attempting to delete a system default category
- **WHEN** a user requests deletion of a category where `is_system = true`
- **THEN** the system MUST reject the deletion request with a forbidden status
