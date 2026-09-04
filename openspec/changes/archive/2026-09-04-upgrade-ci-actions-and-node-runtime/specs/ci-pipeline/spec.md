## MODIFIED Requirements

### Requirement: Pull Request Compliance and Convention Gatekeeper
The CI system SHALL enforce pull request title, commit history, and specification consistency quality gates on all pull request events targeting the `main` or `dev` branches, and SHALL support manual execution via `workflow_dispatch` with defensive bypass of PR title validation.

#### Scenario: Valid pull request title and conventional commit messages
- **WHEN** a developer or automated agent opens or synchronizes a pull request targeting `main` or `dev` with title and commit messages adhering to `<type>(<scope>): <Traditional Chinese text>`
- **THEN** the `pr-compliance` job MUST validate that the type matches one of `(feat|fix|refactor|perf|test|style|docs|chore|revert)`, the scope matches one of `(controller|service|repository|entity|dto|config|security|exception|view|common|build|specs)`, contains Traditional Chinese characters (`[\u4e00-\u9fa5]`), does not end with a period, and passes the check

#### Scenario: Rejection of non-compliant commit message or pull request title
- **WHEN** a pull request targeting `main` or `dev` contains a title or commit message without Chinese characters (e.g. English-only summary), with an unauthorized scope, or violating the conventional commit format
- **THEN** the `pr-compliance` job MUST fail with descriptive error messages, blocking the pull request from merging

#### Scenario: OpenSpec specification validity verification
- **WHEN** a pull request is submitted or updated targeting `main` or `dev`, or triggered manually
- **THEN** the `pr-compliance` job MUST install OpenSpec tooling (`@fission-ai/openspec`) under Node.js 22 and execute `openspec validate --all`, failing the check if any syntax, schema, or incomplete artifact validation errors are detected

#### Scenario: Manual workflow dispatch execution with defensive title bypass
- **WHEN** the `ci-pr` workflow is executed manually via `workflow_dispatch` without an active pull request context
- **THEN** the `pr-compliance` job MUST detect that `github.event_name` is not `pull_request`, skip the PR title presence and convention check, and proceed to validate OpenSpec specifications and recent commit messages without failing the workflow
