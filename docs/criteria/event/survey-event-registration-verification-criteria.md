# 설문 연동 행사 신청 (Survey-Event Registration) 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-03-02
> **Scope**: 행사-설문 연결(Event-Survey Linking), 설문 필수 행사 신청(Survey-Required Registration), 설문 없는 행사 신청(Direct Registration), 상태 교차 제약(Cross-Domain State Constraints)
> **상태 모델**: 5축 연동 모델 — Event 3축(visibility + registrationStatus + eventStatus) + Survey 2축(visibility + responseStatus)
> **참고**: Event visibility 축은 EVT-INV-18에 의해 공개 API 접근 제한으로 신청 시점 이전에 걸러진다. 신청 가능성 판단의 주요 축은 registrationStatus, eventStatus, survey.responseStatus이다.
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)

## 목적

이 문서는 **행사(Event)와 설문(Survey) 도메인을 연동**하여, 설문 응답이 행사 신청의 전제조건이 되는 기능에서 **반드시 지켜져야 하는 규칙**을 명시한다. 코드 변경 시 검증 기준으로 사용한다.

현재 두 도메인은 완전히 독립적이다(cross-domain FK 없음). 이 기능은 두 도메인 사이에 **선택적 연결 관계**를 도입하여:

1. 행사 생성 시 설문을 연결할 수 있다 (선택 사항)
2. 설문이 연결된 행사에 신청할 때는 **설문 응답과 행사 신청을 하나의 API로 원자적으로 처리**한다
3. 설문이 없는 행사는 기존처럼 바로 신청 가능하다

### API 분리 원칙

시스템은 3종류의 독립 API 계층을 유지한다:

| API 계층 | 엔드포인트 (예시) | 역할 |
|----------|-----------------|------|
| **행사 단독 API** | `POST /events/{eventId}/registrations` | 설문 없는 행사 신청. 기존 로직 그대로 |
| **설문 단독 API** | `POST /surveys/{surveyId}/responses` | 행사와 무관한 독립 설문 응답 |
| **행사-설문 통합 API** | `POST /events/{eventId}/registrations` (with `surveyAnswers`) | 설문 연동 행사 신청. 설문 응답 + 행사 신청을 단일 트랜잭션으로 처리 |

- 행사 단독 API와 통합 API는 **동일 엔드포인트**를 사용하되, `event.surveyId`의 유무에 따라 서버가 분기한다
- 설문 단독 API는 행사 존재를 모른다 — 기존 설문 도메인 코드 변경 없음

QA Testing 용어 정리 wiki의 10개 영역 중, 이 도메인에 직접 관련된 7개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | Event-Survey 연결 관계, 설문 응답 필수 여부, 기존 규칙과의 호환 |
| 2 | 상태 모델 | 행사-설문 연동 시 상태 교차 제약 (설문 PUBLISHED+OPEN 조건, 행사 registrationStatus 조건) |
| 3 | 시스템 경계와 책임 분리 | 행사 도메인과 설문 도메인 간 결합도 관리, 트랜잭션 경계 |
| 4 | 입력 도메인 분할과 경계값 | 설문 연결/해제 시점, 응답 시점, 신청 시점의 경계 조건 |
| 5 | 권한/보안 정책 | 설문 연동 행사의 RBAC, 설문 accessLevel과 행사 권한의 관계 |
| 6 | 관측 가능성 | 설문-행사 연동 이력, 신청 거부 사유 추적 |
| 7 | 테스트 전략 | 검증 항목별 테스트 매핑, 도메인 간 통합 테스트 |

---

## 기존 검증 기준과의 관계

이 문서는 기존 검증 기준서의 **확장**이다. 기존 불변조건을 깨뜨리지 않으면서 새로운 교차 도메인 제약을 추가한다.

| 기존 문서 | 관계 | 주요 참조 항목 |
|----------|------|-------------|
| [행사 검증 기준서](./event-verification-criteria.md) | **호환** | EVT-INV-05 (초기 상태), EVT-INV-07 (상태별 수정 정책), EVT-INV-12 (유효 복합 상태) |
| [행사 신청 검증 기준서](./event-registration-verification-criteria.md) | **확장** | REG-INV-05 (OPEN + 기간 내 신청), REG-INV-01 (중복 신청 방지), REG-INV-06 (시간 겹침) |
| [설문 검증 기준서](../survey/survey-criteria-v1.md) | **호환** | INV-01 (중복 응답 방지), INV-09 (PUBLISHED+OPEN 응답 가능), INV-26 (응답 수정) |

**호환성 원칙**: 설문이 연결되지 않은 행사는 모든 기존 행사/신청 불변조건이 그대로 적용된다. 설문이 연결된 행사는 기존 불변조건에 추가로 이 문서의 불변조건이 적용된다.

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

### 1.1. Event-Survey 연결 관계

#### SEVT-INV-01: 행사-설문 연결은 선택 사항

> 행사는 설문 없이 생성될 수 있다. 설문이 연결되지 않은 행사(`surveyId == null`)는 기존 행사 규칙이 그대로 적용된다.

- **사전조건**: 행사 생성/수정 요청
- **사후조건**: `event.surveyId == null`인 행사는 기존 `EventRegistrationService.registerEvent()` 로직이 변경 없이 동작
- **검증 방법**: 설문 미연결 행사에 기존 행사 신청 테스트를 그대로 실행하여 통과 확인 (회귀 테스트)

#### SEVT-INV-02: 행사당 설문은 최대 1개

> 하나의 행사에 연결할 수 있는 설문은 최대 1개이다. 1:N 관계가 아닌 1:0..1 관계이다.

- **사전조건**: 행사 생성/수정 시 `surveyId` 파라미터
- **사후조건**: `event.surveyId`는 null 또는 유효한 단일 설문 ID
- **위반 시**: 여러 설문이 하나의 행사에 연결되어 신청 조건이 모호해짐
- **DB 제약**: `events.event_survey_id` 컬럼 — nullable FK (또는 nullable Long, 약한 참조)

#### SEVT-INV-03: 설문-행사 1:1 연결 제약

> 하나의 설문은 최대 하나의 행사에만 연결할 수 있다. 설문:행사 = 1:1 관계이다.

- **근거**: 행사 상태 변경 시 설문 상태를 자동 연동하기 위해 1:1 관계가 필요. N:1 관계에서는 한 행사의 취소가 다른 행사에 연결된 같은 설문에 영향을 주는 부작용이 발생
- **사후조건**: 동일한 `surveyId`가 두 개 이상의 행사에 설정될 수 없음
- **DB 제약**: `events.event_survey_id` 컬럼에 UNIQUE 제약 (`uk_events_survey_id`). nullable이므로 다수의 NULL은 허용
- **위반 시 예외**: `SurveyAlreadyLinkedToEventException` (409)
- **검증 시점**: 행사 생성 시 `findBySurveyId()`, 행사 수정 시 `existsBySurveyIdAndIdNot()`으로 검증

#### SEVT-INV-04: 연결 대상 설문의 존재 검증

> 행사에 설문을 연결할 때, 해당 설문이 존재하고 활성 상태여야 한다.

- **사전조건**: `surveyId != null`인 행사 생성/수정 요청
- **검증**: `Survey`가 존재하고 `deleted == false`이고 `trashedAt == null`
- **위반 시 예외**: `SurveyNotFoundException` 또는 적절한 예외
- **설계 결정**: 연결 시점에 설문의 `visibility`나 `responseStatus`는 검증하지 않음. 설문이 아직 UNPUBLISHED 상태여도 행사에 미리 연결해둘 수 있음. 실제 검증은 신청 시점에 수행

#### SEVT-INV-05: 설문 연결은 행사 생성 시 또는 수정 시 설정

> 설문 연결(`surveyId`)은 행사 생성 시 지정하거나, 행사 수정 시 추가/변경/해제할 수 있다.

- **수정 가능 상태**: EVT-INV-07의 상태별 수정 정책을 따름
  - `eventStatus == UPCOMING`: 설문 연결/변경/해제 가능
  - `eventStatus == ONGOING`: 설문 연결/변경/해제 가능 (정보성 필드에 준함)
  - `eventStatus == CANCELED`: 설문 연결/변경/해제 가능
  - `eventStatus == COMPLETED`: 수정 불가
- **EVT-INV-07과의 관계**: EVT-INV-07의 ONGOING 수정 표는 기존 필드(title, description 등) 기준이며, `surveyId`는 이 문서(SEVT-INV-05)의 정의를 우선한다. `surveyId`는 정보성 필드에 준하여 ONGOING 상태에서도 변경 가능하다. **구현 시 EVT-INV-07의 상태별 수정 표에 `surveyId` 행을 명시적으로 추가하여 이 문서와 일관성을 유지할 것을 권장한다.**
- **설문 변경은 행사 수정 API를 통해 이루어짐**: 설문 연결/변경/해제는 행사 수정 API(EVT-INV-07)를 통해 수행되며, ONGOING 상태의 부분 수정 제한 대상이 아닌 필드로 처리된다. 별도의 설문 연결 API는 존재하지 않는다.
- **신청자 존재 시 설문 변경 제약**: 이미 설문 응답을 완료하고 행사에 신청한 사용자가 존재하는 상태에서 설문을 변경하면 기존 신청의 근거가 달라짐. 이에 대한 정책:
  - **정책 A (확정 — 허용)**: 설문 변경을 허용하되, 기존 신청은 유효하게 유지. 새 신청자만 새 설문 기준 적용. 운영 유연성 우선. (DECISION-01 확정)
  - ~~정책 B (엄격 — 차단): 활성 신청이 존재하면 설문 변경 차단. 데이터 정합성 우선.~~
- **설문 해제 시**: `surveyId = null`로 설정. 이후 신청자는 설문 없이 바로 신청 가능. 기존 설문 응답이 있는 신청은 유효하게 유지

### 1.2. 설문 응답 필수 여부

#### SEVT-INV-06: 설문 연결 행사의 신청 — 통합 API를 통한 원자적 처리

> `event.surveyId != null`인 행사에 신청할 때, 설문 응답 데이터(`surveyAnswers`)를 신청 요청에 포함하여 **설문 응답 저장과 행사 신청을 단일 트랜잭션으로 처리**한다.

- **사전조건**: `event.surveyId != null`
- **요청 데이터**: 행사 신청 요청 body에 `surveyAnswers` 필드 포함 (설문 질문에 대한 응답 배열)
- **처리 흐름**: 1) 설문 응답 유효성 검증 → 2) `SurveyResponse` 저장 → 3) `EventRegistration` 생성 → 4) 커밋 (또는 전체 롤백)
- **원자성 보장**: 설문 응답 저장과 행사 신청이 하나의 트랜잭션에서 처리된다. 정원 초과 등으로 신청이 실패하면 설문 응답도 저장되지 않는다
- **위반 시 예외**:
  - `surveyAnswers`가 누락된 경우: `SurveyResponseRequiredException` (신규 예외)
  - 설문 응답 유효성 검증 실패: 기존 설문 도메인의 validation 예외 위임
- **이미 설문 응답이 존재하는 경우**: 동일 설문에 이미 응답을 제출한 사용자는 `surveyAnswers`를 생략할 수 있다. 이 경우 기존 응답 존재 여부(`existsBySurveyIdAndUserId`)만 확인하고 신청을 진행한다
- **기존 응답 존재 + `surveyAnswers` 포함 시**: `surveyAnswers`는 무시하고 기존 응답으로 신청을 진행한다. 응답을 수정하려면 설문 단독 수정 API(`PUT /surveys/{surveyId}/responses/me`, INV-26)를 별도로 호출해야 한다. 근거: 행사 신청과 설문 응답 수정은 별개의 관심사이며, 통합 API에서 암묵적 갱신을 허용하면 INV-01(중복 응답 방지) 위반 위험이 있다.
- **검증 시점**: 신청(`registerEvent`) 및 재신청(`reRegister`) 시 모두 적용
- **승인(approve) 시**: 설문 응답 재검증을 수행하지 않음.
  - 근거: 신청 시점(WAITING 생성 시)에 이미 검증이 완료되었고, 응답 삭제는 1차 미지원(설문 INV-27)이므로 승인 시점에 응답이 소멸할 수 없음.
  - 향후 응답 삭제(INV-27 미구현 → 구현 전환) 시 이 정책을 재검토할 것.
- **검증 기준의 기준 시점**: 행사의 `surveyId`가 변경되었더라도 항상 현재 `event.surveyId` 기준으로 검증. 과거 연결되었던 설문의 응답은 고려하지 않는다.
- **교차 참조**: 설문 INV-01 (중복 응답 방지), REG-INV-05 (OPEN + 기간 내 신청)

#### SEVT-INV-07: 설문 미연결 행사의 기존 동작 보존

> `event.surveyId == null`인 행사는 설문 응답 검증을 수행하지 않는다. 기존 `registerEvent()` 로직이 그대로 적용된다.

- **검증 방법**: 기존 행사 신청 테스트 전체가 변경 없이 통과하는지 확인 (회귀 테스트)
- **교차 참조**: REG-INV-01~14 전체

#### SEVT-INV-08: 통합 API의 원자성과 롤백 보장

> 설문 연결 행사의 신청에서, 설문 응답 저장과 행사 신청 사이에 **중간 상태(고아 데이터)**가 발생하지 않는다. 트랜잭션 실패 시 양쪽 모두 롤백된다.

> **SEVT-INV-06과의 역할 분리**: SEVT-INV-06은 "설문 응답 필수 여부 및 기존 응답 재사용 정책"을 정의하고, SEVT-INV-08은 "원자성과 롤백 보장"을 정의한다.

- **고아 데이터 방지**: 설문 응답만 저장되고 행사 신청이 실패하는 상태가 발생하지 않음. 정원 초과, 시간 겹침 등으로 신청이 실패하면 설문 응답도 저장되지 않음
- **롤백 시나리오**: 설문 응답 저장 성공 → 정원 확인 실패(`EventCapacityFullException`) → 설문 응답 + 행사 신청 모두 롤백
- **3종 API와의 관계**:
  - 행사 단독 API: `surveyId == null`인 행사 → 기존 로직 그대로 (설문 무관)
  - 설문 단독 API: 행사와 무관한 독립 설문 → 기존 설문 응답 API 그대로
  - 행사-설문 통합 API: `surveyId != null`인 행사 → 이 불변조건 적용
- **설문 단독 API와의 독립성**: 설문 단독 API로 제출된 응답은 행사 신청을 트리거하지 않는다. 단, 통합 API 호출 시 이미 해당 설문에 응답이 존재하면 `surveyAnswers`를 생략할 수 있다 (SEVT-INV-06 참조)

#### SEVT-INV-09: 설문 응답 수정이 기존 신청에 영향 없음

> 사용자가 이미 신청 완료한 후 설문 응답을 수정(PUT 전체 교체, INV-26)하더라도, 기존 행사 신청의 상태는 변경되지 않는다.

- **근거**: 설문 응답은 신청의 **전제조건**(gate)이지 **지속조건**(invariant)이 아님. 한번 통과한 후에는 행사 신청이 독립적으로 관리됨
- **주의사항**: 설문 응답 삭제 기능은 현재 미지원 (설문 INV-27). 만약 향후 응답 삭제가 지원되더라도, 이미 완료된 행사 신청은 영향받지 않아야 함

### 1.3. 연결된 설문의 상태 제약

#### SEVT-INV-10: 신청 시점의 설문 상태 검증

> 설문 연결 행사에 신청할 때, 연결된 설문이 응답을 수집하고 있거나 이미 수집을 마감한 상태여야 한다.

- **명시적 검증 조건**: `survey.responseStatus != NOT_STARTED && survey.trashedAt == null && survey.deleted == false`
- **`visibility`는 검증 대상이 아님**: `visibility`(PUBLISHED/UNPUBLISHED)는 신청 가능 여부 판단에 사용하지 않는다. 근거: `visibility`는 설문의 목록 노출 여부를 제어하는 속성이며, 이미 응답을 완료한 사용자의 행사 신청 자격에 영향을 주지 않아야 한다. 또한 UNPUBLISHED 전환 시 자동으로 `responseStatus`가 CLOSED로 변경되므로(설문 INV-20), `responseStatus`만으로 충분한 판단이 가능하다.

- **설문 상태별 신청 가능 여부** (전체 조합 매트릭스):

| 설문 visibility | 설문 responseStatus | 신청 가능 여부 | 근거 |
|:---:|:---:|:---:|------|
| PUBLISHED | OPEN | **가능** (응답 미제출이면 먼저 응답 유도) | 정상 시나리오 |
| PUBLISHED | CLOSED | **가능** (기존 응답 존재 시) | 마감 후에도 기존 응답으로 신청 가능 |
| PUBLISHED | NOT_STARTED | **불가** | 설문이 아직 시작되지 않아 응답 불가능 |
| UNPUBLISHED | NOT_STARTED | **불가** | 비공개 + 미시작, 응답 경로 없음 |
| UNPUBLISHED | OPEN | **발생 불가** | 설문 INV-20에 의해 UNPUBLISHED 전환 시 responseStatus가 자동으로 CLOSED로 변경되므로 이 조합은 시스템에서 발생하지 않음 |
| UNPUBLISHED | CLOSED | **가능** (기존 응답 존재 시) | INV-20에 의해 PUBLISHED+OPEN에서 비공개 전환 시 자동 CLOSED로 변경됨. 전환 전 OPEN 상태에서 응답 제출이 가능했으므로 기존 응답 존재 가능 |

- **핵심 판단 기준**: `survey.responseStatus != NOT_STARTED`. 한번이라도 응답 수집이 시작되었으면(`OPEN` 또는 `CLOSED`), 해당 설문에 대한 기존 응답이 존재할 수 있으므로 신청을 허용
- **NOT_STARTED 차단 근거**: 응답 수집이 시작되지 않은 설문에는 응답을 제출할 수 있는 경로 자체가 없으므로, SEVT-INV-06의 전제조건(응답 존재)을 충족할 수 없음
- **위반 시 예외**: `SurveyNotReadyException` (신규 예외)

#### SEVT-INV-11: 설문 삭제/휴지통 시 행사 신청 정책

> 연결된 설문이 휴지통에 이동하거나 영구 삭제된 경우의 정책:

| 설문 상태 | 기존 신청 | 신규 신청 | 근거 |
|----------|:---:|:---:|------|
| 활성 (trashedAt == null) | 유효 | 가능 | 정상 상태 |
| 휴지통 (trashedAt != null) | **유효** | **불가** | 기존 신청은 보존하되, 새 응답/신청 차단 |
| 영구 삭제 (deleted == true) | **유효** | **불가** | 기존 신청은 보존하되, 설문 자체 조회 불가 |

- **기존 신청 유효 근거**: SEVT-INV-09와 동일. 설문 응답은 신청의 전제조건이지 지속조건이 아님
- **신규 신청 차단 근거**: 휴지통/삭제된 설문에는 새 응답을 제출할 수 없으므로 (설문 INV-16) 전제조건 충족 불가
- **검증**: 신청 시 `survey.trashedAt == null && survey.deleted == false` 확인. 위반 시 `SurveyNotFoundException` 또는 `SurveyNotReadyException`

### 1.4. 기존 불변조건과의 호환

#### SEVT-INV-12: 기존 행사 신청 불변조건 전체 보존

> 설문 연동 행사에서도 기존 행사 신청 불변조건(REG-INV-01~14)이 모두 적용된다. 설문 응답 검증은 기존 검증에 **추가**되는 것이지, 기존 검증을 **대체**하지 않는다.

- **검증 순서** (권장):
  1. 권한 확인 (REG-INV-04: 준회원 차단)
  2. Lazy Evaluation (행사 상태 갱신)
  3. 중복 신청 확인 (REG-INV-01)
  4. 등록 상태 확인 (REG-INV-05: registrationStatus == OPEN)
  5. 신청 기간 확인
  6. **설문 응답 확인 (SEVT-INV-06, 10, 11)** ← 신규 추가
  7. 시간 겹침 확인 (REG-INV-06)
  8. 정원 확인 및 원자적 UPDATE

- **visibility 검증 위치**: 행사 visibility(`PUBLISHED`/`UNPUBLISHED`)는 검증 순서에 별도 단계로 포함하지 않음. 근거: EVT-INV-18에 의해 공개 API에서 UNPUBLISHED 행사 조회 자체가 차단(404)되므로, 신청 로직에 도달하기 이전에 이미 걸러진다. 관리자 API에서는 visibility 제약 없이 행사를 조회하지만, 관리자 API에서 직접 신청하는 시나리오는 없다.
- **설문 검증 위치 근거**: 6번에 배치하는 이유는, 설문 응답 부재 시 정원 소진이나 시간 겹침 검증보다 먼저 빠르게 실패하여 불필요한 DB 조회를 방지하기 위함

#### SEVT-INV-13: 기존 설문 불변조건 전체 보존

> 설문 도메인의 기존 불변조건(INV-01~30)은 모두 유지된다. 행사에 연결되었다는 이유로 설문의 동작이 변경되지 않는다.

- **특히 중요한 항목**:
  - INV-01: 회원 중복 응답 방지 (설문당 1회 응답)
  - INV-09: PUBLISHED + OPEN 상태에서만 응답 가능
  - INV-26: 응답 수정은 OPEN 상태에서만 가능

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. 행사-설문 연동 상태 교차 제약

행사의 3축 상태(visibility + registrationStatus + eventStatus)와 설문의 2축 상태(visibility + responseStatus)가 교차하는 지점에서 신청 가능 여부가 결정된다. 단, 행사 visibility 축은 EVT-INV-18에 의해 공개 API에서 UNPUBLISHED 행사 접근이 차단되므로, 신청 가능 여부 판단의 실질적 교차 축은 registrationStatus × survey.responseStatus이다.

#### 신청 가능 조건 (모든 조건 AND)

```
신청 가능 = 행사 조건 AND 설문 조건

행사 조건:
  - event.visibility == PUBLISHED (EVT-INV-18: 공개 API 접근 전제)
  - event.registrationStatus == OPEN
  - registrationStartAt <= now <= registrationEndAt
  - 정원 여유 (선착순) 또는 WAITING 가능 (선발제)

설문 조건 (event.surveyId != null일 때만):
  - survey.responseStatus != NOT_STARTED
  - survey.trashedAt == null
  - survey.deleted == false
  - surveyResponse(surveyId, userId) 존재
```

#### 2-1-1. 행사 상태 × 설문 상태 교차 매트릭스

`event.surveyId != null`인 행사에 대해, 행사 visibility, registrationStatus와 설문 responseStatus 조합별 신청 가능 여부:

| 행사 visibility | 행사 registrationStatus | 설문 responseStatus | 신청 가능 | 근거 |
|:---:|:---:|:---:|:---:|------|
| UNPUBLISHED | ANY | ANY | **불가** | EVT-INV-18: 비공개 행사는 공개 API에서 조회 불가 (404 반환) |
| PUBLISHED | NOT_STARTED | ANY | **불가** | 행사 등록 미시작 (REG-INV-05) |
| PUBLISHED | OPEN | NOT_STARTED | **불가** | 설문 미시작, 응답 경로 없음 (SEVT-INV-10) |
| PUBLISHED | OPEN | OPEN | **가능** (응답 존재 시) | 정상 시나리오 |
| PUBLISHED | OPEN | CLOSED | **가능** (응답 존재 시) | 설문 마감 후 기존 응답으로 신청 |
| PUBLISHED | CLOSED | ANY | **불가** | 행사 등록 마감 (REG-INV-05) |

#### 2-1-2. 설문 라이프사이클과 행사 라이프사이클의 시간 관계

설문과 행사의 라이프사이클은 독립적이지만, 실질적으로 다음과 같은 시간 순서가 운영상 권장된다:

```
시간 →

설문: [UNPUBLISHED] → publish → [PUBLISHED, NOT_STARTED] → openResponse → [PUBLISHED, OPEN] → closeResponse → [PUBLISHED, CLOSED]
                                                                    ↓
행사: [NOT_STARTED, UPCOMING] ─── registrationStartAt 도래 ──→ [OPEN, UPCOMING] → ... → [CLOSED, ...]
                                                                    ↑
                                    설문 OPEN이 행사 OPEN보다 선행되어야
                                    사용자가 응답 후 신청 가능
```

- **권장 순서**: 설문 `openResponse` → 행사 `registrationStartAt` 도래
- **강제하지 않는 이유**: 시스템이 시간 순서를 강제하면 운영 유연성이 저하됨. 행사가 먼저 OPEN되어도, 사용자가 설문에 아직 응답하지 못했을 뿐 시스템 무결성은 훼손되지 않음
- **사용자 경험**: 행사가 OPEN이고 설문이 아직 NOT_STARTED이면, 프론트엔드에서 "설문이 아직 시작되지 않았습니다" 안내 표시 (SEVT-INV-10에 의해 신청 차단)

### 2-2. 설문 상태 변경이 행사에 미치는 영향

| 설문 상태 변경 | 기존 행사 신청 | 신규 행사 신청 | 비고 |
|-------------|:---:|:---:|------|
| OPEN → CLOSED (수동/자동 마감) | 유효 | 가능 (기존 응답 존재 시) | 설문 마감 ≠ 행사 마감 |
| PUBLISHED → UNPUBLISHED (비공개 전환) | 유효 | 가능 (기존 응답 존재 시) | 비공개 전환 시 자동 CLOSED (INV-20) |
| trash() (휴지통 이동) | 유효 | **불가** | SEVT-INV-11 |
| permanentDelete() | 유효 | **불가** | SEVT-INV-11 |
| CLOSED → OPEN (응답 재개) | 유효 | 가능 (새 응답 제출 가능) | 운영 유연성 |

**핵심 원칙**: 설문의 상태 변경은 기존 행사 신청의 유효성에 영향을 주지 않는다.

### 2-3. 행사 상태 변경이 설문에 미치는 영향

행사 상태 변경 시 연결된 설문의 상태를 `EventSurveySyncService` (이벤트 리스너)를 통해 자동 동기화한다.

| 행사 상태 변경 (EventChangeType) | 설문 액션 | 비고 |
|-------------|:---:|------|
| `EVENT_CANCELED` (행사 취소) | OPEN → CLOSED (`closeResponse()`) | 행사 취소 시 설문 응답 불필요 |
| `EVENT_UNPUBLISHED` (행사 비공개) | PUBLISHED → UNPUBLISHED (`unpublish()`, 자동으로 OPEN → CLOSED) | 비공개 전환 시 설문도 비공개 |
| `EVENT_PUBLISHED` (행사 공개) | UNPUBLISHED → PUBLISHED (`publish()`) | 공개 시 설문도 공개 |
| `REGISTRATION_CLOSED_MANUAL` (모집 마감) | OPEN → CLOSED (`closeResponse()`) | 모집 마감 시 설문도 마감 |
| `REGISTRATION_REOPENED` (모집 재개) | CLOSED → OPEN (`openResponse()`) | 모집 재개 시 설문도 재개 |
| `EVENT_REACTIVATED` (행사 재활성화) | UNPUBLISHED → PUBLISHED + CLOSED → OPEN | 재활성화 시 설문도 공개 + 응답 재개 |
| `REGISTRATION_CANCELED_BY_ADMIN` (개별 신청 취소) | **변경 없음** | 개별 신청 취소이므로 설문 무관 |
| 행사 삭제 (soft delete) | **변경 없음** | 삭제는 도메인 이벤트를 발행하지 않음 |
| Lazy Evaluation에 의한 등록 시작 (NOT_STARTED → OPEN) | NOT_STARTED → PUBLISHED + OPEN (`publishSurveyIfUnpublished()` + `openResponse()`) | `EventStatusHelper`가 전이 감지 시 `EventSurveySyncService.openSurveyForRegistration(eventId)` 직접 호출 |

**동기화 방식**:
- **명시적 상태 변경** (사용자 액션): `@EventListener` + `TransactionTemplate(PROPAGATION_REQUIRES_NEW)` — `RecordEventStatusChangeService`와 동일 패턴
- **Lazy Evaluation 전이** (시간 기반 자동 전이): `EventStatusHelper`가 NOT_STARTED→OPEN 전이 감지 시 `EventSurveySyncService.openSurveyForRegistration(eventId)`를 직접 호출. 도메인 이벤트를 발행하지 않고 직접 호출하는 이유는 Helper가 이벤트를 발행하면 SRP 위반이며, 트랜잭션 커밋 전 이벤트 발행으로 인한 불일치 가능성이 있기 때문

두 방식 모두 best-effort: 동기화 실패 시 로그만 기록하고 행사 작업에 영향 없음.

**엣지 케이스**:
- `surveyId == null`: 아무 동작 안 함
- 설문이 삭제/휴지통 상태: skip + 경고 로그
- 설문이 이미 목표 상태: 멱등 처리 (변경 없음, DEBUG 로그)
- `EVENT_PUBLISHED` 시 설문 publish 실패 (질문 0개 등): 경고 로그, 행사 공개는 정상 진행

**핵심 원칙**: 행사의 상태 변경은 이벤트 리스너를 통해 연결된 설문의 상태에 영향을 준다. SEVT-INV-03의 1:1 관계 제약 하에서 안전하게 동기화된다.

---

## 3. 시스템 경계와 책임 분리 (System Boundary & SoC)

### 3-1. 도메인 간 결합 설계

#### 단방향 의존: 행사 → 설문

```
Event 도메인                      Survey 도메인
┌────────────────┐               ┌────────────────┐
│ Event          │               │ Survey         │
│  surveyId: Long│──── 조회 ───→ │  id: Long      │
│                │               │                │
│ EventRegistra- │               │ SurveyResponse │
│ tionService    │──── 조회 ───→ │  surveyId      │
│                │               │  userId        │
└────────────────┘               └────────────────┘
```

- **Event → Survey**: Event 엔티티가 `surveyId` (Long)로 설문을 참조. JPA 연관관계(`@ManyToOne`) 없이 ID만 보유
- **Survey → Event**: Survey는 Event의 존재를 모름. 어떤 참조도 없음
- **의존 방향 근거**: "설문이 행사 신청의 전제조건"이므로, 행사 도메인이 설문 도메인을 조회하는 것이 자연스러움
- **상태 동기화**: `EventSurveySyncService`가 설문 상태를 동기화. 명시적 상태 변경은 `EventStatusChanged` 도메인 이벤트를 수신하여 처리하고, Lazy Evaluation 전이(NOT_STARTED→OPEN)는 `EventStatusHelper`가 직접 호출하여 처리. 행사 도메인이 설문 도메인의 상태 전이 메서드(`publish()`, `unpublish()`, `openResponse()`, `closeResponse()`)를 호출하는 단방향 의존을 유지

#### 약한 참조 (Weak Reference) 채택 이유

| 방식 | FK 제약 | 장점 | 단점 |
|------|:---:|------|------|
| **약한 참조 (Long surveyId)** | 없음 | 도메인 독립성 유지, soft delete 호환, 마이그레이션 용이 | 참조 무결성 수동 관리 필요 |
| 강한 참조 (@ManyToOne Survey) | 있음 | 참조 무결성 자동 보장 | 도메인 간 결합, 설문 삭제 시 cascade 문제 |

- **약한 참조 채택**: 두 도메인의 독립적 배포/변경을 보장하고, soft delete 시 FK 제약 충돌을 방지
- **참조 무결성**: 서비스 레벨에서 설문 존재 여부를 검증 (SEVT-INV-04, 11)
- **교차 참조**: 저장소 도메인의 약한 참조 패턴과 동일한 설계 철학

### 3-2. 트랜잭션 경계

#### 3종 API별 트랜잭션 모델

| API 계층 | 트랜잭션 | 서비스 |
|----------|:---:|------|
| 행사 단독 API (surveyId == null) | Tx-1 | `EventRegistrationService.registerEvent()` — 기존 그대로 |
| 설문 단독 API (행사 무관) | Tx-A | `SurveyResponseService.submitResponse()` — 기존 그대로 |
| **행사-설문 통합 API** (surveyId != null) | **Tx-C (단일)** | `EventRegistrationService.registerEventWithSurvey()` — **신규** |

#### 통합 API 트랜잭션 (Tx-C)

설문 응답 저장과 행사 신청을 **단일 트랜잭션**으로 처리한다.

- **원자성**: 설문 응답 저장 + 행사 신청이 모두 성공하거나 모두 롤백
- **고아 데이터 방지**: 정원 초과 등으로 신청이 실패하면 설문 응답도 저장되지 않음
- **정합성 책임**: 서버가 담당. 클라이언트는 한 번의 API 호출로 완료

```
registerEventWithSurvey(eventId, userId, surveyAnswers) {
    // 기존 검증 (권한, 상태, 중복, 기간, 시간 겹침 등) ...

    // 설문 상태 검증
    Survey survey = surveyRepository.findById(event.surveyId);
    validateSurveyState(survey);  // NOT_STARTED, 삭제, 휴지통 체크

    // 설문 응답 처리
    boolean hasResponse = surveyResponseRepository
        .existsBySurveyIdAndUserId(event.surveyId, userId);

    if (hasResponse) {
        // 기존 응답 존재 → surveyAnswers 유무와 무관하게 기존 응답으로 진행
        // (응답 수정은 설문 단독 API로만 가능, SEVT-INV-06)
    } else if (surveyAnswers != null) {
        // 기존 응답 없음 + 새 응답 데이터 포함 → 새 응답 저장
        saveSurveyResponse(survey, userId, surveyAnswers);
    } else {
        // 기존 응답 없음 + surveyAnswers 미포함 → 거부
        throw new SurveyResponseRequiredException();
    }

    // 행사 신청 로직 (정원 확인, 원자적 UPDATE 등) ...
}
```

### 3-3. 서비스 레이어 책임 분리

| 서비스 | 책임 | 설문 연동 관련 변경 |
|--------|------|:---:|
| `EventService` | 행사 CRUD, 상태 관리 | 생성/수정 시 `surveyId` 처리 (존재 검증) |
| `EventRegistrationService` | 행사 신청, 취소, 승인/거절 | **`registerEventWithSurvey()` 신규 메서드 추가** — 설문 응답 저장 + 행사 신청을 단일 트랜잭션으로 처리 |
| `SurveyService` | 설문 CRUD, 상태 관리 | **변경 없음** |
| `SurveyResponseService` | 설문 응답 CRUD | **변경 없음** (통합 API에서 응답 저장 로직을 위임받아 호출될 수 있음) |
| `SurveyAnswerValidator` | 설문 응답 유효성 검증 | **변경 없음** (통합 API에서 검증 로직을 재사용) |

- **변경 최소화 원칙**: 설문 도메인의 서비스 코드는 변경하지 않음. 행사 도메인의 서비스가 설문 도메인의 기존 서비스/검증기를 **호출**하는 방식
- **위임 패턴**: `EventRegistrationService`가 `SurveyResponseService`의 내부 로직(응답 저장, 유효성 검증)을 직접 구현하지 않고, 기존 서비스에 위임하여 로직 중복 방지

---

## 4. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 4-1. 행사 생성 시 설문 연결 입력값

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 |
|------|-----------|-----------|--------|
| `surveyId` (nullable) | null (설문 미연결), 존재하는 활성 설문 ID | 존재하지 않는 ID, 삭제된 설문 ID, 휴지통 설문 ID | null (미연결), 유효 ID (연결) |

### 4-2. 설문 연결 행사 신청 시점 경계값

| 시나리오 | 설문 응답 존재 | 설문 상태 | 신청 결과 |
|---------|:---:|:---:|:---:|
| 응답 제출 완료, 설문 OPEN | O | OPEN | **성공** |
| 응답 제출 완료, 설문 CLOSED (마감 후) | O | CLOSED | **성공** |
| 응답 미제출, 설문 OPEN | X | OPEN | **실패** (SurveyResponseRequiredException) |
| 응답 미제출, 설문 CLOSED | X | CLOSED | **실패** (SurveyResponseRequiredException) |
| 응답 미제출, 설문 NOT_STARTED | X | NOT_STARTED | **실패** (SurveyNotReadyException) |
| 응답 제출 완료, 설문 NOT_STARTED | 논리적 불가능 | NOT_STARTED | **불가** (응답은 OPEN일 때만 제출 가능) |
| 응답 제출 완료, 설문 휴지통 | O | ANY (trashed) | **실패** (SurveyNotFoundException) |
| 응답 제출 완료, 설문 삭제 | O | ANY (deleted) | **실패** (SurveyNotFoundException) |

### 4-3. 설문 연결 변경 경계값

> **"기존 신청자"의 범위**: 활성 신청(isActive == true, 즉 WAITING/REGISTERED/APPROVED 상태)을 가진 사용자를 의미한다. CANCELED 상태의 신청자는 재신청 시 현재 `event.surveyId` 기준이 적용되므로(SEVT-INV-06) "기존 신청자"에 포함하지 않는다.

| 시나리오 | 기존 surveyId | 새 surveyId | 기존 활성 신청자 | 결과 |
|---------|:---:|:---:|:---:|:---:|
| 처음 설문 연결 | null | 유효 ID | 없음 | **성공** |
| 처음 설문 연결 | null | 유효 ID | 있음 | **성공** (기존 신청 유지, DECISION-01 확정) |
| 설문 변경 | ID-A | ID-B | 없음 | **성공** |
| 설문 변경 | ID-A | ID-B | 있음 (ID-A 응답 기반) | **성공** (기존 신청 유지, DECISION-01 확정) |
| 설문 해제 | 유효 ID | null | 있음 | **성공** (기존 신청 유지) |
| 설문 해제 | 유효 ID | null | 없음 | **성공** |
| 삭제된 설문으로 변경 | null | 삭제 ID | 없음 | **실패** (SurveyNotFoundException) |
| 휴지통 설문으로 변경 | null | 휴지통 ID | 없음 | **실패** (SurveyNotFoundException) |

### 4-4. 설문 1:1 연결 제약 경계값

| 시나리오 | 결과 |
|---------|:---:|
| 행사 A에 설문 S 연결 → 행사 B에 동일 설문 S 연결 시도 | **실패** (`SurveyAlreadyLinkedToEventException`, 409) |
| 행사 A에 설문 S 연결 → 행사 A 수정 시 동일 설문 S 유지 | **성공** (자기 자신 제외 검증) |
| 행사 A에 설문 S 연결 → 행사 A에서 설문 해제 → 행사 B에 설문 S 연결 | **성공** (해제 후 재연결) |
| 행사 A에 설문 S 연결 → 행사 A soft delete → 행사 B에 설문 S 연결 시도 | **주의** (@SQLRestriction에 의한 soft delete 필터링 동작에 따라 다름) |

### 4-5. 재신청(reRegister) 시 설문 검증 경계값

| 시나리오 | 기존 상태 | 설문 응답 | 결과 |
|---------|:---:|:---:|:---:|
| 취소 후 재신청, 응답 존재 | CANCELED | O | **성공** (REG-INV-11 + SEVT-INV-06) |
| 취소 후 재신청, 응답 미존재 (불가능 시나리오) | CANCELED | X | **실패** (SurveyResponseRequiredException) |
| 취소 후 재신청, 설문 해제됨 | CANCELED | O (과거) | **성공** (surveyId == null이므로 설문 검증 생략) |

---

## 5. 권한/보안 정책 (RBAC & Authorization)

### 5-1. 설문 accessLevel과 행사 권한의 관계

설문의 `accessLevel`과 행사 신청 권한은 **독립적**으로 적용된다.

| 검증 | 담당 도메인 | 적용 시점 |
|------|:---:|:---:|
| 행사 신청 권한 (MEMBER 이상) | 행사 | 신청 시 (REG-INV-04) |
| 설문 응답 권한 (accessLevel) | 설문 | 응답 제출 시 (설문 INV-09) |

- **정합성 조건**: 설문의 `accessLevel`이 행사 신청 권한보다 높으면, 행사 신청 자격은 있지만 설문 응답 자격이 없는 사용자가 발생
  - 예: 행사 신청은 MEMBER 이상, 설문 accessLevel이 OPERATOR → MEMBER는 설문 응답 불가 → 행사 신청 불가
  - **시스템 차원 강제 여부**: 강제하지 않음. 운영진의 책임으로 위임. 단, 프론트엔드에서 경고 표시 권장
- **SEC-SEVT-01**: 설문 `accessLevel`이 행사 신청 가능 권한보다 제한적인 경우, 프론트엔드 경고 (백엔드 차단 아님)

### 5-2. 역할별 접근 제어 매트릭스 (신규/변경 항목만)

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 행사 생성 (설문 연결 포함) | 401 | 403 | 403 | **O** | **O** |
| 행사 수정 (설문 변경) | 401 | 403 | 403 | **O** | **O** |
| 설문 연결 행사 신청 | 401 | **403** | **O** (설문 응답 필요) | **O** (설문 응답 필요) | **O** (설문 응답 필요) |
| 설문 응답 제출 | (accessLevel에 따름) | (accessLevel에 따름) | (accessLevel에 따름) | **O** | **O** |

> **참고**: ASSOCIATE의 설문 응답 제출 권한은 설문의 `accessLevel`에 따라 결정되지만, ASSOCIATE는 행사 신청 자체가 차단(SEC-SEVT-02, REG-INV-04)되므로 설문 응답 권한 도달 이전에 403이 반환된다. 따라서 ASSOCIATE의 설문 응답 권한은 설문-행사 연동 맥락에서는 실질적으로 무의미하다.

### 5-3. 권한 검증 체크리스트

| ID | 검증 항목 | 예상 결과 | 검증 위치 |
|----|----------|----------|----------|
| SEC-SEVT-02 | 준회원이 설문 연결 행사 신청 시도 | `AssociateMemberNotAllowedException` (403) | `EventRegistrationService` (기존 REG-INV-04) |
| SEC-SEVT-03 | 일반 회원이 설문 연결 포함 행사 생성 시도 | `EventAccessDeniedException` (403) | `EventService` (기존 SEC-EVT-02) |
| SEC-SEVT-04 | 설문 응답 없이 설문 연결 행사 신청 시도 | `SurveyResponseRequiredException` | `EventRegistrationService` (신규) |
| SEC-SEVT-05 | 설문 accessLevel 부족으로 응답 불가 → 행사 신청 시도 | `SurveyResponseRequiredException` | 설문 응답 자체가 없으므로 SEVT-INV-06에 의해 차단 |
| SEC-SEVT-06 | 비인가 접근이 설문 연결 상태를 변경하지 않는지 (부작용 없음) | DB 변경 없음 | 트랜잭션 롤백 확인 |

---

## 6. 관측 가능성 (Observability & Audit)

### 6-1. 컨트롤러/서비스 로그 메시지 (신규)

| 이벤트 | 로그 레벨 | 로그 메시지 |
|--------|:---:|-----------|
| 설문 연결 행사 생성 | `info` | `행사 생성 요청 - userId: {}, title: {}, surveyId: {}` |
| 설문 연결 변경 | `info` | `행사 수정 - eventId: {}, surveyId 변경: {} → {}` |
| 설문 응답 미존재로 신청 거부 | `info` | `행사 신청 거부 - eventId: {}, userId: {}, 사유: 설문 응답 미존재 (surveyId: {})` |
| 설문 NOT_STARTED로 신청 거부 | `info` | `행사 신청 거부 - eventId: {}, userId: {}, 사유: 설문 미시작 (surveyId: {}, responseStatus: NOT_STARTED)` |
| 설문 삭제/휴지통으로 신청 거부 | `warn` | `행사 신청 거부 - eventId: {}, userId: {}, 사유: 연결된 설문이 삭제됨 (surveyId: {})` |
| 설문 응답 확인 후 신청 진행 | `debug` | `설문 응답 확인 완료 - eventId: {}, userId: {}, surveyId: {}` (DECISION-05에 따라 존재 여부만 확인하므로 responseId 미포함) |

### 6-2. 관측 가능성 항목

| 항목 | 필요 여부 | 근거 |
|------|:---:|------|
| 설문 연결/해제 이력 | **권장** | 어떤 설문이 언제 연결/해제되었는지 추적 (행사 수정 이력에 포함 가능) |
| 설문 응답 기반 신청 거부 횟수 | **선택** | 설문 미응답으로 인한 신청 실패 빈도 모니터링 |
| 설문-행사 연결 관계 조회 | **필수** | 운영진이 어떤 행사에 어떤 설문이 연결되어 있는지 확인 |

### 6-3. 관측 가능성 누락 사항

| 항목 | 현황 | 영향 |
|------|------|------|
| 설문 연결 변경 감사 이력 | **없음** (신규 기능) | 설문 연결/해제 이력 추적 불가 — 기존 EventStatusChangeHistory 패턴 활용 가능 |
| 설문 응답 → 행사 신청 매핑 | **없음** | 어떤 설문 응답이 어떤 행사 신청의 근거인지 추적 불가 — registrationResponseId 컬럼 추가로 해결 가능 (보류) |

---

## 7. 테스트 전략 (Test Strategy)

### 7-1. 테스트 레벨별 검증 항목 매핑

#### 도메인 단위 테스트 (순수 Java)

| 테스트 대상 | 검증 항목 | 우선도 |
|-----------|----------|:---:|
| `Event.create()` with surveyId | SEVT-INV-01, 02 (surveyId 설정/null) | 높음 |
| `Event.update()` with surveyId 변경 | SEVT-INV-05 (상태별 수정 가능 여부) | 높음 |
| `Event.getSurveyId()` null 반환 | SEVT-INV-07 (미연결 시 null) | 낮음 |

#### 서비스 단위 테스트 (Mockito)

| 테스트 대상 | 검증 항목 | Mock 대상 | 우선도 |
|-----------|----------|----------|:---:|
| 설문 미연결 행사 신청 (기존 로직 보존) | SEVT-INV-07, 12 | EventRepo, UserRepo, RegistrationRepo | **높음** |
| 설문 연결 행사 신청 — 응답 존재 | SEVT-INV-06 | + SurveyRepo, SurveyResponseRepo | **높음** |
| 설문 연결 행사 신청 — 응답 미존재 | SEVT-INV-06 | + SurveyRepo, SurveyResponseRepo | **높음** |
| 설문 NOT_STARTED 행사 신청 거부 | SEVT-INV-10 | + SurveyRepo | **높음** |
| 설문 CLOSED 행사 신청 — 응답 존재 | SEVT-INV-10 | + SurveyRepo, SurveyResponseRepo | 중간 |
| 설문 휴지통 행사 신청 거부 | SEVT-INV-11 | + SurveyRepo | 중간 |
| 설문 삭제 행사 신청 거부 | SEVT-INV-11 | + SurveyRepo | 중간 |
| 행사 생성 — 존재하지 않는 설문 ID | SEVT-INV-04 | + SurveyRepo | **높음** |
| 행사 생성 — 삭제된 설문 ID | SEVT-INV-04 | + SurveyRepo | 중간 |
| 행사 수정 — 설문 연결/해제 | SEVT-INV-05 | + SurveyRepo | 중간 |
| 재신청 — 설문 응답 존재 확인 | SEVT-INV-06 | + SurveyRepo, SurveyResponseRepo | 중간 |
| 동일 설문 다중 행사 — 응답 1회로 양쪽 신청 | SEVT-INV-03, 06 | + SurveyRepo, SurveyResponseRepo | 중간 |
| 설문 응답 수정 후 기존 신청 유효 | SEVT-INV-09 | + SurveyResponseRepo | 낮음 |

#### 통합 테스트 (@SpringBootTest, non-transactional)

| 테스트 시나리오 | 검증 항목 | 우선도 |
|-------------|----------|:---:|
| 설문 응답 제출 → 행사 신청 E2E (선착순) | SEVT-INV-06, 08, 10, 12 | **높음** |
| 설문 응답 제출 → 행사 신청 E2E (선발제) | SEVT-INV-06, 08, 12 | **높음** |
| 설문 응답 미제출 → 행사 신청 거부 | SEVT-INV-06 | **높음** |
| 설문 미연결 행사 신청 (회귀 테스트) | SEVT-INV-07, 12 | **높음** |
| 설문 OPEN → CLOSED → 기존 응답으로 신청 | SEVT-INV-10 | 중간 |
| 설문 휴지통 이동 → 신규 신청 차단, 기존 신청 유지 | SEVT-INV-11 | 중간 |
| 동시 신청 (설문 응답 존재 + 정원 경합) | SEVT-INV-06, REG-INV-01 | 중간 |
| 동시 설문 응답 제출 + 행사 신청 경합 (F8) | SEVT-INV-08, INV-01 | 중간 |
| 행사 생성 → 설문 연결 → 수정 → 설문 변경 → 신청 흐름 | SEVT-INV-04, 05, 06 | 중간 |
| 선발제 행사 → 설문 응답 → 신청(WAITING) → 설문 CLOSED/휴지통 → 승인 → 성공 (E6) | SEVT-INV-06 (승인 시 재검증 안 함) | 중간 |

### 7-2. 테스트-검증 항목 매핑 (구현 전 — 모두 미작성)

#### 불변조건 커버리지

| 불변조건 | 커버 테스트 | 상태 |
|---------|-----------|------|
| SEVT-INV-01 (설문 연결 선택 사항) | - | **누락** (신규) |
| SEVT-INV-02 (행사당 설문 최대 1개) | - | **누락** (신규) |
| SEVT-INV-03 (설문 재사용 가능) | - | **누락** (신규) |
| SEVT-INV-04 (설문 존재 검증) | - | **누락** (신규) |
| SEVT-INV-05 (상태별 설문 변경) | - | **누락** (신규) |
| SEVT-INV-06 (설문 응답 필수) | - | **누락** (신규, **최고 우선순위**) |
| SEVT-INV-07 (미연결 행사 보존) | - | **누락** (회귀 테스트로 커버) |
| SEVT-INV-08 (통합 API 원자적 처리) | - | **누락** (신규) |
| SEVT-INV-09 (응답 수정 무영향) | - | **누락** (신규) |
| SEVT-INV-10 (신청 시 설문 상태 검증) | - | **누락** (신규) |
| SEVT-INV-11 (설문 삭제/휴지통 정책) | - | **누락** (신규) |
| SEVT-INV-12 (기존 행사 신청 불변조건 보존) | - | **누락** (회귀 테스트로 커버) |
| SEVT-INV-13 (기존 설문 불변조건 보존) | - | **누락** (회귀 테스트로 커버) |

#### 권한 검증 커버리지

| 검증 | 커버 테스트 | 상태 |
|------|-----------|------|
| SEC-SEVT-02 (준회원 차단) | - | **누락** (기존 SEC-REG-01로 커버 가능) |
| SEC-SEVT-03 (일반 회원 생성 차단) | - | **누락** (기존 SEC-EVT-02로 커버 가능) |
| SEC-SEVT-04 (설문 응답 없이 신청) | - | **누락** (신규) |
| SEC-SEVT-05 (설문 accessLevel 부족) | - | **누락** (신규) |
| SEC-SEVT-06 (비인가 부작용 없음) | - | **누락** (신규) |

### 7-3. 발견된 설계 결정 필요 사항

구현 전 확정해야 할 설계 결정 사항:

| ID | 결정 사항 | 선택지 | 권장 |
|----|----------|--------|:---:|
| ~~DECISION-01~~ | ~~신청자 존재 시 설문 변경 허용 여부~~ | ~~A: 허용 (기존 신청 유지), B: 차단~~ | **A (확정)** — 운영 유연성 우선. SEVT-INV-05, 4-3 경계값 테이블이 이 결정을 전제로 작성됨 |
| DECISION-02 | Event-Survey 참조 방식 | A: Long surveyId (약한 참조), B: @ManyToOne (강한 참조) | **A** |
| DECISION-03 | 설문 상태 검증 수준 | A: responseStatus != NOT_STARTED만 검증, B: PUBLISHED + OPEN 필수 | **A** |
| DECISION-04 | 설문 accessLevel ≠ 행사 권한 시 | A: 프론트 경고만, B: 백엔드 차단 | **A** |
| DECISION-05 | 설문 응답 ID를 신청 레코드에 저장할지 | A: 저장 안 함 (존재 여부만 확인), B: surveyResponseId 컬럼 추가 | **A** (1차) |
| DECISION-06 | 행사-설문 연결 정보를 행사 생성 DTO에 포함할지 별도 API로 분리할지 | A: CreateEventRequest에 포함, B: 별도 연결 API | **A** |
| ~~DECISION-07~~ | ~~설문 응답과 행사 신청 API 분리 여부~~ | ~~A: 분리 (2회 호출), B: 통합 (1회 호출)~~ | **B (확정)** — 통합 API 채택. 정합성 보장을 위해 설문 응답 + 행사 신청을 단일 트랜잭션으로 처리 |

---

## 8. 대표 시나리오 흐름 (Quick Reference)

### 8-1. 정상 시나리오

| # | 시나리오 | 흐름 |
|:-:|:--------|:----|
| S1 | 설문 연결 행사 — 기본 흐름 | 운영진: 설문 생성 → 설문 공개+응답시작 → 행사 생성(surveyId 연결) → 사용자: 행사 신청(surveyAnswers 포함) → 설문 응답 + 신청 원자적 처리 → 성공 |
| S2 | 설문 미연결 행사 — 기존 흐름 | 운영진: 행사 생성(surveyId=null) → 사용자: 행사 신청 → 성공 (기존과 동일) |
| S3 | 설문 마감 후 신청 | 운영진: 설문 CLOSED → 사용자: 설문에 이미 응답했으므로 행사 신청(surveyAnswers 생략) → 기존 응답 확인 → 성공 |
| S4 | 행사 상태 변경 → 설문 연동 | 행사 취소 → 설문 응답 자동 마감 / 행사 공개 → 설문 자동 공개 / 행사 재활성화 → 설문 공개 + 응답 재개 |
| S5 | 설문 연결 후 해제 | 행사 수정으로 surveyId = null → 이후 신청자는 설문 없이 바로 신청 |

### 8-2. 실패 시나리오

| # | 시나리오 | 흐름 | 예상 결과 |
|:-:|:--------|:----|:---------|
| F1 | 설문 미응답 신청 시도 | 행사 신청(surveyAnswers 미포함, 기존 응답도 없음) | `SurveyResponseRequiredException` |
| F2 | 설문 NOT_STARTED 행사 신청 | 설문 아직 미시작 → 행사 신청 | `SurveyNotReadyException` |
| F3 | 설문 휴지통 행사 신청 | 설문 휴지통 이동 → 행사 신청 | `SurveyNotFoundException` |
| F4 | 존재하지 않는 설문 연결 | 행사 생성(surveyId=99999) | `SurveyNotFoundException` |
| F5 | 삭제된 설문 연결 | 행사 생성(surveyId=삭제된 ID) | `SurveyNotFoundException` |
| F6 | 설문 응답과 함께 신청 시 정원 초과 | 행사 신청(surveyAnswers 포함) → 정원 이미 초과 → **설문 응답도 롤백** | `EventCapacityFullException` |
| F7 | 설문 accessLevel 부족 | MEMBER가 OPERATOR 전용 설문 응답 시도 (설문 단독 API) → 거부 → 이후 행사 신청 시도 (통합 API) | 설문 도메인에서 거부 → 행사 신청 시 응답 미존재 → `SurveyResponseRequiredException` |
| F8 | 동시 설문 응답 제출 경합 | 복수 사용자가 동일 설문에 동시 최초 응답 + 행사 신청 요청 → 한 명이 INV-01(중복 응답 방지)에 의해 설문 저장 실패 | 설문 도메인 예외가 통합 API 트랜잭션에서 전파 → 설문 응답 + 행사 신청 모두 롤백. 기존 설문 도메인 예외를 그대로 반환 |

### 8-3. 엣지 케이스 시나리오

| # | 시나리오 | 흐름 | 예상 결과 |
|:-:|:--------|:----|:---------|
| E1 | 설문 응답 → 설문 삭제 → 행사 신청 | 응답 제출 → 운영진이 설문 삭제 → 행사 신청 시도 | **실패** (SEVT-INV-11, 설문 삭제됨) |
| E2 | 설문 응답 → 행사 신청 → 설문 삭제 | 신청 완료 후 설문 삭제 | 기존 신청 **유효** (SEVT-INV-09) |
| E3 | 설문 응답 → 행사 신청 → 행사 취소 → 재활성화 → 설문 변경 → 재신청 | 복잡한 라이프사이클 | 재신청 시 현재 `event.surveyId` 기준 적용: 새 설문에 응답이 존재하면 **성공**, 응답이 없으면 **실패** (`SurveyResponseRequiredException`) |
| E4 | 설문 응답 수정 중 행사 신청 시도 (동시) | Tx-1: 응답 수정, Tx-2: 행사 신청 | Tx-2에서 기존 응답 존재하므로 신청 성공 (SEVT-INV-09) |
| E5 | 행사 OPEN + 설문 비공개 전환 → 신청 | 설문 unpublish() → CLOSED 자동 전환 → 기존 응답으로 신청 | **성공** (SEVT-INV-10, responseStatus == CLOSED) |
| E6 | 선발제 승인 시점에 설문 상태 | WAITING → 운영진 승인 → 설문 상태 무관 | **성공** (승인은 설문 검증 안 함, 신청 시점에만 검증) |

---

## DB 변경 명세 (참고)

### events 테이블 변경

```sql
-- V47: 설문 연결 컬럼 추가
ALTER TABLE events ADD COLUMN event_survey_id BIGINT NULL;
-- FK 제약은 V47에서 추가됨 (fk_events_survey)

-- V53: 1:1 관계 제약 추가 (하나의 설문은 하나의 행사에만 연결 가능)
ALTER TABLE events ADD CONSTRAINT uk_events_survey_id UNIQUE (event_survey_id);
-- MySQL: nullable UNIQUE는 다수의 NULL을 허용
```

### 신규 예외 클래스

| 예외 | 용도 | HTTP Status | 상태코드 선택 근거 |
|------|------|:---:|------|
| `SurveyResponseRequiredException` | 설문 응답 미존재 시 행사 신청 거부 | 400 | 클라이언트가 설문 응답을 먼저 제출해야 하는 전제조건 미충족. 요청 자체의 구조적 오류가 아닌 비즈니스 전제조건 위반이므로 400 (Bad Request) 또는 422 (Unprocessable Entity) 중 400 선택 — 프로젝트 전반의 비즈니스 예외 처리 관례와 일치 |
| `SurveyNotReadyException` | 설문이 NOT_STARTED 상태일 때 행사 신청 거부 | 400 | 연결된 설문이 아직 응답 수집을 시작하지 않아 신청이 불가능한 상태. 서버 측 리소스 상태 문제이나, 409 (Conflict)가 아닌 400을 선택 — 프로젝트에서 도메인 상태 위반을 400으로 일관 처리하는 관례 적용 |
| `SurveyAlreadyLinkedToEventException` | 이미 다른 행사에 연결된 설문을 연결하려 할 때 (1:1 제약 위반) | 409 | 동일 설문이 이미 다른 행사에 연결되어 있는 상태 충돌. SEVT-INV-03 적용 |

---

## 관련 문서

- [행사 검증 기준서](./event-verification-criteria.md) — 행사 관리(CRUD, 상태), 3축 상태 모델 정의 (visibility + registrationStatus + eventStatus)
- [행사 신청 검증 기준서](./event-registration-verification-criteria.md) — 행사 신청 FSM, 동시성 제어, 3축 모델 연동
- [설문 검증 기준서](../survey/survey-criteria-v1.md) — 설문 2축 상태 모델, 질문 유형, 응답 규칙
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC) — 용어 및 개념 참조
