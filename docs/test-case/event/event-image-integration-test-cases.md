# 행사-이미지 연계 (Event-Image Integration) 테스트 케이스

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-03-03 |
| 검증 기준 문서 | `docs/criteria/event/event-image-integration-verification-criteria.md` |
| 대상 기능 | 행사 포스터 이미지 Object Key 필드, 이미지 참조 검증(COMPLETED 상태/프리픽스), 참조 무결성(EventFileReferenceChecker), 이미지 정리 정책, API 응답 노출 정책 |
| 테스트 케이스 수 | 총 55개 |

## 카테고리 요약

| # | 카테고리 | 테스트 케이스 수 | 커버리지 대상 |
|---|---------|:---:|-------------|
| 1 | 도메인 규칙과 불변조건 | 22 | EVT-IMG-INV-01 ~ EVT-IMG-INV-08 |
| 2 | 상태 교차 매트릭스 | 9 | 2-1 eventStatus별 이미지 연결/변경 가능 여부 (Pairwise) |
| 3 | 입력 경계값 | 8 | 4-1 posterImageObjectKey 입력값, 4-2 Object Key 길이 |
| 4 | 권한/보안 정책 | 7 | SEC-EVT-IMG-01 ~ SEC-EVT-IMG-03, RBAC 매트릭스 |
| 5 | 부정 시나리오 | 6 | N-01 ~ N-08 (중복 제외 핵심) |
| 6 | 관측 가능성 | 3 | 로그 메시지 |

---

## 1. 도메인 규칙과 불변조건

### EVT-IMG-INV-01: 포스터 이미지 Object Key Nullable 정책

#### TC-001: posterImageObjectKey를 null로 행사 생성 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | OPERATOR 이상 사용자 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출 시 `posterImageObjectKey`를 null로 전달<br>2. 생성된 Event 객체 확인 |
| **입력 데이터** | `posterImageObjectKey: null`, 기타 필수 필드(title, description, eventStatus 등) 유효값 |
| **기대 결과** | 행사 생성 성공, `event.getPosterImageObjectKey() == null` |
| **비고** | EVT-IMG-INV-01. 이미지 없는 행사는 유효한 기본 동작 |

#### TC-002: posterImageObjectKey를 null로 행사 수정 성공 (이미지 해제)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `posterImageObjectKey = "events/2026/03/01/{UUID}.png"`인 기존 행사 존재 (UPCOMING 상태) |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출 시 `posterImageObjectKey`를 null로 전달<br>2. 수정된 Event 확인 |
| **입력 데이터** | `posterImageObjectKey: null` |
| **기대 결과** | 행사 수정 성공, `event.getPosterImageObjectKey() == null` |
| **비고** | EVT-IMG-INV-01, EVT-IMG-INV-07 |

### EVT-IMG-INV-02: 이미지 Object Key는 COMPLETED 상태 파일만 참조 가능

#### TC-003: COMPLETED 상태의 Object Key로 행사 생성 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="events/2026/03/01/{UUID}.png", status=COMPLETED, deleted=false)` 존재 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, 해당 Object Key 전달<br>2. 행사 생성 결과 확인 |
| **입력 데이터** | `posterImageObjectKey: "events/2026/03/01/{UUID}.png"` (COMPLETED) |
| **기대 결과** | 행사 생성 성공, `event.getPosterImageObjectKey()`에 해당 Key 저장 |
| **비고** | EVT-IMG-INV-02 핵심 정상 경로 |

#### TC-004: REQUESTED 상태의 Object Key로 행사 생성 시 거부 (400)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="events/2026/03/01/{UUID}.png", status=REQUESTED, deleted=false)` 존재 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, 해당 Object Key 전달 |
| **입력 데이터** | `posterImageObjectKey: "events/2026/03/01/{UUID}.png"` (REQUESTED) |
| **기대 결과** | 비즈니스 예외 발생 (400 Bad Request), 행사 생성되지 않음 |
| **비고** | EVT-IMG-INV-02, N-01 |

#### TC-005: CONFIRMING 상태의 Object Key로 행사 생성 시 거부 (400)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="events/2026/03/01/{UUID}.png", status=CONFIRMING, deleted=false)` 존재 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, 해당 Object Key 전달 |
| **입력 데이터** | `posterImageObjectKey` (CONFIRMING 상태) |
| **기대 결과** | 비즈니스 예외 발생 (400 Bad Request) |
| **비고** | EVT-IMG-INV-02 |

#### TC-006: FAILED 상태의 Object Key로 행사 생성 시 거부 (400)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="events/2026/03/01/{UUID}.png", status=FAILED, deleted=false)` 존재 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, 해당 Object Key 전달 |
| **입력 데이터** | `posterImageObjectKey` (FAILED 상태) |
| **기대 결과** | 비즈니스 예외 발생 (400 Bad Request) |
| **비고** | EVT-IMG-INV-02, N-02 |

#### TC-007: EXPIRED 상태의 Object Key로 행사 생성 시 거부 (400)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="events/2026/03/01/{UUID}.png", status=EXPIRED, deleted=false)` 존재 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, 해당 Object Key 전달 |
| **입력 데이터** | `posterImageObjectKey` (EXPIRED 상태) |
| **기대 결과** | 비즈니스 예외 발생 (400 Bad Request) |
| **비고** | EVT-IMG-INV-02, N-03 |

#### TC-008: 존재하지 않는 Object Key로 행사 생성 시 거부 (404)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 해당 Object Key에 대응하는 FileMetadata가 DB에 존재하지 않음 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, `"events/2026/03/01/nonexistent.png"` 전달 |
| **입력 데이터** | `posterImageObjectKey: "events/2026/03/01/nonexistent.png"` |
| **기대 결과** | FileMetadata 미발견 예외 (404 Not Found) |
| **비고** | EVT-IMG-INV-02, N-07 |

#### TC-009: Soft Delete된 FileMetadata의 Object Key로 행사 생성 시 거부 (404)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="events/...", status=COMPLETED, deleted=true)` -- @SQLRestriction에 의해 조회 불가 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, 해당 Object Key 전달 |
| **입력 데이터** | Soft Delete된 FileMetadata의 `posterImageObjectKey` |
| **기대 결과** | FileMetadata 미발견 예외 (404 Not Found) -- @SQLRestriction 동작 확인 |
| **비고** | EVT-IMG-INV-02, N-04. Soft Delete가 조회를 차단하여 존재하지 않는 Key와 동일하게 처리됨 |

### EVT-IMG-INV-03: Object Key `events/` 프리픽스 강제 정책

#### TC-010: `events/` 프리픽스 Object Key로 행사 생성 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="events/2026/03/01/{UUID}.png", status=COMPLETED)` 존재 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, `events/` 프리픽스 Key 전달 |
| **입력 데이터** | `posterImageObjectKey: "events/2026/03/01/{UUID}.png"` |
| **기대 결과** | 행사 생성 성공 |
| **비고** | EVT-IMG-INV-03, DECISION-01 확정(A안) |

#### TC-011: `posts/` 프리픽스 Object Key로 행사 생성 시 거부 (400)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="posts/2026/03/01/{UUID}.png", status=COMPLETED)` 존재 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, `posts/` 프리픽스 Key 전달 |
| **입력 데이터** | `posterImageObjectKey: "posts/2026/03/01/{UUID}.png"` |
| **기대 결과** | 400 Bad Request -- 도메인 간 이미지 혼용 방지 |
| **비고** | EVT-IMG-INV-03, DECISION-01. 프리픽스 검증은 FileMetadata 상태 검증 **이전에** 수행되어야 함 |

#### TC-012: `profiles/` 프리픽스 Object Key로 행사 생성 시 거부 (400)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="profiles/2026/03/01/{UUID}.png", status=COMPLETED)` 존재 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, `profiles/` 프리픽스 Key 전달 |
| **입력 데이터** | `posterImageObjectKey: "profiles/2026/03/01/{UUID}.png"` |
| **기대 결과** | 400 Bad Request |
| **비고** | EVT-IMG-INV-03. 다른 도메인 프리픽스 추가 검증 |

### EVT-IMG-INV-04: 참조 무결성 -- 행사가 참조 중인 이미지 삭제 차단

#### TC-013: 행사에서 참조 중인 Object Key로 파일 삭제 시 409 Conflict

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `Event(posterImageObjectKey="events/.../{UUID}.png", deleted=false)` 존재, 해당 FileMetadata 존재 |
| **테스트 절차** | 1. `FileDeleteService.deleteFile()` 호출, 행사가 참조 중인 Object Key 전달<br>2. `EventFileReferenceChecker.isReferenced()` 동작 확인 |
| **입력 데이터** | 행사에서 참조 중인 `objectKey` |
| **기대 결과** | `FileReferenceExistsException` 발생 (409 Conflict), 파일 삭제되지 않음 |
| **비고** | EVT-IMG-INV-04, N-06. EventFileReferenceChecker가 FileReferenceChecker 인터페이스를 구현하여 Bean으로 등록됨 |

#### TC-014: 행사에서 참조하지 않는 Object Key로 파일 삭제 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 어떤 행사에서도 참조하지 않는 `FileMetadata(objectKey="events/.../{UUID}.png")` 존재 |
| **테스트 절차** | 1. `FileDeleteService.deleteFile()` 호출, 참조되지 않는 Object Key 전달 |
| **입력 데이터** | 어떤 행사에서도 참조하지 않는 `objectKey` |
| **기대 결과** | 파일 삭제(Soft Delete) 성공 |
| **비고** | EVT-IMG-INV-04 정상 경로 |

#### TC-015: Soft Delete된 행사가 참조하는 Object Key로 파일 삭제 허용

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `Event(posterImageObjectKey="events/.../{UUID}.png", deleted=true)` -- Soft Delete된 행사. 활성 행사는 해당 Key를 참조하지 않음 |
| **테스트 절차** | 1. `FileDeleteService.deleteFile()` 호출<br>2. `EventFileReferenceChecker.isReferenced()` 결과 확인 |
| **입력 데이터** | Soft Delete된 행사만 참조하는 `objectKey` |
| **기대 결과** | `isReferenced() == false`, 파일 삭제 성공 |
| **비고** | EVT-IMG-INV-04. Soft Delete된 행사는 참조 무결성 검사에서 제외됨 (existsByPosterImageObjectKeyAndDeletedFalse 사용) |

### EVT-IMG-INV-05: 이미지 변경 시 기존 이미지 자동 정리 없음

#### TC-016: 행사 이미지를 A에서 B로 변경 후 A의 FileMetadata 유지 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 행사에 이미지 A(`events/.../a.png`, COMPLETED) 연결됨, 이미지 B(`events/.../b.png`, COMPLETED) 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey`를 B로 변경<br>2. 행사의 `posterImageObjectKey`가 B로 변경됨 확인<br>3. 이미지 A의 FileMetadata 상태가 COMPLETED로 유지되는지 확인 |
| **입력 데이터** | `posterImageObjectKey: "events/.../b.png"` |
| **기대 결과** | 행사의 Key가 B로 변경, 이미지 A의 FileMetadata `status == COMPLETED` 유지, S3 객체 삭제 안 됨 |
| **비고** | EVT-IMG-INV-05. 자동 정리 없음 -- 고아 이미지는 별도 운영으로 처리 |

### EVT-IMG-INV-06: 행사 삭제(Soft Delete) 시 이미지 자동 삭제 없음

#### TC-017: 이미지 연결된 행사를 Soft Delete 후 이미지 FileMetadata 유지 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 행사에 이미지 `events/.../poster.png` (COMPLETED) 연결됨 |
| **테스트 절차** | 1. `EventService.deleteEvent()` 호출 (Soft Delete)<br>2. 행사가 Soft Delete됨 확인<br>3. 이미지의 FileMetadata 상태 확인 |
| **입력 데이터** | 이미지 연결된 행사의 `eventId` |
| **기대 결과** | 행사 `deleted == true`, 이미지 FileMetadata `status == COMPLETED`, `deleted == false` 유지 |
| **비고** | EVT-IMG-INV-06 |

### EVT-IMG-INV-07: 이미지 해제(null로 변경) 허용

#### TC-018: 이미지가 연결된 행사의 posterImageObjectKey를 null로 수정하여 이미지 해제

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `Event(posterImageObjectKey="events/.../poster.png")` 존재 (UPCOMING 상태) |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey = null`<br>2. 행사 상세 조회 |
| **입력 데이터** | `posterImageObjectKey: null` |
| **기대 결과** | 행사의 `posterImageObjectKey == null`, 이전 이미지 FileMetadata는 COMPLETED 유지 (EVT-IMG-INV-05) |
| **비고** | EVT-IMG-INV-07 |

### EVT-IMG-INV-08: 목록/상세 응답의 posterImageObjectKey 노출 정책

#### TC-019: 공개 목록 조회 응답에 posterImageObjectKey 필드 포함 (이미지 연결 행사)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | `posterImageObjectKey`가 설정된 행사 존재 (PUBLISHED, UPCOMING), MEMBER 사용자 인증 |
| **테스트 절차** | 1. `GET /api/events` 호출<br>2. 응답 JSON에서 `posterImageObjectKey` 필드 확인 |
| **입력 데이터** | MEMBER 인증 토큰 |
| **기대 결과** | 응답 JSON에 `posterImageObjectKey` 필드가 non-null 값으로 포함 |
| **비고** | EVT-IMG-INV-08 |

#### TC-020: 공개 목록 조회 응답에 posterImageObjectKey 필드가 null로 포함 (이미지 미연결 행사)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | `posterImageObjectKey == null`인 행사 존재, MEMBER 사용자 인증 |
| **테스트 절차** | 1. `GET /api/events` 호출<br>2. 응답 JSON에서 `posterImageObjectKey` 필드 확인 |
| **입력 데이터** | MEMBER 인증 토큰 |
| **기대 결과** | 응답 JSON에 `posterImageObjectKey` 필드가 **null 값으로 포함** (필드 자체가 생략되지 않음) |
| **비고** | EVT-IMG-INV-08. 프론트엔드는 필드 존재 여부가 아닌 null 여부로 판단 |

#### TC-021: 공개 상세 조회 응답에 posterImageObjectKey 필드 포함 (MEMBER)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | `posterImageObjectKey` 설정된 행사 존재, MEMBER 사용자 인증 |
| **테스트 절차** | 1. `GET /api/events/{eventId}` 호출<br>2. 응답 JSON에서 `posterImageObjectKey` 필드 확인 |
| **입력 데이터** | MEMBER 인증 토큰, 유효한 `eventId` |
| **기대 결과** | 200 OK, 응답에 `posterImageObjectKey` 필드 포함 |
| **비고** | EVT-IMG-INV-08 |

#### TC-022: 관리자 API 목록/상세 응답에 posterImageObjectKey 필드 포함 (OPERATOR)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | `posterImageObjectKey` 설정된 행사 존재, OPERATOR 사용자 인증 |
| **테스트 절차** | 1. `GET /api/admin/events` 호출 → 응답 확인<br>2. `GET /api/admin/events/{eventId}` 호출 → 응답 확인 |
| **입력 데이터** | OPERATOR 인증 토큰 |
| **기대 결과** | 관리자 목록/상세 응답 모두에 `posterImageObjectKey` 필드 포함 |
| **비고** | EVT-IMG-INV-08 |

---

## 2. 상태 교차 매트릭스 (eventStatus × 이미지 연결/변경)

### TC-023: UPCOMING 행사 -- 이미지 연결 (null → COMPLETED Key)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | UPCOMING 상태 행사 (`posterImageObjectKey == null`), COMPLETED 상태 이미지 Key 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey`에 COMPLETED Key 전달 |
| **입력 데이터** | UPCOMING 행사, `posterImageObjectKey: "events/.../img.png"` |
| **기대 결과** | 수정 성공, 이미지 연결됨 |
| **비고** | 매트릭스 2-1: UPCOMING → O |

### TC-024: UPCOMING 행사 -- 이미지 해제 (COMPLETED Key → null)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | UPCOMING 상태 행사 (`posterImageObjectKey` 설정됨) |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey = null` |
| **입력 데이터** | `posterImageObjectKey: null` |
| **기대 결과** | 수정 성공, 이미지 해제 |
| **비고** | 매트릭스 2-1: UPCOMING → O |

### TC-025: UPCOMING 행사 -- 이미지 변경 (Key A → Key B)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | UPCOMING 상태 행사 (이미지 A 연결), 이미지 B (COMPLETED) 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey`를 Key B로 변경 |
| **입력 데이터** | `posterImageObjectKey: "events/.../b.png"` |
| **기대 결과** | 수정 성공, Key가 B로 변경 |
| **비고** | 매트릭스 2-1: UPCOMING → O |

### TC-026: ONGOING 행사 -- 이미지 연결 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | ONGOING 상태 행사 (`posterImageObjectKey == null`), COMPLETED 이미지 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey` 설정 |
| **입력 데이터** | ONGOING 행사, 유효한 COMPLETED Key |
| **기대 결과** | 수정 성공 -- `posterImageObjectKey`는 정보성 필드이므로 ONGOING에서도 수정 가능 |
| **비고** | 매트릭스 2-1: ONGOING → O. EVT-INV-07 ONGOING 허용 필드 패턴 동일 |

### TC-027: ONGOING 행사 -- 이미지 해제 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | ONGOING 상태 행사 (이미지 연결됨) |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey = null` |
| **입력 데이터** | `posterImageObjectKey: null` |
| **기대 결과** | 수정 성공, 이미지 해제 |
| **비고** | 매트릭스 2-1: ONGOING → O |

### TC-028: ONGOING 행사 -- 이미지 변경 성공 (Key A → Key B)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | ONGOING 상태 행사 (이미지 A 연결), 이미지 B (COMPLETED) 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, Key B로 변경 |
| **입력 데이터** | ONGOING 행사, `posterImageObjectKey: Key B` |
| **기대 결과** | 수정 성공 |
| **비고** | 매트릭스 2-1: ONGOING → O |

### TC-029: COMPLETED 행사 -- 이미지 연결 시도 시 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | COMPLETED 상태 행사 (`posterImageObjectKey == null`), COMPLETED 이미지 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey` 설정 시도 |
| **입력 데이터** | COMPLETED 행사, 유효한 COMPLETED Key |
| **기대 결과** | `EventNotEditableException` 발생 -- COMPLETED는 종단 상태, 모든 수정 불가 |
| **비고** | 매트릭스 2-1: COMPLETED → X. EVT-INV-06, EVT-INV-07, N-05 |

### TC-030: COMPLETED 행사 -- 이미지 해제 시도 시 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | COMPLETED 상태 행사 (이미지 연결됨) |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey = null` 시도 |
| **입력 데이터** | COMPLETED 행사, `posterImageObjectKey: null` |
| **기대 결과** | `EventNotEditableException` 발생 |
| **비고** | 매트릭스 2-1: COMPLETED → X |

### TC-031: CANCELED 행사 -- 이미지 연결 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | CANCELED 상태 행사 (`posterImageObjectKey == null`), COMPLETED 이미지 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey` 설정 |
| **입력 데이터** | CANCELED 행사, 유효한 COMPLETED Key |
| **기대 결과** | 수정 성공 |
| **비고** | 매트릭스 2-1: CANCELED → O. 전체 필드 수정 가능 |

---

## 3. 입력 경계값

### DECISION-03: 빈 문자열/공백 문자열 처리 (null 변환)

#### TC-032: 빈 문자열 `""` → null 변환으로 이미지 해제

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 이미지 연결된 UPCOMING 행사 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey = ""`<br>2. 수정된 행사 확인 |
| **입력 데이터** | `posterImageObjectKey: ""` |
| **기대 결과** | `""` → null 변환, 행사의 `posterImageObjectKey == null` (이미지 해제) |
| **비고** | DECISION-03 확정(A안), N-08. 프론트엔드 구현 편의 |

#### TC-033: 공백 문자열 `"   "` → null 변환으로 이미지 해제

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 이미지 연결된 UPCOMING 행사 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posterImageObjectKey = "   "`<br>2. 수정된 행사 확인 |
| **입력 데이터** | `posterImageObjectKey: "   "` (공백 3자) |
| **기대 결과** | 공백 → null 변환, 이미지 해제 |
| **비고** | DECISION-03 확정(A안) |

### Object Key 길이 경계값

#### TC-034: 일반 길이 Object Key (~60자) 허용

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="events/2026/03/01/{UUID}.png", status=COMPLETED)` 존재 (약 60자) |
| **테스트 절차** | 1. 해당 Key로 행사 생성 |
| **입력 데이터** | ~60자 Object Key |
| **기대 결과** | 허용 |
| **비고** | 일반 사용 시나리오 |

#### TC-035: 500자 Object Key (DB 컬럼 최대 길이) 허용

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey=<500자 Key>, status=COMPLETED)` 존재 |
| **테스트 절차** | 1. 500자 Key로 행사 생성 |
| **입력 데이터** | 500자 Object Key (events/ 프리픽스 포함) |
| **기대 결과** | 허용 -- FileMetadata.objectKey max=500과 동일 |
| **비고** | 실제 ObjectKeyGenerator가 생성하는 Key는 ~60자이므로 사실상 도달 불가 |

### 행사 생성 시 이미지 연결

#### TC-036: 행사 생성 시 posterImageObjectKey와 함께 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | COMPLETED 이미지 존재, OPERATOR 인증 |
| **테스트 절차** | 1. `POST /api/admin/events` 호출, 요청 body에 `posterImageObjectKey` 포함<br>2. 응답(EventCreateResponse)에 `posterImageObjectKey` 포함 확인 |
| **입력 데이터** | `CreateEventRequest` with `posterImageObjectKey: "events/.../img.png"` |
| **기대 결과** | 201 Created, 응답에 `posterImageObjectKey` 포함 |
| **비고** | E2E 정상 경로 |

### 행사 수정 시 이미지 변경 (COMPLETED Key → 다른 COMPLETED Key)

#### TC-037: 행사 수정 API로 이미지 변경

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | 이미지 A 연결된 행사, 이미지 B (COMPLETED) 존재, OPERATOR 인증 |
| **테스트 절차** | 1. `PUT /api/admin/events/{eventId}` 호출, `posterImageObjectKey`를 Key B로 변경<br>2. 응답 확인<br>3. `GET /api/events/{eventId}` 호출, 변경된 Key 확인 |
| **입력 데이터** | `UpdateEventRequest` with `posterImageObjectKey: Key B` |
| **기대 결과** | 200 OK, 상세 조회 시 Key B 반환 |
| **비고** | E2E 이미지 변경 경로 |

### 행사 수정 시 이미지 해제 (COMPLETED Key → null)

#### TC-038: 행사 수정 API로 이미지 해제 후 상세 조회에서 null 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | 이미지 연결된 행사, OPERATOR 인증 |
| **테스트 절차** | 1. `PUT /api/admin/events/{eventId}` 호출, `posterImageObjectKey: null`<br>2. `GET /api/events/{eventId}` 호출 |
| **입력 데이터** | `UpdateEventRequest` with `posterImageObjectKey: null` |
| **기대 결과** | 200 OK, 상세 조회 시 `posterImageObjectKey: null` |
| **비고** | EVT-IMG-INV-07 E2E |

### 수정 시에도 이미지 참조 검증 적용

#### TC-039: 행사 수정 시 REQUESTED 상태 Key로 변경 시도 → 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 이미지 연결된 UPCOMING 행사, `FileMetadata(objectKey=새Key, status=REQUESTED)` 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, 새 REQUESTED Key 전달 |
| **입력 데이터** | 수정 요청에 REQUESTED 상태 Key |
| **기대 결과** | 비즈니스 예외 (400 Bad Request) -- 수정 시에도 생성과 동일한 검증 적용 |
| **비고** | EVT-IMG-INV-02. 생성뿐 아니라 수정에서도 검증 필수 |

---

## 4. 권한/보안 정책

### RBAC 매트릭스

#### TC-040: ASSOCIATE가 공개 목록 조회 시 posterImageObjectKey 포함 응답 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | 이미지 연결된 행사 존재, ASSOCIATE 사용자 인증 |
| **테스트 절차** | 1. `GET /api/events` 호출 (ASSOCIATE 토큰) |
| **입력 데이터** | ASSOCIATE 인증 토큰 |
| **기대 결과** | 200 OK, 응답에 `posterImageObjectKey` 필드 포함 |
| **비고** | EVT-IMG-INV-08. ASSOCIATE도 목록에서 Key 확인 가능 |

#### TC-041: ASSOCIATE가 공개 상세 조회 시 403 응답 (기존 SEC-EVT-01 유지)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | 이미지 연결된 행사 존재, ASSOCIATE 사용자 인증 |
| **테스트 절차** | 1. `GET /api/events/{eventId}` 호출 (ASSOCIATE 토큰) |
| **입력 데이터** | ASSOCIATE 인증 토큰 |
| **기대 결과** | 403 Forbidden -- 기존 SEC-EVT-01 정책 변경 없음 |
| **비고** | EVT-IMG-INV-08. 이미지 연계로 인한 추가 제약 없음, 기존 RBAC 유지 확인 |

#### TC-042: 비인증 사용자가 행사 목록 조회 시 401

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `GET /api/events` 호출 (인증 없음) |
| **입력 데이터** | 인증 토큰 없음 |
| **기대 결과** | 401 Unauthorized |
| **비고** | RBAC 매트릭스 |

#### TC-043: MEMBER가 행사 생성 시도 시 403

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | MEMBER 사용자 인증 |
| **테스트 절차** | 1. `POST /api/admin/events` 호출 (MEMBER 토큰), 이미지 Key 포함 |
| **입력 데이터** | MEMBER 인증 토큰, 유효한 `CreateEventRequest` |
| **기대 결과** | 403 Forbidden -- 행사 생성은 OPERATOR+ 전용 |
| **비고** | RBAC 매트릭스. 이미지 연결 권한 = 행사 CRUD 권한 |

### SEC-EVT-IMG-01, SEC-EVT-IMG-02: 다른 사용자 이미지 사용 정책

#### TC-044: 운영진 A가 업로드한 이미지를 운영진 B가 행사에 연결 -- 허용

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | OPERATOR A가 업로드한 이미지 (COMPLETED), OPERATOR B 인증 |
| **테스트 절차** | 1. OPERATOR B가 `EventService.createEvent()` 호출, A의 이미지 Object Key 전달 |
| **입력 데이터** | OPERATOR B의 요청, A가 업로드한 COMPLETED Key |
| **기대 결과** | 행사 생성 성공 -- 업로드자 소유권 검증하지 않음 |
| **비고** | SEC-EVT-IMG-01, DECISION-02 확정(A안: 소유권 무관). 동아리 운영 특성상 운영진 간 이미지 공유 허용 |

#### TC-045: MEMBER가 업로드한 이미지를 OPERATOR가 행사에 연결 -- 허용

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | MEMBER가 업로드한 이미지 (COMPLETED), OPERATOR 인증 |
| **테스트 절차** | 1. OPERATOR가 `EventService.createEvent()` 호출, MEMBER의 이미지 Object Key 전달 |
| **입력 데이터** | OPERATOR의 요청, MEMBER가 업로드한 COMPLETED Key |
| **기대 결과** | 행사 생성 성공 |
| **비고** | SEC-EVT-IMG-02. COMPLETED 상태만 확인, 업로드자 역할/신분 무관 |

### SEC-EVT-IMG-03: 미인증 사용자 이미지 다운로드 차단

#### TC-046: 미인증 사용자가 이미지 다운로드 URL 요청 시 401

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 컨트롤러 통합 |
| **사전 조건** | 이미지 연결된 행사 존재 |
| **테스트 절차** | 1. `GET /api/storage/download-url?objectKey={key}` 호출 (인증 없음) |
| **입력 데이터** | 인증 토큰 없음, 유효한 Object Key |
| **기대 결과** | 401 Unauthorized |
| **비고** | SEC-EVT-IMG-03, SEC-STOR-04. Storage 모듈 RBAC |

---

## 5. 부정 시나리오 (Negative Scenarios)

> TC-004~TC-009에서 이미 커버된 N-01~N-04, N-07과 TC-029의 N-05, TC-013의 N-06을 제외한 추가 시나리오

### N-08 확장: 빈 문자열 행사 생성 시 처리

#### TC-047: 행사 생성 시 빈 문자열 posterImageObjectKey → null 변환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, `posterImageObjectKey = ""`<br>2. 생성된 행사 확인 |
| **입력 데이터** | `posterImageObjectKey: ""` |
| **기대 결과** | null 변환, 이미지 미연결 행사 생성 성공 |
| **비고** | N-08, DECISION-03. 생성 시에도 빈 문자열 → null 변환 적용 |

### 복합 부정: 프리픽스 위반 + COMPLETED 상태 조합

#### TC-048: COMPLETED 상태이지만 `posts/` 프리픽스인 Key로 행사 수정 시 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | `FileMetadata(objectKey="posts/.../img.png", status=COMPLETED)` 존재, UPCOMING 행사 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `posts/` 프리픽스 Key 전달 |
| **입력 데이터** | `posterImageObjectKey: "posts/.../img.png"` |
| **기대 결과** | 400 Bad Request -- `events/` 프리픽스 강제 |
| **비고** | EVT-IMG-INV-03. 프리픽스 검증이 상태 검증보다 우선 |

### 복합 부정: 존재하지 않는 Key + 수정 시나리오

#### TC-049: 행사 수정 시 존재하지 않는 Object Key로 변경 시도 → 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 이미지 연결된 UPCOMING 행사 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, 존재하지 않는 Key 전달 |
| **입력 데이터** | `posterImageObjectKey: "events/2026/03/01/nonexistent.png"` |
| **기대 결과** | FileMetadata 미발견 예외 (404 Not Found) |
| **비고** | EVT-IMG-INV-02. 수정 시에도 참조 검증 적용 확인 |

### EventFileReferenceChecker 단위 동작

#### TC-050: EventFileReferenceChecker -- 참조 행사 존재 시 true 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | `EventRepository.existsByPosterImageObjectKeyAndDeletedFalse(key)` → true (Mock) |
| **테스트 절차** | 1. `EventFileReferenceChecker.isReferenced(key)` 호출 |
| **입력 데이터** | 활성 행사가 참조 중인 Object Key |
| **기대 결과** | `true` 반환 |
| **비고** | EVT-IMG-INV-04 단위 테스트 |

#### TC-051: EventFileReferenceChecker -- 참조 행사 없을 시 false 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | `EventRepository.existsByPosterImageObjectKeyAndDeletedFalse(key)` → false (Mock) |
| **테스트 절차** | 1. `EventFileReferenceChecker.isReferenced(key)` 호출 |
| **입력 데이터** | 어떤 행사에서도 참조하지 않는 Key |
| **기대 결과** | `false` 반환 |
| **비고** | EVT-IMG-INV-04 단위 테스트 |

#### TC-052: EventFileReferenceChecker -- Soft Delete 행사만 참조 시 false 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | Soft Delete된 행사만 해당 Key 참조 → `existsByPosterImageObjectKeyAndDeletedFalse(key)` → false (Mock) |
| **테스트 절차** | 1. `EventFileReferenceChecker.isReferenced(key)` 호출 |
| **입력 데이터** | Soft Delete된 행사만 참조하는 Key |
| **기대 결과** | `false` 반환 -- Soft Delete 행사는 참조로 간주하지 않음 |
| **비고** | EVT-IMG-INV-04. Repository 메서드명에 `DeletedFalse` 포함 필수 |

---

## 6. 관측 가능성 (Observability)

#### TC-053: 이미지 포함 행사 생성 시 로그 메시지 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | COMPLETED 이미지 존재 |
| **테스트 절차** | 1. 이미지 포함 행사 생성<br>2. 로그 출력 확인 |
| **입력 데이터** | 유효한 `posterImageObjectKey` |
| **기대 결과** | INFO 로그: `행사 생성: eventId={}, posterImageObjectKey={}` |
| **비고** | 6-1 로그 메시지 |

#### TC-054: 이미지 변경 시 이전/이후 Key 로그 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | 이미지 A 연결된 행사 |
| **테스트 절차** | 1. 이미지를 B로 변경<br>2. 로그 출력 확인 |
| **입력 데이터** | `posterImageObjectKey: Key B` |
| **기대 결과** | INFO 로그: `행사 수정 - eventId: {}, posterImageObjectKey 변경: {} -> {}` (이전/이후 모두 기록) |
| **비고** | 6-1 로그 메시지 |

#### TC-055: 이미지 참조 검증 실패 시 WARN 로그 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 |
| **사전 조건** | REQUESTED 상태 FileMetadata 존재 |
| **테스트 절차** | 1. REQUESTED Key로 행사 생성 시도<br>2. 예외 발생 확인<br>3. 로그 출력 확인 |
| **입력 데이터** | REQUESTED 상태 Key |
| **기대 결과** | WARN 로그: `이미지 참조 검증 실패: objectKey={}, status={}` |
| **비고** | 6-1 로그 메시지 |

---

## 부록: 검증 기준 → 테스트 케이스 추적 매트릭스

| 검증 기준 ID | 관련 TC |
|-------------|---------|
| EVT-IMG-INV-01 | TC-001, TC-002 |
| EVT-IMG-INV-02 | TC-003 ~ TC-009, TC-039, TC-049 |
| EVT-IMG-INV-03 | TC-010 ~ TC-012, TC-048 |
| EVT-IMG-INV-04 | TC-013 ~ TC-015, TC-050 ~ TC-052 |
| EVT-IMG-INV-05 | TC-016 |
| EVT-IMG-INV-06 | TC-017 |
| EVT-IMG-INV-07 | TC-002, TC-018 |
| EVT-IMG-INV-08 | TC-019 ~ TC-022, TC-040, TC-041 |
| SEC-EVT-IMG-01 | TC-044 |
| SEC-EVT-IMG-02 | TC-045 |
| SEC-EVT-IMG-03 | TC-046 |
| DECISION-01 | TC-010 ~ TC-012, TC-048 |
| DECISION-02 | TC-044, TC-045 |
| DECISION-03 | TC-032, TC-033, TC-047 |
| N-01 | TC-004 |
| N-02 | TC-006 |
| N-03 | TC-007 |
| N-04 | TC-009 |
| N-05 | TC-029, TC-030 |
| N-06 | TC-013 |
| N-07 | TC-008 |
| N-08 | TC-032, TC-047 |
| 상태 매트릭스 (Pairwise) | TC-023 ~ TC-031 |
