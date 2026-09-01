## Purpose

Provides secure, stateless user registration, authentication, password verification, and identity context management for the daily ledger platform.

## Requirements

### Requirement: User Registration
The system SHALL allow new users to register an account by providing a unique username, a password, an email address, and an optional display name.

#### Scenario: Successful user registration
- **WHEN** a client sends a registration request with a unique username, valid email, and compliant password
- **THEN** the system MUST create the user account with password securely hashed (via BCrypt) and return a 200/201 success response

#### Scenario: Registration with duplicated username
- **WHEN** a client attempts to register with a username that already exists in the system
- **THEN** the system MUST reject the request with an error code and a descriptive message indicating the username is already taken

#### Scenario: Registration with invalid input fields
- **WHEN** a client submits registration data missing mandatory fields (such as empty username or blank password)
- **THEN** the system MUST reject the request with validation errors

### Requirement: User Login and Token Issuance
The system SHALL authenticate user credentials and issue a signed stateless JWT token upon successful authentication.

#### Scenario: Successful login with valid credentials
- **WHEN** a user submits valid username and password credentials to the login endpoint
- **THEN** the system MUST verify the credentials against the stored password hash and return a response containing the JWT token, expiration details, and user profile summary

#### Scenario: Failed login with invalid credentials
- **WHEN** a user provides an incorrect password or non-existent username
- **THEN** the system MUST reject the authentication request with an unauthorized error status without leaking whether the username exists

### Requirement: Authenticated Identity Context and Profile Access
The system SHALL extract and validate the JWT token from incoming request headers, establishing the user context for protected API operations.

#### Scenario: Accessing current user profile with valid token
- **WHEN** an authenticated client sends a request to `/api/v1/auth/me` with a valid Bearer token
- **THEN** the system MUST return the current user's identity details (id, username, email, displayName)

#### Scenario: Accessing protected endpoints without a token or with an invalid token
- **WHEN** a client attempts to access a protected endpoint without an `Authorization` header or with an expired/tampered token
- **THEN** the system MUST reject the request with HTTP 401 Unauthorized status
