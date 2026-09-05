## Context

See `proposal.md` for background and problem motivation. Currently, `application-test.yml` and `application.yml` use unbracketed Map keys (`format_sql`, `com.tibame`, etc.) causing `YAML_SHOULD_ESCAPE` diagnostics in Spring Tools 4. Concurrently, `JwtTokenServiceImpl` consumes `jwt.secret` and `jwt.expiration-ms` via scattered `@Value` injections without a configuration processor or `@ConfigurationProperties` class, triggering `YAML_UNKNOWN_PROPERTY` diagnostics and depriving developers of IDE autocomplete and type safety.

## Goals / Non-Goals

**Goals:**
- Eliminate all 5 IDE diagnostic warnings/hints across `application.yml` and `application-test.yml` (`YAML_SHOULD_ESCAPE`, `YAML_UNKNOWN_PROPERTY`).
- Introduce `spring-boot-configuration-processor` to compile-time generate `META-INF/spring-configuration-metadata.json`.
- Encapsulate JWT configuration into `com.tibame.config.JwtProperties` with `@ConfigurationProperties(prefix = "jwt")`.
- Refactor `JwtTokenServiceImpl` to inject `JwtProperties` in the Spring container constructor while retaining an overloaded constructor `(String secret, long expirationMs)` for pure unit testing without Spring context.
- Standardize all Map keys in `application.yml` and `application-test.yml` using bracket notation `"[key]"`.
- Maintain 100% test compatibility across all unit, integration, and E2E test suites.

**Non-Goals:**
- Modifying JWT signing algorithms (HMAC-SHA256), payload structures, token lifecycle, or business contracts.
- Suppressing IDE diagnostics globally through workspace settings (we pursue structural compliance).
- Converting non-map properties to bracket notation.

## Decisions

### Decision 1: Option A (Full Specification & Configuration Properties) over Option B (Suppress Warnings)
- **Rationale**: Option A aligns with Spring Boot 3 enterprise best practices. It aggregates configuration, enables type-safe compile-time checking, centralizes default values, and produces IDE metadata for autocomplete and inline documentation.
- **Alternatives Considered**: Option B (only escaping Map keys and disabling `YAML_UNKNOWN_PROPERTY` in IDE settings) was rejected because it fails to provide code completion and leaves configuration scattered.

### Decision 2: Add `spring-boot-configuration-processor` as `<optional>true</optional>`
- **Rationale**: The annotation processor is solely needed during compilation to inspect `@ConfigurationProperties` and generate metadata under `target/classes/META-INF/spring-configuration-metadata.json`. Marking it optional ensures it is not packaged into final production artifacts.

### Decision 3: Backward-Compatible Constructor Overloading in `JwtTokenServiceImpl`
- **Rationale**: The Spring-managed constructor will inject `JwtProperties`:
  ```java
  @Autowired
  public JwtTokenServiceImpl(JwtProperties jwtProperties) {
      this(jwtProperties.getSecret(), jwtProperties.getExpirationMs());
  }
  ```
  The legacy parameter-based constructor `public JwtTokenServiceImpl(String secret, long expirationMs)` is preserved so isolated unit tests (e.g. `TokenServiceTest`) do not require synthetic bean instantiation or mocks.

### Decision 4: Bracket Notation `"[key]"` for YAML Map Keys
- **Rationale**: Keys in `spring.jpa.properties.hibernate` and `logging.level` map to `Map<String, ?>`. Keys with `.` (dots) or `_` (underscores) are ambiguous under Spring Boot relaxed binding. Enclosing them in brackets (e.g., `"[format_sql]"`, `"[com.tibame]"`) explicitly declares them as literal keys.

## Risks / Trade-offs

- **[Risk] IDE Metadata Stale or Delayed** → **Mitigation**: Trigger `mvn compile` or build project after adding dependencies and `JwtProperties` to immediately generate `META-INF/spring-configuration-metadata.json` on the classpath.
- **[Risk] Existing Tests or Mocking Broken** → **Mitigation**: Retaining the overloaded parameter constructor in `JwtTokenServiceImpl` preserves complete backward compatibility for non-Spring unit tests.
