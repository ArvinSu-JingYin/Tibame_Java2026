## ADDED Requirements

### Requirement: Automated Configuration Governance and Test Gates
The test infrastructure SHALL provide automated verification test gates in the Maven test phase to statically lint YAML configuration files against escaping violations and ensure configuration metadata generation for strongly-typed properties.

#### Scenario: Static linting of YAML Map keys for escaping violations
- **WHEN** the test suite executes `YamlConfigurationLintTest` during `mvn test`
- **THEN** all target YAML configuration files (including `application.yml` and `application-test.yml`) MUST be parsed and scanned, asserting that any Map key containing dots or underscores under hierarchical property prefixes (such as `logging.level` or `properties.hibernate`) is escaped with bracket notation `["..."]`, failing the test build if violations are detected

#### Scenario: Compile-time configuration metadata existence and prefix validation
- **WHEN** the test suite executes `ConfigurationMetadataTest` during or after compilation
- **THEN** the test MUST verify the presence of `target/classes/META-INF/spring-configuration-metadata.json` and validate that custom configuration properties (such as `jwt` and `crypto.password.policy`) are registered, failing the test build if metadata is missing or incomplete

#### Scenario: Enforcement of strongly-typed configuration injection
- **WHEN** business services or components require custom application configurations
- **THEN** components MUST inject strongly-typed `@ConfigurationProperties` classes instead of ad-hoc `@Value` annotations for hierarchical properties, adhering to the repository's configuration boundary rules
