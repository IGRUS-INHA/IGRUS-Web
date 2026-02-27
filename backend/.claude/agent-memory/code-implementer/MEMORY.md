# Code Implementer Memory

## Project Patterns

### SoftDeletableEntity
- Soft delete method is `delete(Long deletedBy)` (NOT `softDelete`)
- Located in `igrus.web.common.domain.SoftDeletableEntity`
- Fields: `deleted` (boolean), `deletedAt` (Instant), `deletedBy` (Long)

### AuthenticatedUser
- Record at `igrus.web.security.auth.common.domain.AuthenticatedUser`
- Fields: `userId` (Long), `studentId` (String), `role` (String)

### Service Pattern
- Class-level: `@Slf4j @Transactional @RequiredArgsConstructor @Service`
- Read-only methods: `@Transactional(readOnly = true)` override
- Single responsibility per service (one public method per service class)

### Scheduler Pattern
- `@Scheduled` and `@Transactional` must NOT be on the same method (proxy AOP)
- Split into: Scheduler (`@Component`) calls Service (`@Service @Transactional`)
- Reference: `SuspensionAutoLiftScheduler` -> `ChangeUserStatusService`

### DTO Pattern
- Java records with Bean Validation annotations
- Request naming: `{Action}{Domain}Request` (e.g., CreatePresignedUrlRequest)
- Response naming: `{Domain}{Action}Response` or `{Action}{Domain}Response`

### Application Properties
- Storage config path: `igrus.storage.s3.bucket-name`, `igrus.storage.upload-url-expiration`, etc.
- S3 dependency: `spring-cloud-aws-starter-s3:3.4.0` (auto-configures S3Client)
- S3Presigner requires manual Bean registration in S3Config

### Flyway
- Current latest version: V45 (file_metadata table)
- V41-V44 used by survey feature

### Error Handling
- `CustomBaseException` with constructors: (ErrorCode), (ErrorCode, String), (ErrorCode, Throwable)
- Domain-specific ErrorCode enum implements `ErrorCode` interface
- Each exception class maps to one ErrorCode

## Test Infrastructure Patterns

### OutputCaptureExtension (Log Verification)
- Test profile logback root level is WARN (`logback-spring.xml` -> `<springProfile name="test">`)
- Must add `logging.level.{package}: INFO` in test `application.yml` for log capture to work
- `CapturedOutput` parameter injection does NOT work in `@Nested` inner classes - move log tests to top-level
- `@ExtendWith(OutputCaptureExtension.class)` must be on the class that directly declares the test method

### TestExternalServiceConfig
- `@TestConfiguration` -> NOT auto-discovered by component scan
- Must be `@Import`ed by each test class or base class
- S3Client + S3Presigner mock beans added here (since `spring.cloud.aws.s3.enabled: false` in test)
- `IgrusWebApplicationTests` also needs `@Import(TestExternalServiceConfig.class)` for context loading

### ServiceIntegrationTestBase
- NOT `@Transactional` - each service call has its own transaction
- `cleanupDatabase()` uses native DELETE queries (handles soft-delete restrictions)
- `file_metadata` table cleanup added in Phase 3.5 (between users and independent tables)

### Clock Mock in Tests
- `@CreatedDate` uses real system clock, NOT the mock Clock bean
- When comparing `completedAt` (mock clock) vs `createdAt` (real clock), compare against known mock value, not against createdAt
- Pattern: `assertThat(saved.getCompletedAt()).isEqualTo(Instant.parse("2026-02-26T10:00:00Z"))`
