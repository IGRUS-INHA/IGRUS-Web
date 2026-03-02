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
- Latest version as of 2026-03-02: V45 (V45__create_file_metadata_table.sql)
- Version conflict check is essential before committing (backend CLAUDE.md rule 17)

### OpenAPI Generator Config (Verified from build.gradle)
- `useTags: 'true'` -- tag-based interface generation (1 tag = 1 interface)
- `skipDefaultInterface: 'true'` -- NO default methods; implementing class MUST implement ALL methods
- `interfaceOnly: 'true'` -- generates interfaces only (no controller stubs)
- `documentationProvider: 'none'` -- no SpringDoc annotations in generated code
- `useBeanValidation: 'true'` -- Bean Validation annotations on generated interfaces
- Tag name conversion: spaces removed, PascalCase + "Api" suffix (e.g., "Password Authentication" -> "PasswordAuthenticationApi")

### OpenAPI Tag Structure (Verified 2026-03-01)
- Total 25 tags defined in openapi.yaml
- `Inquiry` tag shared by 3 controllers (Admin/Guest/Member) -- requires tag split before contract-first migration
- `PostLike` tag has NO space (unlike `Comment Like` which has a space)
- `MyPage` tag has NO space

## Common Plan Issues (Patterns to Watch)

### Frequently Missing Items
1. API version prefix `/api/v1/`
2. Frontend test tasks (backend tests well-planned, frontend tests often absent)
3. Concurrency handling for state transitions
4. URL path encoding for path variables containing slashes
5. Cross-document API path consistency (plan may be fixed but criteria/test-case docs still use old paths)
6. Documentation update tasks (required by root CLAUDE.md but often omitted)
7. `skipDefaultInterface: 'true'` implications not considered when multiple controllers share a tag

### Recurring Ambiguities
- Transaction boundary design presented with multiple options instead of a single recommendation
- DTO naming deviations from convention without explicit justification
- SDK/library choice presented as "A or B" without final decision
- HttpServletRequest/Response handling in openapi-generator presented as "check if supported" without concrete strategy
- Cross-domain service reuse: "delegate to existing service OR extract internal logic" left undecided
- Method signature changes: "overload OR extend" left undecided

### Verified Pattern: operationId count verification
- Always count operationIds in the actual YAML file; plan descriptions may have typos (e.g., "13" vs actual "12")
- Use `grep -c "operationId:" <file>` to verify

## Cross-Domain Integration Patterns (Verified 2026-03-02)

### SurveyResponseService.submitResponse()
- Calls `survey.isAcceptingResponses()` which checks `PUBLISHED + OPEN + trashedAt == null`
- Also calls `validateAccessLevel()` for survey accessLevel
- Plans reusing this method for cross-domain integration MUST check if these validations conflict
- `SurveyAnswerValidator.validate(survey, answers)` is standalone (no access/state checks)
- Cross-domain should prefer `SurveyAnswerValidator` + direct repo calls over full `submitResponse()`

### Event Registration Flow
- `registerEvent(Long eventId, Long userId)` -- current signature has NO surveyAnswers param
- `handleReRegistration()` is private, called at step 5 (before validateEventIsOpen)
- Latest Flyway for events: part of V45 series

### SurveyResponse Entity Pattern
- `SurveyResponse.create(Survey survey, User user)` -- takes User object, NOT userId
- `createAnswers(response, survey, answers)` -- private method in SurveyResponseService (line 190)
- Answer creation is separate from response creation; plans must account for both steps
- Plans that bypass submitResponse() need a strategy for createAnswers() reuse (extract component or duplicate)

## Review History

### Storage: image-presigned-url-task-plan.md
- Round 1: FAIL (3 Critical: API path missing /api/v1/, Spring Boot version wrong, Flyway column naming missing)
- Round 2: PASS (All 3 Critical resolved. 5 Recommended: cross-doc API paths, DTO naming, DELETE path slash encoding, transaction design decision, SDK version confirmation)

### OpenAPI Contract-First Migration: task-plan.md
- Round 1: FAIL (2 Critical: Inquiry tag split undecided + skipDefaultInterface makes partial impl impossible, HttpServletRequest/Response strategy undecided + suggested alternatives technically infeasible)
- Round 2: PASS (Both Critical resolved. TASK-003 splits Inquiry into 3 tags with operationId-level detail. TASK-004 defines RequestContextHolder PoC with 3 verification items + fallback. 5 Recommended: operationId count typo 13->12, frontend Orval impact analysis, Phase 1 pilot feedback loop clarification, documentation task during migration, TASK-004 fallback completeness)

### Runtime Validation: runtime-validation-task-plan.md
- Round 1: FAIL (2 Critical: TASK-220 `implementation` scope without risk justification, TASK-221 test context caching strategy undecided)
- Round 2: PASS (Both Critical resolved. TASK-220 has risk analysis table + alternative review. TASK-221 confirms "Filter activation after migration complete" with justifications + PoC items + rejected alternatives)

### Test Infrastructure Constraints (Verified 2026-03-02)
- `backend/src/test/CLAUDE.md` strictly forbids adding `@ActiveProfiles`, `@Import`, or `@MockitoBean` in test subclasses
- All integration tests share a single Spring Context via `ServiceIntegrationTestBase` (`@ActiveProfiles("test")`)
- Adding a new profile (e.g., `test-validation`) creates a separate context, increasing CI time
- Plans that add runtime behavior to `test` profile (e.g., Servlet Filters) MUST analyze impact on context caching

### Survey-Event Registration: task-plan.md
- Round 1: FAIL (2 Critical: SurveyResponseService.submitResponse() isAcceptingResponses() conflicts with SEVT-INV-10 + delegation method undecided, survey CLOSED state + surveyAnswers included behavior undefined violating survey INV-09)
- 6 Recommended: DECISION-01 unconfirmed, registerEvent() signature strategy undecided, SEC-SEVT-05 integration test coverage, documentation task missing, frontend test plan absent, requestBody required:false not specified
- Round 2: PASS (Both Critical resolved. submitResponse() replaced with SurveyAnswerValidator.validate() + SurveyResponseRepository.save(). 8-branch decision matrix added for responseStatus x surveyAnswers x existingResponse. 6 Recommended: pseudocode SurveyResponse.create() signature mismatch, createAnswers() reuse strategy undefined, DECISION-01 confirmation inconsistency, documentation TASK missing, frontend test plan absent, registerEvent() signature overload vs extend undecided)

### Gradle Dependency Scope Awareness
- `implementation` scope includes dependency in production JAR
- Libraries only used in dev/test should prefer `testImplementation` or conditional inclusion
- Plans adding dev-only tools via `implementation` should explicitly justify the production footprint

## Review Checklist Additions
- [See review-checklist.md for detailed checklist](./review-checklist.md)
