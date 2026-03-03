# Plan Review Checklist

## Pre-Review Checks
- [ ] API paths use `/api/v1/` prefix
- [ ] Spring Boot version matches `build.gradle` (currently 3.5.9, NOT 4.0.1 from root CLAUDE.md)
- [ ] DTO naming follows `{Action}{Domain}Request` / `{Domain}{Action}Response`
- [ ] Flyway migration version > current latest (V40 as of 2026-02-26)
- [ ] Flyway column names use `{table_name}_{column_name}` format
- [ ] BaseEntity/SoftDeletableEntity fields included in Flyway migrations
- [ ] ErrorCode follows `{Domain}ErrorCode` pattern in `igrus.web.{domain}.exception`
- [ ] Time fields use `Instant` (not LocalDateTime, ZonedDateTime, etc.)

## Cross-Document Consistency
- [ ] API paths consistent across task plan, verification criteria, and test cases
- [ ] Status codes consistent across all three documents
- [ ] DTO field names consistent across all three documents

## Architecture Checks
- [ ] Transaction boundaries explicitly designed (not "A or B")
- [ ] Concurrency handling for state transitions considered
- [ ] Path variables with special characters (slashes, etc.) handled
- [ ] External dependency (S3, etc.) failure/retry policy defined
- [ ] Spring Security path configuration included
- [ ] Downstream service impact analyzed (all callers of affected repository methods checked)
- [ ] When adding access control (visibility, permissions), ALL services querying the domain are updated
- [ ] Design decisions finalized (no "A or B" without resolution)
- [ ] OpenAPI schema changes to Admin*Response require AdminController mapping helper updates
- [ ] @SQLRestriction presence verified per entity (Event has it, FileMetadata does NOT)

## Test Coverage
- [ ] Backend unit tests planned
- [ ] Backend integration tests planned
- [ ] Controller integration tests planned
- [ ] Frontend tests planned (often missing)
- [ ] All verification criteria mapped to at least one test task
- [ ] All test cases mapped to at least one implementation task

## Documentation
- [ ] Documentation update task included (root CLAUDE.md requirement)
- [ ] Swagger/OpenAPI annotations mentioned
- [ ] Orval compatibility considered

## Dependency Management
- [ ] New dependencies use correct Gradle scope (implementation vs testImplementation vs runtimeOnly)
- [ ] Dev/test-only libraries not added as `implementation` without justification
- [ ] Transitive dependency conflicts checked (jackson, swagger-parser, etc.)

## Test Infrastructure
- [ ] No new `@ActiveProfiles` in test subclasses (context caching rules)
- [ ] No new `@Import` in test subclasses
- [ ] No new `@MockitoBean` in test subclasses (use TestExternalServiceConfig instead)
- [ ] Runtime Filter/Interceptor additions to `test` profile analyzed for context caching impact
- [ ] Prod profile tests feasible without requiring production DB or services
