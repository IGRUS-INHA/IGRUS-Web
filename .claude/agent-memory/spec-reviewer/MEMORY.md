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

## OpenAPI Runtime Validation Reviews

### Round 1: TASK-202/221 (Phase C Infrastructure) - FAIL (2026-03-02)
- R1 Critical issues (2):
  1. LevelResolver creation logic duplicated in OpenApiValidatorUtil(test) and OpenApiValidationConfig(main)
  2. Validator creation logic (spec loading, LevelResolver) also duplicated in both classes
- R1 Recommended issues (2):
  1. No graceful degradation in OpenApiValidationConfig constructor -- spec file missing = context failure
  2. LoggingValidationReportHandler.hasErrors() only catches ERROR, not WARN-only scenarios

### Round 2: TASK-202/221 (Phase C Infrastructure) - PASS (2026-03-02)
- All R1 issues resolved via OpenApiValidatorFactory extraction:
  1. createProjectLevelResolver() -- single source of truth for LevelResolver
  2. createValidator() -- single source of truth for Validator creation (spec loading + LevelResolver)
  3. graceful degradation: try-catch in Config constructor, interceptor=null on failure, WARN log
  4. hasWarnings() method added to LoggingValidationReportHandler for WARN-only scenarios
- Key architecture after R2:
  - OpenApiValidatorFactory (src/main): utility class with static methods, SPEC_FILE_PATH constant
  - OpenApiValidatorUtil (src/test): delegates to Factory, adds singleton (volatile+DCL) + matchesOpenApiSpec()
  - OpenApiValidationConfig (src/main): delegates to Factory, @Profile+@ConditionalOnProperty dual gate
- R2 Recommended (non-blocking):
  1. SPEC_FILE_PATH is package-private, could be private
  2. Filter Bean always created even when Interceptor is null (graceful degradation edge case)

### Infrastructure Layer Checklist
1. Singleton pattern for expensive validator initialization (volatile + DCL or Holder pattern)
2. @Profile + @ConditionalOnProperty dual gating for gradual rollout
3. LevelResolver settings must match across test util and runtime config -- use shared factory
4. ValidationReportHandler must NOT throw exceptions (WARN log only per task plan)
5. Spec file path must be relative from backend/ working directory
6. Common logic between src/main and src/test MUST be in src/main shared factory (not duplicated)
7. Graceful degradation: heavy initialization in @Configuration constructor must be try-catch guarded

## OpenAPI Contract-First Migration Reviews

### Group 7 R1: TASK-020/060/070 - PASS (2026-03-02)
- 41 endpoints (16+12+13) fully migrated across 3 controllers
- Key verified patterns:
  1. ServletContextUtil usage in PasswordAuthController (login/logout/refresh/recoverAccount)
  2. PUBLIC_PATHS covers /api/v1/auth/password/** -- no @PreAuthorize needed for auth endpoints
  3. operationId collision suffix: getMyLikes1, getMyBookmarks1 in MyPageController
  4. SurveyApi createSurvey uses generated UpdateSurveyRequest (schema reuse by openapi-generator)
  5. All MyPage/Survey methods have @PreAuthorize("isAuthenticated()") matching original @AuthenticationPrincipal

### Contract-First Migration Checklist
1. @Override count must match interface method count exactly
2. No residual Swagger annotations (@Tag, @Operation, @ApiResponse, @Parameter, @SecurityRequirement)
3. No residual Spring MVC annotations (@RequestMapping, @GetMapping, etc.) -- interface provides these
4. @PreAuthorize maintained: absent = public API (verify via SecurityPaths), present = auth required
5. SecurityUtils.requireCurrentUser() replaces @AuthenticationPrincipal in every authenticated method
6. PageableUtils.of(page, size, sort) replaces @ParameterObject Pageable in paginated methods
7. operationId suffix (_1, _2) in method names when same endpoint appears in multiple tags
8. Generated model DTO <-> internal DTO mapping via inline or helper methods
9. HttpServletRequest/Response accessed via ServletContextUtil when interface lacks Servlet params
10. @Validated on class level removed (generated interface already has @Validated)

## OpenAPI Runtime Validation Phase D Reviews

### Round 1: TASK-210/211/222 (Phase D Tests + Filter) - FAIL (2026-03-02)
- TASK-210: All 7 TC-210 test cases fully covered across 7 community controller test files
- TASK-211: All 5 TC-211 test cases covered. getUserDetail 2 cases excluded (spec defect: joinRoute nullable missing)
- TASK-222: 4/5 TC covered (TC-221-01, TC-221-02, TC-222-01, TC-222-02). TC-221-03 NOT implemented
- R1 Critical issues (2):
  1. TC-221-03 (schema mismatch detection test) not implemented -- Filter's core purpose unverified
  2. TASK-222 violates task plan's "ControllerIntegrationTestBase inheritance" rule -- uses independent context with @SpringBootTest, @ActiveProfiles, @Import, @TestPropertySource
- R1 Recommended issues (2):
  1. getUserDetail spec fix (joinRoute nullable) needs tracked follow-up TASK
  2. cleanupDatabase() duplicated between OpenApiValidationFilterTest and ServiceIntegrationTestBase

### Phase D Checklist
1. matchesOpenApiSpec() must be on ALL 2xx responses (not error responses)
2. When spec defects prevent validation, document with code comment AND checklist issue log
3. TC-221-03 (failure detection) is critical -- happy-path-only Filter tests are insufficient
4. Independent test contexts (@TestPropertySource) that conflict with task plan rules must be documented/approved
5. cleanupDatabase() duplication risk -- when tables change, must sync both copies
