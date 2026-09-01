## 1. Database Schema & Configuration

- [x] 1.1 Create MS SQL Server DDL script (`schema.sql`) and seed data script (`data.sql`) for `sys_user`, `sys_category`, and `account_record`, and verify table creation and seed data execution against `tibame_account`.
- [x] 1.2 Configure `application.properties` / `application.yml` for MS SQL Server connection (`localhost:1433`, `tibame_account`, `sa`, `1111`) and verify successful Spring Boot database startup.

## 2. Common Foundation & Security Architecture

- [x] 2.1 Implement unified `ApiResponse<T>`, custom exceptions (`ApiException`, `ResourceNotFoundException`, `UnauthorizedException`), and `GlobalExceptionHandler`, verifying standard JSON response structure.
- [x] 2.2 Implement `TokenService` interface and `JwtTokenServiceImpl` with token generation, claims extraction, expiration handling, and unit test verification.
- [x] 2.3 Implement `UserPrincipal`, `UserContext` (ThreadLocal holder), and `JwtAuthenticationFilter` with guaranteed `UserContext.clear()` in `finally` blocks, verifying security filter chain interception.

## 3. Entity & Repository Layer (Spring Data JPA)

- [x] 3.1 Implement `User` entity (`sys_user`) and `UserRepository` with query methods for username uniqueness and credential lookups.
- [x] 3.2 Implement `Category` entity (`sys_category`) and `CategoryRepository` supporting queries for combined system defaults and user-specific custom categories.
- [x] 3.3 Implement `AccountRecord` entity (`account_record`) and `AccountRecordRepository` supporting user-scoped pagination, multi-criteria filtering (date, type, category, keyword), and monthly aggregation sum queries.

## 4. Service & Web API Layer

- [x] 4.1 Implement `AuthService` and `AuthApiController` for `/api/v1/auth/register`, `/api/v1/auth/login`, and `/api/v1/auth/me`, verifying BCrypt hashing and JWT response.
- [x] 4.2 Implement `CategoryService` and `CategoryApiController` for `/api/v1/categories/*` (GET, POST, PUT, DELETE) with system category protection and orphan check on deletion.
- [x] 4.3 Implement `SmartParserService` interface and `RegexSmartParserServiceImpl` stub to establish the extensibility hook for natural language transaction input.
- [x] 4.4 Implement `LedgerService` and `RecordApiController` for `/api/v1/records/*` (CRUD, filtering, and `/summary` monthly aggregation), verifying strict `user_id` isolation.

## 5. Offline Static Assets & Swiss Design System

- [x] 5.1 Setup offline vendor library bundle (Bootstrap 5.3.3, Vue 3.4.x, Axios 1.7.x, SweetAlert2 11.x) under `src/main/resources/static/lib/` and verify zero external CDN requests.
- [x] 5.2 Implement Swiss Design Style CSS system (`swiss-style.css`) with geometric grid, sharp borders (`0px` radius), high contrast monochrome palette, signature Swiss Red (`#DC2626`) accent, and structured numbered index labels (`SYS-LEDGER // 01`).

## 6. Frontend Web UI & Integration (Vue 3 MVVM)

- [x] 6.1 Implement Thymeleaf MVC controller (`ViewController`) and layout templates for `/login` and `/ledger` workbench pages.
- [x] 6.2 Implement Vue 3 authentication app with Axios interceptors for automatic JWT token injection, localStorage persistence, and 401 redirection.
- [x] 6.3 Implement central Google-style quick-entry search bar component with keyboard shortcuts (Enter to record) and SweetAlert2 geometric feedback toasts.
- [x] 6.4 Implement monthly financial summary cards (Total Income, Total Expense, Net Balance) and reactive transaction history table with filtering, pagination, edit modal, and delete confirmation.
- [x] 6.5 Implement category management modal interface allowing users to view system categories and manage custom categories.

## 7. End-to-End Verification & Quality Audit

- [x] 7.1 Verify complete end-to-end user workflow: register ➔ login ➔ quick accounting entry ➔ custom category creation ➔ monthly summary updates ➔ filtering & deletion.
- [x] 7.2 Perform offline air-gapped network audit and Swiss Design Style visual compliance check to ensure full adherence to Definition of Done (DoD).
