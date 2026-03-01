# Spec Reviewer Agent Memory

## Project Patterns

### Flyway Migration
- Latest version on main: V44 (survey_answer_discriminator_column)
- Column naming: `{table_name}_{column_name}` (e.g., `file_metadata_object_key`)
- Version conflicts are a common issue -- always check main branch for latest version

### Entity Hierarchy
- `BaseEntity`: createdAt, updatedAt, createdBy, updatedBy (all Instant, via AuditingEntityListener)
- `SoftDeletableEntity extends BaseEntity`: adds deleted (boolean), deletedAt (Instant), deletedBy (Long)
- All time fields use `Instant` class

### Exception Pattern
- `ErrorCode` interface: getStatus(), getMessage(), getCode()
- Domain-specific `XxxErrorCode enum implements ErrorCode`
- `CustomBaseException` abstract class with constructors: (ErrorCode), (ErrorCode, String), (ErrorCode, Throwable)
- Domain exceptions extend `CustomBaseException`

### Repository Pattern
- JpaRepository-based, method query naming conventions
- Soft delete aware: `findByXxxAndDeletedFalse`

## Review Patterns Found

### Flyway Version Conflicts
- Feature branches may create migrations with same version number as other branches
- First review of storage feature found V41 conflict with survey feature's V41
- Always verify no version collision with main branch migrations

### Foundation Layer Review Scope
- Foundation layers (entities, repos, enums, exceptions) are reviewed for structural correctness
- Service/Controller/Test coverage is out of scope for foundation layer reviews
- Verification criteria and TC fulfillment are partial by design -- only the foundation portions are checked

### TASK-006 Repository Method Gap
- Task plan specified `findByObjectKey(String objectKey)` but implementation only has `findByObjectKeyAndDeletedFalse`
- This is a recurring pattern: plan specifies both soft-delete-aware and raw queries, implementer only creates the soft-delete-aware version

## Storage Domain Service Reviews

### Round 1: TASK-014/008/009/010/011/012 - PASS (2026-02-26)
- All 6 tasks fully met verification criteria and TC expectations
- Key recommended improvements (non-blocking):
  1. PresignedUrlService: try-catch wraps both S3 + DB ops -> DB exceptions get masked as S3OperationFailedException
  2. UploadConfirmService: Content-Length null check allows bypass if null (should fail instead of skip)
  3. FileExpirationService: No pagination for bulk processing (acceptable per task plan "consider" wording)
  4. DownloadUrlService: COMPLETED check throws same 404 as not-found (semantically ambiguous but functionally correct per spec)

### Common Service Layer Checklist
1. Bean Validation annotations match criteria doc section 5-1 input domain
2. State transition domain methods include precondition validation
3. Service logs do NOT include Presigned URL itself (security: section 7-3)
4. S3 exceptions vs business exceptions are separated
5. @Transactional + external call (S3) ordering matches rollback policy

## API Layer Reviews

### Round 1: TASK-013/015 - FAIL (2026-02-26)
- Critical: DELETE endpoint `@DeleteMapping("/{objectKey}")` cannot handle slash-containing Object Keys
  - Object Key format: `posts/2026/02/26/UUID.png` (contains slashes)
  - Single `{objectKey}` path variable only matches one path segment
  - `%2F` encoding rejected by Spring Boot's default security (RequestRejectedException)
  - Fix options: @RequestParam, `{*objectKey}` catch-all, or request body
- Recommended: TC-017 expects HTTP 200 but implementation returns 204 (noContent) for delete
- Recommended: Criteria doc uses `/api/storage/*` but implementation uses `/api/v1/storage/*` (task plan is correct)

### Round 2: TASK-013/015 - PASS (2026-02-26)
- Round 1 Critical resolved: DELETE changed to `@DeleteMapping` + `@RequestParam String objectKey`
- Round 1 Recommended items remain as-is (TC-017 204 vs 200, criteria doc path notation)
- New Recommended: authenticated() vs explicit role restriction for future-proofing
- Security config correctly places DELETE OPERATOR/ADMIN rule before anyRequest().authenticated()
- UserRole enum: ASSOCIATE/MEMBER/OPERATOR/ADMIN -- no SUSPENDED role, so authenticated() = ASSOCIATE+

### API Layer Checklist
1. PathVariable patterns must handle domain values containing slashes (e.g., S3 Object Keys)
2. HTTP method restrictions in Security config must match RBAC matrix
3. Security rule ordering matters -- more specific rules (DELETE) before generic (anyRequest)
4. Verify HTTP status codes in implementation match TC expected results
5. authenticated() is equivalent to ASSOCIATE+ only when no SUSPENDED/lower role exists in UserRole enum

## Test Layer Reviews

### Round 1: TASK-016~021 (Unit + Service Integration Tests) - PASS (2026-02-27)
- All 6 tasks fully met 34 TCs (TC-003, 007-012, 014-028, 029, 031, 033-034, 036, 051-053, 056, 058-062)
- Test infrastructure properly configured:
  - TestExternalServiceConfig: @Primary Mock for S3Client, S3Presigner, Clock
  - ServiceIntegrationTestBase: cleanupDatabase() includes file_metadata table
  - application.yml: igrus.storage properties + spring.cloud.aws.s3.enabled: false
- Key testing techniques observed:
  - ArgumentCaptor for S3 SDK parameter verification (signatureDuration, contentType, contentLength)
  - OutputCaptureExtension for log content verification (userId/contentType/fileSize present, URL absent)
  - Native SQL for overriding @CreatedDate values in scheduler tests
  - @TestConfiguration + volatile variable for controlling FileReferenceChecker behavior
  - verify(never()) for idempotent HEAD skip verification

### Test Layer Checklist
1. SDK call parameters verified via ArgumentCaptor (not just "returns success")
2. Log assertions include both positive (required fields present) and negative (sensitive data absent)
3. Scheduler/time-sensitive tests override createdAt via native SQL (not ReflectionTestUtils on managed entity)
4. Forbidden state transition tests may need architectural interpretation (TC-025: same-key re-upload impossible by design)
5. Boundary tests for time-based queries: strict less-than vs less-than-or-equal matters

## Event Domain Test Reviews

### Round 1: TASK-013~021 (Unit Tests) - FAIL (2026-02-28)
- 50 new tests across 7 tasks
- Critical: TASK-015 missing "already same state -> exception" service tests; TASK-021 missing cancelRegistration after unpublish
- Mock limitation: service tests can't verify newValue in audit events without doAnswer state simulation
- Pattern: task plan numbered items must have 1:1 test mapping -- always count and compare

### Round 2: TASK-013~021 (Unit Tests) - PASS (2026-02-28)
- All 5 R1 issues (2 critical + 3 recommended) resolved
- Key fix patterns:
  1. Same-state exception: doThrow on mock + assertThatThrownBy (service level)
  2. cancelRegistration visibility-independent: separate test with UNPUBLISHED mock event
  3. newValue verification: thenReturn chaining (.thenReturn(UNPUBLISHED).thenReturn(PUBLISHED)) to simulate state change
  4. OPEN auto-close at service level: verify(mockEvent).unpublish() (domain test handles actual state assertion)
  5. registrationStatus solo filter: straightforward Repository mock verification

### Event Domain Test Checklist
1. Same-state transitions must be tested at BOTH domain level (actual exception) AND service level (doThrow mock)
2. Visibility-independent operations (cancel, approve, revert) need explicit UNPUBLISHED event tests
3. Audit event newValue: use thenReturn chaining on mock to simulate pre/post state
4. Service-level tests for domain state changes: verify domain method call, not final state (state tested in domain tests)
5. Multi-axis filtering: test each axis independently + at least one cross-axis combination
