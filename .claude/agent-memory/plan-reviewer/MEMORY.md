# Plan Reviewer Memory

## Project Conventions (Verified)

### API Path Convention
- All API endpoints use `/api/v1/` prefix (e.g., `/api/v1/boards`, `/api/v1/events`, `/api/v1/admin/users`)
- Plans that omit the version prefix are a CRITICAL issue

### Spring Boot Version
- Actual version: **3.5.9** (verified from `build.gradle` plugin `org.springframework.boot` version)
- Root CLAUDE.md incorrectly states "Spring Boot 4.0.1" -- always verify against `build.gradle`

### AWS Dependencies
- `spring-cloud-aws-starter-secrets-manager:3.4.0` is already in use
- New `spring-cloud-aws` modules should use the same version (3.4.0) for consistency

### DTO Naming Convention (from backend CLAUDE.md)
- Request: `{Action}{Domain}Request` (e.g., `CreatePostRequest`)
- Response: `{Domain}{Action}Response` (e.g., `PostDetailResponse`)
- Plans often use `{Action}{Domain}Response` instead -- flag as Recommended fix

### Entity Base Classes
- `BaseEntity`: `createdAt`, `updatedAt`, `createdBy`, `updatedBy` (JPA Auditing)
- `SoftDeletableEntity extends BaseEntity`: adds `deleted`, `deletedAt`, `deletedBy`
- Flyway migrations MUST align column names with JPA's naming strategy for these fields

### ErrorCode Pattern
- Each domain has its own `{Domain}ErrorCode` enum implementing `ErrorCode` interface
- Located in `igrus.web.{domain}.exception` package
- Exception classes extend `CustomBaseException`

### Flyway Migration
- Latest version as of 2026-02-27: V45
- Version conflict check is essential before committing (backend CLAUDE.md rule 17)

## Common Plan Issues (Patterns to Watch)

### Frequently Missing Items
1. API version prefix `/api/v1/`
2. Frontend test tasks (backend tests well-planned, frontend tests often absent)
3. Concurrency handling for state transitions
4. URL path encoding for path variables containing slashes
5. Cross-document API path consistency (plan may be fixed but criteria/test-case docs still use old paths)

### Recurring Ambiguities
- Transaction boundary design presented with multiple options instead of a single recommendation
- DTO naming deviations from convention without explicit justification
- SDK/library choice presented as "A or B" without final decision
- Service class placement presented as "A or B" without final decision (e.g., AdminEventService vs EventService)
- Controller package placement presented as two options without decision

### Frequently Missing Downstream Impact Analysis
- When adding visibility/access control to a domain, check ALL services that query that domain
- Example: Adding visibility filter to EventService but missing EventRegistrationService
- Pattern: Search for all `Repository.findById()` / `Repository.findByXxx()` callers across services

## Review History

### Storage: image-presigned-url-task-plan.md
- Round 1: FAIL (3 Critical: API path missing /api/v1/, Spring Boot version wrong, Flyway column naming missing)
- Round 2: PASS (All 3 Critical resolved. 5 Recommended: cross-doc API paths, DTO naming, DELETE path slash encoding, transaction design decision, SDK version confirmation)

### Event: event-visibility-task-plan.md
- Round 1: FAIL (2 Critical: EventRegistrationService visibility filter missing, admin list API missing visibility filter param)
- 5 Recommended: Flyway column name pattern verification, documentation TASK missing, AdminEventService split undecided, unpublish audit trail for registration close, controller package undecided
- Round 2: PASS (Both Critical resolved via TASK-020/021 addition and TASK-007/010/011/017 updates)
- 5 Recommended persisting: AdminEventService split still undecided, criteria doc log message missing visibility, GAP-EVT-45 not in criteria doc, documentation TASK still missing, unpublish registration close audit trail undecided

## Review Checklist Additions
- [See review-checklist.md for detailed checklist](./review-checklist.md)
