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
- Latest version: V46 (event_visibility column)

### Config Pattern
- `Clock` Bean already exists in `ClockConfig` (Asia/Seoul timezone)
- `spring-cloud-aws-starter-s3:3.4.0` already in build.gradle
- Storage properties in `application.yml` under `app.storage`

### Time
- Always use `java.time.Instant` (never LocalDateTime)
- Use injected `Clock` bean for testability

### Visibility Pattern (Survey/Event)
- Enum with `canTransitionTo(target)` method: `this != target`
- Entity `publish()`: validates canTransitionTo, then sets PUBLISHED
- Entity `unpublish()`: validates canTransitionTo, auto-closes OPEN status, then sets UNPUBLISHED
- Event unpublish additionally sets `closeReason = MANUAL_CLOSE` when auto-closing
- InvalidEventStateTransitionException supports EventStatus, RegistrationStatus, and EventVisibility

### SecurityConfig Pattern
- More specific paths BEFORE generic `/api/v1/admin/**` hasRole("ADMIN")
- OPERATOR+ paths use `.hasAnyRole("OPERATOR", "ADMIN")`
- `/api/v1/admin/events/**` added to OPERATOR+ block

## File Paths
- ErrorCode interface: `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
- CustomBaseException: `backend/src/main/java/igrus/web/common/exception/CustomBaseException.java`
- SoftDeletableEntity: `backend/src/main/java/igrus/web/common/domain/SoftDeletableEntity.java`
- BaseEntity: `backend/src/main/java/igrus/web/common/domain/BaseEntity.java`
- ClockConfig: `backend/src/main/java/igrus/web/common/config/ClockConfig.java`
- ApiSecurityConfig: `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`

## Test Fix Patterns After Visibility Changes
- When adding visibility filter to public APIs, ALL test Event creation paths need `event.publish()`
- Integration tests: Check BOTH helper methods AND inline Event creation blocks in test methods
- Unit tests (mock-based): Add `when(event.getVisibility()).thenReturn(EventVisibility.PUBLISHED)` to mock event setup
- Service tests using `findByIdAndNotDeleted`: If service changed to `findByIdAndVisibility`, update mock accordingly
- Common miss: Inline Event creation in specific test methods (e.g., INT-017, INT-018) that don't use shared helper methods

## Test Package Structure
- Event domain tests: `igrus.web.event.domain` (NOT `unit/`)
- Event service tests: `igrus.web.event.service` (NOT `unit/`)
- Event DTO tests: `igrus.web.event.dto`
- Event integration tests: `igrus.web.event.integration`
- gradlew is inside `backend/` directory, not project root

## Unit Test Patterns (Mock-based)
- Use `@ExtendWith(MockitoExtension.class)` + `@MockitoSettings(strictness = Strictness.LENIENT)`
- Mock Event with `mock(Event.class)` and stub all getters used by DTO `from()` methods
- Verify `eventPublisher.publishEvent()` with `ArgumentCaptor` for audit events
- Domain tests (EventTest) use real objects via `Event.create()` static factory
- SurveyVisibilityTest pattern: 4 transition tests + optional displayName/description tests

## Known Issues
- `PreSignupVerifyCodeService` test has a flaky time-dependent failure (unrelated to event changes)
- DTO record changes (adding fields) may break tests that directly call constructors; prefer `from()` factory methods
