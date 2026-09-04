## 1. Build Configuration & Metadata Processor

- [x] 1.1 Add `spring-boot-configuration-processor` (`<optional>true</optional>`) dependency to `pom.xml` and verify `mvn test-compile` succeeds without dependency resolution issues

## 2. Strongly-Typed JWT Configuration Component

- [x] 2.1 Create `com.tibame.config.JwtProperties` annotated with `@ConfigurationProperties(prefix = "jwt")` and `@Component`, and verify that `mvn compile` generates `target/classes/META-INF/spring-configuration-metadata.json` containing `jwt.secret` and `jwt.expiration-ms`
- [x] 2.2 Refactor `com.tibame.common.crypto.token.impl.JwtTokenServiceImpl` to inject `JwtProperties` via `@Autowired` constructor while retaining the legacy `(String secret, long expirationMs)` constructor, and verify that `TokenServiceTest` passes

## 3. Standardize YAML Configurations & IDE Diagnostics Elimination

- [x] 3.1 Update `src/test/resources/application-test.yml` using bracket notation `"[format_sql]"`, `"[com.tibame]"`, `"[org.springframework.web]"`, and `"[org.hibernate.SQL]"`, verifying that `YAML_SHOULD_ESCAPE` and `YAML_UNKNOWN_PROPERTY` warnings in the IDE Problems panel are completely resolved
- [x] 3.2 Update `src/main/resources/application.yml` using bracket notation `"[format_sql]"`, `"[com.tibame]"`, `"[org.springframework.web]"`, and `"[org.hibernate.SQL]"` to maintain environment configuration consistency across all profiles

## 4. Quality Gate & Integration Verification

- [x] 4.1 Run complete unit and integration test suite via `mvn test` and verify 100% test pass rate
- [x] 4.2 Run end-to-end authentication and security tests (e.g., `TenantIsolationSecurityE2ETest`, `AuthApiE2ETest`) and verify Zero-Warning Definition of Done (DoD) compliance
