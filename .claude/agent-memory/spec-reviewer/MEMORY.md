# Spec Reviewer Agent Memory

See `review-history.md` for detailed per-review results.

## Project Patterns

### Flyway Migration
- Latest version on main: V46 (add_event_visibility_column)
- Column naming: `{table_name}_{column_name}` (e.g., `file_metadata_object_key`)
- **CRITICAL**: FK REFERENCES must match actual PK column names (`surveys_id` NOT `survey_id`)
- Task plans may contain wrong column names -- always cross-check with actual migration files

### Entity Hierarchy
- `BaseEntity`: createdAt, updatedAt, createdBy, updatedBy (Instant, AuditingEntityListener)
- `SoftDeletableEntity extends BaseEntity`: deleted (boolean), deletedAt (Instant), deletedBy (Long)

### Exception Pattern
- `ErrorCode` interface -> domain-specific `XxxErrorCode enum implements ErrorCode`
- `CustomBaseException` abstract -> domain exceptions extend it

### Repository Pattern
- JpaRepository-based, soft delete aware: `findByXxxAndDeletedFalse`

## Recurring Review Patterns

### Common Issues Found
1. Flyway version conflicts between feature branches
2. Task plan specifies both raw and soft-delete queries, implementer only creates soft-delete-aware version
3. PathVariable patterns not handling domain values with slashes (S3 Object Keys)
4. Logic duplication between src/main and src/test (should use shared factory)
5. Task plan FK column names may be wrong -- always verify against actual migrations

### When Criteria Doc and Task Plan Differ
- Task plan takes precedence for implementation review (it's the concrete spec)
- Flag the gap as Recommended, not Critical
- Example: criteria says "ignore answers" but task plan says "throw duplicate exception" -- task plan wins

## Domain Checklists

### Storage Service Layer
1. Bean Validation matches criteria input domain
2. State transition methods include precondition validation
3. Logs do NOT include sensitive data (e.g., Presigned URLs)
4. @Transactional + external call ordering matches rollback policy

### API Layer
1. PathVariable patterns must handle domain values with slashes
2. Security config rule ordering: specific before generic
3. HTTP status codes must match TC expected results
4. authenticated() = ASSOCIATE+ (no SUSPENDED role in this project)

### Test Layer
1. SDK params verified via ArgumentCaptor
2. Log assertions: positive (required fields) + negative (sensitive data absent)
3. Time-sensitive tests override createdAt via native SQL
4. Boundary tests: strict less-than vs less-than-or-equal

### Infrastructure Layer
1. Singleton for expensive initialization (volatile + DCL)
2. @Profile + @ConditionalOnProperty dual gating
3. Shared factory for common logic between src/main and src/test
4. Graceful degradation: try-catch in @Configuration constructors

### Contract-First Migration
1. @Override count = interface method count
2. No residual Swagger/Spring MVC annotations
3. SecurityUtils.requireCurrentUser() replaces @AuthenticationPrincipal
4. PageableUtils.of() replaces @ParameterObject Pageable

### Survey-Event Foundation
1. FK REFERENCES must match actual PK column names
2. Extracting private methods to @Component: verify all call sites updated
3. OpenAPI nullable fields must NOT be in `required` list

### Survey-Event Entity/Service
1. Entity factory/update signatures include surveyId as last parameter
2. Service: distinguish "not found" from "trashed" (trashedAt != null)
3. When task plan splits impl and tests into separate TASKs, only review impl scope
4. COMPLETED state blocks update() for new fields (existing EVT-INV-07 covers)

### Survey-Event Integration API
1. Branch matrix: verify ALL 8 cases (OPEN/CLOSED/NOT_STARTED x answers x existing response)
2. Atomicity: registerEventWithSurvey() as private method within @Transactional class
3. Re-registration: if task plan only specifies "response existence check", don't flag missing surveyAnswers as Critical
4. Controller hardcoding null for surveyAnswers is acceptable when OpenAPI spec TASK is pending
5. Log levels: info for business rejections, warn for abnormal ops, debug for confirmations

### Survey-Event OpenAPI & DTO
1. openapi-generator initializes nullable arrays as `new ArrayList<>()` -- controller must check `isEmpty()` too, not just null
2. `requestBody.required: false` in OpenAPI generates `@RequestBody(required = false)` correctly
3. Generated model reuses `UpdateMyResponseRequestAnswersInner` for SubmitAnswerRequest -- verify all 5 fields match
4. surveyId must appear in ALL response DTOs (Detail, List, Create) + Admin variants (7 schemas total)
5. Controller mapping helpers must include `.surveyId()` for all response types (EventController, AdminEventController)

### Survey-Event Test Group
1. Branch matrix: ensure ALL 8 cases have corresponding tests, especially #4 (CLOSED+answers+no response)
2. When checklist says "covered by existing tests", verify the existing test actually tests the survey-linked scenario
3. Controller-level security tests (TC-050~055) require MockMvc/HTTP assertions, not just Mockito service tests
4. TC-015 maps to CLOSED #5 (not OPEN) due to actual code branch logic -- accept if reasoning is documented
5. TC-060 (concurrency) is high difficulty and acceptable as Recommended, not Critical

## Review Results Summary

| Domain | Group | TASKs | Result | Date |
|--------|-------|-------|--------|------|
| Storage | Service | 014/008-012 | PASS | 2026-02-26 |
| Storage | API | 013/015 | R1 FAIL -> R2 PASS | 2026-02-26 |
| Storage | Test | 016-021 | PASS | 2026-02-27 |
| OpenAPI Validation | Phase C | 202/221 | R1 FAIL -> R2 PASS | 2026-03-02 |
| OpenAPI Validation | Phase D | 210/211/222 | FAIL | 2026-03-02 |
| Contract-First | Group 7 | 020/060/070 | PASS | 2026-03-02 |
| Survey-Event Reg | Group 1 | 001/003/006/011 | R1 FAIL -> R2 PASS | 2026-03-02 |
| Survey-Event Reg | Group 2 | 002/004/005/007 | PASS | 2026-03-02 |
| Survey-Event Reg | Group 3 | 008/009/010/015 | PASS | 2026-03-02 |
| Survey-Event Reg | Group 4 | 012/013/014 | PASS | 2026-03-02 |
| Survey-Event Reg | Group 5 (Test) | 016/017/018/019/020 | R1 FAIL -> R2 PASS | 2026-03-03 |
