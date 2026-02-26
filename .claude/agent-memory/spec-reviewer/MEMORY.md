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
