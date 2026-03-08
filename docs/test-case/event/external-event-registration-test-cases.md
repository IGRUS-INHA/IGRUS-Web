# 외부인 행사 신청 (External Event Registration) 테스트 케이스

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-03-06 |
| 검증 기준 문서 | `docs/criteria/event/external-event-registration-verification-criteria.md` |
| 대상 기능 | 외부인(비회원) 행사 신청, allowExternal 플래그, 준회원(ASSOCIATE) 조건부 허용, 외부인 중복 방지, 정원 공유, 외부인 설문 연동, 관리자 취소 |
| 테스트 케이스 수 | 총 80개 |

## 카테고리 요약

| # | 카테고리 | 테스트 케이스 수 | 커버리지 대상 |
|---|---------|:---:|-------------|
| 1 | 도메인 규칙과 불변조건 | 30 | EXT-INV-01 ~ EXT-INV-12 |
| 2 | 상태 모델 (FSM) | 10 | 선착순/선발제 FSM, 금지된 전이, currentCount 변경 |
| 3 | 입력 경계값 (BVA) | 16 | 4-1 필드별 BVA, 4-3 중복 경계값, 4-4 정원 경계값 |
| 4 | allowExternal 동치 분할 | 6 | 4-2 allowExternal x 신청자 유형 6가지 조합 |
| 5 | 권한/보안 정책 | 7 | SEC-EXT-01 ~ SEC-EXT-07 |
| 6 | 동시성 | 4 | 정원 경합, 중복 경합, 신청+취소 경합 |
| 7 | 관측 가능성 | 4 | 로그 메시지, 감사 추적 |
| 8 | 기존 불변조건 회귀 테스트 | 3 | REG-INV-04, SEC-REG-01 변경 검증 |

---

## 1. 도메인 규칙과 불변조건

### EXT-INV-01: 외부인 신청은 allowExternal == true 행사에서만 가능

#### TC-001: allowExternal=true 행사에 외부인 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.allowExternal == true`, PUBLISHED, registrationStatus == OPEN, 신청 기간 내, 정원 여유 있음 |
| **테스트 절차** | 1. `ExternalEventRegistrationService.registerExternal()` 호출<br>2. 신청 결과 확인 |
| **입력 데이터** | `eventId: {allowExternal=true 행사}`, `name: "홍길동"`, `studentId: "12345678"`, `phone: "01012345678"`, `department: "컴퓨터공학과"` |
| **기대 결과** | 신청 성공, `EventRegistration` 생성, `isExternal == true`, `status == REGISTERED` (선착순) 또는 `WAITING` (선발제) |
| **비고** | EXT-INV-01 정상 흐름 |

#### TC-002: allowExternal=false 행사에 외부인 신청 시 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.allowExternal == false`, PUBLISHED, OPEN |
| **테스트 절차** | 1. `ExternalEventRegistrationService.registerExternal()` 호출 |
| **입력 데이터** | `eventId: {allowExternal=false 행사}`, 유효한 외부인 정보 |
| **기대 결과** | `ExternalRegistrationNotAllowedException` 발생 (400 Bad Request) |
| **비고** | EXT-INV-01 위반 검증 |

### EXT-INV-02: 외부인 중복 신청 방지 — studentId 기준

#### TC-003: 동일 행사에 동일 studentId로 재신청 시 409 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.allowExternal == true`, 동일 행사에 `studentId: "12345678"`로 활성 신청(REGISTERED)이 이미 존재 |
| **테스트 절차** | 1. 동일 studentId, 다른 phone으로 `registerExternal()` 호출 |
| **입력 데이터** | `studentId: "12345678"`, `phone: "01099999999"` (다른 번호) |
| **기대 결과** | `ExternalAlreadyRegisteredException` 발생 (409 Conflict) |
| **비고** | EXT-INV-02. studentId 단독 중복으로 차단 |

#### TC-004: 이전 신청이 CANCELED 상태인 동일 studentId로 재신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 동일 행사에 `studentId: "12345678"`로 CANCELED 상태 신청이 존재, 정원 여유 있음 |
| **테스트 절차** | 1. 동일 studentId로 `registerExternal()` 호출<br>2. 신청 성공 확인 |
| **입력 데이터** | `studentId: "12345678"`, `phone: "01012345678"` |
| **기대 결과** | 신청 성공 (CANCELED 제외 중복 검증), 새 `EventRegistration` 생성 |
| **비고** | DECISION-02 확정: 취소 후 재신청 가능 |

### EXT-INV-03: 외부인 중복 신청 방지 — phone 기준

#### TC-005: 동일 행사에 동일 phone으로 재신청 시 409 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 동일 행사에 `phone: "01012345678"`로 활성 신청이 이미 존재 |
| **테스트 절차** | 1. 다른 studentId, 동일 phone으로 `registerExternal()` 호출 |
| **입력 데이터** | `studentId: "99999999"` (다른 학번), `phone: "01012345678"` |
| **기대 결과** | `ExternalAlreadyRegisteredException` 발생 (409 Conflict) |
| **비고** | EXT-INV-03. phone 단독 중복으로 차단 |

#### TC-006: 이전 신청이 CANCELED 상태인 동일 phone으로 재신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 동일 행사에 `phone: "01012345678"`로 CANCELED 상태 신청 존재, 정원 여유 있음 |
| **테스트 절차** | 1. 동일 phone으로 `registerExternal()` 호출 |
| **입력 데이터** | `studentId: "11111111"`, `phone: "01012345678"` |
| **기대 결과** | 신청 성공 |
| **비고** | DECISION-02 확정: CANCELED 제외 중복 검증 |

#### TC-007: 동일 studentId + 동일 phone으로 재신청 시 409 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 동일 행사에 `studentId: "12345678"`, `phone: "01012345678"`로 활성 신청 존재 |
| **테스트 절차** | 1. 완전히 동일한 studentId + phone으로 `registerExternal()` 호출 |
| **입력 데이터** | `studentId: "12345678"`, `phone: "01012345678"` |
| **기대 결과** | `ExternalAlreadyRegisteredException` 발생 (409 Conflict) |
| **비고** | EXT-INV-02 + EXT-INV-03 동시 중복 |

#### TC-008: 다른 행사에 동일 studentId + 동일 phone으로 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 행사 A에 `studentId: "12345678"`, `phone: "01012345678"`로 활성 신청 존재, 행사 B는 별도(allowExternal=true, OPEN) |
| **테스트 절차** | 1. 행사 B에 동일 studentId, phone으로 `registerExternal()` 호출 |
| **입력 데이터** | `eventId: {행사 B}`, `studentId: "12345678"`, `phone: "01012345678"` |
| **기대 결과** | 신청 성공 (행사별 독립적 중복 검사) |
| **비고** | 중복 검사는 행사 단위로 수행 |

### EXT-INV-04: 정원 공유 — 회원과 외부인 동일 capacity/currentCount

#### TC-009: 회원 3명 + 외부인 1명 신청 후 정원(5)에 외부인 추가 신청 시 정원 채움

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `capacity: 5`, `currentCount: 4` (회원 3 + 외부인 1), allowExternal=true, OPEN |
| **테스트 절차** | 1. 외부인 신청 `registerExternal()` 호출<br>2. `currentCount` 변경 확인 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | 신청 성공, `currentCount: 5`, registrationStatus가 CLOSED로 전환 |
| **비고** | EXT-INV-04. 정원 경계 — 마지막 1자리 |

#### TC-010: 정원 가득 찬 상태에서 외부인 신청 시 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `capacity: 5`, `currentCount: 5`, OPEN |
| **테스트 절차** | 1. 외부인 신청 `registerExternal()` 호출 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | `EventCapacityFullException` 발생 (400 Bad Request) |
| **비고** | EXT-INV-04. 원자적 UPDATE `incrementCurrentCountIfAvailable`에서 차단 |

#### TC-011: 외부인만으로 정원 가득 찬 경우 회원 신청도 차단됨

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `capacity: 5`, `currentCount: 5` (전부 외부인), allowExternal=true, OPEN |
| **테스트 절차** | 1. 회원(MEMBER)이 `registerEvent()` 호출 |
| **입력 데이터** | `userId: {MEMBER 사용자}`, `eventId: {정원 가득 찬 행사}` |
| **기대 결과** | `EventCapacityFullException` 발생 (동일 capacity/currentCount 공유) |
| **비고** | EXT-INV-04. 단일 테이블 정원 공유 검증 |

### EXT-INV-05: 준회원(ASSOCIATE) 조건부 허용

#### TC-012: 준회원이 allowExternal=true 행사에 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | ASSOCIATE 사용자 로그인, `event.allowExternal == true`, PUBLISHED, OPEN, 기간 내 |
| **테스트 절차** | 1. `/registrations` 엔드포인트로 `registerEvent()` 호출 |
| **입력 데이터** | `userId: {ASSOCIATE 사용자}`, `eventId: {allowExternal=true 행사}` |
| **기대 결과** | 201 Created, 신청 성공 |
| **비고** | EXT-INV-05, REG-INV-04 변경. 준회원은 기존 회원 엔드포인트 사용 |

#### TC-013: 준회원이 allowExternal=false 행사에 신청 시 403 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | ASSOCIATE 사용자 로그인, `event.allowExternal == false`, PUBLISHED, OPEN |
| **테스트 절차** | 1. `/registrations` 엔드포인트로 `registerEvent()` 호출 |
| **입력 데이터** | `userId: {ASSOCIATE 사용자}`, `eventId: {allowExternal=false 행사}` |
| **기대 결과** | `AssociateMemberNotAllowedException` 발생 (403 Forbidden) |
| **비고** | EXT-INV-05. 기존 동작 유지 |

### EXT-INV-06: allowExternal 기본값 false

#### TC-014: Event 생성 시 allowExternal 미지정하면 기본값 false

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `Event.create()` 호출 시 `allowExternal` 파라미터 미지정 (또는 null)<br>2. 생성된 Event 객체의 `allowExternal` 값 확인 |
| **입력 데이터** | `allowExternal: null` (또는 미전달), 기타 필수 필드 유효값 |
| **기대 결과** | `event.getAllowExternal() == false` |
| **비고** | EXT-INV-06, DECISION-05 |

#### TC-015: Event 생성 시 allowExternal=true 명시적 설정

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `Event.create()` 호출 시 `allowExternal: true` 전달<br>2. 생성된 Event 객체의 `allowExternal` 값 확인 |
| **입력 데이터** | `allowExternal: true` |
| **기대 결과** | `event.getAllowExternal() == true` |
| **비고** | EXT-INV-06 |

### EXT-INV-07: 외부인 신청도 registrationStatus == OPEN + 기간 내 필수

#### TC-016: registrationStatus가 CLOSED인 행사에 외부인 신청 시 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.registrationStatus == CLOSED`, allowExternal=true, PUBLISHED |
| **테스트 절차** | 1. `registerExternal()` 호출 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | `EventNotOpenException` 발생 |
| **비고** | EXT-INV-07. 기존 REG-INV-05와 동일 검증 |

#### TC-017: 신청 기간 외 외부인 신청 시 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.registrationStatus == OPEN`, allowExternal=true, PUBLISHED, 현재 시각이 신청 기간 밖 |
| **테스트 절차** | 1. `registerExternal()` 호출 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | `EventNotInRegistrationPeriodException` 발생 |
| **비고** | EXT-INV-07 |

### EXT-INV-08: UNPUBLISHED 행사에서 외부인 신청 차단

#### TC-018: UNPUBLISHED 행사에 외부인 신청 시 404 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.visibility == UNPUBLISHED`, allowExternal=true |
| **테스트 절차** | 1. `registerExternal()` 호출 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | 404 Not Found (행사가 존재하지 않는 것처럼 처리) |
| **비고** | EXT-INV-08, EVT-INV-18. 정보 은폐 목적 |

### EXT-INV-09: 외부인 신청 취소는 관리자만 가능

#### TC-019: OPERATOR가 외부인 신청 취소 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | OPERATOR 사용자 로그인, 외부인 신청(REGISTERED 상태) 존재 |
| **테스트 절차** | 1. `POST /api/v1/registrations/{registrationId}/cancel` 호출<br>2. 신청 상태 변경 확인<br>3. `currentCount` 감소 확인 |
| **입력 데이터** | `registrationId: {외부인 활성 신청 ID}` |
| **기대 결과** | 200 OK, `status == CANCELED`, `currentCount--` |
| **비고** | EXT-INV-09, DECISION-03, DECISION-08 |

#### TC-020: ADMIN이 외부인 신청 취소 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | ADMIN 사용자 로그인, 외부인 신청(REGISTERED 상태) 존재 |
| **테스트 절차** | 1. `POST /api/v1/registrations/{registrationId}/cancel` 호출 |
| **입력 데이터** | `registrationId: {외부인 활성 신청 ID}` |
| **기대 결과** | 200 OK, `status == CANCELED` |
| **비고** | EXT-INV-09. ADMIN도 OPERATOR+ 포함 |

#### TC-021: MEMBER가 외부인 신청 취소 시도 시 403 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | MEMBER 사용자 로그인, 외부인 신청 존재 |
| **테스트 절차** | 1. `POST /api/v1/registrations/{registrationId}/cancel` 호출 |
| **입력 데이터** | `registrationId: {외부인 신청 ID}` |
| **기대 결과** | 403 Forbidden (`AccessDeniedException`) |
| **비고** | EXT-INV-09, SEC-EXT-05 |

### EXT-INV-10: 외부인 신청 정보 필수 필드

#### TC-022: 모든 필수 필드를 유효한 값으로 제공 시 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `ExternalRegisterEventRequest` 생성, 4개 필수 필드 모두 유효값<br>2. Bean Validation 통과 확인 |
| **입력 데이터** | `name: "홍길동"`, `studentId: "12345678"`, `phone: "01012345678"`, `department: "컴퓨터공학과"` |
| **기대 결과** | Validation 통과 (위반 0건) |
| **비고** | EXT-INV-10 |

#### TC-023: name이 null인 경우 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `ExternalRegisterEventRequest` 생성, `name: null`<br>2. Bean Validation 실행 |
| **입력 데이터** | `name: null`, 나머지 필드 유효값 |
| **기대 결과** | Validation 위반 발생 (400 Bad Request) |
| **비고** | EXT-INV-10 |

#### TC-024: studentId가 빈 문자열인 경우 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `ExternalRegisterEventRequest` 생성, `studentId: ""`<br>2. Bean Validation 실행 |
| **입력 데이터** | `studentId: ""`, 나머지 필드 유효값 |
| **기대 결과** | Validation 위반 발생 (400 Bad Request) |
| **비고** | EXT-INV-10 |

#### TC-025: phone이 null인 경우 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `ExternalRegisterEventRequest` 생성, `phone: null`<br>2. Bean Validation 실행 |
| **입력 데이터** | `phone: null`, 나머지 필드 유효값 |
| **기대 결과** | Validation 위반 발생 (400 Bad Request) |
| **비고** | EXT-INV-10 |

#### TC-026: department가 null인 경우 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `ExternalRegisterEventRequest` 생성, `department: null`<br>2. Bean Validation 실행 |
| **입력 데이터** | `department: null`, 나머지 필드 유효값 |
| **기대 결과** | Validation 위반 발생 (400 Bad Request) |
| **비고** | EXT-INV-10 |

### EXT-INV-11: 외부인도 설문 연결 행사에 신청 가능

#### TC-027: 설문 연결 행사에 외부인이 설문 응답과 함께 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId != null`, allowExternal=true, PUBLISHED, OPEN, 설문 OPEN 상태 |
| **테스트 절차** | 1. `registerExternal()` 호출, `surveyAnswers` 포함<br>2. `ExternalSurveyResponse` 저장 확인<br>3. `EventRegistration` 생성 확인<br>4. 원자적 트랜잭션 검증 |
| **입력 데이터** | 유효한 외부인 정보 + `surveyAnswers: [{questionId: 1, answer: "답변1"}]` |
| **기대 결과** | 신청 성공, `ExternalSurveyResponse` 테이블에 응답 저장, `EventRegistration` 생성 |
| **비고** | EXT-INV-11, DECISION-04 (옵션 B: 별도 테이블) |

#### TC-028: 설문 연결 행사에 설문 응답 없이 외부인 신청 시 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId != null`, allowExternal=true, PUBLISHED, OPEN |
| **테스트 절차** | 1. `registerExternal()` 호출, `surveyAnswers: null` |
| **입력 데이터** | 유효한 외부인 정보, `surveyAnswers: null` |
| **기대 결과** | 400 Bad Request — 설문 응답 필수 |
| **비고** | EXT-INV-11, Section 4-1 surveyAnswers BVA |

### EXT-INV-12: 동일 학번 가입 회원 존재 시 거부

#### TC-029: 동일 studentId로 가입된 회원이 존재하면 외부인 신청 거부

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `User(studentId: "12345678")` 존재, allowExternal=true, OPEN |
| **테스트 절차** | 1. `registerExternal()` 호출, `studentId: "12345678"` |
| **입력 데이터** | `studentId: "12345678"` (이미 가입된 회원의 학번) |
| **기대 결과** | 400 Bad Request — "해당 학번으로 가입된 회원이 존재하므로 로그인 후 신청하세요" |
| **비고** | EXT-INV-12. 허위 신청 방지 효과 |

#### TC-030: 동일 studentId 회원이 존재하지 않으면 외부인 신청 정상 진행

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `studentId: "99999999"`로 가입된 User 없음, allowExternal=true, OPEN, 정원 여유 |
| **테스트 절차** | 1. `registerExternal()` 호출, `studentId: "99999999"` |
| **입력 데이터** | `studentId: "99999999"` |
| **기대 결과** | 신청 성공 |
| **비고** | EXT-INV-12 정상 흐름 |

---

## 2. 상태 모델 (FSM)

### 선착순(AUTO_APPROVE) 행사

#### TC-031: 선착순 행사에 외부인 신청 시 즉시 REGISTERED 상태

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.approvalMethod == AUTO_APPROVE`, allowExternal=true, OPEN, 정원 여유 |
| **테스트 절차** | 1. `registerExternal()` 호출<br>2. 생성된 신청의 상태 확인<br>3. `currentCount` 증가 확인 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | `status == REGISTERED`, `currentCount++` (원자적 UPDATE) |
| **비고** | Section 2-1 선착순 FSM |

#### TC-032: 선착순 행사에서 관리자가 외부인 신청 취소 시 CANCELED + currentCount 감소

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 외부인 신청(REGISTERED), OPERATOR 로그인 |
| **테스트 절차** | 1. `POST /registrations/{id}/cancel` 호출<br>2. 상태 및 currentCount 확인 |
| **입력 데이터** | `registrationId: {REGISTERED 외부인 신청}` |
| **기대 결과** | `status == CANCELED`, `currentCount--` |
| **비고** | Section 2-1, 2-2. DECISION-03 |

### 선발제(MANUAL_APPROVE) 행사

#### TC-033: 선발제 행사에 외부인 신청 시 WAITING 상태 (currentCount 변경 없음)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.approvalMethod == MANUAL_APPROVE`, allowExternal=true, OPEN |
| **테스트 절차** | 1. `registerExternal()` 호출<br>2. 생성된 신청의 상태 확인 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | `status == WAITING`, currentCount 변경 없음 |
| **비고** | Section 2-1 선발제 FSM |

#### TC-034: 선발제 행사에서 외부인 신청 승인 시 APPROVED + currentCount 증가

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 외부인 신청(WAITING), OPERATOR 로그인, 정원 여유 |
| **테스트 절차** | 1. `POST /registrations/{id}/approve` 호출<br>2. 상태 및 currentCount 확인 |
| **입력 데이터** | `registrationId: {WAITING 외부인 신청}` |
| **기대 결과** | `status == APPROVED`, `currentCount++` (원자적 UPDATE) |
| **비고** | Section 2-1, DECISION-07. 기존 승인 API 재사용 |

#### TC-035: 선발제 행사에서 외부인 신청 거절 시 REJECTED (currentCount 변경 없음)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 외부인 신청(WAITING), OPERATOR 로그인 |
| **테스트 절차** | 1. `POST /registrations/{id}/reject` 호출 |
| **입력 데이터** | `registrationId: {WAITING 외부인 신청}` |
| **기대 결과** | `status == REJECTED`, currentCount 변경 없음 |
| **비고** | Section 2-1 선발제 FSM |

#### TC-036: 선발제 행사에서 APPROVED 외부인 신청 취소 시 CANCELED + currentCount 감소

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 외부인 신청(APPROVED), OPERATOR 로그인 |
| **테스트 절차** | 1. `POST /registrations/{id}/cancel` 호출 |
| **입력 데이터** | `registrationId: {APPROVED 외부인 신청}` |
| **기대 결과** | `status == CANCELED`, `currentCount--` |
| **비고** | Section 2-2. APPROVED 취소 시에만 currentCount 감소 |

### 금지된 전이

#### TC-037: CANCELED 상태의 외부인 신청에 reRegister 시도 시 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 외부인 신청(CANCELED 상태), 동일 studentId |
| **테스트 절차** | 1. 기존 회원 reRegister 로직이 외부인에게 적용되지 않음을 확인 |
| **입력 데이터** | 외부인 신청이므로 `user == null`, reRegister 불가 |
| **기대 결과** | 재신청 경로 없음. 새 외부인 신청은 가능 (TC-004 참조) |
| **비고** | DECISION-03. 인증 수단 부재로 재신청 불가 |

#### TC-038: REJECTED 상태의 외부인 신청에 직접 approve 시도 시 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 외부인 신청(REJECTED), OPERATOR 로그인 |
| **테스트 절차** | 1. `POST /registrations/{id}/approve` 호출 |
| **입력 데이터** | `registrationId: {REJECTED 외부인 신청}` |
| **기대 결과** | 예외 발생 — 되돌리기(revert) 후 재승인 필요 |
| **비고** | REG-INV-09. 기존 FSM 규칙 동일 적용 |

#### TC-039: REGISTERED 상태의 외부인 신청에 approve 시도 시 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 선착순 행사, 외부인 신청(REGISTERED) |
| **테스트 절차** | 1. `POST /registrations/{id}/approve` 호출 |
| **입력 데이터** | `registrationId: {REGISTERED 외부인 신청}` |
| **기대 결과** | 예외 발생 — 선착순 행사에서 approve 불가 |
| **비고** | 선착순과 선발제 FSM 차이 검증 |

#### TC-040: WAITING 상태의 외부인 신청에 이미 활성 중복 존재 시 재신청 불가

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 선발제 행사, 동일 studentId로 WAITING 상태 신청 존재 |
| **테스트 절차** | 1. 동일 studentId로 새 외부인 신청 시도 |
| **입력 데이터** | 동일 studentId |
| **기대 결과** | 409 Conflict (EXT-INV-02 중복 방지) |
| **비고** | WAITING도 활성 신청에 포함 |

---

## 3. 입력 경계값 (BVA)

### 3-1. name 필드 (1-50자)

#### TC-041: name 1자 (최소 경계) 정상 신청

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `name: "김"` (1자)으로 Validation 실행 |
| **입력 데이터** | `name: "김"` |
| **기대 결과** | Validation 통과 |
| **비고** | Section 4-1 name BVA 하한 |

#### TC-042: name 50자 (최대 경계) 정상 신청

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `name: "가" * 50` (50자)로 Validation 실행 |
| **입력 데이터** | `name: "가가가...가"` (50자) |
| **기대 결과** | Validation 통과 |
| **비고** | Section 4-1 name BVA 상한 |

#### TC-043: name 51자 (상한 초과) 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `name: "가" * 51` (51자)로 Validation 실행 |
| **입력 데이터** | `name: "가가가...가"` (51자) |
| **기대 결과** | Validation 위반 (400 Bad Request) |
| **비고** | Section 4-1 name BVA 상한 초과 |

### 3-2. studentId 필드 (1-20자)

#### TC-044: studentId 1자 (최소 경계) 정상

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `studentId: "1"` (1자)로 Validation 실행 |
| **입력 데이터** | `studentId: "1"` |
| **기대 결과** | Validation 통과 |
| **비고** | Section 4-1 studentId BVA 하한 |

#### TC-045: studentId 20자 (최대 경계) 정상

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `studentId: "12345678901234567890"` (20자)로 Validation 실행 |
| **입력 데이터** | `studentId: "12345678901234567890"` |
| **기대 결과** | Validation 통과 |
| **비고** | Section 4-1 studentId BVA 상한 |

#### TC-046: studentId 21자 (상한 초과) 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `studentId: "123456789012345678901"` (21자)로 Validation 실행 |
| **입력 데이터** | `studentId: "123456789012345678901"` |
| **기대 결과** | Validation 위반 (400 Bad Request) |
| **비고** | Section 4-1 studentId BVA 상한 초과 |

### 3-3. phone 필드 (1-20자)

#### TC-047: phone 1자 (최소 경계) 정상

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `phone: "1"` (1자)로 Validation 실행 |
| **입력 데이터** | `phone: "1"` |
| **기대 결과** | Validation 통과 |
| **비고** | Section 4-1 phone BVA 하한 |

#### TC-048: phone 20자 (최대 경계) 정상

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `phone: "01012345678901234567"` (20자)로 Validation 실행 |
| **입력 데이터** | `phone: "01012345678901234567"` |
| **기대 결과** | Validation 통과 |
| **비고** | Section 4-1 phone BVA 상한 |

#### TC-049: phone 21자 (상한 초과) 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `phone: "010123456789012345678"` (21자)로 Validation 실행 |
| **입력 데이터** | `phone: "010123456789012345678"` |
| **기대 결과** | Validation 위반 (400 Bad Request) |
| **비고** | Section 4-1 phone BVA 상한 초과 |

### 3-4. department 필드 (1-100자)

#### TC-050: department 1자 (최소 경계) 정상

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `department: "공"` (1자)로 Validation 실행 |
| **입력 데이터** | `department: "공"` |
| **기대 결과** | Validation 통과 |
| **비고** | Section 4-1 department BVA 하한 |

#### TC-051: department 100자 (최대 경계) 정상

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `department: "가" * 100` (100자)로 Validation 실행 |
| **입력 데이터** | `department: "가가가...가"` (100자) |
| **기대 결과** | Validation 통과 |
| **비고** | Section 4-1 department BVA 상한 |

#### TC-052: department 101자 (상한 초과) 400 에러

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 단위 (Bean Validation) |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `department: "가" * 101` (101자)로 Validation 실행 |
| **입력 데이터** | `department: "가가가...가"` (101자) |
| **기대 결과** | Validation 위반 (400 Bad Request) |
| **비고** | Section 4-1 department BVA 상한 초과 |

### 3-5. surveyAnswers 경계값

#### TC-053: 설문 미연결 행사에 surveyAnswers=null로 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == null`, allowExternal=true, OPEN |
| **테스트 절차** | 1. `registerExternal()` 호출, `surveyAnswers: null` |
| **입력 데이터** | `surveyAnswers: null` |
| **기대 결과** | 신청 성공, 설문 관련 저장 없음 |
| **비고** | Section 4-1 surveyAnswers |

#### TC-054: 설문 미연결 행사에 surveyAnswers 제공 시 무시하고 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == null`, allowExternal=true, OPEN |
| **테스트 절차** | 1. `registerExternal()` 호출, `surveyAnswers: [{...}]` 포함 |
| **입력 데이터** | `surveyAnswers: [{questionId: 1, answer: "답변"}]` |
| **기대 결과** | 신청 성공, surveyAnswers 무시, `ExternalSurveyResponse` 저장 없음 |
| **비고** | Section 4-1 surveyAnswers. 설문 미연결 시 응답 무시 |

### 3-6. 정원 경계값 — 회원+외부인 혼합

#### TC-055: 외부인만 4명 + 정원 5 → 외부인 추가 신청 성공 (마지막 자리)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `capacity: 5`, `currentCount: 4` (외부인 4명), OPEN |
| **테스트 절차** | 1. 외부인 신청 호출<br>2. currentCount 확인 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | 신청 성공, `currentCount: 5` |
| **비고** | Section 4-4 |

#### TC-056: 회원 5명으로 정원 가득 → 외부인 신청 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `capacity: 5`, `currentCount: 5` (회원 5명), OPEN |
| **테스트 절차** | 1. 외부인 신청 호출 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | `EventCapacityFullException` (400) |
| **비고** | Section 4-4. 회원이 채워도 외부인 차단 |

---

## 4. allowExternal 동치 분할

### 4-2. 6가지 조합 테스트

#### TC-057: allowExternal=true + 외부인 → 201 Created

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=true, PUBLISHED, OPEN, 정원 여유 |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations/external` 호출 (인증 없음) |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | 201 Created |
| **비고** | Section 4-2 조합 1 |

#### TC-058: allowExternal=true + 준회원(ASSOCIATE) → 201 Created

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=true, PUBLISHED, OPEN, ASSOCIATE 사용자 로그인 |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations` 호출 (Bearer 토큰 포함) |
| **입력 데이터** | ASSOCIATE 사용자의 인증 토큰 |
| **기대 결과** | 201 Created (EXT-INV-05) |
| **비고** | Section 4-2 조합 2 |

#### TC-059: allowExternal=true + 정회원(MEMBER+) → 201 Created

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=true, PUBLISHED, OPEN, MEMBER 사용자 로그인 |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations` 호출 |
| **입력 데이터** | MEMBER 사용자의 인증 토큰 |
| **기대 결과** | 201 Created (기존과 동일) |
| **비고** | Section 4-2 조합 3. 기존 동작 회귀 검증 |

#### TC-060: allowExternal=false + 외부인 → 400 Bad Request

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=false, PUBLISHED, OPEN |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations/external` 호출 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | 400 Bad Request (EXT-INV-01) |
| **비고** | Section 4-2 조합 4 |

#### TC-061: allowExternal=false + 준회원(ASSOCIATE) → 403 Forbidden

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=false, PUBLISHED, OPEN, ASSOCIATE 사용자 로그인 |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations` 호출 |
| **입력 데이터** | ASSOCIATE 사용자의 인증 토큰 |
| **기대 결과** | 403 Forbidden (EXT-INV-05) |
| **비고** | Section 4-2 조합 5 |

#### TC-062: allowExternal=false + 정회원(MEMBER+) → 201 Created

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=false, PUBLISHED, OPEN, MEMBER 사용자 로그인 |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations` 호출 |
| **입력 데이터** | MEMBER 사용자의 인증 토큰 |
| **기대 결과** | 201 Created (기존과 동일, allowExternal 무관) |
| **비고** | Section 4-2 조합 6. 정회원 기존 동작 불변 |

---

## 5. 권한/보안 정책 (SEC-EXT)

#### TC-063: SEC-EXT-01 — 외부인이 allowExternal=false 행사에 신청 시 400

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=false, PUBLISHED, OPEN |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations/external` 호출 (인증 없음) |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | 400 Bad Request (서비스 레벨 EXT-INV-01 검증) |
| **비고** | SEC-EXT-01 |

#### TC-064: SEC-EXT-02 — 외부인 엔드포인트에 인증 토큰 없이 접근 시 정상 처리

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=true, PUBLISHED, OPEN, 정원 여유 |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations/external` 호출, Authorization 헤더 없음<br>2. 정상 응답 확인 |
| **입력 데이터** | 유효한 외부인 정보, 인증 토큰 없음 |
| **기대 결과** | 201 Created (인증 불필요, `security: []`) |
| **비고** | SEC-EXT-02. SecurityConfig 설정 검증 |

#### TC-065: SEC-EXT-03 — 준회원이 allowExternal=false 행사에 신청 시 403

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=false, ASSOCIATE 사용자 로그인 |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations` 호출 |
| **입력 데이터** | ASSOCIATE 인증 토큰 |
| **기대 결과** | 403 Forbidden (`AssociateMemberNotAllowedException`) |
| **비고** | SEC-EXT-03, EXT-INV-05 |

#### TC-066: SEC-EXT-04 — 준회원이 allowExternal=true 행사에 신청 시 201

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=true, ASSOCIATE 사용자 로그인, PUBLISHED, OPEN |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations` 호출 |
| **입력 데이터** | ASSOCIATE 인증 토큰 |
| **기대 결과** | 201 Created |
| **비고** | SEC-EXT-04, EXT-INV-05 |

#### TC-067: SEC-EXT-05 — 일반 회원(MEMBER)이 외부인 신청 취소 시 403

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | MEMBER 사용자 로그인, 외부인 신청 존재 |
| **테스트 절차** | 1. `POST /api/v1/registrations/{registrationId}/cancel` 호출 |
| **입력 데이터** | MEMBER 인증 토큰, `registrationId: {외부인 신청}` |
| **기대 결과** | 403 Forbidden |
| **비고** | SEC-EXT-05, EXT-INV-09 |

#### TC-068: SEC-EXT-06 — OPERATOR가 외부인 신청 취소 시 정상

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | OPERATOR 사용자 로그인, 외부인 신청(REGISTERED) 존재 |
| **테스트 절차** | 1. `POST /api/v1/registrations/{registrationId}/cancel` 호출 |
| **입력 데이터** | OPERATOR 인증 토큰, `registrationId: {외부인 신청}` |
| **기대 결과** | 200 OK, 취소 성공 |
| **비고** | SEC-EXT-06, EXT-INV-09 |

#### TC-069: SEC-EXT-07 — 외부인이 allowExternal=true + UNPUBLISHED 행사에 신청 시 404

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | allowExternal=true, `visibility == UNPUBLISHED` |
| **테스트 절차** | 1. `POST /api/v1/events/{eventId}/registrations/external` 호출 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | 404 Not Found (정보 은폐, 행사가 존재하지 않는 것처럼 처리) |
| **비고** | SEC-EXT-07, EXT-INV-08, EVT-INV-18 |

---

## 6. 동시성 테스트

#### TC-070: 회원 + 외부인이 마지막 1자리에 동시 신청

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 서비스 통합 (멀티스레드) |
| **사전 조건** | `capacity: 5`, `currentCount: 4`, allowExternal=true, OPEN |
| **테스트 절차** | 1. 2개 스레드에서 동시에 회원 `registerEvent()` + 외부인 `registerExternal()` 호출<br>2. 결과 확인 |
| **입력 데이터** | 스레드 1: MEMBER 사용자, 스레드 2: 외부인 정보 |
| **기대 결과** | 하나만 성공 (201), 나머지 `EventCapacityFullException` (400). 원자적 UPDATE 보장 |
| **비고** | Section 7-2. `incrementCurrentCountIfAvailable` 원자적 UPDATE |

#### TC-071: 2명의 외부인이 동일 studentId로 동시 신청

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 서비스 통합 (멀티스레드) |
| **사전 조건** | allowExternal=true, OPEN, 정원 여유 |
| **테스트 절차** | 1. 2개 스레드에서 동시에 동일 `studentId: "12345678"`로 `registerExternal()` 호출 |
| **입력 데이터** | 스레드 1, 2 모두 `studentId: "12345678"`, 다른 phone |
| **기대 결과** | DECISION-02: DB UNIQUE 없으므로 둘 다 성공할 수 있음 (극히 드문 중복 허용). 서비스 레벨 검증 통과 시 중복 레코드 생성 가능 |
| **비고** | Section 7-2, EXT-INV-02. 동시성 제어 한계 인지 테스트 |

#### TC-072: 2명의 외부인이 동일 phone으로 동시 신청

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 서비스 통합 (멀티스레드) |
| **사전 조건** | allowExternal=true, OPEN, 정원 여유 |
| **테스트 절차** | 1. 2개 스레드에서 동시에 동일 `phone: "01012345678"`로 `registerExternal()` 호출 |
| **입력 데이터** | 스레드 1, 2 모두 `phone: "01012345678"`, 다른 studentId |
| **기대 결과** | DECISION-02: DB UNIQUE 없으므로 둘 다 성공할 수 있음 (극히 드문 중복 허용) |
| **비고** | Section 7-2, EXT-INV-03 |

#### TC-073: 외부인 신청 + 관리자 취소 동시 발생

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 서비스 통합 (멀티스레드) |
| **사전 조건** | 외부인 신청(REGISTERED) 존재, allowExternal=true, OPEN |
| **테스트 절차** | 1. 스레드 1: 새 외부인 신청, 스레드 2: 기존 외부인 신청 취소 동시 실행 |
| **입력 데이터** | 스레드 1: 새 외부인 정보, 스레드 2: `registrationId: {기존 신청}` |
| **기대 결과** | 트랜잭션 격리에 의해 순차 처리. 두 작업 모두 성공하거나, currentCount 정합성 유지 |
| **비고** | Section 7-2 |

---

## 7. 관측 가능성

#### TC-074: 외부인 신청 성공 시 INFO 로그 출력

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | allowExternal=true, OPEN, 정원 여유 |
| **테스트 절차** | 1. `registerExternal()` 호출<br>2. 로그 출력 확인 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | INFO 로그: `외부인 행사 신청 완료 - eventId: {}, studentId: {}, registrationId: {}` |
| **비고** | Section 6-2 |

#### TC-075: 외부인 중복 신청 시 INFO 로그 출력 (studentId 중복)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 동일 행사에 동일 studentId로 활성 신청 존재 |
| **테스트 절차** | 1. 동일 studentId로 `registerExternal()` 호출<br>2. 로그 출력 확인 |
| **입력 데이터** | 중복 studentId |
| **기대 결과** | INFO 로그: `외부인 행사 신청 거부 - eventId: {}, studentId: {}, 사유: 학번 중복` |
| **비고** | Section 6-2 |

#### TC-076: allowExternal=false 행사에 외부인 신청 시 INFO 로그 출력

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | allowExternal=false |
| **테스트 절차** | 1. `registerExternal()` 호출<br>2. 로그 출력 확인 |
| **입력 데이터** | 유효한 외부인 정보 |
| **기대 결과** | INFO 로그: `외부인 행사 신청 거부 - eventId: {}, 사유: 외부인 신청 비허용 행사` |
| **비고** | Section 6-2 |

#### TC-077: 외부인 신청의 감사 추적 — createdBy null, 관리자 취소 시 updatedBy 기록

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 외부인 신청 완료 상태 |
| **테스트 절차** | 1. 외부인 신청 레코드의 `createdBy`, `updatedBy` 확인<br>2. 관리자 취소 후 `updatedBy` 확인 |
| **입력 데이터** | 외부인 신청 후 관리자 취소 |
| **기대 결과** | 신청 시: `createdBy == null` (비인증 요청), 관리자 취소 후: `updatedBy == {operatorUserId}` |
| **비고** | Section 6-3. SecurityAuditorAware에서 비인증 시 null 반환 |

---

## 8. 기존 불변조건 회귀 테스트

#### TC-078: REG-INV-04 변경 회귀 — allowExternal=false 행사에서 MEMBER 신청 기존 동작 유지

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.allowExternal == false`, MEMBER 사용자 로그인, PUBLISHED, OPEN |
| **테스트 절차** | 1. 기존 `registerEvent()` 호출<br>2. 정상 신청 확인 |
| **입력 데이터** | MEMBER 사용자, allowExternal=false 행사 |
| **기대 결과** | 201 Created (allowExternal 값과 무관하게 MEMBER+ 정상 신청) |
| **비고** | REG-INV-04 변경(Section 0-1)으로 인한 기존 동작 회귀 검증 |

#### TC-079: SEC-REG-01 변경 회귀 — allowExternal=false + ASSOCIATE 시 기존 403 동작 유지

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.allowExternal == false`, ASSOCIATE 사용자 로그인 |
| **테스트 절차** | 1. `registerEvent()` 호출<br>2. 403 에러 확인 |
| **입력 데이터** | ASSOCIATE 사용자, allowExternal=false 행사 |
| **기대 결과** | 403 Forbidden (`AssociateMemberNotAllowedException`) |
| **비고** | SEC-REG-01 변경(Section 0-2) 회귀 검증. 기존 동작이 allowExternal=false에서 유지되는지 확인 |

#### TC-080: RegistrationListResponse 스키마 변경 — 외부인 신청 포함 시 응답 필드 검증 [확인 필요]

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 회원 신청 1건 + 외부인 신청 1건 존재, OPERATOR 로그인 |
| **테스트 절차** | 1. 신청자 목록 조회 API 호출<br>2. 응답의 회원 신청 레코드 필드 확인<br>3. 응답의 외부인 신청 레코드 필드 확인 |
| **입력 데이터** | `eventId: {회원+외부인 혼합 신청 행사}` |
| **기대 결과** | 회원 신청: `userId != null`, `isExternal == false`, `phone == null`<br>외부인 신청: `userId == null`, `userEmail == null`, `userGender == null`, `userGrade == null`, `isExternal == true`, `phone != null` |
| **비고** | Section 0-3 스키마 변경 검증. [확인 필요] 신청자 목록 조회 API 엔드포인트 확인 필요 |

---

## 커버리지 매트릭스

| 검증 항목 | 테스트 케이스 | 커버리지 |
|----------|-------------|:---:|
| EXT-INV-01: allowExternal 검사 | TC-001, TC-002, TC-060, TC-063 | 완전 |
| EXT-INV-02: studentId 중복 방지 | TC-003, TC-004, TC-007, TC-008, TC-071 | 완전 |
| EXT-INV-03: phone 중복 방지 | TC-005, TC-006, TC-007, TC-072 | 완전 |
| EXT-INV-04: 정원 공유 | TC-009, TC-010, TC-011, TC-055, TC-056, TC-070 | 완전 |
| EXT-INV-05: 준회원 조건부 허용 | TC-012, TC-013, TC-058, TC-061, TC-065, TC-066 | 완전 |
| EXT-INV-06: allowExternal 기본값 | TC-014, TC-015 | 완전 |
| EXT-INV-07: OPEN + 기간 내 검증 | TC-016, TC-017 | 완전 |
| EXT-INV-08: UNPUBLISHED 차단 | TC-018, TC-069 | 완전 |
| EXT-INV-09: 관리자만 취소 | TC-019, TC-020, TC-021, TC-067, TC-068 | 완전 |
| EXT-INV-10: 필수 필드 검증 | TC-022 ~ TC-026 | 완전 |
| EXT-INV-11: 설문 연동 | TC-027, TC-028, TC-053, TC-054 | 완전 |
| EXT-INV-12: 동일 학번 가입 회원 | TC-029, TC-030 | 완전 |
| SEC-EXT-01 ~ SEC-EXT-07 | TC-063 ~ TC-069 | 완전 |
| Section 2 FSM | TC-031 ~ TC-040 | 완전 |
| Section 4-1 BVA | TC-041 ~ TC-056 | 완전 |
| Section 4-2 동치 분할 | TC-057 ~ TC-062 | 완전 |
| Section 7-2 동시성 | TC-070 ~ TC-073 | 완전 |
| Section 6 관측 가능성 | TC-074 ~ TC-077 | 완전 |
| Section 0 기존 불변조건 회귀 | TC-078 ~ TC-080 | 완전 |
