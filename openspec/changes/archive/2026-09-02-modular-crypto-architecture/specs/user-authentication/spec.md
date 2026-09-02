## MODIFIED Requirements

### Requirement: User Registration
The system SHALL allow new users to register an account by providing a unique username, a password, an email address, and an optional display name, validating password strength before account creation.

#### Scenario: Successful user registration
- **WHEN** a client sends a registration request with a unique username, valid email, and compliant password meeting complexity policies
- **THEN** the system MUST create the user account with the password securely hashed via the modular `PasswordService` (defaulting to BCrypt) and return a 200/201 success response

#### Scenario: Registration with duplicated username
- **WHEN** a client attempts to register with a username that already exists in the system
- **THEN** the system MUST reject the request with an error code and a descriptive message indicating the username is already taken

#### Scenario: Registration with invalid input fields
- **WHEN** a client submits registration data missing mandatory fields (such as empty username or blank password)
- **THEN** the system MUST reject the request with validation errors

#### Scenario: Registration with non-compliant password policy
- **WHEN** a client submits a registration request with a password failing complexity rules (e.g. insufficient length or character requirements)
- **THEN** the system MUST reject the request with a password policy violation error

### Requirement: User Login and Token Issuance
The system SHALL authenticate user credentials, evaluate whether the stored password hash requires security parameter upgrade, and issue a signed stateless JWT token upon successful authentication.

#### Scenario: Successful login with valid credentials
- **WHEN** a user submits valid username and password credentials to the login endpoint
- **THEN** the system MUST verify the credentials against the stored password hash via `PasswordService`, seamlessly upgrade the stored hash in the database if `needsUpgrade` returns true, and return a response containing the JWT token, expiration details, and user profile summary

#### Scenario: Failed login with invalid credentials
- **WHEN** a user provides an incorrect password or non-existent username
- **THEN** the system MUST reject the authentication request with an unauthorized error status without leaking whether the username exists

## ADDED Requirements

### Requirement: Password Policy Enforcement
The system SHALL provide a configurable password policy validator enforcing length and character composition rules across user creation and password update workflows.

#### Scenario: Password policy validation pass
- **WHEN** a password satisfying minimum length and character variety rules is validated
- **THEN** the validator MUST return success without throwing validation exceptions

#### Scenario: Password policy validation failure
- **WHEN** a password violating configured rules is validated
- **THEN** the validator MUST throw an invalid password exception with specific rule breach descriptions
