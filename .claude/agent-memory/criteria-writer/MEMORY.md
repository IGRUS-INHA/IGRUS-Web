# Criteria Writer Agent Memory

## QA Wiki Terminology Mapping
- 10 domains in wiki: Domain Rules & Invariants, State Models, System Boundaries, External Dependency Failure Policies, Input Domain Partitioning & BVA, Authorization & Security, Observability, Test Strategy, Quality Goals, Test Effectiveness Verification
- No explicit priority levels defined in wiki -- use domain-specific prioritization
- Use wiki terms only; never invent QA terminology

## Document Structure Conventions
- Header: Status (Draft/Final), Last Updated, Scope, Reference (QA wiki link)
- Sections numbered by wiki domain (1-8 depending on relevance)
- Invariant IDs: `{DOMAIN}-INV-{NN}` (e.g., STOR-INV-01, EVT-INV-01)
- Security IDs: `SEC-{DOMAIN}-{NN}` (e.g., SEC-STOR-01, SEC-EVT-11)
- GAP IDs: `GAP-{DOMAIN}-{NN}` (e.g., GAP-EVT-25)
- File naming: `{feature}-verification-criteria.md` in Korean or English
- All content in Korean

## Folder Structure
- `docs/criteria/` -- root for all criteria
- `docs/criteria/event/` -- event-related criteria
- `docs/criteria/storage/` -- storage/image upload criteria
- `docs/criteria/user/signup/` -- user signup criteria

## Common Patterns
- State diagrams use box-drawing characters in fenced code blocks
- Frontend-only states must be explicitly annotated (e.g., UPLOADING is UI-only, not in DB)
- Idempotency policies must be consistent across sections (input domain, API spec, failure policy)
- DB status enum and UI status enum may differ -- always clarify which is which
- Soft Delete is the default delete strategy for this project

## Recurring Edge Cases
- Authentication: always test 401 for unauthenticated access
- Ownership: always test 403 for cross-user resource access
- Idempotency: duplicate API calls must be explicitly addressed
- Orphan cleanup: any resource with async lifecycle needs cleanup policy
- Reference integrity: deletion must check for dependent entities

## Cross-Domain Criteria Patterns
- When two independent domains are connected, use **weak reference** (Long ID, no FK) to preserve soft delete compatibility
- Cross-domain invariants use `SEVT-INV-{NN}` prefix pattern (Survey-Event)
- Prerequisite vs Continuous condition distinction: survey response is a **gate** (checked once at registration), not an **invariant** (not continuously enforced after registration)
- Single-direction dependency: Event -> Survey (Event reads Survey, Survey doesn't know Event exists)
- Transaction separation: separate Tx for each domain operation (response submit vs event registration)
- State cross-constraints: when domains have independent FSMs, document the cross-matrix of valid combinations
- DECISION table pattern: list unresolved design decisions with options and recommendations for implementer

## Key Design Decisions (Event-Survey Linking)
- Event.surveyId: nullable Long, no JPA @ManyToOne, no FK constraint (weak reference)
- Survey response is gate-condition only: checked at registration time, not continuously enforced
- Survey reuse: one survey can be linked to multiple events (1:N)
- Survey state check at registration: responseStatus != NOT_STARTED (not PUBLISHED+OPEN required)
- Existing registrations preserved when survey is changed/removed/deleted
- Survey domain code unchanged -- only Event domain services modified

## Key Design Decisions (Storage Domain)
- UPLOADING state: frontend-only, not stored in DB. DB states: REQUESTED/CONFIRMING/COMPLETED/FAILED/EXPIRED
- Image access: single policy (no public/private distinction). Authenticated users (ASSOCIATE+) can download all images
- Confirm API idempotency: already-COMPLETED objectKey returns 200 OK on re-call
- File deletion order: S3 delete first, then DB soft delete
- Orphan cleanup: 24h TTL via S3 Lifecycle + DB scheduler (hourly)
- fileName extension: not validated, Content-Type is authoritative

## Key Design Decisions (Event Domain)
- 3-axis state model: visibility + registrationStatus + eventStatus
- Visibility follows SurveyVisibility pattern: bidirectional, canTransitionTo = this != target
- Default visibility on create: UNPUBLISHED; DB migration default: PUBLISHED (existing data)
- Unpublish + OPEN registration = auto close (MANUAL_CLOSE), same as Survey.unpublish()
- Publish/unpublish: no reason required (unlike cancel/reactivate/close/reopen)
- Admin API: /api/v1/admin/events/** OPERATOR+ via SecurityConfig (must precede /api/v1/admin/**)
- Public API: only PUBLISHED events; UNPUBLISHED returns 404
- EVT-INV-16~23 for Visibility invariants; GAP-EVT-25~44 for Visibility test gaps; SEC-EVT-11~17 for admin API
- EventStatusChangeHistory.reason is nullable (@Column TEXT, no NOT NULL) -- publish/unpublish store reason=null, DDL change not needed
- EventChangeType.java currently has 4 values only; EVENT_PUBLISHED/EVENT_UNPUBLISHED are NOT yet implemented
- Event.java has no visibility field yet -- all Visibility code items are "(신규 구현 필요)"
- COMPLETED/CANCELED events always have registrationStatus=CLOSED (cross-axis invariant), so unpublish has no registration side-effect
- Publish/unpublish auth: SecurityConfig URL rule only (no service-level validateOperatorPermission), unlike Survey which uses service-level check

## Key Design Decisions (EventGroup)
- EventGroup: new feature, no existing code/spec. Pure CRUD + association management
- Recommended: weak reference pattern (Event.groupId nullable Long, no FK) -- consistent with Event.surveyId
- 1:N relationship (one event belongs to max 1 group) -- simpler than M:N
- Group delete policy: clear groupId on associated events (option A), not cascade delete
- Public API for read (PUBLISHED events only) + Admin API for CRUD
- EventGroup has no FSM -- only ACTIVE/DELETED via SoftDeletableEntity
- Group does NOT affect event state (visibility/registrationStatus/eventStatus) -- pure logical grouping
- ID prefixes: EGRP-INV-{NN}, SEC-EGRP-{NN}, GAP-EGRP-{NN}
- Column naming: event_group_{column} pattern
- DECISION table: 11 items (DECISION-01~11) covering relationship model, cardinality, delete policy, API placement, sort order, ungrouped display, idempotent add (DECISION-07), non-member remove (DECISION-08), PUT vs PATCH (DECISION-09), pagination (DECISION-10), public API auth level (DECISION-11)
- DB UNIQUE: MySQL 8 Generated Column pattern for soft-delete-compatible unique constraint (`IF(deleted=FALSE, name, NULL) STORED` + UNIQUE on generated column)
- Modify API: PUT (full update) -- only 2 fields (name, description), per backend/CLAUDE.md "PUT: 전체 수정"
- eventCount: excludes soft-deleted events; public API also excludes UNPUBLISHED

## Key Design Decisions (Event-Image Integration)
- posterImageObjectKey: nullable String (VARCHAR 500), no FK to file_metadata (weak reference, same as surveyId)
- Only COMPLETED status FileMetadata can be referenced by Event
- Object Key prefix `events/` for event images (enforced by ObjectKeyGenerator purpose param)
- No auto-delete of old images on change or event soft-delete (manual cleanup by operator)
- EventFileReferenceChecker: new FileReferenceChecker implementation, checks non-deleted events
- Soft-deleted events excluded from reference integrity checks (file can be deleted)
- Single poster image field (not multi-image); future expansion via separate event_images table
- Cross-domain invariant ID prefix: EVT-IMG-INV-{NN}; Security prefix: SEC-EVT-IMG-{NN}
- DB column: event_poster_image_object_key VARCHAR(500) NULL
- Image editability follows EVT-INV-07 (COMPLETED eventStatus blocks all edits including image)

## Review Feedback Patterns
- Always verify actual source code before marking "(현재 구현 일치)" -- check enum values, entity fields, method existence
- "(구현 예정)" and "(신규 구현 필요)" must be consistently used; prefer "(신규 구현 필요)" for clarity
- When a feature crosses existing + new code, split the "관련 코드" section into separate `(현재 구현 일치)` and `**(신규 구현 필요)**` lines
- Test coverage table: existing items affected by new features should be "부분 커버 ({feature} 미검증)"
- Cross-axis invariant implications must be explicitly stated (e.g., COMPLETED->registrationStatus always CLOSED)
- **Cross-document reference rule**: When referencing another doc's invariant (e.g., EVT-INV-07), verify the invariant actually covers the new field. If not, define an independent rule and add [ACTION REQUIRED] to update the other doc after implementation.
- **DECISION cross-referencing**: Every section affected by a DECISION must reference it by ID (DECISION-01, not just "DECISION"). DECISION table must list all affected sections ("영향 범위").
- **RBAC asymmetry**: ASSOCIATE can access event list (200 OK) but NOT event detail (403, SEC-EVT-01). When new fields are added to response DTOs, explicitly state the list vs detail exposure policy.
- **Security reference accuracy**: STOR-INV-06 = upload restriction (unauthenticated), SEC-STOR-04 = download URL restriction (unauthenticated). Do not confuse upload vs download references.
- **Input domain DECISION linkage**: When an input value's expected result depends on an unresolved DECISION, the input domain table must say "DECISION-XX에 따라 다름" instead of a definitive result.
- **DB UNIQUE for soft-delete entities**: Always add DB-level unique constraint. For soft-delete compatible uniqueness in MySQL 8, use Generated Column pattern: `col_unique_key GENERATED ALWAYS AS (IF(deleted=FALSE, col, NULL)) STORED` + UNIQUE on generated column. Service-level check alone has race conditions.
- **Self-exclusion in update uniqueness**: When INV defines unique constraint, the body text MUST explicitly state "수정 시 자기 자신을 제외하고 중복 검사". Repository method: `existsByNameAndIdNotAndDeletedFalse(name, id)`.
- **Undecided policies must be DECISION items**: Any "... 또는 ..." or "DECISION 필요" in body text must be registered as formal DECISION-XX in the DECISION table with affected sections listed.
- **HTTP method rationale required**: PUT vs PATCH must be explicitly decided with reference to backend/CLAUDE.md ("PUT: 전체 수정, PATCH: 부분 수정").
- **Audit fields documentation**: SoftDeletableEntity -> BaseEntity provides 7 audit fields (createdAt, updatedAt, createdBy, updatedBy, deleted, deletedAt, deletedBy). Document in EGRP-INV-05 (initial state) and 5-2 (observability).
- **API response status codes**: Document 201 Created for resource creation, 204 No Content for deletion. Don't leave ambiguous.
- **DECISION status vs INV body language**: When DECISION items are "권장안" (not confirmed), INV body text must NOT use definitive language as if the decision is already made. Either: (1) convert DECISIONs to "확정:" or (2) add "(DECISION-0N 확정 후 갱신 필요)" annotation to each dependent INV. Add a disclaimer note at the top of Section 1.
- **DB UNIQUE race condition exception conversion**: When DB UNIQUE constraints are used for dedup, always document the `DataIntegrityViolationException` -> business exception conversion policy for race conditions. Reference REG-INV-01 pattern.
- **Business exception names**: Always specify the expected exception class name (e.g., `ExternalRegistrationNotAllowedException`), not just the HTTP status code.
- **FSM forbidden transitions table**: For every FSM, include a "금지된 전이" table listing invalid state transitions with reasons. Especially important when external actors have different capabilities (e.g., no reRegister for externals).
- **ACTION REQUIRED -> GAP registration**: Every [ACTION REQUIRED] item in Section 0/9 must have a corresponding GAP-{DOMAIN}-{NN} entry in the GAP table (Section 7-3).
- **Missing endpoint detection**: When an INV references an operation (e.g., "관리자만 취소 가능"), verify the corresponding endpoint exists in OpenAPI spec AND in the endpoint table (Section 3-3). If missing, add it with [ACTION REQUIRED].
- **Permission matrix DECISION linkage**: When a row in the permission matrix depends on an unresolved DECISION, annotate the row with "(DECISION-XX 확정 후 갱신 필요)" instead of fixed values like "N/A".

## Key Design Decisions (External Event Registration)
- allowExternal: Boolean field on Event entity, default false, DB column `event_allow_external BOOLEAN NOT NULL DEFAULT FALSE`
- External endpoint: `POST /api/v1/events/{eventId}/registrations/external`, security: [] (no auth)
- ExternalRegisterEventRequest: name(1-50), studentId(1-20), phone(1-20), department(1-100), surveyAnswers(optional)
- Duplicate prevention: studentId and phone each independently checked per event (not combined)
- Capacity sharing: member + external share same capacity/currentCount, same atomic UPDATE queries
- ASSOCIATE conditional: allowExternal=true -> ASSOCIATE can register via existing `/registrations` endpoint
- External cancel: OPERATOR+ only (no self-cancel for externals, no auth means)
- ID prefixes: EXT-INV-{NN}, SEC-EXT-{NN}
- 8 DECISION items (DECISION-01~08): data model (single table vs separate), duplicate DB constraint, cancel policy, survey response storage, default value, time overlap, manual approve handling, cancel endpoint design
- DECISION-01 confirmed: Option A (single table, user nullable)
- DECISION-03 confirmed: Option A (admin-only cancel)
- DECISION-08 confirmed: Option A (extend existing admin API pattern: POST /registrations/{id}/cancel)
- MySQL 8 Generated Column for CANCELED-excluded UNIQUE: `IF(status != 'CANCELED', student_id, NULL) STORED`
- Existing doc impact: REG-INV-04 (ASSOCIATE block -> conditional), SEC-REG-01 (403 -> conditional)
- RegistrationListResponse schema changes: userId/userEmail/userGender/userGrade nullable, phone+isExternal added
- Section 0 "기존 문서 영향 분석" pattern: document cross-doc changes with [ACTION REQUIRED] for post-implementation updates

## Key Design Decisions (Post/Inquiry S3 Integration)
- PostImage.imageUrl and InquiryAttachment.fileUrl store S3 Object Key, NOT actual URLs -- field naming mismatch
- AttachmentInfo.fileUrl has `@Pattern(regexp = "^https?://...")` URL validation -- INCOMPATIBLE with Object Key format, must be removed
- Post: multi-image (1:N via PostImage, max 5), BaseEntity (no soft delete on PostImage itself)
- Inquiry: multi-attachment (1:N via InquiryAttachment, max 3), no edit after creation
- PostFileReferenceChecker: must join PostImage -> Post to check Post.deleted (PostImage has no soft delete)
- InquiryFileReferenceChecker: Inquiry has @SQLRestriction, but explicit JPQL safer for cross-entity join
- Guest inquiry attachment download problem: SEC-STOR-04 requires auth for download URL, guests can't download their own attachments
- ID prefixes: POST-IMG-INV-{NN}, INQ-ATT-INV-{NN}, COMMON-S3-INV-{NN}, SEC-POST-IMG-{NN}, SEC-INQ-ATT-{NN}
- 8 DECISION items (DECISION-01~08) covering prefix validation, ownership, validation order, inquiry purpose, fileName/fileSize source, duplicate keys, guest download, field renaming
