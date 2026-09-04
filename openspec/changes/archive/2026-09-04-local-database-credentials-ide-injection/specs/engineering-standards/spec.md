## ADDED Requirements

### Requirement: Local Development Credential Isolation and IDE Launch Configurations
The development environment SHALL enforce zero-code credential injection for local database instances via externalized process environment variables and provide standardized, Git-isolated IDE launch configurations.

#### Scenario: Zero-code MS SQL startup via IDE environment variable injection
- **WHEN** a developer launches the application targeting a local MS SQL Server instance with custom database credentials
- **THEN** the credentials (`DB_PASSWORD`, `DB_USERNAME`) MUST be injected dynamically via process environment variables configured in `.vscode/launch.json` under active profile `mssql`, requiring zero code or YAML file modifications in the tracked repository

#### Scenario: Prevention of credential leakage to version control
- **WHEN** local execution configurations containing personal database credentials are created in `.vscode/launch.json`
- **THEN** Git ignore rules MUST exclude `.vscode/launch.json` from version control, ensuring `git status` remains clean and preventing credential exposure in commit history, while providing a committed `.vscode/launch.json.example` template for onboarding

#### Scenario: Seamless fallback to embedded database without credentials
- **WHEN** the application is launched using the default profile or H2 testing configuration without external credentials provided
- **THEN** the system MUST successfully connect to the in-memory H2 database with default development credentials without throwing missing-property or connection authentication exceptions

#### Scenario: Remote CI pipeline decoupling and zero interference
- **WHEN** automated CI workflows (such as `ci-pr.yml` and `ci-main.yml`) execute Maven builds and test phases in ephemeral cloud runners
- **THEN** the build pipeline MUST operate completely independent of developer workstation IDE configurations, utilizing default H2 configurations without requiring or referencing local database credentials
