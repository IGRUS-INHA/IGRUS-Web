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
- Latest version: V41 (file_metadata table)

### Config Pattern
- `Clock` Bean already exists in `ClockConfig` (Asia/Seoul timezone)
- `spring-cloud-aws-starter-s3:3.4.0` already in build.gradle
- Storage properties in `application.yml` under `app.storage`

### Time
- Always use `java.time.Instant` (never LocalDateTime)
- Use injected `Clock` bean for testability

## File Paths
- ErrorCode interface: `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
- CustomBaseException: `backend/src/main/java/igrus/web/common/exception/CustomBaseException.java`
- SoftDeletableEntity: `backend/src/main/java/igrus/web/common/domain/SoftDeletableEntity.java`
- BaseEntity: `backend/src/main/java/igrus/web/common/domain/BaseEntity.java`
- ClockConfig: `backend/src/main/java/igrus/web/common/config/ClockConfig.java`
