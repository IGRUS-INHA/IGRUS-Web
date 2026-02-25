# 설문(Survey) 통합 테스트 케이스

**작성일**: 2026-02-25
**버전**: 1.0
**관련 스펙**: [설문 검증 기준서](../../criteria/survey/survey-criteria-v1.md)
**우선순위**: P0

> 이 문서는 설문 시스템의 통합 테스트 케이스를 기술한다. 컨트롤러 RBAC, E2E 시나리오 흐름, 응답 제출을 검증한다.
> - ⬜ 미구현 (신규 작성 필요)

---

## 1. 개요

설문 시스템의 통합 테스트 케이스이다. 단위 테스트에서 검증하기 어려운 다음 영역을 커버한다:

- **컨트롤러 RBAC**: MockMvc를 통한 HTTP 레벨 인증/인가 검증 (역할별 접근 제어)
- **E2E 시나리오 흐름**: 2축 상태 모델(visibility + responseStatus) 기반 전체 라이프사이클 시나리오
- **응답 제출 통합**: 설문 상태/권한/경합 조건에 따른 응답 제출 검증
- **목록 조회 통합**: 휴지통/삭제/공개 상태에 따른 목록 노출 규칙 검증

---

## 2. 테스트 케이스

### 2.1 Survey 컨트롤러 RBAC 테스트 (MockMvc)

> **테스트 방식**: `ServiceIntegrationTestBase` + `@AutoConfigureMockMvc` 기반 MockMvc 테스트. HTTP 레벨에서 인증/인가를 검증한다.
> **역할**: 비인증, ASSOCIATE, MEMBER, OPERATOR, ADMIN
> **참조**: 검증 기준서 4-1 역할별 접근 제어 매트릭스

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-SRV-001 | 비인증 사용자 설문 생성 → 401 | 인증 없음 | `POST /api/v1/surveys` 요청 (토큰 없이) | 401 Unauthorized | ⬜ |
| INT-SRV-002 | 비인증 사용자 설문 목록 조회 → 401 | 인증 없음 | `GET /api/v1/surveys` 요청 (토큰 없이) | 401 Unauthorized | ⬜ |
| INT-SRV-003 | MEMBER 설문 생성 → 403 | MEMBER 토큰 | `POST /api/v1/surveys` 요청 | 403 Forbidden (SEC-01: 비운영진 설문 생성 차단) | ⬜ |
| INT-SRV-004 | ASSOCIATE 설문 생성 → 403 | ASSOCIATE 토큰 | `POST /api/v1/surveys` 요청 | 403 Forbidden (SEC-01: 비운영진 설문 생성 차단) | ⬜ |
| INT-SRV-005 | MEMBER 설문 수정 → 403 | MEMBER 토큰, 기존 설문 존재 | `PUT /api/v1/surveys/{id}` 요청 | 403 Forbidden (비운영진 설문 수정 차단) | ⬜ |
| INT-SRV-006 | MEMBER 설문 공개(publish) → 403 | MEMBER 토큰, (U,NS) 설문 존재 | `POST /api/v1/surveys/{id}/publish` 요청 | 403 Forbidden (SEC-02: 비운영진 발행 차단) | ⬜ |
| INT-SRV-007 | MEMBER 비공개 전환(unpublish) → 403 | MEMBER 토큰, (P,C) 설문 존재 | `POST /api/v1/surveys/{id}/unpublish` 요청 | 403 Forbidden (SEC-09: 비운영진 비공개 전환 차단) | ⬜ |
| INT-SRV-008 | MEMBER 응답 시작(openResponse) → 403 | MEMBER 토큰, (P,NS) 설문 존재 | `POST /api/v1/surveys/{id}/open-response` 요청 | 403 Forbidden (비운영진 응답 시작 차단) | ⬜ |
| INT-SRV-009 | MEMBER 응답 마감(closeResponse) → 403 | MEMBER 토큰, (P,O) 설문 존재 | `POST /api/v1/surveys/{id}/close-response` 요청 | 403 Forbidden (비운영진 응답 마감 차단) | ⬜ |
| INT-SRV-010 | MEMBER 휴지통 이동 → 403 | MEMBER 토큰, 활성 설문 존재 | `POST /api/v1/surveys/{id}/trash` 요청 | 403 Forbidden (비운영진 휴지통 이동 차단) | ⬜ |
| INT-SRV-011 | MEMBER 휴지통 복원 → 403 | MEMBER 토큰, 휴지통 설문 존재 | `POST /api/v1/surveys/{id}/restore` 요청 | 403 Forbidden (비운영진 휴지통 복원 차단) | ⬜ |
| INT-SRV-012 | MEMBER 영구 삭제 → 403 | MEMBER 토큰, 휴지통 설문 존재 | `DELETE /api/v1/surveys/{id}` 요청 | 403 Forbidden (비운영진 영구 삭제 차단) | ⬜ |
| INT-SRV-013 | MEMBER 설문 복사 → 403 | MEMBER 토큰, 활성 설문 존재 | `POST /api/v1/surveys/{id}/copy` 요청 | 403 Forbidden (SEC-10: 비운영진 설문 복사 차단) | ⬜ |
| INT-SRV-014 | MEMBER 질문 복사 → 403 | MEMBER 토큰, 설문+질문 존재 | `POST /api/v1/surveys/{surveyId}/questions/{questionId}/copy` 요청 | 403 Forbidden (SEC-11: 비운영진 질문 복사 차단) | ⬜ |
| INT-SRV-015 | MEMBER 결과 조회 → 403 | MEMBER 토큰, (P,C) 설문 존재 | `GET /api/v1/surveys/{id}/results` 요청 | 403 Forbidden (SEC-05: 비운영진 결과 조회 차단) | ⬜ |
| INT-SRV-016 | OPERATOR 설문 생성 → 성공 | OPERATOR 토큰 | `POST /api/v1/surveys` 요청 (유효한 설문 데이터) | 201 Created, 설문 생성 확인 | ⬜ |
| INT-SRV-017 | ADMIN 설문 생성 → 성공 | ADMIN 토큰 | `POST /api/v1/surveys` 요청 (유효한 설문 데이터) | 201 Created, 설문 생성 확인 | ⬜ |
| INT-SRV-018 | 비인증 PUBLIC 설문 응답 → 성공 | 인증 없음, (P,O) + accessLevel=PUBLIC 설문 존재 | `POST /api/v1/surveys/{id}/responses` 요청 (토큰 없이, 유효한 응답 데이터) | 201 Created, 응답 제출 성공 (user=null) | ⬜ |
| INT-SRV-019 | 비인증 ASSOCIATE 설문 응답 → 401 | 인증 없음, (P,O) + accessLevel=ASSOCIATE 설문 존재 | `POST /api/v1/surveys/{id}/responses` 요청 (토큰 없이) | 401 Unauthorized (SEC-03: 비회원 ASSOCIATE 설문 응답 차단) | ⬜ |
| INT-SRV-020 | ASSOCIATE가 MEMBER 설문 응답 → 403 | ASSOCIATE 토큰, (P,O) + accessLevel=MEMBER 설문 존재 | `POST /api/v1/surveys/{id}/responses` 요청 | 403 Forbidden (SEC-04: ASSOCIATE가 MEMBER 설문 응답 차단) | ⬜ |
| INT-SRV-021 | MEMBER가 MEMBER 설문 응답 → 성공 | MEMBER 토큰, (P,O) + accessLevel=MEMBER 설문 존재 | `POST /api/v1/surveys/{id}/responses` 요청 (유효한 응답 데이터) | 201 Created, 응답 제출 성공 | ⬜ |
| INT-SRV-022 | MEMBER 휴지통 목록 조회 → 403 | MEMBER 토큰 | `GET /api/v1/surveys/trash` 요청 | 403 Forbidden (비운영진 휴지통 목록 조회 차단) | ⬜ |
| INT-SRV-023 | OPERATOR 휴지통 목록 조회 → 성공 | OPERATOR 토큰, 휴지통 설문 1건 이상 존재 | `GET /api/v1/surveys/trash` 요청 | 200 OK, 휴지통 설문 목록 반환 | ⬜ |

### 2.2 E2E 시나리오 흐름 (검증 기준서 표 5: F1~F20)

> **테스트 방식**: `ServiceIntegrationTestBase` 기반 서비스 통합 테스트. 2축 상태 모델의 전체 라이프사이클 흐름을 DB 레벨에서 검증한다.
> **상태 표기**: `(visibility, responseStatus, trashed여부)` — 예: `(P, O, active)` = PUBLISHED + OPEN + 활성

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-SRV-030 | F1: 기본 흐름 — 공개 → 응답 시작 → 응답 마감 | OPERATOR 권한, (U,NS) 설문 (질문 1개 이상) | 1. publish() → 상태 확인 2. openResponse() → 상태 확인 3. closeResponse() → 상태 확인 | (U,NS) → (P,NS) → (P,O) → (P,C) 순서로 전이. 각 단계에서 DB flush/clear 후 상태 확인 | ⬜ |
| INT-SRV-031 | F2: 즉시 응답 시작 — publishAndOpen | OPERATOR 권한, (U,NS) 설문 (질문 1개 이상) | 1. publishAndOpen() → 상태 확인 2. closeResponse() → 상태 확인 | (U,NS) → (P,O) → (P,C). publishAndOpen이 publish + openResponse를 원자적으로 수행 | ⬜ |
| INT-SRV-032 | F3: 응답 일시 중지 후 재개 | OPERATOR 권한, (P,O) 설문, deadline=null 또는 미래 시점 | 1. closeResponse() → (P,C) 확인 2. openResponse() → (P,O) 확인 | (P,O) → (P,C) → (P,O). CLOSED → OPEN 재개 성공, 응답 상태 양방향 전이 | ⬜ |
| INT-SRV-033 | F4: 공개 후 대기 — 응답 시작 전 미리보기 상태 | OPERATOR 권한, (U,NS) 설문 (질문 1개 이상) | 1. publish() → (P,NS) 확인 2. (P,NS) 상태에서 응답 제출 시도 | (U,NS) → (P,NS). 응답 제출 시도 시 NOT_STARTED이므로 거부 (INV-09) | ⬜ |
| INT-SRV-034 | F5: 마감일 자동 마감 | OPERATOR 권한, (P,O) 설문, deadline=과거 시점으로 설정 | 1. (P,O) 설문에 deadline을 과거 시점으로 설정 2. 자동 마감 스케줄러 또는 응답 시점 검증 실행 3. 상태 확인 | (P,O) → (P,C). deadline 경과 시 responseStatus가 자동으로 CLOSED 전환 (INV-08) | ⬜ |
| INT-SRV-035 | F6: 휴지통 이동 후 복원 | OPERATOR 권한, (P,O,active) 설문 | 1. trash() → (P,O,trashed) 확인 (trashedAt != null) 2. restore() → (P,O,active) 확인 (trashedAt == null) | 복원 시 visibility=PUBLISHED, responseStatus=OPEN 유지. 상태 변경 없이 trashedAt만 변경 | ⬜ |
| INT-SRV-036 | F7: 휴지통 → 영구 삭제 | OPERATOR 권한, (P,C,active) 설문 | 1. trash() → (P,C,trashed) 확인 2. permanentDelete() → deleted=true 확인 | (P,C) → trash → permanentDelete. 2단계 삭제 흐름 (INV-03). deleted=true, 자식 엔티티 soft delete 처리 | ⬜ |
| INT-SRV-037 | F8: 비공개에서 바로 휴지통 이동 후 복원 | OPERATOR 권한, (U,NS,active) 설문 | 1. trash() → (U,NS,trashed) 확인 2. restore() → (U,NS,active) 확인 | 초기 상태(UNPUBLISHED, NOT_STARTED)에서도 휴지통 이동/복원 가능. 복원 시 원래 상태 유지 | ⬜ |
| INT-SRV-038 | F9: 마감 후 마감일 수정 후 재개 | OPERATOR 권한, (P,C) 설문, deadline=과거 | 1. update()로 deadline을 미래 시점으로 변경 2. openResponse() → (P,O) 확인 | (P,C) → deadline 수정 → (P,O). 마감일을 미래로 변경하면 응답 재개 가능 (INV-11) | ⬜ |
| INT-SRV-039 | F10: 마감일 경과 후 재개 시도 실패 | OPERATOR 권한, (P,C) 설문, deadline=과거 시점 | 1. openResponse() 시도 | 예외 발생: 마감일이 과거이므로 재개 거부 (INV-11). (P,C) 상태 유지 | ⬜ |
| INT-SRV-040 | F11: 끝난 설문 숨기기 | OPERATOR 권한, (P,C) 설문 | 1. unpublish() → (U,C) 확인 | (P,C) → (U,C). 마감된 설문을 비공개로 전환. responseStatus=CLOSED 유지 (이미 CLOSED이므로 자동 마감 불필요) | ⬜ |
| INT-SRV-041 | F12: 진행 중 설문 내리기 (자동마감) | OPERATOR 권한, (P,O) 설문 | 1. unpublish() → (U,C) 확인 | (P,O) → (U,C). PUBLISHED+OPEN에서 unpublish 시 responseStatus 자동 CLOSED (INV-20) | ⬜ |
| INT-SRV-042 | F13: 숨긴 설문 다시 공개 | OPERATOR 권한, (U,C) 설문 (질문 1개 이상) | 1. publish() → (P,C) 확인 | (U,C) → (P,C). 재공개 시 responseStatus=CLOSED 유지. 질문 수 재검증 (INV-04, INV-13) | ⬜ |
| INT-SRV-043 | F14: 숨긴 설문 공개 + 응답 재개 | OPERATOR 권한, (U,C) 설문 (질문 1개 이상), deadline=null 또는 미래 | 1. publishAndOpen() → (P,O) 확인 | (U,C) → (P,O). publishAndOpen이 publish + openResponse를 원자적으로 수행하여 재공개+응답재개 동시 처리 | ⬜ |
| INT-SRV-044 | F15: 공개 미리보기 취소 | OPERATOR 권한, (P,NS) 설문 | 1. unpublish() → (U,NS) 확인 | (P,NS) → (U,NS). NOT_STARTED 상태에서 비공개 전환. responseStatus 변경 없음 (OPEN이 아니므로 자동 마감 불필요) | ⬜ |
| INT-SRV-045 | F16: 전체 라이프사이클 | OPERATOR 권한, (U,NS) 설문 (질문 1개 이상), deadline=null | 1. publish() → (P,NS) 2. openResponse() → (P,O) 3. closeResponse() → (P,C) 4. unpublish() → (U,C) 5. publish() → (P,C) 6. openResponse() → (P,O) 7. closeResponse() → (P,C) | (U,NS)→(P,NS)→(P,O)→(P,C)→(U,C)→(P,C)→(P,O)→(P,C). 전체 양방향 전이 사이클 검증 | ⬜ |
| INT-SRV-046 | F17: 설문 복사 (기본) | OPERATOR 권한, (P,O) 설문 (질문+선택지+응답 존재) | 1. 설문 복사 실행 2. 복사된 설문 상태 확인 3. 복사된 설문 응답 수 확인 4. 복사된 질문/선택지 확인 | 새 설문: (U,NS,active), 제목=" (복사본)" 접미사, deadline=null, 응답 0건, 질문/선택지 복사됨, 새 ID 부여 (INV-21, INV-22, INV-25) | ⬜ |
| INT-SRV-047 | F18: 설문 복사 후 수정 발행 흐름 | OPERATOR 권한, 원본 (P,O) 설문 존재 | 1. 설문 복사 → (U,NS) 2. update()로 제목/마감일 수정 3. publish() → (P,NS) 4. openResponse() → (P,O) | 복사본 (U,NS) → 수정 → (P,NS) → (P,O). 복사 후 독립적인 라이프사이클 운영 가능 | ⬜ |
| INT-SRV-048 | F19: 같은 설문 내 질문 복사 | OPERATOR 권한, (U,NS) 설문 (질문 2개, 선택지/행 포함) | 1. 질문 1개 복사 실행 2. 복사된 질문 확인 (새 ID, displayOrder 맨 뒤) 3. 복사된 선택지/행 확인 | 질문 복사본이 같은 설문에 추가됨. 새 ID 부여 (INV-25), displayOrder가 기존 질문 뒤에 배치, 선택지/행도 복사 | ⬜ |
| INT-SRV-049 | F20: 질문 수 상한 도달 시 복사 실패 | OPERATOR 권한, 질문 49개인 설문 | 1. 질문 2개 이상 복사 시도 | 예외 발생: SURVEY_QUESTION_LIMIT_EXCEEDED (INV-24). 질문 수 50개 상한 초과 방지 | ⬜ |

### 2.3 응답 제출 통합 테스트

> **테스트 방식**: `ServiceIntegrationTestBase` 기반 서비스 통합 테스트. 설문 상태/권한/경합 조건에 따른 응답 제출 검증.
> **참조**: INV-01, INV-09, INV-12, INV-15, INV-16, INV-19

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-SRV-060 | 회원 응답 제출 성공 (PUBLISHED+OPEN) | MEMBER 토큰, (P,O,active) + accessLevel=MEMBER 설문, 필수/선택 질문 포함 | 1. 모든 필수 질문에 유효한 응답 포함하여 제출 2. DB에서 SurveyResponse 및 SurveyAnswer 확인 | 201 Created. SurveyResponse(user=회원, survey=설문) 생성. 각 질문별 SurveyAnswer 정상 저장 | ⬜ |
| INT-SRV-061 | 동일 회원 중복 응답 제출 → 에러 | MEMBER 토큰, (P,O) 설문, 해당 회원이 이미 응답 제출 완료 | 1. 동일 설문에 동일 회원으로 재응답 제출 시도 | 예외 발생: 중복 응답 방지 (INV-01). (survey_id, user_id) unique constraint 위반 | ⬜ |
| INT-SRV-062 | 비회원 PUBLIC 설문 응답 제출 성공 | 인증 없음, (P,O,active) + accessLevel=PUBLIC 설문 | 1. 토큰 없이 유효한 응답 데이터로 제출 2. DB에서 SurveyResponse 확인 | 201 Created. SurveyResponse(user=null) 생성. 비회원은 중복 응답 unique constraint 대상 아님 | ⬜ |
| INT-SRV-063 | 필수 질문 응답 누락 시 제출 거부 | MEMBER 토큰, (P,O) 설문, required=true 질문 존재 | 1. 필수 질문에 대한 응답을 누락하고 제출 시도 | 예외 발생: 필수 질문 응답 누락 (INV-12). SurveyResponse 미생성, DB 변경 없음 | ⬜ |
| INT-SRV-064 | 응답 제출 중 설문 마감 경합 | MEMBER 토큰, (P,O) 설문, deadline=현재 시각 직후 | 1. 설문 폼 열기 시점에는 OPEN 2. 작성 중 deadline 경과 또는 운영진이 closeResponse() 3. 제출 시점에 (P,C) 상태 4. 응답 제출 시도 | 예외 발생: 제출 시점에 survey.responseStatus == CLOSED 재검증 (INV-15). 마감된 설문에 응답 저장 방지 | ⬜ |
| INT-SRV-065 | 마감된 설문 응답 제출 → 거부 | MEMBER 토큰, (P,C,active) 설문 | 1. CLOSED 상태의 설문에 응답 제출 시도 | 예외 발생: responseStatus != OPEN (INV-09). SurveyResponse 미생성 | ⬜ |
| INT-SRV-066 | 휴지통 설문 응답 제출 → 거부 | MEMBER 토큰, (P,O,trashed) 설문 | 1. 휴지통에 있는 설문에 응답 제출 시도 | 예외 발생: trashedAt != null (INV-16). 상태가 PUBLISHED+OPEN이어도 휴지통이면 응답 불가 | ⬜ |
| INT-SRV-067 | UNPUBLISHED 설문 응답 제출 → 거부 | MEMBER 토큰, (U,NS,active) 설문 | 1. 비공개 설문에 응답 제출 시도 | 예외 발생: visibility != PUBLISHED (INV-09). UNPUBLISHED 설문은 응답 불가 | ⬜ |
| INT-SRV-068 | accessLevel 변경 후 기존 응답자 본인 응답 조회 성공 | MEMBER 토큰, 해당 MEMBER가 이전에 (accessLevel=MEMBER) 설문에 응답 완료, 운영진이 accessLevel을 OPERATOR로 변경 | 1. MEMBER가 본인 응답 조회 요청 | 200 OK. 본인 응답 반환 (INV-19). accessLevel이 축소되어 설문 접근 불가 상태이지만, 이미 제출한 본인 응답은 조회 가능 | ⬜ |

### 2.4 설문 목록 조회 통합 테스트

> **테스트 방식**: `ServiceIntegrationTestBase` 기반 서비스 통합 테스트. 상태별 목록 노출 규칙 검증.
> **참조**: INV-17

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-SRV-075 | 일반 목록에서 휴지통 설문 제외 | 활성 설문 2건 + 휴지통 설문 1건(trashedAt != null) 존재 | 1. 일반 설문 목록 조회 (OPERATOR 권한) | 200 OK. 활성 설문 2건만 반환. 휴지통 설문(trashedAt != null) 제외 (INV-17) | ⬜ |
| INT-SRV-076 | 일반 목록에서 deleted 설문 제외 | 활성 설문 2건 + 영구 삭제 설문 1건(deleted=true) 존재 | 1. 일반 설문 목록 조회 (OPERATOR 권한) | 200 OK. 활성 설문 2건만 반환. 영구 삭제 설문(deleted=true) 제외 | ⬜ |
| INT-SRV-077 | PUBLISHED 설문만 비운영진에게 노출 | PUBLISHED 설문 2건 + UNPUBLISHED 설문 1건 존재 | 1. MEMBER 권한으로 설문 목록 조회 | 200 OK. PUBLISHED 설문 2건만 반환. UNPUBLISHED 설문은 비운영진에게 미노출 | ⬜ |
| INT-SRV-078 | 운영진 전체 설문 목록 (UNPUBLISHED 포함) 조회 | PUBLISHED 설문 2건 + UNPUBLISHED 설문 1건 존재 (모두 active) | 1. OPERATOR 권한으로 설문 목록 조회 | 200 OK. PUBLISHED 2건 + UNPUBLISHED 1건 = 총 3건 반환. 운영진은 모든 공개 상태의 활성 설문 조회 가능 | ⬜ |
| INT-SRV-079 | 휴지통 목록 조회 (trashedAt != null && deleted = false) | 활성 설문 2건 + 휴지통 설문 2건 + 영구 삭제 설문 1건 존재 | 1. OPERATOR 권한으로 휴지통 목록 조회 | 200 OK. 휴지통 설문 2건만 반환. 활성 설문(trashedAt=null) 및 영구 삭제(deleted=true) 설문 제외 | ⬜ |

---

## 3. 검증 기준 매핑

### 불변조건(INV) 커버리지

| 검증 기준 | 커버 테스트 ID |
|----------|-------------|
| INV-01 (회원 중복 응답 방지) | INT-SRV-061 |
| INV-03 (휴지통 2단계 삭제) | INT-SRV-036, INT-SRV-037 |
| INV-04 (질문 수 1~50) | INT-SRV-049 |
| INV-08 (마감일 자동 응답 마감) | INT-SRV-034 |
| INV-09 (PUBLISHED+OPEN에서만 응답 가능) | INT-SRV-033, INT-SRV-065, INT-SRV-067 |
| INV-11 (응답 재개 시 마감일 검증) | INT-SRV-038, INT-SRV-039 |
| INV-12 (필수 질문 응답 누락 방지) | INT-SRV-063 |
| INV-15 (응답 제출 중 설문 마감 경합) | INT-SRV-064 |
| INV-16 (휴지통 설문 응답 불가) | INT-SRV-066 |
| INV-17 (휴지통 설문 목록 제외) | INT-SRV-075 |
| INV-19 (본인 응답 조회 accessLevel 무관) | INT-SRV-068 |
| INV-20 (비공개 전환 시 자동 응답 마감) | INT-SRV-041 |
| INV-21 (설문 복사 시 상태 초기화) | INT-SRV-046 |
| INV-22 (복사 시 응답 데이터 제외) | INT-SRV-046 |
| INV-23 (복사 시 soft delete 요소 제외) | INT-SRV-046 |
| INV-24 (질문 복사 시 질문 수 제한 검증) | INT-SRV-049 |
| INV-25 (복사된 엔티티 새 ID 부여) | INT-SRV-046, INT-SRV-048 |

### 보안 검증(SEC) 커버리지

| 검증 기준 | 커버 테스트 ID |
|----------|-------------|
| SEC-01 (비운영진 설문 생성) | INT-SRV-003, INT-SRV-004 |
| SEC-02 (비운영진 발행) | INT-SRV-006 |
| SEC-03 (비회원 ASSOCIATE 설문 응답) | INT-SRV-019 |
| SEC-04 (ASSOCIATE MEMBER 설문 응답) | INT-SRV-020 |
| SEC-05 (비운영진 결과 조회) | INT-SRV-015 |
| SEC-06 (비인가 부작용 없음) | INT-SRV-001~015 (모든 403/401 케이스에서 DB 변경 없음 검증) |
| SEC-07 (accessLevel 축소 후 본인 응답 조회) | INT-SRV-068 |
| SEC-09 (비운영진 비공개 전환) | INT-SRV-007 |
| SEC-10 (비운영진 설문 복사) | INT-SRV-013 |
| SEC-11 (비운영진 질문 복사) | INT-SRV-014 |

---

## 4. 구현 현황 요약

| 카테고리 | 전체 | ⬜ |
|---------|:---:|:---:|
| 컨트롤러 RBAC 테스트 (INT-SRV-001~023) | 23 | 23 |
| E2E 시나리오 흐름 (INT-SRV-030~049) | 20 | 20 |
| 응답 제출 통합 테스트 (INT-SRV-060~068) | 9 | 9 |
| 설문 목록 조회 통합 테스트 (INT-SRV-075~079) | 5 | 5 |
| **합계** | **57** | **57** |

---

## 5. 구현된 테스트 클래스 (예정)

### 5.1 SurveyControllerIntegrationTest
- **파일**: `backend/src/test/java/igrus/web/survey/integration/SurveyControllerIntegrationTest.java`
- **기반**: `ServiceIntegrationTestBase` + `@AutoConfigureMockMvc` + MockMvc
- **테스트**: INT-SRV-001~023 (23개)

### 5.2 SurveyE2EIntegrationTest
- **파일**: `backend/src/test/java/igrus/web/survey/integration/SurveyE2EIntegrationTest.java`
- **기반**: `ServiceIntegrationTestBase` (non-transactional)
- **테스트**: INT-SRV-030~049 (20개)

### 5.3 SurveyResponseIntegrationTest
- **파일**: `backend/src/test/java/igrus/web/survey/integration/SurveyResponseIntegrationTest.java`
- **기반**: `ServiceIntegrationTestBase` (non-transactional)
- **테스트**: INT-SRV-060~068 (9개)

### 5.4 SurveyListIntegrationTest
- **파일**: `backend/src/test/java/igrus/web/survey/integration/SurveyListIntegrationTest.java`
- **기반**: `ServiceIntegrationTestBase` (non-transactional)
- **테스트**: INT-SRV-075~079 (5개)

---

## 6. 테스트 실행 가이드

### 전체 통합 테스트

```bash
# 전체 설문 통합 테스트 실행
./gradlew test --tests "igrus.web.survey.integration.*"
```

### 개별 실행

```bash
# 컨트롤러 RBAC 테스트 (INT-SRV-001~023)
./gradlew test --tests "igrus.web.survey.integration.SurveyControllerIntegrationTest"

# E2E 시나리오 흐름 (INT-SRV-030~049)
./gradlew test --tests "igrus.web.survey.integration.SurveyE2EIntegrationTest"

# 응답 제출 통합 테스트 (INT-SRV-060~068)
./gradlew test --tests "igrus.web.survey.integration.SurveyResponseIntegrationTest"

# 목록 조회 통합 테스트 (INT-SRV-075~079)
./gradlew test --tests "igrus.web.survey.integration.SurveyListIntegrationTest"
```

### 주의사항

- 통합 테스트는 `@Transactional`이 **없으므로** 각 테스트에서 `cleanupDatabase()`로 데이터를 정리해야 한다.
- 컨트롤러 테스트는 실제 JWT 토큰을 생성하여 MockMvc에 전달한다.
- E2E 시나리오 테스트는 DB flush/clear 후 상태를 검증하여 영속성 컨텍스트 캐시 문제를 방지한다.
- 응답 제출 경합 테스트(INT-SRV-064)는 타이밍에 민감하므로, 서비스 레이어에서 제출 시점 재검증 로직을 테스트한다.

---

## 7. 관련 문서

- [설문 검증 기준서](../../criteria/survey/survey-criteria-v1.md) — INV-01~25, SEC-01~11, 상태 매트릭스, 시나리오 F1~F20
- [설문 도메인 테스트 케이스](./survey-domain-test-cases.md) — Survey 엔티티/서비스 단위 테스트

---

## 8. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-25 | 최초 작성. 컨트롤러 RBAC(23), E2E 시나리오(20), 응답 제출(9), 목록 조회(5) 총 57건 |
