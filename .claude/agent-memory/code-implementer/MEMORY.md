# Code Implementer Agent Memory

## Project Structure
- Monorepo: `backend/` (Spring Boot 3.5.9, Java 21) + `frontend/` (React 19, TS, Vite 7)
- Backend base package: `igrus.web`
- Domain packages: `igrus.web.{domain}.{layer}` (e.g., `igrus.web.storage.config`)

## Key Patterns

### Entity Pattern
- Inherit `SoftDeletableEntity` -> `BaseEntity`
- Use `@AttributeOverrides` for column naming: `{table_name}_{column_name}`
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Getter`
- Static factory methods (e.g., `create(...)`)
- Domain validation in entity methods
- Reference: `Post.java` for full pattern

### ErrorCode Pattern
- Enum implements `ErrorCode` interface (`getStatus()`, `getMessage()`, `getCode()`)
- `getCode()` returns `this.name()`
- Custom exceptions extend `CustomBaseException`
- One exception class per error scenario
- Reference: `CommunityErrorCode.java`, `BoardNotFoundException.java`

### Flyway Migration Pattern
- Column naming: `{table_name}_{column_name}` (e.g., `file_metadata_object_key`)
- Timestamp columns: `TIMESTAMP(6)` with DEFAULT CURRENT_TIMESTAMP(6)
- Boolean defaults: `DEFAULT FALSE`
- FK added separately with `ALTER TABLE ... ADD CONSTRAINT`
- ENGINE=InnoDB, CHARSET=utf8mb4, COLLATE=utf8mb4_unicode_ci
- Latest version: V47 (add_survey_id_to_events)

### Config Pattern
- `Clock` Bean already exists in `ClockConfig` (Asia/Seoul timezone)
- `spring-cloud-aws-starter-s3:3.4.0` already in build.gradle
- Storage properties in `application.yml` under `app.storage`

### Time
- Always use `java.time.Instant` (never LocalDateTime)
- Use injected `Clock` bean for testability

### Contract-First Migration Pattern
- Controllers `implements {Tag}Api` (e.g., `StorageApi`, `BoardApi`)
- No `@RequestMapping`, `@Operation`, `@ApiResponse`, `@Tag`, `@SecurityRequirement`
- Auth: `SecurityUtils.requireCurrentUser()` (not `@AuthenticationPrincipal`)
- Pagination: `PageableUtils.of(page, size, sort)` (not `@ParameterObject Pageable`)
- Servlet access: `ServletContextUtil.getCurrentRequest()/.getCurrentResponse()` (RequestContextHolder-based)
- DTO mapping: inline fluent setters, no MapStruct/Mapper classes
- Page responses: `PageResponseMapper.toSpringPageResponse()` for Spring Page wrapper types
- Generated code: `build/generated/openapi/` (igrus.web.generated.api/model)
- Inquiry split: Guest Inquiry(2), Member Inquiry(3), Admin Inquiry(7) - 3 separate tags/interfaces
- PageableUtils bug fix: Spring `@RequestParam(defaultValue="prop,DIR")` with `List<String>` splits on comma -> `["prop", "DIR"]`. Fixed by merging standalone direction strings with previous property.
- Page content with `List<Object>`: When generated model has `List<Object>` content, cast internal DTOs to Object: `content.stream().map(c -> (Object) c).toList()`
- Enum params as String: Generated API may use `String` for enum params (e.g., `changeType`). MUST use `EnumUtils.fromStringOrNull(EnumType.class, stringValue)` for safe conversion (returns 400, not 500). NEVER use raw `EnumType.valueOf(stringValue)` directly - it throws `IllegalArgumentException` which becomes 500 instead of 400.
- Generated model Boolean vs boolean: Generated models use `Boolean` (nullable). Use `Boolean.TRUE.equals(x)` for null-safe boolean conversion.
- Generated model Integer vs int: Generated models use `Integer` (nullable). Use `x != null ? x : 0` for int default.
- OpenAPI minLength mismatch: `minLength: 0` in spec does NOT match `@NotBlank` in internal DTOs. Blank strings pass `@Size(min=0)` but fail `@NotBlank`. Fix: set `minLength: 1` in spec for fields that were `@NotBlank`.
- openapi-generator UP-TO-DATE cache: When only referenced YAML files change (not root openapi.yaml), generator may show UP-TO-DATE. Fix: delete `build/generated/openapi/` before regenerating.
- SurveyQuestion controllers: service-level permission check (`validateOperatorPermission`), so use `@PreAuthorize("isAuthenticated()")` at controller level (not OPERATOR/ADMIN).
- PasswordSignupRequest internal DTO field order: studentId, name, email, password, phoneNumber, department, motivation, wishes, interests, customInterest, joinRoute, customJoinRoute, gender, grade, enrollmentStatus, privacyConsent, verificationToken. ALWAYS read the actual record definition before mapping.
- TemporaryStudentIdSignupRequest: same as PasswordSignupRequest but WITHOUT studentId (starts from name).
- MyPageApi operationId collision: `getMyBookmarks1` and `getMyLikes1` (with `1` suffix) due to same operationId in other tags (PostLike, Bookmark).
- SurveyApi createSurvey: Takes `generated.model.UpdateSurveyRequest` (not CreateSurveyRequest) because generator reuses same schema.
- Generated model @AssertTrue not supported: OpenAPI spec cannot express `@AssertTrue` constraint. Generated model only has `@NotNull` for boolean fields. Test failures expected for `privacyConsent: false` validation.
- Generated model @NotBlank vs @NotNull: Generated models use `@NotNull @Size(min=N)` instead of `@NotBlank`. Blank/whitespace-only strings pass if `min=0`. Must set `minLength: 1` in spec AND test with non-whitespace chars.

### swagger-request-validator PoC (TASK-200)
- Library: `com.atlassian.oai:swagger-request-validator-mockmvc:2.46.0` (test), `swagger-request-validator-spring-webmvc:2.46.0` (runtime)
- OpenAPI 3.1.0 limitation: validator does NOT detect type mismatches (array vs object, boolean vs string). Same schemas work correctly under 3.0.3.
- Multifile spec loading works (`$ref` resolution automatic). Bundled file recommended for CI stability.
- CSRF `_csrf` param: set `validation.request.parameter.query.unexpected` to IGNORE
- Security validation: set `validation.request.security.missing` to IGNORE
- Performance: init ~3s, per-request ~1ms (pure validation)
- No dependency conflicts with Spring Boot 3.5.9
- PoC test: `backend/src/test/java/igrus/web/common/openapi/SwaggerRequestValidatorPocTest.java`
- PoC result doc: `docs/feature/openapi-contract-first/poc-result-200.md`
- Runtime validation checklist: `docs/feature/openapi-contract-first/runtime-validation-checklist.md`
- Common factory: `OpenApiValidatorFactory` in `igrus.web.common.config` -- Single Source of Truth for LevelResolver + Validator creation. Both `OpenApiValidationConfig` (runtime) and `OpenApiValidatorUtil` (test) delegate to this factory.
- Graceful degradation: `OpenApiValidationConfig` constructor wraps `OpenApiValidatorFactory.createValidator()` in try-catch. On failure, interceptor=null, Filter registered but interceptor not added.

### OpenAPI Test Validation Pattern (TASK-210/211)
- Add `OpenApiValidatorUtil.matchesOpenApiSpec()` only to 2xx response tests
- Skip 4xx/5xx responses and 204 No Content (no body)
- Import: `import igrus.web.common.OpenApiValidatorUtil;`
- Usage: `.andExpect(OpenApiValidatorUtil.matchesOpenApiSpec())`
- Known spec mismatch: `UserDetailResponse.joinRoute` not nullable in spec but User can have null joinRoute

### Independent Context Test Pattern (TASK-222)
- Use `@TestPropertySource` for property overrides that would break context caching
- Cannot extend `ServiceIntegrationTestBase` (different context due to @TestPropertySource)
- Must replicate `cleanupDatabase()` and `User.create()` patterns manually
- `User.create()` signature: (studentId, name, email, phone, dept, motivation, wishes, gender, grade, enrollmentStatus, interests, customInterest, joinRoute, customJoinRoute) -- NO UserRole param
- Call `user.changeRole(role)` separately after create
- Bean name verification: `applicationContext.containsBean("beanMethodName")` instead of type-based check

### Signature Change Propagation
- When adding fields to record DTOs (e.g., `CreateEventRequest`, `UpdateEventRequest`), ALL call sites must be updated: controllers, services, tests, and integration tests
- `replace_all` on constructor patterns must be carefully scoped: `isEqualTo(EnumType.VALUE)` can match `EnumType.VALUE);` pattern and get corrupted (e.g., `isEqualTo(EnumType.VALUE, null)`)
- Controller DTO mapping from generated model to internal DTO requires updating when internal DTO changes
- After OpenAPI spec changes, must `rm -rf build/generated/openapi/` then `./gradlew openApiGenerate` to force regeneration
- `@InjectMocks` in unit tests: when adding new dependencies to a service, must add corresponding `@Mock` field in test class

### Cross-Domain Weak Reference Pattern
- Use `Long surveyId` field (not `@ManyToOne`) for cross-domain FK references
- Validation in service layer: `repository.findByIdAndDeletedFalse()` + check `trashedAt != null`
- Exception from referenced domain's package (e.g., `SurveyNotFoundException` from `igrus.web.survey.exception`)

## File Paths
- ErrorCode interface: `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
- CustomBaseException: `backend/src/main/java/igrus/web/common/exception/CustomBaseException.java`
- SoftDeletableEntity: `backend/src/main/java/igrus/web/common/domain/SoftDeletableEntity.java`
- BaseEntity: `backend/src/main/java/igrus/web/common/domain/BaseEntity.java`
- ClockConfig: `backend/src/main/java/igrus/web/common/config/ClockConfig.java`
- EnumUtils: `backend/src/main/java/igrus/web/common/util/EnumUtils.java`
- InvalidEnumValueException: `backend/src/main/java/igrus/web/common/exception/InvalidEnumValueException.java`
- SecurityUtils: `backend/src/main/java/igrus/web/common/util/SecurityUtils.java`
- PageableUtils: `backend/src/main/java/igrus/web/common/util/PageableUtils.java`
- PageResponseMapper: `backend/src/main/java/igrus/web/common/util/PageResponseMapper.java`
- ServletContextUtil: `backend/src/main/java/igrus/web/common/util/ServletContextUtil.java`
- Migration guide: `docs/feature/openapi-contract-first/migration-guide.md`
- Checklist: `docs/feature/openapi-contract-first/implementation-checklist.md`
- OpenApiValidatorFactory: `backend/src/main/java/igrus/web/common/config/OpenApiValidatorFactory.java`
- OpenApiValidationConfig: `backend/src/main/java/igrus/web/common/config/OpenApiValidationConfig.java`
- OpenApiValidatorUtil: `backend/src/test/java/igrus/web/common/OpenApiValidatorUtil.java`
