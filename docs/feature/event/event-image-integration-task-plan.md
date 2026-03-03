# 행사-이미지 연계 (Event-Image Integration) 작업 계획

## 개요

- **기능 설명**: 행사(Event) 엔티티와 S3 이미지 파일의 연계. Event 엔티티에 `posterImageObjectKey` 필드를 추가하여 포스터 이미지를 연결하고, 이미지 참조 검증(COMPLETED 상태, `events/` 프리픽스), `EventFileReferenceChecker` 구현(FileReferenceChecker 인터페이스), 이미지 정리 정책(자동 삭제 없음), API 응답에 이미지 정보 포함, DB 마이그레이션, OpenAPI 스펙 업데이트를 수행한다.
- **관련 문서**
  - 검증 기준서: [`docs/criteria/event/event-image-integration-verification-criteria.md`](../../criteria/event/event-image-integration-verification-criteria.md)
  - 테스트 케이스: [`docs/test-case/event/event-image-integration-test-cases.md`](../../test-case/event/event-image-integration-test-cases.md)
  - 참조 인터페이스: [`FileReferenceChecker.java`](../../../backend/src/main/java/igrus/web/storage/service/FileReferenceChecker.java)
  - 참조 서비스: [`FileDeleteService.java`](../../../backend/src/main/java/igrus/web/storage/service/FileDeleteService.java)
- **작성일**: 2026-03-03
- **수정일**: 2026-03-03 (리뷰 피드백 반영: API 경로 프리픽스 수정, AdminEventController 매핑 헬퍼 TASK 추가)
- **설계 핵심**: Event와 FileMetadata 간 약한 참조 (JPA FK 없음, 문자열 Object Key로 연결). 기존 `Event.surveyId` 패턴과 동일.

> **API 경로 프리픽스 주의사항**: 이 작업 계획에서는 실제 프로젝트의 OpenAPI 스펙에 맞춰 `/api/v1/events`, `/api/v1/admin/events` 경로를 사용한다. 검증 기준서 및 테스트 케이스 문서에서 `/api/events`, `/api/admin/events`로 기재된 경로는 `/api/v1/events`, `/api/v1/admin/events`를 의미한다.

---

## 작업 목록

### 1. DB 마이그레이션

#### TASK-001: Flyway 마이그레이션 -- event_poster_image_object_key 컬럼 추가

- **작업 ID**: TASK-001
- **작업명**: `events` 테이블에 `event_poster_image_object_key` 컬럼 추가 마이그레이션 스크립트 작성
- **설명**: Flyway 마이그레이션 스크립트 `V48__add_poster_image_object_key_to_events.sql`을 작성한다. `events` 테이블에 `event_poster_image_object_key VARCHAR(500) NULL` 컬럼을 추가한다. 기존 행사 데이터는 이미지 미연결 상태이므로 기본값 NULL이 적절하다. FK 제약은 추가하지 않는다 (약한 참조 설계, 검증 기준서 3-3 참조). 현재 Flyway 최신 버전은 V47이므로 V48을 사용한다.
  - **DDL 스펙**:
    - 컬럼명: `event_poster_image_object_key`
    - 타입: `VARCHAR(500)` (FileMetadata.objectKey와 동일)
    - Nullable: `YES` (이미지 없는 행사 허용, EVT-IMG-INV-01)
    - 기본값: `NULL`
    - 인덱스: 단독 인덱스 불필요 (빈도 낮은 참조 무결성 검사만 사용)
    - FK: 없음 (약한 참조)
- **관련 검증 기준**: EVT-IMG-INV-01 (Nullable 정책), 검증 기준서 8-1 (DB 마이그레이션)
- **관련 테스트 케이스**: TC-001 (null로 행사 생성)
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 2. 도메인 계층

#### TASK-002: Event 엔티티에 posterImageObjectKey 필드 추가

- **작업 ID**: TASK-002
- **작업명**: `Event` 엔티티에 `posterImageObjectKey` 필드 및 관련 메서드 추가
- **설명**: `Event.java`에 다음을 추가한다:
  1. `posterImageObjectKey` 필드: `@Column(name = "event_poster_image_object_key", length = 500)` -- `String` 타입, nullable
  2. `Event.create()` 메서드에 `String posterImageObjectKey` 파라미터 추가. null 허용
  3. `Event.update()` 메서드에 `String posterImageObjectKey` 파라미터 추가. COMPLETED 상태에서는 기존 `EventNotEditableException`이 발생하므로 추가 검증 불필요. ONGOING 상태에서 `posterImageObjectKey`는 정보성 필드이므로 수정 가능 (검증 기준서 2-1 매트릭스)
  4. `hasPosterImage()` 편의 메서드 추가: `return this.posterImageObjectKey != null`
  - **주의**: `posterImageObjectKey`는 ONGOING 상태에서도 수정 가능하다 (EVT-INV-07 ONGOING 허용 필드 패턴 동일). `update()` 메서드의 ONGOING 제한 로직에 영향받지 않는다.
  - **[ACTION REQUIRED]**: 구현 완료 시 `event-verification-criteria.md` EVT-INV-07의 "ONGOING 상태 필드별 수정 가능 여부" 표에 `posterImageObjectKey | O | 정보성 필드` 행을 추가할 것 (검증 기준서 2-1 참조).
- **관련 검증 기준**: EVT-IMG-INV-01 (Nullable), EVT-IMG-INV-07 (이미지 해제), 검증 기준서 2-1 (상태 교차 매트릭스), 검증 기준서 8-3 (엔티티 변경)
- **관련 테스트 케이스**: TC-001, TC-002, TC-018, TC-023~TC-031
- **선행 작업**: TASK-001
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-003: EventRepository에 posterImageObjectKey 기반 쿼리 추가

- **작업 ID**: TASK-003
- **작업명**: `EventRepository`에 `existsByPosterImageObjectKeyAndDeletedFalse` 메서드 추가
- **설명**: `EventRepository`에 `EventFileReferenceChecker`가 사용할 쿼리 메서드를 추가한다. `@SQLRestriction("event_deleted = false")`이 적용되므로 별도의 `deleted = false` 조건은 JPA derived query에서 불필요하지만, 메서드명에 `DeletedFalse`를 포함하여 의도를 명확히 한다. 단, `@SQLRestriction`은 `findAll()`/`findById()` 등의 SELECT에 자동 적용되므로 실제 쿼리에서는 자동으로 `event_deleted = false` 필터가 동작한다.
  - `boolean existsByPosterImageObjectKey(String posterImageObjectKey)` -- `@SQLRestriction`에 의해 Soft Delete된 행사는 자동으로 제외됨
  - 또는 명시적으로 JPQL 사용: `@Query("SELECT COUNT(e) > 0 FROM Event e WHERE e.posterImageObjectKey = :objectKey")` -- 역시 `@SQLRestriction`이 적용됨
- **관련 검증 기준**: EVT-IMG-INV-04 (참조 무결성), 검증 기준서 8-4 (EventFileReferenceChecker)
- **관련 테스트 케이스**: TC-013, TC-014, TC-015, TC-050, TC-051, TC-052
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-004: EventFileReferenceChecker 구현

- **작업 ID**: TASK-004
- **작업명**: `EventFileReferenceChecker` 클래스 구현 (`FileReferenceChecker` 인터페이스)
- **설명**: `igrus.web.event.service` 패키지에 `EventFileReferenceChecker`를 구현한다. `FileReferenceChecker` 인터페이스의 `isReferenced(String objectKey)` 메서드를 구현하여, 해당 Object Key를 `posterImageObjectKey`로 참조하는 활성(deleted=false) 행사가 있는지 확인한다. `@Component`로 등록하면 `FileDeleteService`가 `List<FileReferenceChecker>`를 통해 자동으로 수집하여 참조 무결성 검사에 포함한다.
  - 로그 메시지: 참조 차단 시 `파일 삭제 차단 (행사 참조): objectKey={}, eventId={}` (WARN 레벨). eventId를 로깅하려면 존재하는 행사를 조회해야 하므로, `existsBy` 대신 `findByPosterImageObjectKey`를 사용하거나, 단순히 objectKey만 로깅하는 방안도 가능. 검증 기준서 6-1의 로그 규격을 따른다.
- **관련 검증 기준**: EVT-IMG-INV-04 (참조 무결성), 검증 기준서 3-2 (구성요소 책임), 검증 기준서 8-4
- **관련 테스트 케이스**: TC-013, TC-014, TC-015, TC-050, TC-051, TC-052
- **선행 작업**: TASK-003
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-005: 이미지 참조 검증 에러코드 및 예외 클래스 추가

- **작업 ID**: TASK-005
- **작업명**: `EventErrorCode`에 이미지 참조 검증 관련 에러코드 추가 및 예외 클래스 구현
- **설명**: 이미지 참조 검증 실패 시 사용할 에러코드 및 예외를 추가한다:
  1. `EventErrorCode`에 추가:
     - `IMAGE_NOT_COMPLETED` (400) -- Object Key의 FileMetadata가 존재하지만 COMPLETED 상태가 아님
     - `IMAGE_NOT_FOUND` (404) -- Object Key에 대응하는 FileMetadata가 존재하지 않음 (Soft Delete 포함)
     - `INVALID_IMAGE_PREFIX` (400) -- Object Key가 `events/` 프리픽스로 시작하지 않음
  2. 예외 클래스 추가 (기존 Event 예외 패턴 준수):
     - `InvalidImageReferenceException` -- 이미지 참조 검증 실패 (COMPLETED 아님 / 미존재 / 프리픽스 위반)
  - 또는 기존 예외를 활용하여 에러 메시지로 구분하는 방식도 가능. 기존 Event 도메인의 예외 패턴과 일관성을 유지한다.
- **관련 검증 기준**: EVT-IMG-INV-02 (COMPLETED 상태 검증), EVT-IMG-INV-03 (프리픽스 강제)
- **관련 테스트 케이스**: TC-004~TC-009, TC-011, TC-012, TC-048, TC-049
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 3. DTO 계층

#### TASK-006: CreateEventRequest에 posterImageObjectKey 필드 추가

- **작업 ID**: TASK-006
- **작업명**: `CreateEventRequest` record에 `posterImageObjectKey` 필드 추가
- **설명**: `CreateEventRequest` record에 `String posterImageObjectKey` 필드를 추가한다. nullable이며 required가 아니다. `@Size(max = 500)` 어노테이션으로 최대 길이를 제한한다 (DB 컬럼과 동일). `@NotBlank` 등은 사용하지 않는다 (DECISION-03: 빈 문자열은 null로 변환). Javadoc의 `@param` 설명도 추가한다.
- **관련 검증 기준**: 검증 기준서 8-2 (OpenAPI 스펙), 검증 기준서 8-3 (엔티티 변경)
- **관련 테스트 케이스**: TC-001, TC-003, TC-036, TC-047
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-007: UpdateEventRequest에 posterImageObjectKey 필드 추가

- **작업 ID**: TASK-007
- **작업명**: `UpdateEventRequest` record에 `posterImageObjectKey` 필드 추가
- **설명**: `UpdateEventRequest` record에 `String posterImageObjectKey` 필드를 추가한다. nullable이며 required가 아니다. `@Size(max = 500)` 어노테이션 적용. Javadoc 업데이트.
- **관련 검증 기준**: 검증 기준서 8-2 (OpenAPI 스펙)
- **관련 테스트 케이스**: TC-002, TC-018, TC-032, TC-033, TC-037, TC-038, TC-039, TC-048, TC-049
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-008: EventDetailResponse에 posterImageObjectKey 필드 추가

- **작업 ID**: TASK-008
- **작업명**: `EventDetailResponse` record에 `posterImageObjectKey` 필드 추가
- **설명**: `EventDetailResponse` record에 `String posterImageObjectKey` 필드를 추가한다. `from(Event, boolean, boolean)` 정적 팩토리 메서드에서 `event.getPosterImageObjectKey()`를 매핑한다. nullable이며, 이미지 미연결 행사의 경우 null 값으로 응답에 포함된다 (필드 자체가 생략되지 않음). Javadoc의 `@param` 설명 추가.
- **관련 검증 기준**: EVT-IMG-INV-08 (목록/상세 노출 정책), 검증 기준서 8-2 (OpenAPI 스펙)
- **관련 테스트 케이스**: TC-019, TC-020, TC-021, TC-022, TC-038, TC-040, TC-041
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-009: EventListResponse에 posterImageObjectKey 필드 추가

- **작업 ID**: TASK-009
- **작업명**: `EventListResponse` record에 `posterImageObjectKey` 필드 추가
- **설명**: `EventListResponse` record에 `String posterImageObjectKey` 필드를 추가한다. `from(Event)` 정적 팩토리 메서드에서 `event.getPosterImageObjectKey()`를 매핑한다. nullable. Javadoc 업데이트.
- **관련 검증 기준**: EVT-IMG-INV-08 (목록/상세 노출 정책), 검증 기준서 8-2 (OpenAPI 스펙)
- **관련 테스트 케이스**: TC-019, TC-020, TC-040
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-010: EventCreateResponse에 posterImageObjectKey 필드 추가

- **작업 ID**: TASK-010
- **작업명**: `EventCreateResponse` record에 `posterImageObjectKey` 필드 추가
- **설명**: `EventCreateResponse` record에 `String posterImageObjectKey` 필드를 추가한다. `from(Event)` 정적 팩토리 메서드에서 `event.getPosterImageObjectKey()`를 매핑한다. 행사 생성 응답에 연결된 이미지 정보를 반환하여 프론트엔드에서 즉시 확인 가능하도록 한다.
- **관련 검증 기준**: 검증 기준서 8-2 (OpenAPI 스펙)
- **관련 테스트 케이스**: TC-036
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 4. 서비스 계층

#### TASK-011: EventService에 이미지 참조 검증 로직 추가

- **작업 ID**: TASK-011
- **작업명**: `EventService.createEvent()` 및 `updateEvent()`에 이미지 참조 검증 로직 구현
- **설명**: `EventService`에 이미지 참조 검증을 위한 private 메서드를 추가하고, `createEvent()`와 `updateEvent()`에서 호출한다:
  1. `validatePosterImageObjectKey(String posterImageObjectKey)` private 메서드 추가:
     - **빈 문자열/공백 문자열 → null 변환** (DECISION-03 확정: A안). 서비스 레벨에서 `StringUtils.hasText()` 등으로 검사하여, 빈 문자열이나 공백만 있는 문자열을 null로 변환한다.
     - null이면 검증을 건너뛴다 (이미지 미연결/해제, EVT-IMG-INV-01)
     - **프리픽스 검증** (EVT-IMG-INV-03, DECISION-01 확정: A안): `objectKey.startsWith("events/")`인지 확인. 위반 시 400 Bad Request. 프리픽스 검증은 FileMetadata 상태 검증보다 **선행**한다.
     - **FileMetadata 존재 + 상태 검증** (EVT-IMG-INV-02): `FileMetadataRepository.findByObjectKeyAndDeletedFalse(objectKey)`로 조회. 미존재 시 404 Not Found (Soft Delete된 파일도 `@SQLRestriction`에 의해 미조회이므로 동일). 존재하지만 `status != COMPLETED`이면 400 Bad Request.
  2. `createEvent()` 수정:
     - 날짜 검증 후, 설문 검증 후에 이미지 참조 검증 호출
     - `Event.create()` 호출 시 `posterImageObjectKey` 전달 (null 변환 적용 후)
     - 이미지 포함 행사 생성 시 INFO 로그: `행사 생성: eventId={}, posterImageObjectKey={}`
  3. `updateEvent()` 수정:
     - 날짜 검증 후, 설문 검증 후에 이미지 참조 검증 호출
     - `Event.update()` 호출 시 `posterImageObjectKey` 전달 (null 변환 적용 후)
     - 이미지 변경 시 INFO 로그: `행사 수정 - eventId: {}, posterImageObjectKey 변경: {} -> {}`
     - 이미지 해제 시 INFO 로그: `행사 수정 - eventId: {}, posterImageObjectKey 해제: {} -> null`
     - 이미지 참조 검증 실패 시 WARN 로그: `이미지 참조 검증 실패: objectKey={}, status={}`
  - **의존성**: `FileMetadataRepository`를 `EventService`에 주입해야 한다. Event 모듈에서 Storage 모듈의 Repository에 의존하게 되지만, 검증 기준서 3-1의 의존 방향(Event -> Storage)과 일치한다.
- **관련 검증 기준**: EVT-IMG-INV-01 (Nullable), EVT-IMG-INV-02 (COMPLETED 검증), EVT-IMG-INV-03 (프리픽스 강제), EVT-IMG-INV-05 (이미지 변경 시 유지), EVT-IMG-INV-07 (이미지 해제), DECISION-01, DECISION-02, DECISION-03, 검증 기준서 6-1 (로그)
- **관련 테스트 케이스**: TC-001~TC-012, TC-016, TC-018, TC-032, TC-033, TC-036~TC-039, TC-044, TC-045, TC-047~TC-049, TC-053~TC-055
- **선행 작업**: TASK-002, TASK-005, TASK-006, TASK-007
- **구현 범위**: backend
- **예상 난이도**: 상

---

### 5. OpenAPI 스펙 업데이트

#### TASK-012: OpenAPI 스펙에 posterImageObjectKey 필드 추가

- **작업 ID**: TASK-012
- **작업명**: `openapi/schemas/events.yaml`의 7개 스키마에 `posterImageObjectKey` 필드 추가
- **설명**: OpenAPI 스펙 파일 `openapi/schemas/events.yaml`에 다음 스키마에 `posterImageObjectKey` 필드를 추가한다:

  | 스키마 | nullable | required 여부 |
  |--------|----------|:---:|
  | `CreateEventRequest` | true | false |
  | `UpdateEventRequest` | true | false |
  | `EventDetailResponse` | true | - |
  | `EventListResponse` | true | - |
  | `AdminEventDetailResponse` | true | - |
  | `AdminEventListResponse` | true | - |
  | `EventCreateResponse` | true | - |

  필드 정의 형식:
  ```yaml
  posterImageObjectKey:
    type:
      - string
      - 'null'
    maxLength: 500
    description: 행사 포스터 이미지 Object Key (null이면 이미지 미연결)
  ```
  - 요청 DTO에서는 `required`에 포함하지 않는다 (nullable).
  - Orval 재생성 필요 (프론트엔드 연동 시).
- **관련 검증 기준**: 검증 기준서 8-2 (OpenAPI 스펙 변경)
- **관련 테스트 케이스**: TC-019~TC-022, TC-036~TC-038, TC-040
- **선행 작업**: 없음
- **구현 범위**: backend (openapi/ 디렉토리)
- **예상 난이도**: 하

---

### 6. 테스트

#### TASK-013: EventFileReferenceChecker 단위 테스트

- **작업 ID**: TASK-013
- **작업명**: `EventFileReferenceCheckerTest` 단위 테스트 작성
- **설명**: `EventFileReferenceChecker`의 `isReferenced()` 메서드에 대한 단위 테스트를 작성한다. `EventRepository`를 Mock으로 주입한다:
  1. 참조 행사 존재 시 `true` 반환 (TC-050)
  2. 참조 행사 없을 시 `false` 반환 (TC-051)
  3. Soft Delete된 행사만 참조 시 `false` 반환 (TC-052) -- `@SQLRestriction`에 의해 Soft Delete 행사는 조회되지 않으므로 `existsBy` 결과가 false
- **관련 검증 기준**: EVT-IMG-INV-04 (참조 무결성)
- **관련 테스트 케이스**: TC-050, TC-051, TC-052
- **선행 작업**: TASK-004
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-014: EventService 이미지 참조 검증 서비스 통합 테스트

- **작업 ID**: TASK-014
- **작업명**: `EventServiceTest`에 이미지 참조 검증 관련 서비스 통합 테스트 추가
- **설명**: `EventServiceTest` 클래스에 이미지 참조 검증 관련 테스트를 추가한다. 실제 DB(H2)를 사용하여 `FileMetadata`와 `Event`를 생성하고 검증한다:
  1. **EVT-IMG-INV-01 (Nullable 정책)**:
     - TC-001: `posterImageObjectKey = null`로 행사 생성 성공
     - TC-002: 기존 이미지 있는 행사에서 `posterImageObjectKey = null`로 수정하여 이미지 해제 성공
  2. **EVT-IMG-INV-02 (COMPLETED 상태 검증)**:
     - TC-003: COMPLETED 상태 Object Key로 행사 생성 성공
     - TC-004: REQUESTED 상태 Object Key로 행사 생성 시 거부 (400)
     - TC-005: CONFIRMING 상태 Object Key로 행사 생성 시 거부 (400)
     - TC-006: FAILED 상태 Object Key로 행사 생성 시 거부 (400)
     - TC-007: EXPIRED 상태 Object Key로 행사 생성 시 거부 (400)
     - TC-008: 존재하지 않는 Object Key로 행사 생성 시 거부 (404)
     - TC-009: Soft Delete된 FileMetadata의 Object Key로 행사 생성 시 거부 (404)
  3. **EVT-IMG-INV-03 (프리픽스 강제)**:
     - TC-010: `events/` 프리픽스 Object Key로 행사 생성 성공
     - TC-011: `posts/` 프리픽스 Object Key로 행사 생성 시 거부 (400)
     - TC-012: `profiles/` 프리픽스 Object Key로 행사 생성 시 거부 (400)
  4. **EVT-IMG-INV-05 (이미지 변경 시 유지)**:
     - TC-016: 이미지 A에서 B로 변경 후 A의 FileMetadata COMPLETED 유지 확인
  5. **EVT-IMG-INV-06 (행사 삭제 시 유지)**:
     - TC-017: 이미지 연결된 행사 Soft Delete 후 이미지 FileMetadata COMPLETED 유지 확인
  6. **EVT-IMG-INV-07 (이미지 해제)**:
     - TC-018: `posterImageObjectKey = null`로 수정하여 이미지 해제 후 이전 이미지 유지 확인
  7. **DECISION-03 (빈 문자열/공백 → null 변환)**:
     - TC-032: 빈 문자열 `""` → null 변환 확인
     - TC-033: 공백 문자열 `"   "` → null 변환 확인
     - TC-047: 행사 생성 시 빈 문자열 → null 변환 확인
  8. **수정 시 검증**:
     - TC-039: 행사 수정 시 REQUESTED 상태 Key로 변경 시도 → 거부 (400)
     - TC-048: COMPLETED 상태이지만 `posts/` 프리픽스 Key로 수정 시 거부 (400)
     - TC-049: 존재하지 않는 Object Key로 수정 시도 → 거부 (404)
  9. **SEC-EVT-IMG-01, SEC-EVT-IMG-02 (다른 사용자 이미지 사용)**:
     - TC-044: 운영진 A가 업로드한 이미지를 운영진 B가 행사에 연결 -- 허용
     - TC-045: MEMBER가 업로드한 이미지를 OPERATOR가 행사에 연결 -- 허용
- **관련 검증 기준**: EVT-IMG-INV-01~07, SEC-EVT-IMG-01, SEC-EVT-IMG-02, DECISION-01~03
- **관련 테스트 케이스**: TC-001~TC-012, TC-016~TC-018, TC-032, TC-033, TC-039, TC-044, TC-045, TC-047~TC-049
- **선행 작업**: TASK-011
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-015: EventService 이미지-상태 교차 매트릭스 서비스 통합 테스트

- **작업 ID**: TASK-015
- **작업명**: `EventServiceTest`에 eventStatus별 이미지 연결/변경/해제 교차 테스트 추가
- **설명**: 행사의 3축 상태(eventStatus)에 따른 이미지 연결/변경/해제 가능 여부를 테스트한다:
  1. TC-023: UPCOMING 행사 -- 이미지 연결 (null -> COMPLETED Key) 성공
  2. TC-024: UPCOMING 행사 -- 이미지 해제 (COMPLETED Key -> null) 성공
  3. TC-025: UPCOMING 행사 -- 이미지 변경 (Key A -> Key B) 성공
  4. TC-026: ONGOING 행사 -- 이미지 연결 성공
  5. TC-027: ONGOING 행사 -- 이미지 해제 성공
  6. TC-028: ONGOING 행사 -- 이미지 변경 성공 (Key A -> Key B)
  7. TC-029: COMPLETED 행사 -- 이미지 연결 시도 시 거부 (EventNotEditableException)
  8. TC-030: COMPLETED 행사 -- 이미지 해제 시도 시 거부 (EventNotEditableException)
  9. TC-031: CANCELED 행사 -- 이미지 연결 성공
- **관련 검증 기준**: 검증 기준서 2-1 (eventStatus별 이미지 연결 교차 매트릭스), EVT-IMG-INV-01, EVT-IMG-INV-07
- **관련 테스트 케이스**: TC-023~TC-031
- **선행 작업**: TASK-011
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-016: FileDeleteService 참조 무결성 서비스 통합 테스트

- **작업 ID**: TASK-016
- **작업명**: FileDeleteService에서 EventFileReferenceChecker를 통한 참조 무결성 검사 통합 테스트
- **설명**: `FileDeleteService`의 `deleteFile()` 메서드가 `EventFileReferenceChecker`를 통해 참조 무결성을 올바르게 검사하는지 통합 테스트한다:
  1. TC-013: 행사에서 참조 중인 Object Key로 파일 삭제 시 `FileReferenceExistsException` (409 Conflict)
  2. TC-014: 행사에서 참조하지 않는 Object Key로 파일 삭제 성공
  3. TC-015: Soft Delete된 행사만 참조하는 Object Key로 파일 삭제 허용
  - **주의**: `FileDeleteService`는 `S3Client`를 사용하므로, 통합 테스트에서 S3Client를 Mock으로 대체하거나, TC-014/TC-015는 S3 연동이 필요한 부분을 Mock 처리한다.
- **관련 검증 기준**: EVT-IMG-INV-04 (참조 무결성)
- **관련 테스트 케이스**: TC-013, TC-014, TC-015
- **선행 작업**: TASK-004
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-017: 컨트롤러 통합 테스트 -- API 요청/응답 이미지 필드 검증

- **작업 ID**: TASK-017
- **작업명**: 행사 컨트롤러 통합 테스트에 posterImageObjectKey API 필드 검증 추가
- **설명**: MockMvc 기반 컨트롤러 통합 테스트에서 API 요청/응답에 `posterImageObjectKey` 필드가 올바르게 포함되는지 검증한다:
  1. **행사 생성 API**:
     - TC-036: `POST /api/v1/admin/events` -- `posterImageObjectKey` 포함 요청 시 201 Created, 응답에 `posterImageObjectKey` 포함
  2. **행사 수정 API**:
     - TC-037: `PUT /api/v1/admin/events/{eventId}` -- `posterImageObjectKey` 변경 요청 시 200 OK
     - TC-038: `PUT /api/v1/admin/events/{eventId}` -- `posterImageObjectKey: null` 요청 시 이미지 해제, 상세 조회에서 null 확인
  3. **공개 목록 조회**:
     - TC-019: `GET /api/v1/events` (MEMBER) -- 이미지 연결 행사의 `posterImageObjectKey` 필드 non-null 포함
     - TC-020: `GET /api/v1/events` (MEMBER) -- 이미지 미연결 행사의 `posterImageObjectKey` 필드가 null로 포함 (필드 생략 아님)
  4. **공개 상세 조회**:
     - TC-021: `GET /api/v1/events/{eventId}` (MEMBER) -- `posterImageObjectKey` 필드 포함
  5. **관리자 API**:
     - TC-022: `GET /api/v1/admin/events` (OPERATOR) -- 목록 응답에 `posterImageObjectKey` 포함
     - TC-022: `GET /api/v1/admin/events/{eventId}` (OPERATOR) -- 상세 응답에 `posterImageObjectKey` 포함
  6. **RBAC 검증**:
     - TC-040: ASSOCIATE 공개 목록 조회 시 `posterImageObjectKey` 포함 응답 확인
     - TC-041: ASSOCIATE 공개 상세 조회 시 403 응답 (기존 SEC-EVT-01 유지)
     - TC-042: 비인증 사용자 행사 목록 조회 시 401
     - TC-043: MEMBER 행사 생성 시도 시 403
- **관련 검증 기준**: EVT-IMG-INV-08 (목록/상세 노출 정책), 검증 기준서 5-1 (RBAC)
- **관련 테스트 케이스**: TC-019~TC-022, TC-036~TC-043
- **선행 작업**: TASK-008, TASK-009, TASK-010, TASK-011, TASK-012, TASK-022
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-018: Object Key 길이 경계값 테스트

- **작업 ID**: TASK-018
- **작업명**: Object Key 길이 경계값 서비스 통합 테스트
- **설명**: Object Key의 길이 경계값을 테스트한다:
  1. TC-034: 일반 길이 Object Key (~60자) 허용
  2. TC-035: 500자 Object Key (DB 컬럼 최대 길이) 허용
  - 실제 `ObjectKeyGenerator`가 생성하는 Key는 ~60자이므로 500자 도달은 사실상 불가능하지만, DB 컬럼 제약과의 정합성을 검증한다.
- **관련 검증 기준**: 검증 기준서 4-2 (Object Key 길이 경계값)
- **관련 테스트 케이스**: TC-034, TC-035
- **선행 작업**: TASK-011
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-019: SEC-EVT-IMG-03 (미인증 다운로드 차단) 테스트

- **작업 ID**: TASK-019
- **작업명**: 미인증 사용자 이미지 다운로드 URL 요청 시 401 확인 테스트
- **설명**: 이미지가 연결된 행사의 Object Key로 `GET /api/v1/storage/download-url` 요청 시 미인증 사용자는 401 Unauthorized를 받는지 확인한다. 이 테스트는 Storage 모듈의 기존 RBAC 정책(SEC-STOR-04)을 확인하는 것이므로, 기존 Storage 컨트롤러 테스트에서 이미 커버되고 있을 가능성이 높다. 기존 테스트와 중복되면 확인만 하고 별도 작성하지 않는다.
- **관련 검증 기준**: SEC-EVT-IMG-03
- **관련 테스트 케이스**: TC-046
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-020: 관측 가능성 (로그 메시지) 테스트

- **작업 ID**: TASK-020
- **작업명**: 이미지 연결/변경/해제/검증 실패 시 로그 메시지 검증 테스트
- **설명**: 서비스 통합 테스트에서 로그 출력을 검증한다. LogCaptor 또는 SLF4J Test 등을 사용하여 로그 레벨과 메시지를 확인한다:
  1. TC-053: 이미지 포함 행사 생성 시 INFO 로그 확인 (`행사 생성: eventId={}, posterImageObjectKey={}`)
  2. TC-054: 이미지 변경 시 이전/이후 Key INFO 로그 확인 (`행사 수정 - eventId: {}, posterImageObjectKey 변경: {} -> {}`)
  3. TC-055: 이미지 참조 검증 실패 시 WARN 로그 확인 (`이미지 참조 검증 실패: objectKey={}, status={}`)
  - **참고**: 로그 테스트는 우선순위 하(Low)이며, 다른 테스트에서 이미 동작이 검증된 경우 로그 테스트는 선택적이다.
- **관련 검증 기준**: 검증 기준서 6-1 (서비스별 로그 메시지)
- **관련 테스트 케이스**: TC-053, TC-054, TC-055
- **선행 작업**: TASK-011
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 7. 문서 업데이트

#### TASK-021: event-verification-criteria.md EVT-INV-07 업데이트

- **작업 ID**: TASK-021
- **작업명**: 행사 검증 기준서 EVT-INV-07 ONGOING 필드 목록에 posterImageObjectKey 추가
- **설명**: `docs/criteria/event/event-verification-criteria.md`의 EVT-INV-07 "ONGOING 상태 필드별 수정 가능 여부" 표에 `posterImageObjectKey | O | 정보성 필드` 행을 추가한다. 이는 검증 기준서 2-1의 [ACTION REQUIRED] 항목이다.
- **관련 검증 기준**: 검증 기준서 2-1 [ACTION REQUIRED]
- **관련 테스트 케이스**: TC-026, TC-027, TC-028
- **선행 작업**: TASK-002
- **구현 범위**: docs
- **예상 난이도**: 하

---

### 8. 컨트롤러 계층

#### TASK-022: AdminEventController 매핑 헬퍼 메서드에 posterImageObjectKey 매핑 추가

- **작업 ID**: TASK-022
- **작업명**: `AdminEventController`의 `mapToAdminEventDetailResponse()` 및 `mapToAdminEventListResponse()` 매핑 헬퍼에 `posterImageObjectKey` 필드 매핑 추가
- **설명**: `AdminEventController.java`에는 서비스 레이어의 DTO(`EventDetailResponse`, `EventListResponse`)를 OpenAPI 생성 DTO(`GetAdminEvent200Response`, `GetAdminEventList200ResponseInner`)로 변환하는 매핑 헬퍼 메서드가 존재한다. OpenAPI 스펙(TASK-012)에 `posterImageObjectKey` 필드를 추가하면 생성 DTO에 `.posterImageObjectKey()` setter가 추가되므로, 매핑 헬퍼에서 이를 호출해야 한다. 누락 시 관리자 API 응답에서 `posterImageObjectKey`가 항상 null로 반환되는 버그가 발생한다.
  1. `mapToAdminEventDetailResponse(EventDetailResponse r)` 메서드에 `.posterImageObjectKey(r.posterImageObjectKey())` 체이닝 추가
  2. `mapToAdminEventListResponse(EventListResponse r)` 메서드에 `.posterImageObjectKey(r.posterImageObjectKey())` 체이닝 추가
  - **참고**: 공개 API 컨트롤러(`EventController`)는 서비스 DTO를 직접 반환하거나 `from()` 팩토리 메서드를 통해 변환하므로, DTO에 필드가 추가되면 자동으로 반영된다. 관리자 API 컨트롤러만 수동 매핑 헬퍼를 사용하므로 별도 작업이 필요하다.
- **관련 검증 기준**: EVT-IMG-INV-08 (관리자 API 응답에 posterImageObjectKey 포함)
- **관련 테스트 케이스**: TC-022 (관리자 API 목록/상세 응답에 posterImageObjectKey 포함)
- **선행 작업**: TASK-008, TASK-009, TASK-012
- **구현 범위**: backend
- **예상 난이도**: 하

---

## 작업 순서 및 의존성

### 의존성 그래프

```
TASK-001 (Flyway 마이그레이션)
  +--(독립)

TASK-005 (에러코드/예외 클래스)
  +--(독립)

TASK-006 (CreateEventRequest 필드 추가)
  +--(독립)

TASK-007 (UpdateEventRequest 필드 추가)
  +--(독립)

TASK-012 (OpenAPI 스펙 업데이트)
  +--(독립)

TASK-001
  +-- TASK-002 (Event 엔티티 posterImageObjectKey)
  |     +-- TASK-003 (EventRepository 쿼리 추가)
  |     |     +-- TASK-004 (EventFileReferenceChecker)
  |     |           +-- TASK-013 (EventFileReferenceChecker 단위 테스트)
  |     |           +-- TASK-016 (FileDeleteService 참조 무결성 통합 테스트)
  |     +-- TASK-008 (EventDetailResponse 필드 추가)
  |     +-- TASK-009 (EventListResponse 필드 추가)
  |     +-- TASK-010 (EventCreateResponse 필드 추가)
  |     +-- TASK-021 (문서 업데이트)

TASK-002 + TASK-005 + TASK-006 + TASK-007
  +-- TASK-011 (EventService 이미지 참조 검증)
        +-- TASK-014 (이미지 참조 검증 서비스 통합 테스트)
        +-- TASK-015 (이미지-상태 교차 매트릭스 테스트)
        +-- TASK-018 (Object Key 길이 경계값 테스트)
        +-- TASK-020 (로그 메시지 테스트)

TASK-008 + TASK-009 + TASK-012
  +-- TASK-022 (AdminEventController 매핑 헬퍼 수정)

TASK-008 + TASK-009 + TASK-010 + TASK-011 + TASK-012 + TASK-022
  +-- TASK-017 (컨트롤러 통합 테스트)

TASK-019 (SEC-EVT-IMG-03 테스트)
  +--(독립, 기존 Storage 테스트 확인)
```

### 권장 실행 순서

**Phase 1 -- 기반 구조 (병렬 가능)**
1. TASK-001: Flyway 마이그레이션
2. TASK-005: 에러코드/예외 클래스
3. TASK-006: CreateEventRequest 필드 추가
4. TASK-007: UpdateEventRequest 필드 추가
5. TASK-012: OpenAPI 스펙 업데이트

**Phase 2 -- 도메인/DTO (TASK-001 완료 후, 병렬 가능)**
6. TASK-002: Event 엔티티 posterImageObjectKey 필드 추가
7. TASK-008: EventDetailResponse 필드 추가 (TASK-002 완료 후)
8. TASK-009: EventListResponse 필드 추가 (TASK-002 완료 후)
9. TASK-010: EventCreateResponse 필드 추가 (TASK-002 완료 후)

**Phase 3 -- Repository/FileReferenceChecker (TASK-002 완료 후)**
10. TASK-003: EventRepository 쿼리 추가
11. TASK-004: EventFileReferenceChecker 구현 (TASK-003 완료 후)

**Phase 4 -- 서비스 로직 및 컨트롤러 매핑 (TASK-002 + TASK-005 + TASK-006 + TASK-007 완료 후)**
12. TASK-011: EventService 이미지 참조 검증 로직
13. TASK-022: AdminEventController 매핑 헬퍼 수정 (TASK-008 + TASK-009 + TASK-012 완료 후)

**Phase 5 -- 테스트 (각 구현 완료 후, 병렬 가능)**
14. TASK-013: EventFileReferenceChecker 단위 테스트
15. TASK-014: 이미지 참조 검증 서비스 통합 테스트
16. TASK-015: 이미지-상태 교차 매트릭스 테스트
17. TASK-016: FileDeleteService 참조 무결성 통합 테스트
18. TASK-017: 컨트롤러 통합 테스트 (TASK-022 완료 필수)
19. TASK-018: Object Key 길이 경계값 테스트
20. TASK-019: SEC-EVT-IMG-03 테스트
21. TASK-020: 로그 메시지 테스트

**Phase 6 -- 문서 (TASK-002 완료 후)**
22. TASK-021: event-verification-criteria.md 업데이트

---

## 구현 시 주의사항

### 기술적 고려사항

1. **약한 참조 설계**: Event와 FileMetadata 간에 JPA `@ManyToOne` 또는 FK 제약을 사용하지 않는다. `posterImageObjectKey`는 `String` 타입 필드로, `FileMetadata.objectKey` 값을 문자열로 저장한다. 기존 `Event.surveyId` (Long, nullable, FK 없음)와 동일한 패턴이다.
2. **FileMetadataRepository 의존**: `EventService`에서 `FileMetadataRepository`를 주입받아 이미지 참조 검증을 수행한다. 이는 Event 모듈에서 Storage 모듈 방향의 의존이며, 검증 기준서 3-1의 의존 방향과 일치한다.
3. **빈 문자열/공백 변환**: DECISION-03에 따라 `""` 및 `"   "`은 서비스 레벨에서 null로 변환한다. `StringUtils.hasText()`를 사용하면 빈 문자열과 공백만 있는 문자열 모두를 감지할 수 있다. 이 변환은 `@NotBlank` 등의 Bean Validation이 아닌 서비스 로직에서 처리한다.
4. **프리픽스 검증 순서**: `events/` 프리픽스 검증은 FileMetadata 상태 검증보다 **먼저** 수행한다 (TC-011 비고 참조). 프리픽스가 잘못된 경우 FileMetadata를 조회할 필요 없이 즉시 거부한다.
5. **`@SQLRestriction` 자동 필터링**: Event 엔티티에 `@SQLRestriction("event_deleted = false")`이 적용되어 있으므로, `EventRepository`의 SELECT 쿼리에서 Soft Delete된 행사는 자동으로 제외된다. `EventFileReferenceChecker`에서 `existsByPosterImageObjectKey()`를 사용하면 Soft Delete된 행사는 자동으로 제외되어 TC-015(Soft Delete 행사 참조 시 삭제 허용)가 보장된다.
6. **ONGOING 상태에서 이미지 수정 가능**: `posterImageObjectKey`는 정보성 필드이므로 ONGOING 상태에서도 수정 가능하다. `Event.update()` 메서드의 ONGOING 제한 로직(eventStartAt, registrationStartAt 변경 차단)에 영향받지 않도록 주의한다. 이미지 관련 파라미터는 ONGOING 제한 검증 이후에 설정되므로 문제없다.
7. **이미지 자동 삭제 없음**: 행사 수정으로 이미지가 변경되거나, 행사가 Soft Delete되더라도 이전 이미지의 FileMetadata와 S3 객체는 자동으로 삭제되지 않는다 (EVT-IMG-INV-05, EVT-IMG-INV-06). 고아 이미지 정리는 별도 스케줄러 또는 수동 운영으로 처리한다.

### 잠재적 위험 요소

1. **기존 테스트 깨짐**: `Event.create()` 및 `Event.update()` 메서드에 `posterImageObjectKey` 파라미터가 추가되면, 기존 테스트에서 해당 메서드를 호출하는 모든 곳에 `null` 인자를 추가해야 한다. 파라미터 추가 대신 `Event.create()` 오버로드 메서드를 추가하는 방법도 고려할 수 있으나, 기존 패턴(모든 필드를 파라미터로 받는 단일 메서드)과 일관성을 유지하는 것이 바람직하다.
2. **Orval 재생성**: OpenAPI 스펙 변경으로 프론트엔드 Orval 자동 생성 코드가 변경된다. `posterImageObjectKey` 필드가 추가되므로 프론트엔드에서 해당 필드를 처리하지 않아도 타입 에러는 발생하지 않지만 (nullable), 프론트엔드 동기화 시점을 조율해야 한다.
3. **DTO record 생성자 호출**: `EventDetailResponse`, `EventListResponse`, `EventCreateResponse`에 필드가 추가되면 기존 테스트에서 record 생성자를 직접 호출하는 곳이 깨질 수 있다. `from()` 팩토리 메서드를 사용하는 곳은 영향 없다.
4. **FileMetadataRepository 주입**: `EventService`에 `FileMetadataRepository`를 새로 주입해야 한다. 기존 생성자 주입 패턴(`@RequiredArgsConstructor`)을 사용하므로 필드 추가만으로 자동 반영된다.
5. **AdminEventController 매핑 헬퍼 누락 위험**: `AdminEventController`의 `mapToAdminEventDetailResponse()`, `mapToAdminEventListResponse()` 메서드는 서비스 DTO를 OpenAPI 생성 DTO로 **수동 매핑**하므로, DTO에 필드를 추가하더라도 매핑 헬퍼에서 `.posterImageObjectKey(r.posterImageObjectKey())`를 명시적으로 호출하지 않으면 관리자 API 응답에서 해당 필드가 항상 null로 반환된다. 공개 API 컨트롤러는 서비스 DTO를 직접 반환하므로 이 문제가 없지만, 관리자 API는 TASK-022에서 반드시 처리해야 한다.

### 기존 코드와의 통합 포인트

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `Event.java` | **수정** | `posterImageObjectKey` 필드 추가, `create()` / `update()` 메서드 파라미터 추가, `hasPosterImage()` 메서드 추가 |
| `EventRepository.java` | **수정** | `existsByPosterImageObjectKey()` 쿼리 메서드 추가 |
| `CreateEventRequest.java` | **수정** | `posterImageObjectKey` 필드 추가 |
| `UpdateEventRequest.java` | **수정** | `posterImageObjectKey` 필드 추가 |
| `EventDetailResponse.java` | **수정** | `posterImageObjectKey` 필드 추가, `from()` 메서드 수정 |
| `EventListResponse.java` | **수정** | `posterImageObjectKey` 필드 추가, `from()` 메서드 수정 |
| `EventCreateResponse.java` | **수정** | `posterImageObjectKey` 필드 추가, `from()` 메서드 수정 |
| `AdminEventController.java` | **수정** | `mapToAdminEventDetailResponse()`, `mapToAdminEventListResponse()` 매핑 헬퍼에 `.posterImageObjectKey()` 매핑 추가 |
| `EventService.java` | **수정** | `FileMetadataRepository` 주입, `validatePosterImageObjectKey()` 메서드 추가, `createEvent()` / `updateEvent()` 수정, 로그 추가 |
| `EventErrorCode.java` | **수정** | 이미지 참조 검증 에러코드 추가 |
| `openapi/schemas/events.yaml` | **수정** | 7개 스키마에 `posterImageObjectKey` 필드 추가 |
| `EventFileReferenceChecker.java` | **신규** | `FileReferenceChecker` 구현체 |
| `InvalidImageReferenceException.java` | **신규** | 이미지 참조 검증 실패 예외 |
| `V48__add_poster_image_object_key_to_events.sql` | **신규** | Flyway 마이그레이션 |
| `event-verification-criteria.md` | **수정** | EVT-INV-07 ONGOING 필드 목록에 posterImageObjectKey 추가 |

---

## 완료 기준

### 검증 기준 충족 체크리스트

- [ ] EVT-IMG-INV-01: `posterImageObjectKey`가 nullable이며, null로 행사 생성/수정 가능
- [ ] EVT-IMG-INV-02: COMPLETED 상태 Object Key만 행사 연결 허용. REQUESTED/CONFIRMING/FAILED/EXPIRED 상태 및 미존재/Soft Delete Key 거부
- [ ] EVT-IMG-INV-03: `events/` 프리픽스 강제 검증. `posts/` `profiles/` 등 다른 프리픽스 거부
- [ ] EVT-IMG-INV-04: EventFileReferenceChecker가 FileReferenceChecker 인터페이스를 구현하여, 참조 중인 이미지 삭제를 차단 (409 Conflict). Soft Delete 행사는 참조에서 제외
- [ ] EVT-IMG-INV-05: 이미지 변경 시 기존 이미지 FileMetadata와 S3 객체가 자동 삭제되지 않음
- [ ] EVT-IMG-INV-06: 행사 Soft Delete 시 연결된 이미지가 자동 삭제되지 않음
- [ ] EVT-IMG-INV-07: `posterImageObjectKey`를 null로 설정하여 이미지 해제 가능, 이전 이미지 유지
- [ ] EVT-IMG-INV-08: 목록/상세/관리자 API 응답 모두에 `posterImageObjectKey` 필드가 포함 (nullable). 미연결 행사는 null로 포함 (필드 미생략)
- [ ] SEC-EVT-IMG-01: 운영진 간 이미지 공유 허용 (업로드자 소유권 검증 없음)
- [ ] SEC-EVT-IMG-02: 일반 회원 업로드 이미지를 운영진이 행사에 연결 허용
- [ ] SEC-EVT-IMG-03: 미인증 사용자 이미지 다운로드 URL 요청 시 401 (기존 SEC-STOR-04)
- [ ] DECISION-01: `events/` 프리픽스 강제 (A안 확정)
- [ ] DECISION-02: 소유권 무관 (A안 확정)
- [ ] DECISION-03: 빈 문자열/공백 문자열 → null 변환 (A안 확정)

### 테스트 통과 체크리스트

**도메인 규칙과 불변조건 (22개)**
- [ ] TC-001: posterImageObjectKey를 null로 행사 생성 성공
- [ ] TC-002: posterImageObjectKey를 null로 행사 수정 성공 (이미지 해제)
- [ ] TC-003: COMPLETED 상태 Object Key로 행사 생성 성공
- [ ] TC-004: REQUESTED 상태 Object Key로 행사 생성 시 거부 (400)
- [ ] TC-005: CONFIRMING 상태 Object Key로 행사 생성 시 거부 (400)
- [ ] TC-006: FAILED 상태 Object Key로 행사 생성 시 거부 (400)
- [ ] TC-007: EXPIRED 상태 Object Key로 행사 생성 시 거부 (400)
- [ ] TC-008: 존재하지 않는 Object Key로 행사 생성 시 거부 (404)
- [ ] TC-009: Soft Delete된 FileMetadata의 Object Key로 행사 생성 시 거부 (404)
- [ ] TC-010: `events/` 프리픽스 Object Key로 행사 생성 성공
- [ ] TC-011: `posts/` 프리픽스 Object Key로 행사 생성 시 거부 (400)
- [ ] TC-012: `profiles/` 프리픽스 Object Key로 행사 생성 시 거부 (400)
- [ ] TC-013: 행사에서 참조 중인 Object Key로 파일 삭제 시 409 Conflict
- [ ] TC-014: 행사에서 참조하지 않는 Object Key로 파일 삭제 성공
- [ ] TC-015: Soft Delete된 행사만 참조하는 Object Key로 파일 삭제 허용
- [ ] TC-016: 이미지 A에서 B로 변경 후 A의 FileMetadata 유지 확인
- [ ] TC-017: 이미지 연결된 행사 Soft Delete 후 이미지 FileMetadata 유지 확인
- [ ] TC-018: posterImageObjectKey를 null로 수정하여 이미지 해제
- [ ] TC-019: 공개 목록 조회 응답에 posterImageObjectKey 필드 포함 (이미지 연결)
- [ ] TC-020: 공개 목록 조회 응답에 posterImageObjectKey 필드가 null로 포함 (이미지 미연결)
- [ ] TC-021: 공개 상세 조회 응답에 posterImageObjectKey 필드 포함 (MEMBER)
- [ ] TC-022: 관리자 API 목록/상세 응답에 posterImageObjectKey 필드 포함 (OPERATOR)

**상태 교차 매트릭스 (9개)**
- [ ] TC-023: UPCOMING 행사 -- 이미지 연결 성공
- [ ] TC-024: UPCOMING 행사 -- 이미지 해제 성공
- [ ] TC-025: UPCOMING 행사 -- 이미지 변경 성공
- [ ] TC-026: ONGOING 행사 -- 이미지 연결 성공
- [ ] TC-027: ONGOING 행사 -- 이미지 해제 성공
- [ ] TC-028: ONGOING 행사 -- 이미지 변경 성공
- [ ] TC-029: COMPLETED 행사 -- 이미지 연결 시도 시 거부
- [ ] TC-030: COMPLETED 행사 -- 이미지 해제 시도 시 거부
- [ ] TC-031: CANCELED 행사 -- 이미지 연결 성공

**입력 경계값 (8개)**
- [ ] TC-032: 빈 문자열 `""` -> null 변환으로 이미지 해제
- [ ] TC-033: 공백 문자열 `"   "` -> null 변환으로 이미지 해제
- [ ] TC-034: 일반 길이 Object Key (~60자) 허용
- [ ] TC-035: 500자 Object Key 허용
- [ ] TC-036: 행사 생성 API에서 posterImageObjectKey 포함 성공
- [ ] TC-037: 행사 수정 API로 이미지 변경
- [ ] TC-038: 행사 수정 API로 이미지 해제 후 상세 조회에서 null 확인
- [ ] TC-039: 행사 수정 시 REQUESTED 상태 Key로 변경 시도 -> 거부

**권한/보안 정책 (7개)**
- [ ] TC-040: ASSOCIATE 공개 목록 조회 시 posterImageObjectKey 포함 응답
- [ ] TC-041: ASSOCIATE 공개 상세 조회 시 403
- [ ] TC-042: 비인증 사용자 행사 목록 조회 시 401
- [ ] TC-043: MEMBER 행사 생성 시도 시 403
- [ ] TC-044: 운영진 A 이미지를 운영진 B가 행사에 연결 -- 허용
- [ ] TC-045: MEMBER 이미지를 OPERATOR가 행사에 연결 -- 허용
- [ ] TC-046: 미인증 사용자 이미지 다운로드 URL 요청 시 401

**부정 시나리오 (6개)**
- [ ] TC-047: 행사 생성 시 빈 문자열 -> null 변환
- [ ] TC-048: COMPLETED 상태 + `posts/` 프리픽스 Key로 수정 시 거부
- [ ] TC-049: 존재하지 않는 Object Key로 수정 시도 -> 거부
- [ ] TC-050: EventFileReferenceChecker -- 참조 행사 존재 시 true
- [ ] TC-051: EventFileReferenceChecker -- 참조 행사 없을 시 false
- [ ] TC-052: EventFileReferenceChecker -- Soft Delete 행사만 참조 시 false

**관측 가능성 (3개)**
- [ ] TC-053: 이미지 포함 행사 생성 시 INFO 로그
- [ ] TC-054: 이미지 변경 시 이전/이후 Key INFO 로그
- [ ] TC-055: 이미지 참조 검증 실패 시 WARN 로그

### 확인이 필요한 사항

1. **EventFileReferenceChecker 로그에 eventId 포함 여부**: 검증 기준서 6-1의 참조 무결성 검사 차단 로그에 `eventId`를 포함하려면, `existsBy` 대신 `findByPosterImageObjectKey`로 조회하여 eventId를 가져와야 한다. 성능과 로그 상세 수준 간의 트레이드오프를 결정해야 한다. 단순히 `isReferenced()`의 boolean 반환만으로는 eventId를 알 수 없으므로, `objectKey`만 로깅하는 것이 현실적일 수 있다.
2. **Event.create() 파라미터 추가 방식**: `posterImageObjectKey`를 기존 `create()` 메서드에 파라미터로 추가하면 기존 호출부가 모두 깨진다. 오버로드 메서드를 추가할지, 기존 메서드를 수정할지 결정해야 한다. 기존 패턴(단일 팩토리 메서드)을 따라 기존 메서드에 파라미터를 추가하고 기존 호출부를 수정하는 것이 바람직하다.
3. **프론트엔드 동기화 시점**: OpenAPI 스펙 변경 후 Orval 재생성 및 프론트엔드 UI(이미지 업로드/표시) 작업 시점을 조율해야 한다.
