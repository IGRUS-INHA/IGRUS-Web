# 행사-이미지 연계 (Event-Image Integration) 검증 기준서

> **Status**: Approved
> **Last Updated**: 2026-03-02
> **Scope**: 행사(Event) 엔티티와 S3 이미지 파일의 연계 -- 포스터 이미지 Object Key 필드, 참조 검증, 이미지 정리 정책, API 응답에 이미지 정보 포함
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)
> **관련 검증 기준서**:
> - [Presigned URL 이미지 업로드/다운로드 검증 기준서](../storage/image-presigned-url-verification-criteria.md) -- S3 기본 기능 (STOR-INV-*)
> - [행사 검증 기준서](./event-verification-criteria.md) -- 행사 CRUD 및 3축 상태 모델 (EVT-INV-*)

## 목적

이 문서는 행사(Event) 도메인과 S3 이미지 스토리지 간의 **연계 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

S3 Presigned URL 기반 업로드/다운로드 자체의 검증은 `STOR-INV-*`에서, 행사 CRUD 및 상태 관리 자체의 검증은 `EVT-INV-*`에서 각각 다루므로, 이 문서는 **두 도메인이 만나는 접점**만 다룬다. 구체적으로:

1. Event 엔티티에 이미지 Object Key 필드 추가 및 제약조건
2. 행사 생성/수정 시 이미지 참조 검증 (FileMetadata 상태 확인)
3. 행사 조회 응답에 이미지 정보 포함
4. EventFileReferenceChecker 구현 (FileReferenceChecker 인터페이스)
5. 행사 삭제/수정 시 이미지 정리 정책
6. DB 마이그레이션 및 OpenAPI 스펙 업데이트

QA Testing 용어 정리 wiki의 10개 영역 중, 이 연계 도메인에 직접 관련된 7개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 이미지 Object Key - FileMetadata 참조 정합성, nullable 정책, 상태 검증 |
| 2 | 상태 모델 | 이미지 연결 가능 조건과 행사 상태(3축)의 교차 제약 |
| 3 | 시스템 경계와 책임 분리 | Event 모듈 ↔ Storage 모듈 간 의존 방향 및 책임 경계 |
| 4 | 입력 도메인 분할과 경계값 | posterImageObjectKey 필드의 유효/무효 동치류 |
| 5 | 권한/보안 정책 | 이미지 연결/해제 권한, 다른 사용자 이미지 사용 가능 여부 |
| 6 | 관측 가능성 | 이미지 연결/해제/변경 로그 |
| 7 | 테스트 전략 | 테스트-검증 항목 매핑, 커버리지 현황 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

### EVT-IMG-INV-01: 포스터 이미지 Object Key Nullable 정책

> `posterImageObjectKey`는 **nullable**이다. 이미지가 없는 행사도 유효하다.

- **사전조건**: 행사 생성/수정 요청
- **사후조건**: `posterImageObjectKey == null`인 행사가 정상적으로 생성/수정/조회 가능
- **위반 시**: NOT NULL 제약으로 이미지 없는 행사 생성 실패
- **관련 코드**: `Event.java` -- `posterImageObjectKey` 필드 **(신규 구현 필요)**
- **검증 방법**: `posterImageObjectKey`를 null로 행사 생성 및 수정 시 성공 확인

### EVT-IMG-INV-02: 이미지 Object Key는 COMPLETED 상태 파일만 참조 가능

> 행사에 연결되는 `posterImageObjectKey`는 `FileMetadata.status == COMPLETED`인 Object Key여야 한다.

- **사전조건**: 행사 생성/수정 요청 시 `posterImageObjectKey`가 non-null로 제공됨
- **사후조건**: 해당 Object Key의 FileMetadata가 존재하고, `status == COMPLETED`이며, `deleted == false`
- **위반 시 예외**: 적절한 비즈니스 예외 (예: `InvalidImageReferenceException` 또는 `400 Bad Request`)
- **관련 코드**: `EventService.createEvent()` / `EventService.updateEvent()` -- 이미지 참조 검증 로직 **(신규 구현 필요)**
- **검증 방법**:
  - COMPLETED 상태의 Object Key로 행사 생성 시 성공 확인
  - REQUESTED / CONFIRMING / FAILED / EXPIRED 상태의 Object Key로 행사 생성 시 거부 확인
  - 존재하지 않는 Object Key로 행사 생성 시 거부 확인
  - Soft Delete된 FileMetadata의 Object Key로 행사 생성 시 거부 확인

### EVT-IMG-INV-03: Object Key `events/` 프리픽스 강제 정책

> 행사에 연결되는 이미지의 Object Key는 `events/` 프리픽스로 시작해야 한다. 행사 서비스에서 프리픽스를 **강제 검증**한다 (DECISION-01 확정: A안 프리픽스 강제).

- **사전조건**: 업로드 URL 생성 시 `purpose = "events"` 지정 → ObjectKeyGenerator가 `events/` 프리픽스 Key 생성
- **사후조건**: Object Key 형식이 `events/{YYYY}/{MM}/{DD}/{UUID}.{ext}`
- **위반 시**: 다른 도메인(posts, profiles)의 이미지를 행사에 부적절하게 연결하는 것을 방지
- **관련 코드**: `ObjectKeyGenerator.generate("events", contentType)` `(현재 구현 일치)`, 행사 서비스 프리픽스 검증 로직 **(신규 구현 필요)**
- **검증 방법**: `events/` 프리픽스 Key 허용, `posts/` 또는 `profiles/` 프리픽스 Key 거부 (400 Bad Request) 확인

### EVT-IMG-INV-04: 참조 무결성 -- 행사가 참조 중인 이미지 삭제 차단

> `posterImageObjectKey`로 참조 중인 파일은 `FileDeleteService`에서 삭제를 거부한다.

- **사전조건**: FileDeleteService가 Object Key로 삭제 요청을 받음
- **사후조건**: EventFileReferenceChecker가 해당 Object Key를 참조하는 행사가 존재하면 `FileReferenceExistsException` 발생 (`409 Conflict`)
- **위반 시**: 행사 포스터가 깨진 이미지(broken image) 표시
- **관련 코드**:
  - `FileReferenceChecker` 인터페이스 `(현재 구현 일치)`
  - `FileDeleteService.checkFileReferences()` `(현재 구현 일치)` -- 모든 FileReferenceChecker 순회
  - `EventFileReferenceChecker` **(신규 구현 필요)** -- `FileReferenceChecker` 구현체
- **검증 방법**:
  - 행사에서 참조 중인 Object Key로 파일 삭제 요청 시 `409 Conflict` 응답 확인
  - 행사에서 참조하지 않는 Object Key로 파일 삭제 요청 시 정상 삭제 확인
  - Soft Delete된 행사가 참조하는 Object Key로 파일 삭제 요청 시 삭제 허용 확인 (Soft Delete된 행사는 참조로 간주하지 않음)

### EVT-IMG-INV-05: 이미지 변경 시 기존 이미지 자동 정리 없음

> 행사 수정으로 `posterImageObjectKey`가 변경되더라도, 이전 이미지 파일은 자동으로 삭제되지 않는다.

- **사전조건**: 행사 수정 시 `posterImageObjectKey`가 A에서 B로 변경됨
- **사후조건**: 이전 이미지(A)의 FileMetadata와 S3 객체는 그대로 유지됨. 운영진이 별도로 파일 삭제 API를 호출해야 정리됨.
- **설계 근거**: 이미지가 다른 곳에서도 참조될 가능성이 있으며, 자동 삭제는 예기치 못한 데이터 손실을 유발할 수 있음. 고아 이미지 정리는 주기적 스케줄러 또는 수동 운영으로 처리.
- **검증 방법**: 행사 이미지를 A에서 B로 변경한 후, A의 FileMetadata가 COMPLETED 상태로 유지되고 S3 객체가 존재함을 확인

### EVT-IMG-INV-06: 행사 삭제(Soft Delete) 시 이미지 자동 삭제 없음

> 행사가 Soft Delete되더라도, 연결된 이미지 파일은 자동으로 삭제되지 않는다.

- **사전조건**: 행사가 Soft Delete됨
- **사후조건**: 연결된 이미지의 FileMetadata와 S3 객체는 그대로 유지됨. Soft Delete된 행사는 참조 무결성 검사에서 제외됨(EVT-IMG-INV-04 참조).
- **설계 근거**: Soft Delete된 행사는 복원 가능성이 있으므로 이미지를 유지. 완전 삭제 시 운영진이 별도로 이미지 정리.
- **검증 방법**: 이미지가 연결된 행사를 삭제한 후, FileMetadata 상태가 COMPLETED로 유지됨을 확인

### EVT-IMG-INV-07: 이미지 해제(null로 변경) 허용

> 행사 수정 시 `posterImageObjectKey`를 null로 설정하여 이미지를 해제할 수 있다.

- **사전조건**: 기존 행사에 `posterImageObjectKey`가 non-null로 설정됨
- **사후조건**: 행사 수정 시 `posterImageObjectKey = null` 전송 → 이미지 해제 완료. 이전 이미지는 EVT-IMG-INV-05에 따라 유지.
- **검증 방법**: 이미지가 연결된 행사의 `posterImageObjectKey`를 null로 수정한 후, 행사 상세 조회 응답에 이미지 정보가 null임을 확인

### EVT-IMG-INV-08: 목록/상세 응답의 posterImageObjectKey 노출 정책

> `posterImageObjectKey`는 목록 응답과 상세 응답 모두에 포함된다. 단, 실제 이미지 다운로드를 위해서는 별도로 다운로드 URL을 요청해야 한다 (SEC-STOR-04에 의해 인증 필수).

- **공개 API 목록 응답 (`EventListResponse`)**: `posterImageObjectKey` 필드 포함 (nullable). ASSOCIATE를 포함한 인증된 사용자 모두 접근 가능.
- **공개 API 상세 응답 (`EventDetailResponse`)**: `posterImageObjectKey` 필드 포함 (nullable). ASSOCIATE는 상세 조회 자체가 403으로 차단됨 (SEC-EVT-01 참조).
- **관리자 API 응답 (`AdminEventListResponse`, `AdminEventDetailResponse`)**: `posterImageObjectKey` 필드 포함 (nullable). OPERATOR+ 접근.
- **ASSOCIATE 접근 비대칭성**: ASSOCIATE는 목록 조회에서 `posterImageObjectKey` 값을 확인할 수 있지만, 상세 조회는 403으로 차단된다. 이는 기존 행사 조회 RBAC 정책(SEC-EVT-01)과 동일한 규칙이며, 이미지 연계로 인한 추가 제약은 없다. Object Key 자체는 보안 민감 정보가 아니므로 (Presigned URL과 달리 서명 정보 미포함) 목록에서 노출해도 무방하다.
- **이미지 미연결 행사 응답 정책**: 이미지 미연결 행사의 경우 `posterImageObjectKey` 필드는 **null 값으로 응답에 포함**된다 (필드 자체가 생략되지 않음). 프론트엔드는 해당 필드가 null인지 여부로 이미지 존재 여부를 판단한다.
- **이미지 실제 접근 경로**: 프론트엔드는 응답에 포함된 `posterImageObjectKey`를 사용하여 Storage 모듈의 다운로드 URL API (`GET /api/storage/download-url?objectKey={key}`)를 호출하고, 반환된 Presigned GET URL로 이미지를 로드한다. 다운로드 URL 요청은 인증이 필수이다 (SEC-STOR-04).
- **검증 방법**:
  - ASSOCIATE가 공개 목록 조회 시 `posterImageObjectKey` 필드가 응답에 포함되는지 확인
  - ASSOCIATE가 공개 상세 조회 시 403 응답 확인 (기존 SEC-EVT-01 동작 유지)
  - MEMBER 이상이 목록/상세 조회 시 `posterImageObjectKey` 필드가 응답에 포함되는지 확인

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. 이미지 연결 가능 조건과 행사 상태 교차 매트릭스

행사의 3축 상태에 따른 이미지 연결/변경 가능 여부를 정의한다.

> **교차 문서 참고**: EVT-INV-07(행사 검증 기준서)은 행사 수정 정책을 정의하지만, 현재 해당 문서의 ONGOING 필드 목록에는 `posterImageObjectKey`가 **아직 포함되어 있지 않다**. `posterImageObjectKey` 필드는 이 문서의 구현 과정에서 신규 추가되는 필드이므로, 아래 매트릭스는 이 문서에서 **독립적으로 정의**한다. 구현 완료 후 EVT-INV-07의 ONGOING 필드 목록에 `posterImageObjectKey: O (정보성 필드)` 항목을 추가하여 양쪽 문서의 일관성을 확보해야 한다.

| eventStatus | 이미지 연결/변경 가능 | 근거 |
|:---:|:---:|------|
| **UPCOMING** | **O** | 전체 필드 수정 가능 (EVT-INV-07 동일) |
| **ONGOING** | **O** | `posterImageObjectKey`는 정보성 필드이므로 ONGOING에서도 수정 가능 (EVT-INV-07 ONGOING 허용 필드 패턴과 동일) |
| **COMPLETED** | **X** | COMPLETED는 종단 상태이며 모든 수정 불가 (EVT-INV-06, EVT-INV-07 COMPLETED 규칙) |
| **CANCELED** | **O** | 전체 필드 수정 가능 (EVT-INV-07 동일) |

> **참고**: visibility(축 1)와 registrationStatus(축 2)는 이미지 연결 가능 여부에 영향을 미치지 않는다. eventStatus(축 3)만이 수정 가능 여부를 결정한다.
>
> **[ACTION REQUIRED]**: 이 문서의 구현 완료 시 `event-verification-criteria.md` EVT-INV-07의 "ONGOING 상태 필드별 수정 가능 여부" 표에 `posterImageObjectKey | O | 정보성 필드` 행을 추가할 것.

### 2-2. 이미지-행사 연결 상태 전이

```
                 행사 생성 (posterImageObjectKey=null 또는 COMPLETED Key)
                                    │
                                    ▼
                         ┌──────────────────┐
                         │   이미지 미연결   │
                         │ (objectKey=null)  │
                         └────────┬─────────┘
                                  │
                    수정 (objectKey=유효한 COMPLETED Key)
                                  │
                                  ▼
                         ┌──────────────────┐
                         │   이미지 연결됨   │
                         │ (objectKey=값)    │
                         └────────┬─────────┘
                         │        │         │
          수정(null)     │  수정(다른 Key)  │  행사 삭제(Soft Delete)
              │          │        │         │
              ▼          │        ▼         │
         이미지 미연결    │   이미지 변경    │  행사 삭제됨
         (기존 이미지     │   (기존 이미지   │  (이미지 유지,
          유지)          │    유지)         │   참조 해제)
                         │                  │
                         └──────────────────┘
```

> 모든 전이에서 기존 이미지 파일은 자동 삭제되지 않는다 (EVT-IMG-INV-05, EVT-IMG-INV-06).

---

## 3. 시스템 경계와 책임 분리 (System Boundary & SoC)

### 3-1. Event 모듈 → Storage 모듈 의존 방향

```
┌───────────────────────────────────────────────────────┐
│                    Event 모듈                          │
│                                                       │
│  Event 엔티티                                         │
│  ├── posterImageObjectKey: String (nullable)           │
│  │   (Storage 모듈의 Object Key를 문자열로 보관)       │
│  │                                                    │
│  EventService                                         │
│  ├── createEvent / updateEvent                        │
│  │   └── FileMetadata 존재+COMPLETED 검증  ──────────┐│
│  │                                                   ││
│  EventFileReferenceChecker (신규)                     ││
│  └── isReferenced(objectKey) ──────────────────────┐ ││
│                                                    │ ││
└────────────────────────────────────────────────────┼─┼┘
                                                     │ │
┌────────────────────────────────────────────────────┼─┼┐
│                    Storage 모듈                     │ ││
│                                                    │ ││
│  FileMetadataRepository                            │ ││
│  ├── findByObjectKeyAndDeletedFalse() ◄────────────┘ ││
│  │   (EventService가 참조 검증 시 호출)               ││
│  │                                                   ││
│  FileDeleteService                                   ││
│  ├── checkFileReferences()                           ││
│  │   └── FileReferenceChecker.isReferenced() ◄───────┘│
│  │       (EventFileReferenceChecker가 Bean으로 등록)   │
│  │                                                    │
│  FileReferenceChecker (인터페이스)                     │
│  └── isReferenced(objectKey): boolean                 │
│                                                       │
└───────────────────────────────────────────────────────┘
```

### 3-2. 각 구성요소 책임

| 구성요소 | 책임 | 하지 않는 것 |
|---------|------|-------------|
| **Event 엔티티** | `posterImageObjectKey`를 문자열 필드로 보관 | FileMetadata 엔티티에 대한 JPA 관계 없음 (약한 참조) |
| **EventService** | 이미지 Object Key 검증(COMPLETED 상태 확인), 행사 생성/수정 시 이미지 연결 | 이미지 업로드/다운로드 URL 생성, S3 직접 접근 |
| **EventFileReferenceChecker** | 해당 Object Key를 참조하는 활성(deleted=false) 행사가 있는지 확인 | 파일 삭제 결정, S3 접근 |
| **FileDeleteService** | 참조 무결성 검사 후 파일 삭제 | 어떤 엔티티가 참조하는지 알 필요 없음 (인터페이스로 추상화) |
| **Frontend** | 이미지 업로드 후 COMPLETED Object Key를 행사 생성/수정 요청에 포함 | 이미지 참조 검증 (서버가 수행) |

### 3-3. 약한 참조 설계 (Weak Reference)

> Event와 FileMetadata 간에 JPA `@ManyToOne` 또는 FK 제약을 사용하지 않는다.

- **이유**: Soft Delete 호환성. Event와 FileMetadata는 각각 독립적으로 Soft Delete되며, FK가 있으면 삭제 순서 충돌 및 orphan 문제가 발생한다.
- **연결 방식**: Event 엔티티의 `posterImageObjectKey` 필드에 FileMetadata의 `objectKey` 값을 문자열로 저장한다.
- **참조 무결성**: 애플리케이션 레벨에서 `FileReferenceChecker` 인터페이스를 통해 보장한다.
- **선행 패턴**: Event.surveyId (Long, nullable, FK 없음)와 동일한 약한 참조 패턴.

---

## 4. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 4-1. posterImageObjectKey 입력값 (행사 생성/수정)

| 분류 | 값 | 예상 결과 | 검증 |
|------|---|----------|------|
| **유효: null** | `null` | 이미지 미연결 행사 생성/수정 성공 | EVT-IMG-INV-01 |
| **유효: COMPLETED Key** | `events/2026/03/01/{UUID}.png` (status=COMPLETED) | 이미지 연결 행사 생성/수정 성공 | EVT-IMG-INV-02 |
| **유효: 빈 문자열** | `""` | null로 변환 → 이미지 해제 (DECISION-03 확정: A안) | EVT-IMG-INV-07, DECISION-03 |
| **유효: 공백만** | `"   "` | null로 변환 → 이미지 해제 (DECISION-03 확정: A안) | EVT-IMG-INV-07, DECISION-03 |
| **무효: 존재하지 않는 Key** | `events/2026/03/01/nonexistent.png` | 거부 (404 Not Found) -- FileMetadata 미발견 | EVT-IMG-INV-02 |
| **무효: Soft Delete된 Key** | `deleted=true`인 FileMetadata의 Object Key | 거부 (404 Not Found) -- @SQLRestriction에 의해 미조회이므로 존재하지 않는 것과 동일 | EVT-IMG-INV-02 |
| **무효: REQUESTED 상태 Key** | 아직 업로드 완료 안 된 Object Key | 거부 (400 Bad Request) -- 존재하지만 상태가 부적절 | EVT-IMG-INV-02 |
| **무효: CONFIRMING 상태 Key** | 검증 진행 중인 Object Key | 거부 (400 Bad Request) -- 존재하지만 상태가 부적절 | EVT-IMG-INV-02 |
| **무효: FAILED 상태 Key** | 업로드 실패한 Object Key | 거부 (400 Bad Request) -- 존재하지만 상태가 부적절 | EVT-IMG-INV-02 |
| **무효: EXPIRED 상태 Key** | 만료된 Object Key | 거부 (400 Bad Request) -- 존재하지만 상태가 부적절 | EVT-IMG-INV-02 |
| **무효: 다른 도메인 Key** | `posts/2026/03/01/{UUID}.png` (status=COMPLETED) | 거부 (400 Bad Request) -- `events/` 프리픽스 강제 (DECISION-01 확정: A안) | EVT-IMG-INV-03, DECISION-01 |

### 4-2. Object Key 길이 경계값

| 테스트 값 | 분류 | 예상 결과 |
|----------|------|----------|
| `events/2026/03/01/{UUID}.png` (약 60자) | 일반 유효 | 허용 |
| 500자 Object Key | DB 컬럼 최대 길이 경계 | 허용 (FileMetadata.objectKey max=500) |
| 501자 Object Key | DB 컬럼 초과 | 거부 (사실상 발생 불가: ObjectKeyGenerator가 생성하는 Key는 ~60자) |

### 4-3. 이미지 연결과 행사 상태 조합 (Pairwise Testing)

| eventStatus | posterImageObjectKey 입력 | 예상 결과 |
|:---:|:---:|:---:|
| UPCOMING | null → COMPLETED Key | 성공 (이미지 연결) |
| UPCOMING | COMPLETED Key → null | 성공 (이미지 해제) |
| UPCOMING | COMPLETED Key A → COMPLETED Key B | 성공 (이미지 변경) |
| ONGOING | null → COMPLETED Key | 성공 (이미지 연결) |
| ONGOING | COMPLETED Key → null | 성공 (이미지 해제) |
| ONGOING | COMPLETED Key A → COMPLETED Key B | 성공 (이미지 변경) |
| COMPLETED | null → COMPLETED Key | 거부 (EventNotEditableException) |
| COMPLETED | COMPLETED Key → null | 거부 (EventNotEditableException) |
| CANCELED | null → COMPLETED Key | 성공 (이미지 연결) |

---

## 5. 권한/보안 정책 (RBAC & Authorization)

### 5-1. 이미지 연결/해제 권한

이미지 연결/해제는 행사 생성/수정의 일부이므로, 행사 CRUD 권한과 동일하다.

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 이미지 연결 (행사 생성) | 401 | 403 | 403 | **O** | **O** |
| 이미지 연결/해제 (행사 수정) | 401 | 403 | 403 | **O** | **O** |
| 이미지 포함 행사 **목록** 조회 (공개) | 401 | **O** (posterImageObjectKey 포함) | **O** | **O** | **O** |
| 이미지 포함 행사 **상세** 조회 (공개) | 401 | **403** (SEC-EVT-01, 준회원 상세 조회 차단) | **O** | **O** | **O** |
| 이미지 포함 행사 조회 (관리자) | 401 | 403 | 403 | **O** | **O** |
| 이미지 다운로드 URL 요청 (Storage API) | 401 (SEC-STOR-04) | **O** | **O** | **O** | **O** |

### 5-2. 다른 사용자 이미지 사용 정책

| ID | 검증 항목 | 예상 결과 | 설명 |
|----|----------|----------|------|
| SEC-EVT-IMG-01 | 운영진 A가 업로드한 이미지를 운영진 B가 행사에 연결 시도 | **허용** | 이미지 업로드자와 행사 작성자가 달라도, Object Key가 COMPLETED 상태이면 연결 가능. 운영진 간 이미지 공유는 동아리 운영 특성상 허용. |
| SEC-EVT-IMG-02 | 일반 회원이 업로드한 이미지를 운영진이 행사에 연결 시도 | **허용** | 이미지 COMPLETED 상태만 확인. 업로드자 역할/신분은 검증하지 않음. |
| SEC-EVT-IMG-03 | 미인증 사용자가 이미지가 연결된 행사의 이미지 다운로드 시도 | **거부 (401)** | 이미지 다운로드 URL 요청은 Storage 모듈의 RBAC 정책을 따름 (SEC-STOR-04: 미인증 사용자의 다운로드 URL 요청 → 401 Unauthorized). |

> **DECISION-02 확정 (A안: 소유권 무관)**: 이미지 업로드자 소유권 검증을 행사 연결 시 수행하지 않는다. COMPLETED 상태만 확인하며, 업로드자가 누구인지와 무관하게 연결을 허용한다. 동아리 운영 특성상 운영진 간 이미지 공유가 필요하기 때문이다.

---

## 6. 관측 가능성 (Observability & Audit)

### 6-1. 서비스별 로그 메시지

| 서비스/시점 | 로그 메시지 | 로그 레벨 |
|-----------|-----------|----------|
| 행사 생성 (이미지 포함) | `행사 생성: eventId={}, posterImageObjectKey={}` | INFO |
| 행사 수정 (이미지 변경) | `행사 수정 - eventId: {}, posterImageObjectKey 변경: {} -> {}` | INFO |
| 행사 수정 (이미지 해제) | `행사 수정 - eventId: {}, posterImageObjectKey 해제: {} -> null` | INFO |
| 이미지 참조 검증 성공 | (별도 로그 불필요, 행사 생성/수정 로그에 포함) | - |
| 이미지 참조 검증 실패 | `이미지 참조 검증 실패: objectKey={}, status={}` | WARN |
| 참조 무결성 검사 차단 | `파일 삭제 차단 (행사 참조): objectKey={}, eventId={}` | WARN |

### 6-2. 로그 주의사항

- Object Key는 로그에 기록 가능 (보안 위험 없음, Presigned URL과 달리 서명 정보 미포함)
- 이미지 변경 시 이전/이후 Object Key를 모두 로깅하여 변경 추적 가능
- 파일 삭제 차단 시 어떤 행사가 참조하는지 eventId를 함께 로깅

---

## 7. 테스트 전략 (Test Strategy)

### 7-1. 테스트 레벨별 전략

| 테스트 레벨 | 범위 | 의존성 처리 |
|-----------|------|-----------|
| **단위 테스트** | Event 엔티티의 posterImageObjectKey 설정/조회, EventFileReferenceChecker 로직 | FileMetadataRepository Mock |
| **서비스 통합 테스트** | EventService의 이미지 참조 검증, 행사 CRUD와 이미지 연계 | 실제 DB (H2), FileMetadataRepository 실제 사용 |
| **컨트롤러 통합 테스트** | API 요청/응답에 이미지 필드 포함 여부, 에러 응답 | MockMvc + 실제 서비스 |

### 7-2. 검증 항목별 테스트 매핑

| 불변조건 | 테스트 범위 | 상태 |
|---------|-----------|------|
| EVT-IMG-INV-01 (Nullable 정책) | 통합: null objectKey로 행사 생성/수정 성공 확인 | 미구현 |
| EVT-IMG-INV-02 (COMPLETED 상태 검증) | 통합: 각 FileUploadStatus별 Object Key로 행사 생성 시 성공/거부 확인 | 미구현 |
| EVT-IMG-INV-03 (프리픽스 강제) | 통합: `events/` 프리픽스 Key 허용, 다른 프리픽스 Key 거부 (400) 확인 (DECISION-01 확정: A안) | 미구현 |
| EVT-IMG-INV-04 (참조 무결성) | 통합: FileDeleteService에서 참조 중인 Key 삭제 차단 확인 | 미구현 |
| EVT-IMG-INV-05 (이미지 변경 시 유지) | 통합: 이미지 변경 후 기존 이미지 FileMetadata 상태 확인 | 미구현 |
| EVT-IMG-INV-06 (행사 삭제 시 유지) | 통합: 행사 Soft Delete 후 이미지 FileMetadata 상태 확인 | 미구현 |
| EVT-IMG-INV-07 (이미지 해제) | 통합: objectKey를 null로 수정 후 조회 응답 확인 | 미구현 |
| EVT-IMG-INV-08 (목록/상세 노출 정책) | 통합: ASSOCIATE 목록 조회 시 posterImageObjectKey 포함 확인, ASSOCIATE 상세 조회 시 403 확인, MEMBER+ 목록/상세 모두 포함 확인 | 미구현 |
| SEC-EVT-IMG-01 (다른 운영진 이미지) | 통합: 사용자 A 업로드 이미지를 사용자 B가 행사에 연결 성공 확인 | 미구현 |
| SEC-EVT-IMG-02 (일반 회원 이미지) | 통합: MEMBER 업로드 이미지를 OPERATOR가 행사에 연결 성공 확인 | 미구현 |
| SEC-EVT-IMG-03 (비인증 다운로드 차단) | 통합: 이미지 다운로드 URL 요청 시 401 확인 | SEC-STOR-04에서 커버 |

### 7-3. 부정 시나리오 (Negative Scenarios)

| # | 시나리오 | 예상 결과 |
|---|---------|----------|
| N-01 | 업로드 미완료(REQUESTED) 이미지를 행사에 연결 | 거부 (이미지 참조 검증 실패) |
| N-02 | 업로드 실패(FAILED) 이미지를 행사에 연결 | 거부 |
| N-03 | 만료(EXPIRED) 이미지를 행사에 연결 | 거부 |
| N-04 | Soft Delete된 이미지를 행사에 연결 | 거부 (FileMetadata 조회 시 @SQLRestriction에 의해 미조회) |
| N-05 | COMPLETED 행사의 이미지를 변경 | 거부 (EventNotEditableException) |
| N-06 | 행사가 참조 중인 이미지를 삭제 | 거부 (409 Conflict) |
| N-07 | 존재하지 않는 Object Key를 행사에 연결 | 거부 (FileMetadata 미발견) |
| N-08 | 빈 문자열 Object Key를 행사에 연결 | null로 변환 → 이미지 해제 (DECISION-03 확정: A안) |

---

## 8. DB 마이그레이션 및 API 스펙 변경 (신규 구현 필요)

### 8-1. DB 마이그레이션

> Flyway 마이그레이션으로 `events` 테이블에 `event_poster_image_object_key` 컬럼을 추가한다.

```sql
-- V{N}__add_poster_image_object_key_to_events.sql
ALTER TABLE events
    ADD COLUMN event_poster_image_object_key VARCHAR(500) NULL;
```

| 항목 | 값 |
|------|---|
| 컬럼명 | `event_poster_image_object_key` |
| 타입 | `VARCHAR(500)` (FileMetadata.objectKey와 동일) |
| Nullable | `YES` (이미지 없는 행사 허용) |
| 기본값 | `NULL` (기존 행사는 이미지 미연결) |
| 인덱스 | 단독 인덱스 불필요 (EventFileReferenceChecker 쿼리 시 `WHERE event_poster_image_object_key = ?`는 빈도 낮음) |
| FK | 없음 (약한 참조, 3-3 참조) |

### 8-2. OpenAPI 스펙 변경

> **구현 상태**: 미반영 -- 이 문서의 구현이 완료되면 `openapi/schemas/events.yaml`에 아래 명시된 스키마별 `posterImageObjectKey` 필드 추가가 필요하다. 현재 `openapi/schemas/events.yaml`에는 해당 필드가 존재하지 않는다.

다음 스키마에 `posterImageObjectKey` 필드를 추가해야 한다:

| 스키마 | 필드 추가 | nullable | required |
|--------|---------|----------|----------|
| `CreateEventRequest` | `posterImageObjectKey: string` | true | false |
| `UpdateEventRequest` | `posterImageObjectKey: string` | true | false |
| `EventDetailResponse` | `posterImageObjectKey: string` | true | - |
| `EventListResponse` | `posterImageObjectKey: string` | true | - |
| `AdminEventDetailResponse` | `posterImageObjectKey: string` | true | - |
| `AdminEventListResponse` | `posterImageObjectKey: string` | true | - |
| `EventCreateResponse` | `posterImageObjectKey: string` | true | - |

### 8-3. Event 엔티티 변경

```java
// Event.java에 추가할 필드
/** 행사 포스터 이미지 Object Key (nullable, FileMetadata.objectKey 약한 참조) */
@Column(name = "event_poster_image_object_key", length = 500)
private String posterImageObjectKey;
```

- `Event.create()` 메서드에 `posterImageObjectKey` 파라미터 추가
- `Event.update()` 메서드에 `posterImageObjectKey` 파라미터 추가
- `CreateEventRequest`, `UpdateEventRequest` record에 필드 추가
- `EventDetailResponse.from()`, `EventListResponse.from()` 매핑에 필드 추가

### 8-4. EventFileReferenceChecker 구현

```java
// 신규 구현 필요
@Component
@RequiredArgsConstructor
public class EventFileReferenceChecker implements FileReferenceChecker {

    private final EventRepository eventRepository;

    @Override
    public boolean isReferenced(String objectKey) {
        // Soft Delete되지 않은 행사 중 해당 Object Key를 참조하는 것이 있는지 확인
        return eventRepository.existsByPosterImageObjectKeyAndDeletedFalse(objectKey);
    }
}
```

- `EventRepository`에 `existsByPosterImageObjectKeyAndDeletedFalse(String objectKey)` 쿼리 메서드 추가 필요

---

## 설계 결정 사항 (DECISION) -- 전체 확정

| ID | 항목 | 선택지 | 결정 |
|----|------|-------|------|
| DECISION-01 | Object Key 프리픽스 검증 (EVT-IMG-INV-03) | A) 서비스에서 `events/` 프리픽스 강제 (`objectKey.startsWith("events/")` 검증 추가) B) 프리픽스 무관하게 COMPLETED Key면 허용 (프리픽스 검증 없음) | **확정: A** -- 프리픽스 강제. 도메인 간 이미지 혼용 방지. **영향 범위**: EVT-IMG-INV-03, 섹션 4-1 "다른 도메인 Key" 행, 섹션 7-2 EVT-IMG-INV-03 테스트. |
| DECISION-02 | 이미지 업로드자 소유권 검증 | A) 소유권 무관 (COMPLETED만 확인) B) 업로드자 == 행사 작성자 또는 OPERATOR+ | **확정: A** -- 소유권 무관. 동아리 운영 특성상 운영진 간 이미지 공유 허용. |
| DECISION-03 | 빈 문자열/공백 문자열 처리 | A) `""` 및 `"   "`을 null로 변환 (이미지 해제로 처리) B) Bean Validation(`@NotBlank` 등)으로 거부 (400) | **확정: A** -- null로 변환. 프론트엔드 구현 편의. **영향 범위**: 섹션 4-1 빈 문자열/공백 행의 예상 결과. |
| DECISION-04 | 복수 이미지 지원 | A) 현재는 단일 포스터 이미지만 B) 추후 복수 이미지(갤러리) 확장 대비 | **확정: A** -- 단일 이미지 우선 구현. 필요 시 별도 테이블(event_images)로 확장. |
| DECISION-05 | 이미지 접근 방식 | A) Event API 응답에 Object Key만 반환, 프론트엔드가 Storage 다운로드 URL API로 별도 요청 B) Event API 응답에 Presigned Download URL을 직접 포함 | **확정: A** -- Object Key만 반환. Event 모듈이 Storage 모듈의 URL 생성에 의존하지 않음 (SoC), URL 만료 관리가 Storage 모듈에 캡슐화됨. EVT-IMG-INV-08에서 상세 흐름 정의. |

---

## 관련 문서

- [Presigned URL 이미지 업로드/다운로드 검증 기준서](../storage/image-presigned-url-verification-criteria.md) -- S3 기본 기능 (STOR-INV-*)
- [행사 검증 기준서](./event-verification-criteria.md) -- 행사 CRUD 및 3축 상태 모델 (EVT-INV-*)
- [설문-행사 신청 연계 검증 기준서](./survey-event-registration-verification-criteria.md) -- 설문-행사 약한 참조 패턴 선례
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)
