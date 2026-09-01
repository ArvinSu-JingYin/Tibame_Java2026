## Context

The system is a personal accounting application requiring rapid single-line transaction entry, clean visual hierarchy, and reliable multi-user isolation. See `proposal.md` for motivation and `specs/` for behavioral requirements.

Constraints:
- MS SQL Server (`localhost:1433`, database: `tibame_account`, user: `sa`).
- Strict No-CDN policy: All static scripts, stylesheets, and font assets must run fully offline from `src/main/resources/static/lib/`.
- Backend adheres to Spring Boot 3.x strict 4-layer architecture (Controller ➔ Service ➔ Repository ➔ MS SQL Server).
- Visual presentation strictly follows Swiss International Typographic Style (geometric grid, sharp borders, sans-serif hierarchy, signature red `#DC2626`).

## Goals / Non-Goals

**Goals:**
- Implement stateless, pluggable JWT authentication with thread-local `UserContext` lifecycle management.
- Provide dual-tier category support: system defaults (shared, immutable by standard users) and custom user categories (scoped to `user_id`).
- Build high-performance transaction logging and monthly aggregation APIs with strict per-user database query scoping.
- Design a modular input architecture: Option A (structured central input bar) as primary UI, with `SmartParserService` interface pre-configured for future natural language expansion (Option B).
- Build a responsive, self-hosted offline Vue 3 frontend integrated with Bootstrap 5.3 and custom Swiss style design tokens.

**Non-Goals:**
- External banking API / Open Banking data synchronization.
- Complex multi-currency real-time conversion (single currency baseline).
- Multi-tenant enterprise role-based access control (RBAC with admin hierarchies is out of scope; simple user isolation is required).

## Decisions

### 1. Backend Layering and Security Architecture
- **Decision**: Use `JwtAuthenticationFilter` (extending `OncePerRequestFilter`) combined with a `UserContext` (ThreadLocal wrapper) rather than a heavy Spring Security session configuration.
- **Rationale**: Keeps the application lightweight, stateless, and fully decoupled for RESTful API testing. The `TokenService` is defined as an interface (`TokenService`) with `JwtTokenServiceImpl` implementation to support pluggable authentication (e.g., future OAuth2/SSO).
- **Alternatives Considered**: 
  - Standard Spring Security Session / Form Login: Rejected due to stateful session overhead and mismatch with SPA/Vue 3 decoupled API architecture.

### 2. Multi-User Data Isolation Strategy
- **Decision**: Enforce `user_id` parameter at the Service and Repository layer for all record queries, creations, mutations, and category lookups.
- **Rationale**: Prevents IDOR (Insecure Direct Object Reference) vulnerabilities. Every query implicitly appends `WHERE user_id = :currentUserId` (or for categories: `WHERE user_id = :currentUserId OR is_system = 1`).
- **Alternatives Considered**:
  - Global tenant filter / Hibernate Filter: Considered overly complex for a 3-table schema. Explicit repository methods provide clear readability and testability.

### 3. Dual-Tier Category Data Model
- **Decision**: Single `sys_category` table with nullable `user_id` and `is_system` BIT flag:
  - System default categories: `user_id = NULL`, `is_system = 1`.
  - User custom categories: `user_id = <userId>`, `is_system = 0`.
- **Rationale**: Eliminates table duplication while allowing a unified `GET /api/v1/categories` query to union both sets efficiently in a single query sorted by `sort_order`.

### 4. Fast Ledger Entry UX (Option A with Option B Hook)
- **Decision**: 
  - Front-end implements **Option A**: A high-speed structured input bar with Type Toggle (Expense/Income), Amount, Category Dropdown, Note, and Record Date.
  - Back-end defines a `SmartParserService` interface with a `RegexSmartParserServiceImpl` placeholder for Option B parsing.
- **Rationale**: Option A guarantees zero input ambiguity and instant validation for daily accounting, while Option B's backend interface allows drop-in NLP/regex text parsing without altering the database schema or API contract.

### 5. Strict Offline (No-CDN) Static Asset Bundle
- **Decision**: Vendored copies of Bootstrap 5.3.3, Vue 3.4.x, Axios 1.7.x, and SweetAlert2 11.x placed in `src/main/resources/static/lib/`.
- **Rationale**: Ensures 100% offline functionality in air-gapped or restricted intranet development environments.

### 6. Swiss Design Style Token System
- **Decision**: Establish custom CSS variables in `swiss-style.css` matching international typographic design standards:
  - Color Tokens: `--swiss-red: #DC2626`, `--swiss-black: #111111`, `--swiss-gray-dark: #262626`, `--swiss-gray-light: #F4F4F5`, `--swiss-bg: #F8F9FA`.
  - Typography: High-contrast sans-serif font stack, uppercase mono-spaced tracking labels (`SYS-LEDGER // 01`), heavy weights (`700`/`800`) for headers.
  - Geometry: `border-radius: 0px` (sharp edges), `border: 1px solid #111111`, zero diffused drop-shadows.

## Risks / Trade-offs

- **[Risk] ThreadLocal Memory Leaks**: If `UserContext` is not cleared after request processing, worker threads in the Tomcat pool could retain stale user identities.
  - ➔ **Mitigation**: `JwtAuthenticationFilter` wraps execution in a `try-finally` block ensuring `UserContext.clear()` is unconditionally invoked.
- **[Risk] Category Deletion with Orphaned Records**: Deleting a custom category could break foreign key constraints or orphan historical ledger records.
  - ➔ **Mitigation**: `CategoryService.deleteCategory()` explicitly checks `account_record` count; if linked records exist, a `409 Conflict` error is returned preventing deletion.
- **[Risk] Precision Loss in Financial Amounts**: Using floating-point numbers (`FLOAT`/`DOUBLE`) causes rounding errors.
  - ➔ **Mitigation**: Use `BigDecimal` in Java entities/DTOs and `DECIMAL(12, 2)` in MS SQL Server.

## Migration Plan

1. **Database Initialization**: Execute `schema.sql` and `data.sql` against MS SQL Server `tibame_account` database (creating `sys_user`, `sys_category`, `account_record` tables and seeding system categories).
2. **Backend Deployment**: Start Spring Boot application; verify connection pool and JPA validation.
3. **Frontend Asset Verification**: Load `/` and `/ledger` in offline browser mode; verify that zero external network calls (CDN) are initiated.
4. **Rollback Strategy**: Dropping tables or executing table migration undo scripts returns the database to clean slate.
