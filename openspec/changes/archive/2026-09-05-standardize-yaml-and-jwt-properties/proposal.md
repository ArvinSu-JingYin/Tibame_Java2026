## Why

Spring Boot Tools (Spring Tools 4) reports 5 diagnostic warnings/hints in `application-test.yml` (`YAML_SHOULD_ESCAPE` due to unescaped Map keys with dots/underscores and `YAML_UNKNOWN_PROPERTY` due to ad-hoc `@Value` injection of `jwt.*` properties without configuration metadata). Standardizing YAML map key escaping and transitioning JWT configuration to strongly-typed `@ConfigurationProperties` with `spring-boot-configuration-processor` eliminates IDE warnings, provides IDE auto-completion and type safety, and satisfies the project's Zero-Warning engineering standards.

## What Changes

- **YAML Map Key Escaping**: Update unescaped Map keys in `src/test/resources/application-test.yml` and `src/main/resources/application.yml` using bracket notation `"[format_sql]"`, `"[com.tibame]"`, `"[org.springframework.web]"`, and `"[org.hibernate.SQL]"`.
- **Configuration Processor Dependency**: Add `spring-boot-configuration-processor` (`<optional>true</optional>`) to `pom.xml` to generate `META-INF/spring-configuration-metadata.json` during compilation.
- **Strongly-Typed JWT Properties**: Create `com.tibame.config.JwtProperties` annotated with `@ConfigurationProperties(prefix = "jwt")` providing default secrets and expiration times.
- **Refactor `JwtTokenServiceImpl`**: Inject `JwtProperties` in the primary Spring constructor while preserving an overloaded `(String secret, long expirationMs)` constructor for test backward compatibility.
- **IDE Zero-Warning Conformance**: Ensure zero warnings in the IDE Problems panel and verify IDE auto-completion for configuration keys.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `engineering-standards`: Enforce Spring Boot YAML configuration key escaping rules and strongly-typed `@ConfigurationProperties` metadata generation under project engineering standards and Zero-Warning DoD.

## Impact

- `pom.xml`: Added compile-time optional dependency `spring-boot-configuration-processor`.
- `com.tibame.config.JwtProperties`: New configuration properties component.
- `com.tibame.common.crypto.token.impl.JwtTokenServiceImpl`: Refactored constructor to accept `JwtProperties`.
- `src/main/resources/application.yml` and `src/test/resources/application-test.yml`: Bracket notation applied to Map keys.
- Test suites (`TokenServiceTest`, `AuthApiE2ETest`, and general unit/integration tests): All existing tests remain 100% compatible.
