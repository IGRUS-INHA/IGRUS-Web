# 행사 그룹(EventGroup) 기능 명세서

**작성일**: 2026-03-05
**버전**: 1.0
**우선순위**: P1
**관련 이슈**: #500

---

## 1. 개요

### 1.1 목적
원데이 클래스처럼 하나의 큰 행사 아래 여러 세부 행사(백엔드, 프론트엔드, AI 등)를 묶는 그룹핑 기능을 제공합니다. 각 세부 행사는 독립적인 정원 관리를 유지하면서, 그룹 단위로 묶어 사용자에게 보여줍니다.

### 1.2 범위
- 행사 그룹 CRUD (생성, 조회, 수정, 삭제)
- 그룹 공개/비공개 (publish/unpublish)
- 그룹-행사 연결/해제 (기존 행사 API의 groupId 필드 활용)
- 그룹 조회 시 하위 행사 집계 (신청 가능 행사 이름 등)
- 기존 행사 API에 그룹 관련 필드 추가

### 1.3 범위 외
- 그룹 자체에 대한 신청/등록 기능 (행사 개별로 처리)
- 그룹 내 행사 순서 관리 (display_order)
- N:M 관계 (행사가 여러 그룹에 속하는 것)

---

## 2. 사용자 스토리

| ID | 역할 | 스토리 | 우선순위 |
|----|------|--------|---------|
| US-EG01 | 운영진 | 여러 세부 행사를 하나의 그룹으로 묶을 수 있다 | P1 |
| US-EG02 | 운영진 | 그룹 정보(이름, 설명)를 수정할 수 있다 | P1 |
| US-EG03 | 운영진 | 그룹을 공개/비공개 전환할 수 있다 | P1 |
| US-EG04 | 운영진 | 그룹을 삭제할 수 있다 (하위 행사 없을 때만) | P1 |
| US-EG05 | 운영진 | 행사 생성/수정 시 그룹을 지정하거나 해제할 수 있다 | P1 |
| US-EG06 | 운영진 | 관리자 페이지에서 모든 그룹(비공개 포함)을 조회할 수 있다 | P1 |
| US-EG07 | 운영진 | 관리자 행사 목록에서 그룹별 필터링을 할 수 있다 | P2 |
| US-EG08 | 회원 | 행사 페이지에서 그룹 목록과 독립 행사를 함께 볼 수 있다 | P1 |
| US-EG09 | 회원 | 그룹을 클릭하면 하위 행사 목록을 볼 수 있다 | P1 |
| US-EG10 | 회원 | 그룹 목록에서 신청 가능한 세부 행사 이름을 바로 확인할 수 있다 | P2 |

---

## 3. 기능 요구사항 (Functional Requirements)

### 3.1 행사 그룹 도메인

| ID | 요구사항 | 설명 |
|----|---------|------|
| FR-EG001 | 1:N 관계 | 하나의 그룹에 여러 행사가 속할 수 있다. 행사는 최대 하나의 그룹에만 속할 수 있다 |
| FR-EG002 | 독립 행사 허용 | 그룹에 속하지 않는 독립 행사(groupId=NULL)를 허용한다 |
| FR-EG003 | 그룹 visibility | UNPUBLISHED / PUBLISHED 두 가지 상태. 초기값 UNPUBLISHED |
| FR-EG004 | 그룹 soft delete | SoftDeletableEntity 상속. 삭제 시 deleted=true 처리 |
| FR-EG005 | 그룹 이름 중복 허용 | 같은 이름의 그룹을 여러 개 생성할 수 있다 |
| FR-EG006 | 빈 그룹 허용 | 하위 행사가 없는 그룹도 공개 목록에 노출된다 |

### 3.2 행사 그룹 CRUD

| ID | 요구사항 | 설명 |
|----|---------|------|
| FR-EG010 | 그룹 생성 | OPERATOR 이상 권한 필요. name(필수, 최대 100자), description(선택, 최대 500자) |
| FR-EG011 | 그룹 수정 | OPERATOR 이상 권한 필요. 누가 만들었든 수정 가능 |
| FR-EG012 | 그룹 삭제 | OPERATOR 이상 권한 필요. 하위 행사가 있으면 삭제 불가 (400 에러) |
| FR-EG013 | 그룹 목록 조회 (공개) | PUBLISHED + not deleted 그룹만 조회. 하위 행사 이름 포함 |
| FR-EG014 | 그룹 내 행사 조회 (공개) | 그룹 정보 + 하위 행사 목록 반환. 그룹이 PUBLISHED + not deleted일 때만 |
| FR-EG015 | 그룹 목록 조회 (관리자) | 전체 그룹 조회 (UNPUBLISHED 포함, soft deleted 제외). visibility 필터 가능. 하위 행사 이름 포함 |
| FR-EG016 | 그룹 상세 조회 (관리자) | 그룹 상세 정보 조회 (수정 페이지용) |

### 3.3 그룹 공개/비공개

| ID | 요구사항 | 설명 |
|----|---------|------|
| FR-EG020 | 그룹 공개 (publish) | UNPUBLISHED → PUBLISHED. OPERATOR 이상 권한 필요 |
| FR-EG021 | 그룹 비공개 (unpublish) | PUBLISHED → UNPUBLISHED. OPERATOR 이상 권한 필요 |
| FR-EG022 | 이미 공개 상태에서 publish → 400 에러 | |
| FR-EG023 | 이미 비공개 상태에서 unpublish → 400 에러 | |

### 3.4 그룹-행사 연결

| ID | 요구사항 | 설명 |
|----|---------|------|
| FR-EG030 | 행사 생성 시 그룹 지정 | CreateEventRequest에 groupId(nullable) 필드 추가. 존재하지 않는 groupId → 404 |
| FR-EG031 | 행사 수정 시 그룹 변경 | UpdateEventRequest에 groupId(nullable) 필드 추가 |
| FR-EG032 | 그룹에서 행사 제거 | 행사 수정 시 groupId=null로 설정하면 그룹에서 분리 → 독립 행사로 전환 |
| FR-EG033 | 삭제된 그룹에 행사 연결 불가 | soft deleted 그룹의 groupId로 행사 생성/수정 시 404 에러 |
| FR-EG034 | UNPUBLISHED 그룹에 행사 연결 가능 | 비공개 그룹에도 행사를 넣을 수 있다 (관리자 준비 단계) |

### 3.5 조회 정책 (Cascade on Read)

| ID | 요구사항 | 설명 |
|----|---------|------|
| FR-EG040 | 그룹 visibility cascade | 그룹이 UNPUBLISHED이면 공개 API에서 하위 행사도 조회 불가 (행사 자체 visibility는 변경하지 않음) |
| FR-EG041 | 그룹 soft delete cascade | 그룹이 soft deleted이면 공개/관리자 API 모두에서 하위 행사를 그룹 경유로 조회 불가 |
| FR-EG042 | 직접 행사 조회 시 그룹 체크 | GET /events/{eventId}로 그룹 행사 직접 조회 시, 그룹이 UNPUBLISHED/deleted면 공개 API에서 차단 |
| FR-EG043 | 관리자 행사 목록에서는 모든 행사 노출 | GET /admin/events는 그룹 visibility와 무관하게 모든 행사를 보여준다 (groupId 필터 가능) |

### 3.6 기존 행사 API 변경

| ID | 요구사항 | 설명 |
|----|---------|------|
| FR-EG050 | 공개 행사 목록 변경 | GET /events는 독립 행사만 반환 (group_id IS NULL) |
| FR-EG051 | 행사 상세 응답에 그룹 정보 추가 | EventDetailResponse에 groupId, groupName 추가 |
| FR-EG052 | 관리자 행사 목록에 그룹 정보 추가 | AdminEventListResponse에 groupId, groupName 추가 |
| FR-EG053 | 관리자 행사 상세에 그룹 정보 추가 | AdminEventDetailResponse에 groupId, groupName 추가 |
| FR-EG054 | 관리자 행사 목록 groupId 필터 | GET /admin/events에 groupId 쿼리 파라미터 추가 |

---

## 4. 비기능 요구사항 (Non-Functional Requirements)

| ID | 요구사항 | 설명 |
|----|---------|------|
| NFR-EG001 | 데이터 정합성 | 그룹 삭제 시 하위 행사 존재 여부를 반드시 체크 |
| NFR-EG002 | 조회 성능 | 그룹 목록 조회 시 하위 행사 집계를 효율적으로 수행 (N+1 방지) |
| NFR-EG003 | 하위 호환성 | 기존 독립 행사(groupId=NULL)는 기존과 동일하게 동작 |

---

## 5. 데이터 모델

### 5.1 EventGroup 엔티티

| 필드 | 타입 | 설명 | 제약조건 |
|------|-----|------|---------|
| id | Long | 그룹 ID | PK, Auto Increment |
| name | String | 그룹 이름 | NOT NULL, 최대 100자 |
| description | String | 그룹 설명 | NULLABLE, 최대 500자 |
| visibility | EventGroupVisibility | 공개 상태 | NOT NULL, ENUM, 기본값 UNPUBLISHED |
| + SoftDeletableEntity 상속 | | | createdAt, updatedAt, deleted, deletedAt 등 |

### 5.2 Event 엔티티 변경

| 필드 | 타입 | 설명 | 제약조건 |
|------|-----|------|---------|
| groupId | Long | 소속 그룹 ID | NULLABLE, FK → event_groups |

### 5.3 Enum 정의

**EventGroupVisibility (그룹 공개 상태)**
| 값 | 설명 |
|----|------|
| UNPUBLISHED | 비공개 (초기값) |
| PUBLISHED | 공개 |

### 5.4 DB 테이블

**event_groups (신규)**
```sql
CREATE TABLE event_groups (
    event_group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_group_name VARCHAR(100) NOT NULL,
    event_group_description VARCHAR(500),
    event_group_visibility VARCHAR(20) NOT NULL DEFAULT 'UNPUBLISHED',
    event_group_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    event_group_deleted_at TIMESTAMP,
    event_group_deleted_by BIGINT,
    event_group_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_group_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    event_group_created_by BIGINT,
    event_group_updated_by BIGINT
);
```

**events 테이블 변경**
```sql
ALTER TABLE events ADD COLUMN event_group_id BIGINT NULL;
ALTER TABLE events ADD CONSTRAINT fk_events_event_group
    FOREIGN KEY (event_group_id) REFERENCES event_groups(event_group_id);
```

---

## 6. 상태 전이 규칙

### 6.1 EventGroupVisibility 상태 전이

```
UNPUBLISHED → PUBLISHED (publish)
PUBLISHED → UNPUBLISHED (unpublish)
```

- visibility 변경 시 하위 행사의 visibility는 변경하지 않음 (cascade on read)
- 그룹 비공개 시 행사의 registrationStatus 등도 변경하지 않음

### 6.2 그룹 삭제 규칙

```
active (deleted=false) → deleted (하위 행사 0건일 때만)
active (deleted=false) → 삭제 차단 (하위 행사 1건 이상)
```

---

## 7. API 엔드포인트

### 7.1 공개 API — 그룹 CRUD

| Method | Endpoint | 설명 | 권한 | 응답 |
|--------|----------|------|------|------|
| POST | /api/v1/event-groups | 그룹 생성 | OPERATOR+ | 201 + EventGroupCreateResponse |
| PUT | /api/v1/event-groups/{id} | 그룹 수정 | OPERATOR+ | 200 + EventGroupDetailResponse |
| DELETE | /api/v1/event-groups/{id} | 그룹 삭제 | OPERATOR+ | 204 (하위 행사 있으면 400) |

### 7.2 공개 API — 그룹 조회

| Method | Endpoint | 설명 | 권한 | 응답 |
|--------|----------|------|------|------|
| GET | /api/v1/event-groups | 그룹 목록 (PUBLISHED만) | ALL | 200 + EventGroupListResponse[] |
| GET | /api/v1/event-groups/{id}/events | 그룹 정보 + 하위 행사 목록 | ALL | 200 + EventGroupEventsResponse |

### 7.3 관리자 API

| Method | Endpoint | 설명 | 권한 | 응답 |
|--------|----------|------|------|------|
| GET | /api/v1/admin/event-groups | 그룹 목록 (전체, visibility 필터) | OPERATOR+ | 200 + AdminEventGroupListResponse[] |
| GET | /api/v1/admin/event-groups/{id} | 그룹 상세 | OPERATOR+ | 200 + AdminEventGroupDetailResponse |
| POST | /api/v1/admin/event-groups/{id}/publish | 그룹 공개 | OPERATOR+ | 200 + AdminEventGroupDetailResponse |
| POST | /api/v1/admin/event-groups/{id}/unpublish | 그룹 비공개 | OPERATOR+ | 200 + AdminEventGroupDetailResponse |

### 7.4 기존 행사 API 변경

| Method | Endpoint | 변경 내용 |
|--------|----------|----------|
| GET | /api/v1/events | 독립 행사만 반환 (group_id IS NULL) |
| GET | /api/v1/events/{id} | 그룹 행사 직접 조회 허용. 단 그룹 UNPUBLISHED/deleted 시 공개 API 차단 |
| POST | /api/v1/events | CreateEventRequest에 groupId 추가 |
| PUT | /api/v1/events/{id} | UpdateEventRequest에 groupId 추가 |
| GET | /api/v1/admin/events | groupId 쿼리 파라미터 추가 (필터). 모든 행사 반환 |

---

## 8. 스키마 정의

### 8.1 요청 스키마

**CreateEventGroupRequest**
| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| name | String | O | 최대 100자 |
| description | String | X | 최대 500자 |

**UpdateEventGroupRequest**
| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| name | String | O | 최대 100자 |
| description | String | X | 최대 500자 |

### 8.2 응답 스키마

**EventGroupCreateResponse**
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 생성된 그룹 ID |
| name | String | 그룹 이름 |
| createdAt | Instant | 생성 시각 |

**EventGroupListResponse (공개)**
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 그룹 ID |
| name | String | 그룹 이름 |
| description | String | 그룹 설명 |
| totalEventCount | Integer | 하위 행사 총 수 (PUBLISHED + not deleted) |
| registrableEventNames | String[] | 신청 가능한 행사 이름 목록 |

**EventGroupEventsResponse (공개 — 그룹 클릭 시)**
| 필드 | 타입 | 설명 |
|------|------|------|
| groupId | Long | 그룹 ID |
| groupName | String | 그룹 이름 |
| groupDescription | String | 그룹 설명 |
| events | EventListResponse[] | 하위 행사 목록 (기존 EventListResponse 재사용) |

**AdminEventGroupListResponse (관리자)**
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 그룹 ID |
| name | String | 그룹 이름 |
| description | String | 그룹 설명 |
| visibility | String | PUBLISHED / UNPUBLISHED |
| totalEventCount | Integer | 하위 행사 총 수 |
| registrableEventNames | String[] | 신청 가능한 행사 이름 목록 |
| createdAt | Instant | 생성 시각 |

**AdminEventGroupDetailResponse (관리자 — 수정 페이지용)**
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 그룹 ID |
| name | String | 그룹 이름 |
| description | String | 그룹 설명 |
| visibility | String | PUBLISHED / UNPUBLISHED |
| totalEventCount | Integer | 하위 행사 총 수 |
| registrableEventNames | String[] | 신청 가능한 행사 이름 목록 |
| createdAt | Instant | 생성 시각 |
| updatedAt | Instant | 수정 시각 |

### 8.3 기존 스키마 변경

**CreateEventRequest 추가 필드**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| groupId | Long | X | 소속 그룹 ID (null이면 독립 행사) |

**UpdateEventRequest 추가 필드**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| groupId | Long | X | 소속 그룹 ID (null로 설정 시 그룹 해제) |

**EventDetailResponse 추가 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| groupId | Long | 소속 그룹 ID (없으면 null) |
| groupName | String | 소속 그룹 이름 (없으면 null) |

**AdminEventListResponse 추가 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| groupId | Long | 소속 그룹 ID (없으면 null) |
| groupName | String | 소속 그룹 이름 (없으면 null) |

**AdminEventDetailResponse 추가 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| groupId | Long | 소속 그룹 ID (없으면 null) |
| groupName | String | 소속 그룹 이름 (없으면 null) |

---

## 9. 예외 처리

| 예외 | ErrorCode | HTTP 상태 | 발생 조건 |
|------|----------|----------|----------|
| EventGroupNotFoundException | EVENT_GROUP_NOT_FOUND | 404 | 그룹 미존재 또는 soft deleted |
| EventGroupHasEventsException | EVENT_GROUP_HAS_EVENTS | 400 | 하위 행사가 있는 그룹 삭제 시도 |
| InvalidEventGroupStateTransitionException | EVENT_GROUP_INVALID_STATE_TRANSITION | 400 | 유효하지 않은 visibility 전이 (이미 PUBLISHED에서 publish 등) |
| EventGroupAccessDeniedException | EVENT_GROUP_ACCESS_DENIED | 403 | 권한 없음 (OPERATOR 미만) |
| EventGroupNotPublishedException | EVENT_GROUP_NOT_PUBLISHED | 404 | 공개 API에서 UNPUBLISHED 그룹 접근 시 |

---

## 10. 불변조건 (Invariants)

| ID | 불변조건 | 설명 |
|----|---------|------|
| INV-EG01 | 1:N 관계 | 행사는 최대 하나의 그룹에만 속할 수 있다 |
| INV-EG02 | 그룹 삭제 선행 조건 | 하위 행사가 0건일 때만 그룹 삭제 가능 |
| INV-EG03 | cascade on read (visibility) | 그룹 UNPUBLISHED → 공개 API에서 하위 행사 그룹 경유 조회 불가 |
| INV-EG04 | cascade on read (soft delete) | 그룹 soft deleted → 모든 API에서 하위 행사 그룹 경유 조회 불가 |
| INV-EG05 | 직접 접근 시 그룹 체크 | GET /events/{id}로 직접 조회 시에도 그룹 visibility/delete 상태를 체크 |
| INV-EG06 | 행사 데이터 보존 | 그룹 visibility 변경 시 하위 행사의 visibility, registrationStatus 등을 변경하지 않음 |
| INV-EG07 | 독립 행사 격리 | 공개 행사 목록(GET /events)은 group_id IS NULL인 행사만 반환 |
| INV-EG08 | 관리자 전체 조회 | 관리자 행사 목록(GET /admin/events)은 그룹 visibility와 무관하게 모든 행사 반환 |
| INV-EG09 | groupId 유효성 | 행사 생성/수정 시 존재하지 않거나 삭제된 그룹의 groupId → 404 에러 |
| INV-EG10 | name 제약 | 그룹 이름은 필수, 최대 100자 |
| INV-EG11 | description 제약 | 그룹 설명은 선택, 최대 500자 |

---

## 11. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-03-05 | - | 최초 작성 (이슈 #500 기반) |
