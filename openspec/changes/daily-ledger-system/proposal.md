## Why

Personal daily accounting and financial tracking often suffers from overly complicated interfaces, high input friction, or heavy external cloud dependencies. To deliver a fast, frictionless, and secure ledger experience, this project builds a modern **Daily Ledger System (日常流水帳系統)**. 

The system leverages a Spring Boot 3.x backend (strict 4-layer architecture with MS SQL Server persistence), a strict No-CDN offline frontend (Vue 3 + Bootstrap 5.3 + Axios + SweetAlert2), and a Swiss Design Style visual system. A central "Google-search-like" input bar minimizes friction during daily expense/income entry, backed by complete user data isolation and JWT stateless security.

## What Changes

- **User Authentication & Security**: Stateless JWT authentication with BCrypt password hashing, registration, login, and thread-local user context isolation (`/api/v1/auth/*`).
- **Category Management**: Dual-tier category structure supporting system-default shared categories (pre-seeded) and user-defined custom categories with full CRUD capabilities (`/api/v1/categories/*`).
- **Daily Ledger Core & Analytics**: Financial transaction recording (expense/income), flexible multi-condition queries and pagination, record editing/deletion, and monthly summary statistics (total income, total expense, net balance) (`/api/v1/records/*`).
- **Extensible Input Processing**: Central structured quick-entry bar (Option A) with an extensible architecture hook for natural language processing (`SmartParserService`, Option B).
- **Offline Swiss-Style UI**: A strictly offline, self-hosted frontend adhering to Swiss International Typographic Style (clean geometric grid, bold sans-serif typography, signature Swiss Red accent `#DC2626`, zero fuzzy drop shadows). Includes interactive authentication pages and a unified ledger workbench with responsive summary cards and transaction tables.
- **Database Schema & Migrations**: MS SQL Server schema (`sys_user`, `sys_category`, `account_record`) with foreign key constraints, indexes, and default category seed data.

## Capabilities

### New Capabilities
- `user-authentication`: User registration, login with JWT token issuance, token validation, user profile retrieval, and thread-local security context.
- `category-management`: Dual-tier category queries (system defaults + user customs), custom category creation, updating, and deletion with usage validation.
- `daily-ledger`: Transaction record CRUD, monthly financial summary aggregations, multi-criteria filtering, and Google-style quick input processing.
- `offline-web-ui`: Self-hosted offline frontend runtime (Vue 3, Bootstrap 5.3, Axios, SweetAlert2) and Swiss Design Style UI for authentication and daily ledger workflows.

### Modified Capabilities
<!-- None: This is a greenfield change creating the baseline capabilities -->

## Impact

- **Backend Codebase**:
  - Controller layer: MVC route controllers and RESTful Web API controllers under `/api/v1/*`.
  - Service layer: `AuthService`, `TokenService` (JWT), `CategoryService`, `LedgerService`, and `SmartParserService` interface.
  - Repository layer: Spring Data JPA repositories with MS SQL Server dialect.
  - Common / Core: Unified `ApiResponse<T>`, global exception handling, JWT filter, and user context holder.
- **Database**:
  - Target database: MS SQL Server `tibame_account` on `localhost:1433`.
  - Tables: `sys_user`, `sys_category`, `account_record`.
- **Frontend / Static Assets**:
  - Offline vendor libraries hosted in `src/main/resources/static/lib/`.
  - Custom Swiss design stylesheets (`swiss-style.css`, `base.css`) and Vue 3 MVVM app scripts.
- **Dependencies**:
  - Maven: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-validation`, `jjwt` (or `com.auth0`), `mssql-jdbc`, `lombok`.
