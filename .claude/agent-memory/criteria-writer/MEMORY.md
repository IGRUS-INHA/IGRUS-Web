# Criteria Writer Agent Memory

## QA Wiki Terminology Mapping
- 10 domains in wiki: Domain Rules & Invariants, State Models, System Boundaries, External Dependency Failure Policies, Input Domain Partitioning & BVA, Authorization & Security, Observability, Test Strategy, Quality Goals, Test Effectiveness Verification
- No explicit priority levels defined in wiki -- use domain-specific prioritization
- Use wiki terms only; never invent QA terminology

## Document Structure Conventions
- Header: Status (Draft/Final), Last Updated, Scope, Reference (QA wiki link)
- Sections numbered by wiki domain (1-8 depending on relevance)
- Invariant IDs: `{DOMAIN}-INV-{NN}` (e.g., STOR-INV-01, EVT-INV-01)
- Security IDs: `SEC-{DOMAIN}-{NN}` (e.g., SEC-STOR-01)
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

## Key Design Decisions (Storage Domain)
- UPLOADING state: frontend-only, not stored in DB. DB states: REQUESTED/CONFIRMING/COMPLETED/FAILED/EXPIRED
- Image access: single policy (no public/private distinction). Authenticated users (ASSOCIATE+) can download all images
- Confirm API idempotency: already-COMPLETED objectKey returns 200 OK on re-call
- File deletion order: S3 delete first, then DB soft delete
- Orphan cleanup: 24h TTL via S3 Lifecycle + DB scheduler (hourly)
- fileName extension: not validated, Content-Type is authoritative
