# 설문 연동 행사 신청 (Survey-Event Registration) 테스트 케이스

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-03-02 |
| 검증 기준 문서 | `docs/criteria/event/survey-event-registration-verification-criteria.md` |
| 대상 기능 | 행사-설문 연결(Event-Survey Linking), 설문 필수 행사 신청(Survey-Required Registration), 설문 없는 행사 신청(Direct Registration), 상태 교차 제약(Cross-Domain State Constraints) |
| 테스트 케이스 수 | 총 72개 |

## 카테고리 요약

| # | 카테고리 | 테스트 케이스 수 | 커버리지 대상 |
|---|---------|:---:|-------------|
| 1 | 도메인 규칙과 불변조건 | 25 | SEVT-INV-01 ~ SEVT-INV-13 |
| 2 | 상태 교차 매트릭스 | 10 | 2-1-1 행사x설문 매트릭스, 2-2 설문 상태 변경 영향, 2-3 행사 상태 변경 영향 |
| 3 | 입력 경계값 | 14 | 4-1 ~ 4-5 (생성 시 연결, 신청 시점, 연결 변경, 다중 연결, 재신청) |
| 4 | 권한/보안 정책 | 6 | SEC-SEVT-02 ~ SEC-SEVT-06 |
| 5 | 대표 시나리오 (정상/실패/엣지) | 14 | S1~S5, F1~F8, E1~E6 (중복 제외) |
| 6 | 관측 가능성 | 3 | 로그 메시지, 감사 이력 |

---

## 1. 도메인 규칙과 불변조건

### SEVT-INV-01: 행사-설문 연결은 선택 사항

#### TC-001: 설문 미연결 행사 생성 시 surveyId가 null로 설정됨

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `Event.create()` 호출 시 `surveyId` 파라미터를 null로 전달<br>2. 생성된 Event 객체의 `surveyId` 확인 |
| **입력 데이터** | `surveyId: null`, 기타 필수 필드(title, description 등) 유효값 |
| **기대 결과** | `event.getSurveyId() == null` |
| **비고** | SEVT-INV-01. 설문 미연결은 기본 동작 |

#### TC-002: 설문 미연결 행사에 기존 행사 신청 로직이 변경 없이 동작함 (회귀 테스트)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == null`인 행사가 PUBLISHED + OPEN + 신청 기간 내 상태, MEMBER 이상 사용자, 중복 신청 없음 |
| **테스트 절차** | 1. `EventRegistrationService.registerEvent()` 호출<br>2. 설문 관련 검증(SurveyRepository, SurveyResponseRepository) 호출이 발생하지 않았는지 verify<br>3. 신청 결과 확인 |
| **입력 데이터** | `eventId: {surveyId == null인 행사}`, `userId: {MEMBER 사용자}` |
| **기대 결과** | 신청 성공, SurveyRepository/SurveyResponseRepository에 대한 호출 0회 |
| **비고** | SEVT-INV-01, SEVT-INV-07, SEVT-INV-12. 설문 연동 코드 추가로 인한 기존 로직 변경 없음을 검증 |

### SEVT-INV-02: 행사당 설문은 최대 1개

#### TC-003: 행사 생성 시 단일 surveyId만 설정됨

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `Event.create()` 호출 시 유효한 `surveyId` 전달<br>2. 생성된 Event 객체의 `surveyId` 확인 |
| **입력 데이터** | `surveyId: 100L`, 기타 필수 필드 유효값 |
| **기대 결과** | `event.getSurveyId() == 100L`, 단일 값만 저장됨 |
| **비고** | SEVT-INV-02. DB 컬럼 `event_survey_id`는 nullable Long (단일 값) |

### SEVT-INV-03: 설문은 여러 행사에 연결 가능 (재사용)

#### TC-004: 동일 설문을 두 행사에 연결 시 모두 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 활성 상태의 설문(surveyId: 100) 존재 (Mock 설정) |
| **테스트 절차** | 1. 행사 A 생성 시 `surveyId: 100` 설정<br>2. 행사 B 생성 시 `surveyId: 100` 설정<br>3. 두 행사 모두 정상 생성 확인 |
| **입력 데이터** | 행사 A: `surveyId: 100`, 행사 B: `surveyId: 100` |
| **기대 결과** | 두 행사 모두 생성 성공, 각각 `event.getSurveyId() == 100` |
| **비고** | SEVT-INV-03. 설문:행사 = 1:N 관계 |

### SEVT-INV-04: 연결 대상 설문의 존재 검증

#### TC-005: 존재하는 활성 설문 ID로 행사 생성 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `Survey(id=100, deleted=false, trashedAt=null)` Mock 반환 설정 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, `surveyId: 100`<br>2. 설문 존재/활성 검증 통과 확인<br>3. 행사 생성 결과 확인 |
| **입력 데이터** | `surveyId: 100` + 기타 필수 필드 |
| **기대 결과** | 행사 생성 성공, `event.surveyId == 100` |
| **비고** | SEVT-INV-04 |

#### TC-006: 존재하지 않는 설문 ID로 행사 생성 시 SurveyNotFoundException

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `SurveyRepository.findById(99999)` → `Optional.empty()` Mock 설정 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, `surveyId: 99999` |
| **입력 데이터** | `surveyId: 99999` (존재하지 않는 ID) |
| **기대 결과** | `SurveyNotFoundException` 발생 |
| **비고** | SEVT-INV-04, 시나리오 F4 |

#### TC-007: 삭제된 설문 ID로 행사 생성 시 SurveyNotFoundException

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `Survey(id=200, deleted=true)` Mock 반환 설정 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, `surveyId: 200` |
| **입력 데이터** | `surveyId: 200` (영구 삭제된 설문) |
| **기대 결과** | `SurveyNotFoundException` 발생 |
| **비고** | SEVT-INV-04, 시나리오 F5 |

#### TC-008: 휴지통 설문 ID로 행사 생성 시 SurveyNotFoundException

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `Survey(id=300, deleted=false, trashedAt=2026-03-01T00:00:00Z)` Mock 반환 설정 |
| **테스트 절차** | 1. `EventService.createEvent()` 호출, `surveyId: 300` |
| **입력 데이터** | `surveyId: 300` (휴지통에 있는 설문) |
| **기대 결과** | `SurveyNotFoundException` 발생 |
| **비고** | SEVT-INV-04 |

### SEVT-INV-05: 설문 연결은 행사 생성 시 또는 수정 시 설정

#### TC-009: UPCOMING 상태 행사에서 설문 연결 변경 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | `event.eventStatus == UPCOMING`인 행사 |
| **테스트 절차** | 1. `Event.update()` 호출, `surveyId` 변경(null → 100)<br>2. 변경 결과 확인 |
| **입력 데이터** | 기존 `surveyId: null` → 신규 `surveyId: 100` |
| **기대 결과** | `event.getSurveyId() == 100`, 변경 성공 |
| **비고** | SEVT-INV-05, UPCOMING 상태 수정 가능 |

#### TC-010: ONGOING 상태 행사에서 설문 연결 변경 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | `event.eventStatus == ONGOING`인 행사, 기존 `surveyId: 100` |
| **테스트 절차** | 1. `Event.update()` 호출, `surveyId` 변경(100 → 200)<br>2. 변경 결과 확인 |
| **입력 데이터** | 기존 `surveyId: 100` → 신규 `surveyId: 200` |
| **기대 결과** | `event.getSurveyId() == 200`, 변경 성공 |
| **비고** | SEVT-INV-05, ONGOING 상태에서도 정보성 필드에 준하여 변경 가능 |

#### TC-011: CANCELED 상태 행사에서 설문 해제 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | `event.eventStatus == CANCELED`인 행사, 기존 `surveyId: 100` |
| **테스트 절차** | 1. `Event.update()` 호출, `surveyId: null`로 변경 |
| **입력 데이터** | 기존 `surveyId: 100` → 신규 `surveyId: null` |
| **기대 결과** | `event.getSurveyId() == null`, 해제 성공 |
| **비고** | SEVT-INV-05, CANCELED 상태 수정 가능 |

#### TC-012: COMPLETED 상태 행사에서 설문 연결 변경 시 수정 불가 예외

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 단위 |
| **사전 조건** | `event.eventStatus == COMPLETED`인 행사 |
| **테스트 절차** | 1. `Event.update()` 호출, `surveyId` 변경 시도 |
| **입력 데이터** | 기존 `surveyId: null` → 신규 `surveyId: 100` |
| **기대 결과** | 수정 불가 예외 발생 (EVT-INV-07에 의한 COMPLETED 상태 수정 차단) |
| **비고** | SEVT-INV-05 |

### SEVT-INV-06: 설문 연결 행사의 신청 -- 통합 API를 통한 원자적 처리

#### TC-013: 설문 연결 행사 신청 시 surveyAnswers 포함 -- 설문 응답 저장 + 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`인 행사, PUBLISHED + OPEN + 신청 기간 내 상태, 설문 responseStatus == OPEN, 사용자가 해당 설문에 미응답, MEMBER 이상 사용자 |
| **테스트 절차** | 1. `EventRegistrationService.registerEventWithSurvey()` 호출, `surveyAnswers` 포함<br>2. SurveyResponse 저장 호출 verify<br>3. EventRegistration 생성 확인 |
| **입력 데이터** | `eventId`, `userId`, `surveyAnswers: [{questionId: 1, answer: "답변1"}, ...]` |
| **기대 결과** | 설문 응답 저장 + 행사 신청 모두 성공 |
| **비고** | SEVT-INV-06, 시나리오 S1. 핵심 Happy Path |

#### TC-014: 기존 설문 응답 존재 시 surveyAnswers 생략 가능 -- 기존 응답으로 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`, 사용자가 해당 설문에 이미 응답 완료(`existsBySurveyIdAndUserId` → true), 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers: null`<br>2. 기존 응답 존재 확인 후 신청 진행 확인<br>3. 새로운 SurveyResponse 저장이 호출되지 않았는지 verify |
| **입력 데이터** | `eventId`, `userId`, `surveyAnswers: null` |
| **기대 결과** | 행사 신청 성공, 새 설문 응답 저장 호출 0회 |
| **비고** | SEVT-INV-06, 시나리오 S3. 이미 응답 존재 시 생략 가능 |

#### TC-015: 기존 응답 존재 + surveyAnswers 포함 시 -- surveyAnswers 무시하고 기존 응답으로 신청

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`, 사용자가 해당 설문에 이미 응답 완료, 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers` 포함<br>2. 새로운 SurveyResponse 저장이 호출되지 않았는지 verify<br>3. 신청 성공 확인 |
| **입력 데이터** | `eventId`, `userId`, `surveyAnswers: [{questionId: 1, answer: "새 답변"}]` |
| **기대 결과** | 행사 신청 성공, surveyAnswers 무시됨 (새 응답 저장 0회), 기존 응답으로 진행 |
| **비고** | SEVT-INV-06. 응답 수정은 설문 단독 수정 API(PUT /surveys/{surveyId}/responses/me, INV-26)로만 가능 |

#### TC-016: 설문 응답 미존재 + surveyAnswers 미포함 시 SurveyResponseRequiredException

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`, 사용자 미응답(`existsBySurveyIdAndUserId` → false), 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers: null` |
| **입력 데이터** | `eventId`, `userId`, `surveyAnswers: null` |
| **기대 결과** | `SurveyResponseRequiredException` 발생 |
| **비고** | SEVT-INV-06, 시나리오 F1 |

### SEVT-INV-07: 설문 미연결 행사의 기존 동작 보존

#### TC-017: 설문 미연결 행사에 surveyAnswers 없이 기존 방식으로 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | `event.surveyId == null`인 행사, PUBLISHED + OPEN + 기간 내, MEMBER 이상 사용자, DB에 행사/사용자 데이터 존재 |
| **테스트 절차** | 1. `POST /api/events/{eventId}/registrations` 호출 (surveyAnswers 미포함)<br>2. 신청 결과 확인 |
| **입력 데이터** | `eventId: {surveyId == null인 행사}` |
| **기대 결과** | HTTP 200/201 응답, 행사 신청 성공 |
| **비고** | SEVT-INV-07, SEVT-INV-12. 시나리오 S2. 기존 행사 신청 회귀 테스트 |

### SEVT-INV-08: 통합 API의 원자성과 롤백 보장

#### TC-018: 설문 응답 저장 성공 후 정원 초과로 신청 실패 시 설문 응답도 롤백

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | `event.surveyId != null`, 행사 정원이 이미 가득 참 (선착순), 설문 OPEN, 사용자 미응답, DB에 행사/사용자/설문 데이터 존재 |
| **테스트 절차** | 1. 설문 응답 + 행사 신청 통합 API 호출<br>2. `EventCapacityFullException` 발생 확인<br>3. DB에서 SurveyResponse가 롤백되어 존재하지 않는지 확인 |
| **입력 데이터** | `eventId`, `userId`, `surveyAnswers: [...]` |
| **기대 결과** | `EventCapacityFullException` 발생, SurveyResponse DB 레코드 없음 (롤백됨), EventRegistration 없음 |
| **비고** | SEVT-INV-08, 시나리오 F6. 고아 데이터 방지 핵심 검증 |

#### TC-019: 설문 응답 유효성 검증 실패 시 행사 신청도 미수행

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId != null`, 설문 OPEN, 사용자 미응답 |
| **테스트 절차** | 1. 유효하지 않은 `surveyAnswers`(필수 질문 답변 누락)로 통합 API 호출<br>2. 설문 유효성 검증 실패 예외 확인<br>3. EventRegistration 생성 호출이 발생하지 않았는지 verify |
| **입력 데이터** | `surveyAnswers: [{questionId: 1, answer: null}]` (필수 답변 누락) |
| **기대 결과** | 설문 도메인 validation 예외 발생, EventRegistration 생성 0회 |
| **비고** | SEVT-INV-08. 설문 응답 유효성 실패가 행사 신청 전에 차단됨 |

### SEVT-INV-09: 설문 응답 수정이 기존 신청에 영향 없음

#### TC-020: 신청 완료 후 설문 응답 수정(PUT) 시 기존 신청 상태 유지

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 사용자가 설문 응답 + 행사 신청 완료 (REGISTERED 또는 WAITING 상태) |
| **테스트 절차** | 1. 설문 응답 수정 API 호출 시뮬레이션 (SurveyResponse 업데이트)<br>2. 기존 EventRegistration의 상태 확인 |
| **입력 데이터** | 수정된 설문 응답 |
| **기대 결과** | EventRegistration 상태 변경 없음 (REGISTERED/WAITING 유지) |
| **비고** | SEVT-INV-09. 설문 응답은 전제조건(gate)이지 지속조건(invariant)이 아님 |

### SEVT-INV-10: 신청 시점의 설문 상태 검증

#### TC-021: 설문 responseStatus == NOT_STARTED인 행사 신청 시 SurveyNotReadyException

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`, `survey.responseStatus == NOT_STARTED`, 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출 |
| **입력 데이터** | `eventId`, `userId`, `surveyAnswers: [...]` |
| **기대 결과** | `SurveyNotReadyException` 발생 |
| **비고** | SEVT-INV-10, 시나리오 F2. 설문 미시작 시 신청 차단 |

#### TC-022: 설문 responseStatus == OPEN이고 응답 존재 시 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `survey.responseStatus == OPEN`, `survey.trashedAt == null`, `survey.deleted == false`, 사용자 응답 존재 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers: null` (기존 응답 재사용) |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | 행사 신청 성공 |
| **비고** | SEVT-INV-10, 정상 매트릭스 (PUBLISHED+OPEN+OPEN) |

#### TC-023: 설문 responseStatus == CLOSED이고 기존 응답 존재 시 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `survey.responseStatus == CLOSED`, `survey.trashedAt == null`, `survey.deleted == false`, 사용자 기존 응답 존재 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers: null` |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | 행사 신청 성공 |
| **비고** | SEVT-INV-10, 시나리오 S3. 설문 마감 ≠ 행사 마감 |

### SEVT-INV-11: 설문 삭제/휴지통 시 행사 신청 정책

#### TC-024: 설문이 휴지통에 있을 때 신규 신청 차단

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`, `survey.trashedAt != null`, 사용자 기존 응답 존재, 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출 |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | `SurveyNotFoundException` 또는 `SurveyNotReadyException` 발생 |
| **비고** | SEVT-INV-11, 시나리오 F3 |

#### TC-025: 설문이 영구 삭제된 상태에서 신규 신청 차단

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`, `survey.deleted == true`, 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출 |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | `SurveyNotFoundException` 발생 |
| **비고** | SEVT-INV-11 |

---

## 2. 상태 교차 매트릭스

### 2-1-1. 행사 상태 x 설문 상태 교차 매트릭스

#### TC-026: UNPUBLISHED 행사 신청 시 404 (공개 API 접근 차단)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | `event.visibility == UNPUBLISHED`, 설문 상태 무관, MEMBER 이상 사용자 |
| **테스트 절차** | 1. `POST /api/events/{eventId}/registrations` 호출 |
| **입력 데이터** | `eventId: {UNPUBLISHED 행사}` |
| **기대 결과** | HTTP 404 응답 (비공개 행사는 공개 API에서 조회 불가, EVT-INV-18) |
| **비고** | 매트릭스 2-1-1 첫 번째 행. 신청 로직 도달 이전에 차단 |

#### TC-027: PUBLISHED + NOT_STARTED(registrationStatus) 행사 신청 시 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.visibility == PUBLISHED`, `event.registrationStatus == NOT_STARTED`, 설문 상태 무관 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출 |
| **입력 데이터** | `eventId: {registrationStatus == NOT_STARTED인 행사}` |
| **기대 결과** | 등록 미시작 관련 예외 발생 (REG-INV-05) |
| **비고** | 매트릭스 2-1-1, 행사 등록 미시작 |

#### TC-028: PUBLISHED + OPEN + 설문 NOT_STARTED 행사 신청 시 SurveyNotReadyException

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 행사 PUBLISHED + OPEN + 기간 내, `survey.responseStatus == NOT_STARTED` |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출 |
| **입력 데이터** | `eventId`, `userId`, `surveyAnswers: [...]` |
| **기대 결과** | `SurveyNotReadyException` 발생 |
| **비고** | 매트릭스 2-1-1. TC-021과 동일 시나리오이나 매트릭스 관점 검증 |

#### TC-029: PUBLISHED + OPEN + 설문 OPEN + 응답 존재 시 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 행사 PUBLISHED + OPEN + 기간 내, `survey.responseStatus == OPEN`, 사용자 응답 존재 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers: null` |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | 행사 신청 성공 |
| **비고** | 매트릭스 2-1-1, 정상 시나리오 |

#### TC-030: PUBLISHED + OPEN + 설문 CLOSED + 기존 응답 존재 시 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 행사 PUBLISHED + OPEN + 기간 내, `survey.responseStatus == CLOSED`, 사용자 기존 응답 존재 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers: null` |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | 행사 신청 성공 |
| **비고** | 매트릭스 2-1-1, 설문 마감 후 기존 응답으로 신청 |

#### TC-031: PUBLISHED + CLOSED(registrationStatus) 행사 신청 시 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.registrationStatus == CLOSED`, 설문 상태 무관 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출 |
| **입력 데이터** | `eventId: {registrationStatus == CLOSED인 행사}` |
| **기대 결과** | 등록 마감 관련 예외 발생 (REG-INV-05) |
| **비고** | 매트릭스 2-1-1, 행사 등록 마감 |

### 2-2. 설문 상태 변경이 행사에 미치는 영향

#### TC-032: 설문 OPEN -> CLOSED 전환 후 기존 신청 유효 유지

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 사용자가 설문 OPEN 상태에서 응답 + 행사 신청 완료, DB에 EventRegistration(isActive==true) 존재 |
| **테스트 절차** | 1. 설문 responseStatus를 CLOSED로 변경<br>2. 기존 EventRegistration 상태 조회 |
| **입력 데이터** | 설문 상태 변경: OPEN → CLOSED |
| **기대 결과** | EventRegistration 상태 변경 없음 (REGISTERED/WAITING 유지) |
| **비고** | 2-2 테이블, 설문 마감이 기존 신청에 무영향 |

#### TC-033: 설문 PUBLISHED -> UNPUBLISHED 전환 후 기존 응답으로 신규 신청 가능

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 설문이 UNPUBLISHED 전환됨 (INV-20에 의해 responseStatus 자동 CLOSED), 사용자 기존 응답 존재, 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers: null` |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | 행사 신청 성공 (UNPUBLISHED + CLOSED 상태에서 기존 응답으로 신청 가능) |
| **비고** | 2-2 테이블, 시나리오 E5, SEVT-INV-10 |

### 2-3. 행사 상태 변경이 설문에 미치는 영향

#### TC-034: 행사 취소(CANCELED) 후 연결된 설문 상태 변경 없음

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`, 설문 PUBLISHED + OPEN 상태 |
| **테스트 절차** | 1. 행사를 CANCELED로 상태 변경<br>2. Survey(id=100)의 상태 확인 |
| **입력 데이터** | 행사 취소 API 호출 |
| **기대 결과** | Survey(id=100)의 visibility, responseStatus 변경 없음 (PUBLISHED + OPEN 유지) |
| **비고** | 2-3 테이블. 양 도메인은 단방향 의존(행사 → 설문) |

#### TC-035: 행사 registrationStatus OPEN -> CLOSED 전환 후 연결된 설문 상태 변경 없음

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`, 설문 PUBLISHED + OPEN 상태 |
| **테스트 절차** | 1. 행사 registrationStatus를 CLOSED로 변경<br>2. Survey(id=100)의 상태 확인 |
| **입력 데이터** | 행사 등록 마감 처리 |
| **기대 결과** | Survey 상태 변경 없음 |
| **비고** | 2-3 테이블. 설문은 행사의 존재를 모름 |

---

## 3. 입력 경계값

### 4-1. 행사 생성 시 설문 연결 입력값

#### TC-036: surveyId == null로 행사 생성 (설문 미연결)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | OPERATOR 이상 사용자 |
| **테스트 절차** | 1. `POST /api/events` 호출, `surveyId: null` (또는 필드 생략) |
| **입력 데이터** | `{ "title": "테스트 행사", "surveyId": null, ... }` |
| **기대 결과** | HTTP 201, 행사 생성 성공, `event.surveyId == null` |
| **비고** | 4-1 유효 동치류: null (미연결) |

#### TC-037: surveyId == 유효 ID로 행사 생성 (설문 연결)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | OPERATOR 이상 사용자, 활성 설문(id=100) DB 존재 |
| **테스트 절차** | 1. `POST /api/events` 호출, `surveyId: 100` |
| **입력 데이터** | `{ "title": "테스트 행사", "surveyId": 100, ... }` |
| **기대 결과** | HTTP 201, 행사 생성 성공, `event.surveyId == 100` |
| **비고** | 4-1 유효 동치류: 존재하는 활성 설문 ID |

### 4-2. 설문 연결 행사 신청 시점 경계값

#### TC-038: 응답 미제출 + 설문 OPEN + surveyAnswers 포함 시 신규 응답 저장 후 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 설문 OPEN, 사용자 미응답, 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers` 포함 |
| **입력 데이터** | `surveyAnswers: [{questionId: 1, answer: "답변"}]` |
| **기대 결과** | 설문 응답 저장 + 행사 신청 성공 |
| **비고** | 4-2 테이블 1행: 응답 미존재 + OPEN + answers 포함 → 성공 |

#### TC-039: 응답 미제출 + 설문 CLOSED + surveyAnswers 미포함 시 SurveyResponseRequiredException

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 설문 CLOSED, 사용자 미응답, 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers: null` |
| **입력 데이터** | `eventId`, `userId`, `surveyAnswers: null` |
| **기대 결과** | `SurveyResponseRequiredException` 발생 |
| **비고** | 4-2 테이블: 응답 미존재 + CLOSED → 실패 (새 응답 제출 불가 + 기존 응답 없음) |

#### TC-040: 응답 제출 완료 + 설문 휴지통 이동 시 신규 신청 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 설문 trashedAt != null, 사용자 기존 응답 존재, 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출 |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | `SurveyNotFoundException` 발생 |
| **비고** | 4-2 테이블: 응답 존재 + 휴지통 → 실패 |

### 4-3. 설문 연결 변경 경계값

#### TC-041: surveyId null → 유효 ID 변경 (처음 연결, 기존 활성 신청자 있음)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == null`, 활성 신청자(WAITING/REGISTERED/APPROVED) 존재, 설문(id=100) 활성 상태 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `surveyId: 100`<br>2. 기존 신청자의 EventRegistration 상태 확인 |
| **입력 데이터** | 기존 `surveyId: null` → 신규 `surveyId: 100` |
| **기대 결과** | 설문 연결 변경 성공, 기존 활성 신청 유지 (DECISION-01 확정: 허용) |
| **비고** | 4-3 테이블 2행. DECISION-01 확정 정책 검증 |

#### TC-042: surveyId ID-A → ID-B 변경 (설문 교체, 기존 활성 신청자 있음)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`, 활성 신청자 존재(설문 100 응답 기반), 설문(id=200) 활성 상태 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `surveyId: 200`<br>2. 기존 신청자의 EventRegistration 상태 확인 |
| **입력 데이터** | 기존 `surveyId: 100` → 신규 `surveyId: 200` |
| **기대 결과** | 설문 변경 성공, 기존 활성 신청 유지 (DECISION-01 확정) |
| **비고** | 4-3 테이블 4행 |

#### TC-043: surveyId 유효 ID → null 변경 (설문 해제)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == 100`, 활성 신청자 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `surveyId: null`<br>2. 기존 신청자의 EventRegistration 상태 확인<br>3. 이후 새 신청자가 설문 없이 신청 가능한지 확인 |
| **입력 데이터** | 기존 `surveyId: 100` → 신규 `surveyId: null` |
| **기대 결과** | 설문 해제 성공, 기존 활성 신청 유지, 이후 신청자는 설문 없이 신청 가능 |
| **비고** | 4-3 테이블 5행, 시나리오 S5 |

#### TC-044: 삭제된 설문 ID로 설문 변경 시 SurveyNotFoundException

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId == null`, 설문(id=300, deleted=true) 존재 |
| **테스트 절차** | 1. `EventService.updateEvent()` 호출, `surveyId: 300` |
| **입력 데이터** | `surveyId: 300` (삭제된 설문) |
| **기대 결과** | `SurveyNotFoundException` 발생 |
| **비고** | 4-3 테이블 7행 |

### 4-4. 동일 설문 다중 행사 연결 경계값

#### TC-045: 동일 설문 연결 두 행사에 응답 1회로 양쪽 신청 성공 (시간 미겹침)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 행사 A, B 모두 `surveyId == 100`, 시간 미겹침, 사용자가 설문 100에 1회 응답 완료 |
| **테스트 절차** | 1. 행사 A `registerEventWithSurvey()` 호출, `surveyAnswers: null`<br>2. 행사 B `registerEventWithSurvey()` 호출, `surveyAnswers: null` |
| **입력 데이터** | 행사 A eventId, 행사 B eventId, 동일 userId |
| **기대 결과** | 행사 A 신청 성공, 행사 B 신청 성공 |
| **비고** | 4-4 테이블, SEVT-INV-03, 시나리오 S4 |

#### TC-046: 동일 설문 연결 두 행사 중 시간 겹침 시 두 번째 신청 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 행사 A, B 모두 `surveyId == 100`, 시간 겹침(REG-INV-06), 사용자가 행사 A 신청 완료 |
| **테스트 절차** | 1. 행사 B `registerEventWithSurvey()` 호출 |
| **입력 데이터** | 행사 B eventId, userId |
| **기대 결과** | 시간 겹침 관련 예외 발생 (REG-INV-06) |
| **비고** | 4-4 테이블 2행. 설문 조건과 무관하게 기존 행사 불변조건(REG-INV-06) 적용 |

### 4-5. 재신청(reRegister) 시 설문 검증 경계값

#### TC-047: 취소 후 재신청 시 설문 응답 존재 확인 후 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 사용자가 행사 신청 후 취소(CANCELED 상태), `event.surveyId != null`, 사용자 설문 응답 존재 |
| **테스트 절차** | 1. `EventRegistrationService.reRegister()` 호출 |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | 재신청 성공 (기존 응답 존재 확인, REG-INV-11 + SEVT-INV-06) |
| **비고** | 4-5 테이블 1행 |

#### TC-048: 취소 후 재신청 시 설문 응답 미존재 시 SurveyResponseRequiredException

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 사용자가 행사 신청 후 취소, `event.surveyId != null`, 사용자 설문 응답 미존재 (이론적 시나리오) |
| **테스트 절차** | 1. `reRegister()` 호출 |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | `SurveyResponseRequiredException` 발생 |
| **비고** | 4-5 테이블 2행 |

#### TC-049: 취소 후 재신청 시 설문이 해제된 경우 설문 검증 생략하고 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 사용자가 설문 응답 + 행사 신청 완료 후 취소(CANCELED), 이후 운영진이 설문 해제(`surveyId = null`)로 수정 |
| **테스트 절차** | 1. `reRegister()` 호출 |
| **입력 데이터** | `eventId: {surveyId == null}`, `userId` |
| **기대 결과** | 재신청 성공, 설문 검증 생략 (surveyId == null이므로 기존 로직으로 진행) |
| **비고** | 4-5 테이블 3행, SEVT-INV-06 기준 시점: 항상 현재 event.surveyId 기준 |

---

## 4. 권한/보안 정책

### SEC-SEVT-02: 준회원이 설문 연결 행사 신청 차단

#### TC-050: ASSOCIATE 사용자가 설문 연결 행사 신청 시 403

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | ASSOCIATE 권한 사용자 로그인, `event.surveyId != null`인 행사 OPEN 상태 |
| **테스트 절차** | 1. ASSOCIATE JWT로 `POST /api/events/{eventId}/registrations` 호출, `surveyAnswers` 포함 |
| **입력 데이터** | `eventId`, `surveyAnswers: [...]` |
| **기대 결과** | HTTP 403 Forbidden (`AssociateMemberNotAllowedException`) |
| **비고** | SEC-SEVT-02, 기존 REG-INV-04 적용 |

### SEC-SEVT-03: 일반 회원이 설문 연결 포함 행사 생성 차단

#### TC-051: MEMBER 사용자가 설문 연결 포함 행사 생성 시 403

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | MEMBER 권한 사용자 로그인 |
| **테스트 절차** | 1. MEMBER JWT로 `POST /api/events` 호출, `surveyId: 100` 포함 |
| **입력 데이터** | `{ "title": "테스트", "surveyId": 100, ... }` |
| **기대 결과** | HTTP 403 Forbidden (`EventAccessDeniedException`) |
| **비고** | SEC-SEVT-03, 기존 SEC-EVT-02 적용. OPERATOR 이상만 행사 생성 가능 |

### SEC-SEVT-04: 설문 응답 없이 설문 연결 행사 신청

#### TC-052: 설문 응답 미존재 상태에서 설문 연결 행사 신청 시 400

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | MEMBER 이상 사용자 로그인, `event.surveyId != null`, 설문 OPEN, 사용자 미응답, DB에 행사/사용자/설문 데이터 존재 |
| **테스트 절차** | 1. `POST /api/events/{eventId}/registrations` 호출, `surveyAnswers` 미포함 |
| **입력 데이터** | `eventId` (surveyAnswers 없음) |
| **기대 결과** | HTTP 400, `SurveyResponseRequiredException` |
| **비고** | SEC-SEVT-04 |

### SEC-SEVT-05: 설문 accessLevel 부족으로 응답 불가 -> 행사 신청 시도

#### TC-053: 설문 accessLevel(OPERATOR) 부족 사용자(MEMBER)가 행사 신청 시 응답 부재로 차단

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 설문 accessLevel == OPERATOR, MEMBER 사용자가 설문 응답 미보유 (설문 단독 API에서 응답 거부됨), `event.surveyId != null` |
| **테스트 절차** | 1. MEMBER 사용자로 `registerEventWithSurvey()` 호출, `surveyAnswers: null` |
| **입력 데이터** | `eventId`, `userId: {MEMBER}`, `surveyAnswers: null` |
| **기대 결과** | `SurveyResponseRequiredException` 발생 (설문 응답 미존재로 차단) |
| **비고** | SEC-SEVT-05, 시나리오 F7. 설문 accessLevel은 별도 검증하지 않으나 응답 부재로 간접 차단 |

### SEC-SEVT-06: 비인가 접근이 설문 연결 상태를 변경하지 않음

#### TC-054: 비인가 사용자의 행사 생성 실패 시 DB에 설문 연결 변경 없음

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (Controller) |
| **사전 조건** | 미인증 사용자 (JWT 없음) |
| **테스트 절차** | 1. JWT 없이 `POST /api/events` 호출, `surveyId: 100` 포함<br>2. DB에서 Event 테이블에 신규 레코드가 없는지 확인 |
| **입력 데이터** | `{ "title": "테스트", "surveyId": 100, ... }` |
| **기대 결과** | HTTP 401, DB에 Event 레코드 생성 없음 |
| **비고** | SEC-SEVT-06. 트랜잭션 롤백에 의한 부작용 없음 확인 |

#### TC-055: 권한 부족 사용자의 행사 신청 실패 시 설문 응답 저장 없음

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | ASSOCIATE 사용자, `event.surveyId != null` |
| **테스트 절차** | 1. ASSOCIATE JWT로 `POST /api/events/{eventId}/registrations` 호출, `surveyAnswers` 포함<br>2. DB에서 SurveyResponse 레코드가 없는지 확인 |
| **입력 데이터** | `eventId`, `surveyAnswers: [...]` |
| **기대 결과** | HTTP 403, SurveyResponse DB 레코드 없음 (권한 검증이 설문 저장보다 선행, SEVT-INV-12 검증 순서) |
| **비고** | SEC-SEVT-06. 검증 순서에 의해 권한 확인(1단계)이 설문 처리(6단계)보다 우선 |

---

## 5. 대표 시나리오 (정상/실패/엣지)

### 정상 시나리오

#### TC-056: [S1] 설문 연결 행사 기본 흐름 E2E (선착순)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 설문(PUBLISHED+OPEN) 생성 완료, 설문 연결 행사 생성 완료(선착순), 행사 OPEN + 기간 내, MEMBER 사용자 DB 존재 |
| **테스트 절차** | 1. `POST /api/events/{eventId}/registrations` 호출, `surveyAnswers` 포함<br>2. 신청 결과 확인<br>3. DB에 SurveyResponse + EventRegistration 모두 존재 확인 |
| **입력 데이터** | `eventId`, `surveyAnswers: [{questionId: 1, answer: "답변"}]` |
| **기대 결과** | HTTP 200/201, EventRegistration(status=REGISTERED) + SurveyResponse 생성 |
| **비고** | 시나리오 S1. 선착순 행사 기본 흐름 |

#### TC-057: [S1] 설문 연결 행사 기본 흐름 E2E (선발제)

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 설문(PUBLISHED+OPEN) 생성 완료, 설문 연결 행사 생성 완료(선발제), 행사 OPEN + 기간 내, MEMBER 사용자 DB 존재 |
| **테스트 절차** | 1. `POST /api/events/{eventId}/registrations` 호출, `surveyAnswers` 포함<br>2. 신청 결과 확인 |
| **입력 데이터** | `eventId`, `surveyAnswers: [{questionId: 1, answer: "답변"}]` |
| **기대 결과** | HTTP 200/201, EventRegistration(status=WAITING) + SurveyResponse 생성 |
| **비고** | 시나리오 S1 변형. 선발제 행사 기본 흐름 |

#### TC-058: [S4] 동일 설문 다중 행사 -- 응답 1회로 두 행사 신청

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 설문(id=100) OPEN, 행사 A/B 모두 surveyId=100, 시간 미겹침, DB에 모든 데이터 존재 |
| **테스트 절차** | 1. 행사 A 신청 (surveyAnswers 포함) → 성공<br>2. 행사 B 신청 (surveyAnswers 생략, 기존 응답 재사용) → 성공 |
| **입력 데이터** | 행사 A: `surveyAnswers: [...]`, 행사 B: `surveyAnswers: null` |
| **기대 결과** | 두 행사 모두 신청 성공, SurveyResponse는 1건만 존재 |
| **비고** | 시나리오 S4, SEVT-INV-03 |

### 실패 시나리오

#### TC-059: [F6] 설문 응답 + 신청 시 정원 초과 -- 설문 응답 롤백

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 설문 연결 행사, 선착순, 정원 1명, 다른 사용자가 이미 신청 완료(정원 소진), 사용자 미응답 |
| **테스트 절차** | 1. `POST /api/events/{eventId}/registrations` 호출, `surveyAnswers` 포함<br>2. 예외 확인<br>3. DB에서 SurveyResponse 미존재 확인 |
| **입력 데이터** | `eventId`, `surveyAnswers: [...]` |
| **기대 결과** | `EventCapacityFullException`, SurveyResponse 롤백 (DB 레코드 없음) |
| **비고** | 시나리오 F6, SEVT-INV-08 |

#### TC-060: [F8] 동시 설문 응답 제출 + 행사 신청 경합

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 예외 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 설문 연결 행사, 동일 사용자가 동시에 2개 스레드에서 통합 API 호출 |
| **테스트 절차** | 1. 2개 스레드에서 동시에 `registerEventWithSurvey()` 호출<br>2. 한 스레드는 성공, 다른 스레드는 실패 확인<br>3. DB에 SurveyResponse 1건, EventRegistration 1건만 존재 확인 |
| **입력 데이터** | 동일 `eventId`, `userId`, `surveyAnswers` |
| **기대 결과** | 1건 성공, 1건 실패 (INV-01 중복 응답 방지 또는 REG-INV-01 중복 신청 방지), 고아 데이터 없음 |
| **비고** | 시나리오 F8. 동시성 검증 |

### 엣지 케이스 시나리오

#### TC-061: [E1] 설문 응답 -> 설문 삭제 -> 행사 신청 시 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 사용자 설문 응답 완료 후, 운영진이 설문 영구 삭제(deleted=true), 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. 설문 응답 제출 (별도 API 또는 직접 DB 삽입)<br>2. 설문 삭제 처리<br>3. 행사 신청 시도 |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | 신청 실패 (`SurveyNotFoundException`) |
| **비고** | 시나리오 E1, SEVT-INV-11 |

#### TC-062: [E2] 설문 응답 -> 행사 신청 -> 설문 삭제 시 기존 신청 유효 유지

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 사용자 설문 응답 + 행사 신청 완료 후, 운영진이 설문 삭제 |
| **테스트 절차** | 1. 설문 응답 + 행사 신청 완료<br>2. 설문 삭제 처리<br>3. EventRegistration 상태 확인 |
| **입력 데이터** | 없음 (상태 확인) |
| **기대 결과** | EventRegistration 상태 유지 (REGISTERED/WAITING/APPROVED) |
| **비고** | 시나리오 E2, SEVT-INV-09 |

#### TC-063: [E3] 복잡한 라이프사이클 -- 신청 -> 취소 -> 설문 변경 -> 재신청

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 사용자 설문A 응답 + 행사 신청 완료 후 취소(CANCELED), 운영진이 설문A → 설문B로 변경, 사용자가 설문B에 응답 완료 |
| **테스트 절차** | 1. 설문A 응답 + 행사 신청<br>2. 행사 신청 취소<br>3. 운영진이 surveyId 변경(A→B)<br>4. 사용자가 설문B 응답 완료<br>5. 재신청 시도 |
| **입력 데이터** | 설문B 응답 존재 상태에서 `reRegister()` |
| **기대 결과** | 재신청 성공 (현재 event.surveyId 기준, 설문B 응답 존재 확인) |
| **비고** | 시나리오 E3. 현재 surveyId 기준 검증 (SEVT-INV-06) |

#### TC-064: [E3 변형] 복잡한 라이프사이클 -- 설문 변경 후 새 설문 미응답 시 재신청 실패

| 항목 | 내용 |
|------|------|
| **우선순위** | 하 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 사용자 설문A 응답 + 행사 신청 완료 후 취소(CANCELED), 운영진이 설문A → 설문B로 변경, 사용자가 설문B에 미응답 |
| **테스트 절차** | 1. 설문A 응답 + 행사 신청 → 취소<br>2. surveyId 변경(A→B)<br>3. 재신청 시도 (설문B 응답 없음) |
| **입력 데이터** | `reRegister()` 호출, 설문B 응답 미존재 |
| **기대 결과** | `SurveyResponseRequiredException` 발생 |
| **비고** | 시나리오 E3 변형. 과거 설문A 응답은 무시, 현재 surveyId 기준 |

#### TC-065: [E5] 설문 비공개 전환(UNPUBLISHED) 후 기존 응답으로 행사 신청 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 설문이 PUBLISHED+OPEN에서 UNPUBLISHED로 전환됨 (INV-20에 의해 responseStatus 자동 CLOSED), 사용자 기존 응답 존재, 행사 OPEN + 기간 내 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers: null` |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | 행사 신청 성공 (responseStatus == CLOSED, visibility는 검증 대상 아님) |
| **비고** | 시나리오 E5, SEVT-INV-10 |

#### TC-066: [E6] 선발제 승인 시점에 설문이 CLOSED/휴지통이어도 승인 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 사용자가 설문 응답 + 행사 신청(WAITING) 완료, 이후 설문이 CLOSED 또는 휴지통 이동됨 |
| **테스트 절차** | 1. 운영진이 해당 사용자의 신청을 승인(approve) 처리<br>2. 승인 결과 확인 |
| **입력 데이터** | 승인 대상 registrationId |
| **기대 결과** | 승인 성공 (WAITING → APPROVED), 설문 상태 재검증 안 함 |
| **비고** | 시나리오 E6, SEVT-INV-06 (승인 시 설문 재검증 안 함). 신청 시점에만 설문 검증 |

### 설문 단독 API 독립성

#### TC-067: 설문 단독 API로 응답 제출 시 행사 신청이 자동 트리거되지 않음

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 통합 (@SpringBootTest) |
| **사전 조건** | 설문(id=100)이 행사에 연결됨, 사용자 미신청 |
| **테스트 절차** | 1. `POST /surveys/{surveyId}/responses` (설문 단독 API) 호출로 응답 제출<br>2. EventRegistration 테이블에서 해당 사용자의 행사 신청 레코드 조회 |
| **입력 데이터** | `surveyId: 100`, 설문 응답 데이터 |
| **기대 결과** | 설문 응답 저장 성공, EventRegistration 레코드 없음 (행사 신청 미발생) |
| **비고** | SEVT-INV-08, 3종 API 독립성. 설문 단독 API는 행사를 모름 |

### 검증 순서 확인

#### TC-068: 중복 신청 확인이 설문 검증보다 선행

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | 사용자가 이미 행사에 활성 신청 존재(REG-INV-01), `event.surveyId != null`, 설문 NOT_STARTED |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출 |
| **입력 데이터** | `eventId`, `userId` (이미 신청 존재) |
| **기대 결과** | 중복 신청 관련 예외 발생 (REG-INV-01), SurveyNotReadyException이 아님 |
| **비고** | SEVT-INV-12 검증 순서: 중복 신청 확인(3)이 설문 검증(6)보다 선행 |

#### TC-069: 설문 상태 검증이 시간 겹침/정원 확인보다 선행

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | `event.surveyId != null`, 설문 NOT_STARTED, 행사 정원 초과 (정원 이미 가득 참) |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출 |
| **입력 데이터** | `eventId`, `userId`, `surveyAnswers: [...]` |
| **기대 결과** | `SurveyNotReadyException` 발생 (설문 검증(6단계)이 정원 확인(8단계)보다 선행) |
| **비고** | SEVT-INV-12 검증 순서. 설문 검증 위치(6번) 근거: 불필요한 DB 조회 방지 |

---

## 6. 관측 가능성

### 6-1. 로그 메시지 검증

#### TC-070: 설문 응답 미존재로 신청 거부 시 info 로그 기록

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | LogCaptor 설정, `event.surveyId != null`, 사용자 미응답 |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출, `surveyAnswers: null`<br>2. 캡처된 로그 메시지 확인 |
| **입력 데이터** | `eventId`, `userId`, `surveyAnswers: null` |
| **기대 결과** | info 레벨 로그에 `eventId`, `userId`, `surveyId`, "설문 응답 미존재" 사유 포함 |
| **비고** | 6-1 로그 메시지 테이블, `SurveyResponseRequiredException` 발생 시 로그 |

#### TC-071: 설문 NOT_STARTED로 신청 거부 시 info 로그 기록

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | LogCaptor 설정, `survey.responseStatus == NOT_STARTED` |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출<br>2. 캡처된 로그 메시지 확인 |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | info 레벨 로그에 `eventId`, `userId`, `surveyId`, "설문 미시작", "responseStatus: NOT_STARTED" 포함 |
| **비고** | 6-1 로그 메시지 테이블 |

#### TC-072: 설문 삭제/휴지통으로 신청 거부 시 warn 로그 기록

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **테스트 레벨** | 서비스 통합 (Mockito) |
| **사전 조건** | LogCaptor 설정, `survey.trashedAt != null` 또는 `survey.deleted == true` |
| **테스트 절차** | 1. `registerEventWithSurvey()` 호출<br>2. 캡처된 로그 메시지 확인 |
| **입력 데이터** | `eventId`, `userId` |
| **기대 결과** | **warn** 레벨 로그에 `eventId`, `userId`, `surveyId`, "연결된 설문이 삭제됨" 사유 포함 |
| **비고** | 6-1 로그 메시지 테이블. 삭제/휴지통은 warn 레벨 (비정상 운영 상황 알림) |

---

## 커버리지 매핑

### 불변조건 커버리지

| 불변조건 | 커버 테스트 케이스 | 정상 | 비정상/경계값 |
|---------|:---:|:---:|:---:|
| SEVT-INV-01 (설문 연결 선택 사항) | TC-001, TC-002, TC-036 | 3 | 0 |
| SEVT-INV-02 (행사당 설문 최대 1개) | TC-003 | 1 | 0 |
| SEVT-INV-03 (설문 재사용 가능) | TC-004, TC-045, TC-058 | 3 | 0 |
| SEVT-INV-04 (설문 존재 검증) | TC-005, TC-006, TC-007, TC-008, TC-037, TC-044 | 2 | 4 |
| SEVT-INV-05 (상태별 설문 변경) | TC-009, TC-010, TC-011, TC-012 | 3 | 1 |
| SEVT-INV-06 (설문 응답 필수) | TC-013, TC-014, TC-015, TC-016, TC-038, TC-047, TC-048, TC-049 | 5 | 3 |
| SEVT-INV-07 (미연결 행사 보존) | TC-002, TC-017 | 2 | 0 |
| SEVT-INV-08 (통합 API 원자성) | TC-018, TC-019, TC-059, TC-060, TC-067 | 1 | 4 |
| SEVT-INV-09 (응답 수정 무영향) | TC-020, TC-062 | 2 | 0 |
| SEVT-INV-10 (설문 상태 검증) | TC-021, TC-022, TC-023, TC-028, TC-029, TC-030, TC-033, TC-065 | 5 | 3 |
| SEVT-INV-11 (설문 삭제/휴지통) | TC-024, TC-025, TC-040, TC-061 | 0 | 4 |
| SEVT-INV-12 (기존 행사 신청 보존) | TC-002, TC-017, TC-046, TC-068, TC-069 | 2 | 3 |
| SEVT-INV-13 (기존 설문 보존) | TC-015, TC-067 | 2 | 0 |

### 권한 검증 커버리지

| 검증 | 커버 테스트 케이스 |
|------|:---:|
| SEC-SEVT-02 (준회원 차단) | TC-050 |
| SEC-SEVT-03 (일반 회원 생성 차단) | TC-051 |
| SEC-SEVT-04 (설문 응답 없이 신청) | TC-052 |
| SEC-SEVT-05 (설문 accessLevel 부족) | TC-053 |
| SEC-SEVT-06 (비인가 부작용 없음) | TC-054, TC-055 |

### 상태 교차 매트릭스 커버리지

| 행사 visibility | 행사 registrationStatus | 설문 responseStatus | 신청 가능 | 커버 TC |
|:---:|:---:|:---:|:---:|:---:|
| UNPUBLISHED | ANY | ANY | 불가 | TC-026 |
| PUBLISHED | NOT_STARTED | ANY | 불가 | TC-027 |
| PUBLISHED | OPEN | NOT_STARTED | 불가 | TC-028 |
| PUBLISHED | OPEN | OPEN | 가능 | TC-029 |
| PUBLISHED | OPEN | CLOSED | 가능 | TC-030 |
| PUBLISHED | CLOSED | ANY | 불가 | TC-031 |

### 대표 시나리오 커버리지

| 시나리오 | 커버 TC | 비고 |
|---------|:---:|------|
| S1 (기본 흐름) | TC-013, TC-056, TC-057 | 단위 + E2E (선착순/선발제) |
| S2 (기존 흐름) | TC-002, TC-017 | 회귀 테스트 |
| S3 (마감 후 신청) | TC-014, TC-023, TC-030 | 기존 응답 재사용 |
| S4 (다중 행사) | TC-045, TC-058 | 설문 재사용 |
| S5 (설문 해제) | TC-043 | 설문 해제 후 신청 |
| F1 (미응답 신청) | TC-016, TC-052 | SurveyResponseRequiredException |
| F2 (NOT_STARTED) | TC-021, TC-028 | SurveyNotReadyException |
| F3 (휴지통) | TC-024, TC-040 | SurveyNotFoundException |
| F4 (미존재 설문) | TC-006 | SurveyNotFoundException |
| F5 (삭제 설문) | TC-007, TC-044 | SurveyNotFoundException |
| F6 (정원 초과 롤백) | TC-018, TC-059 | 원자성 롤백 |
| F7 (accessLevel 부족) | TC-053 | 간접 차단 |
| F8 (동시 경합) | TC-060 | 동시성 |
| E1 (응답→삭제→신청) | TC-061 | 실패 |
| E2 (신청→삭제) | TC-062 | 유효 유지 |
| E3 (복잡 라이프사이클) | TC-063, TC-064 | 성공/실패 분기 |
| E4 (동시 수정+신청) | TC-020 | 기존 응답으로 진행 |
| E5 (비공개 전환) | TC-033, TC-065 | CLOSED 자동 전환 |
| E6 (승인 시 설문 무관) | TC-066 | 승인 시 재검증 안 함 |

### 테스트 레벨 분포

| 테스트 레벨 | 테스트 케이스 수 | TC 번호 |
|-----------|:---:|-----------|
| 단위 테스트 (순수 Java) | 6 | TC-001, TC-003, TC-009, TC-010, TC-011, TC-012 |
| 서비스 통합 (Mockito) | 44 | TC-002, TC-004~008, TC-013~016, TC-019~025, TC-027~031, TC-033~035, TC-038~049, TC-053, TC-065, TC-068~072 |
| 통합 (Controller/MockMvc) | 7 | TC-026, TC-036, TC-037, TC-050, TC-051, TC-052, TC-054 |
| 통합 (@SpringBootTest) | 15 | TC-017, TC-018, TC-032, TC-055~064, TC-066, TC-067 |
