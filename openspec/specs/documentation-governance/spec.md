## Purpose

Defines the repository-level documentation governance standard, dual-track directory architecture (explorations vs. specifications), top-level portal indexing, and document lifecycle transition rules for the project.

## Requirements

### Requirement: Dual-Track Directory Structure
The repository SHALL organize all technical and architectural documentation under `docs/` into two distinct, non-overlapping primary tracks: `docs/explorations/` for research and exploratory artifacts, and `docs/specifications/` for authoritative system contracts and operational guides.

#### Scenario: Segregating exploratory and spikes documents
- **WHEN** developers author architectural spikes, trade-off analyses, troubleshooting post-mortems, or historical consolidated proposals
- **THEN** the documents MUST be stored within `docs/explorations/` and marked as historical or contextual artifacts

#### Scenario: Segregating authoritative specification documents
- **WHEN** developers deliver finalized functional requirements, database DDL schemas, deployment scripts, QA definitions, or user operation manuals (SOPs)
- **THEN** the documents MUST be stored within `docs/specifications/` as the single source of truth (SSOT)

### Requirement: Top-Level Documentation Portal
The repository SHALL provide a central index portal at `docs/README.md` that serves as the entry point for all project documentation.

#### Scenario: Navigating repository documentation from root docs
- **WHEN** a contributor or user navigates to the `docs/` root folder
- **THEN** the system MUST present `docs/README.md` containing the dual-track governance rules, repository documentation map, and guidelines for adding new documents

#### Scenario: Preventing unclassified root markdown clutter
- **WHEN** any new markdown file is added to the documentation system
- **THEN** it MUST NOT be placed directly at `docs/` root except for `docs/README.md`

### Requirement: Topic Navigation Matrix
The top-level portal SHALL provide a cross-track topic matrix mapping major domains (such as Identity & Security, Ledger Core, Testing Pyramid, and DevOps & Scripting) directly to their respective exploration and specification documents.

#### Scenario: Cross-referencing exploration context and finalized specification by domain
- **WHEN** a developer looks up a specific domain (e.g., Testing Strategy or Cryptography Architecture) in `docs/README.md`
- **THEN** the topic matrix MUST provide direct relative hyperlinks to both the exploratory research documents in `docs/explorations/` and the authoritative specifications in `docs/specifications/`

### Requirement: Document Lifecycle and Governance Workflow
The documentation system SHALL govern document evolution aligned with the OpenSpec change lifecycle, transitioning knowledge from initial exploratory spikes into formal specifications upon feature completion.

#### Scenario: Transitioning exploratory findings to authoritative specifications
- **WHEN** an exploration report in `docs/explorations/` leads to an approved and implemented OpenSpec change
- **THEN** the final deliverable specifications MUST be deposited into `docs/specifications/` and indexed in `docs/README.md` without modifying the original exploration context

#### Scenario: Relative hyperlink integrity
- **WHEN** any document is relocated between directories or referenced from `docs/README.md`
- **THEN** all relative markdown links across the documentation repository MUST resolve to valid existing target files
