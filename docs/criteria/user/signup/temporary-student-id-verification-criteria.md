# 임시 학번 발급 및 학번 변경 검증 기준서

> **Status**: Implemented
> **Last Updated**: 2026-02-13
> **Scope**: 임시 학번 발급(Temporary Student ID), 임시 학번 회원가입, 학번 변경(Student ID Update), 임시 학번 이메일 전송
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)

## 목적

이 문서는 학번이 아직 부여되지 않은 신입생을 위한 임시 학번 발급 기능에서 **반드시 지켜져야 하는 규칙**을 명시하여, 구현 및 코드 변경 시 검증 기준으로 사용한다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 변경에 직접 관련된 7개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 임시 학번 형식, 발급 조건, 학번 변경 규칙 |
| 2 | 상태 모델 | 임시 학번 → 실제 학번 전이 생명주기 |
| 3 | 입력 도메인 분할과 경계값 | 학년, 날짜, 학번 형식의 유효/무효 분류 |
| 4 | 시스템 경계와 책임 분리 | 프론트엔드 vs 백엔드 검증, DTO vs 서비스 검증 |
| 5 | 외부 의존성 실패 정책 | 임시 학번 이메일 전송 실패 처리 |
| 6 | 권한/보안 정책 | 학번 변경 시 비밀번호 확인, 본인만 변경 가능 |
| 7 | 테스트 전략 | 검증 항목별 테스트 커버리지 계획 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

### TEMP-INV-01: 임시 학번 형식

> 임시 학번은 `99` + 연도 2자리 + 순번 4자리 형식이며, 총 8자리 숫자이다. (예: `99260001`)

- **형식**: `^99\d{2}\d{4}$` (= `^\d{8}$` 의 부분집합)
- **연도**: 발급 시점의 연도 (Asia/Seoul 기준) 하위 2자리
- **순번**: 해당 연도 내 1부터 시작하는 순차 번호 (0001~9999)
- **사후조건**: 생성된 임시 학번은 8자리 숫자이며, 기존 `validateStudentId()` 검증을 통과
- **위반 시**: `TempStudentIdExhaustedException` (순번 9999 초과 시)
- **관련 코드**: `TempStudentIdGeneratorService.generateTempStudentId()`

### TEMP-INV-02: 임시 학번은 1학년만 사용 가능

> 임시 학번 회원가입은 `grade == 1`인 사용자만 요청할 수 있다.

- **검증 계층**: DTO 레벨 (`@Min(1) @Max(1)`) + 서비스 레벨 (방어적 검증)
- **위반 시**: `MethodArgumentNotValidException` → 400 Bad Request
- **관련 코드**: `TemporaryStudentIdSignupRequest.grade`

### TEMP-INV-03: 임시 학번은 1~2월에만 발급 가능

> 임시 학번 회원가입은 현재 날짜가 1월 또는 2월인 경우에만 가능하다. (Asia/Seoul 기준)

- **검증 계층**: 서비스 레벨 (백엔드 권위적 검증) + 프론트엔드 (UI 노출 제어, 보조)
- **사전조건**: `month == 1 || month == 2` (ZoneId.of("Asia/Seoul") 기준)
- **위반 시**: `TempStudentIdNotAvailableException` → 400 Bad Request (ErrorCode: `TEMP_STUDENT_ID_NOT_AVAILABLE`)
- **관련 코드**: `TempStudentIdSignupService.validateEnrollmentPeriod()`
- **검증 방법**: 3월~12월 날짜로 요청 → 예외 발생 assertion

### TEMP-INV-04: 임시 학번의 유일성

> 발급된 임시 학번은 `users` 테이블 내에서 유일하다.

- **보장 메커니즘**:
  1. 시퀀스 테이블(`temp_student_id_sequences`)의 비관적 잠금(`PESSIMISTIC_WRITE`)으로 동시성 안전
  2. `users.users_student_id` UNIQUE 제약조건 (최종 안전망)
- **사전조건**: 시퀀스 테이블에서 해당 연도의 다음 순번 조회 (FOR UPDATE)
- **사후조건**: 반환된 임시 학번이 기존 사용자와 중복되지 않음
- **관련 코드**: `TempStudentIdSequenceRepository.findByYearForUpdate()`, `TempStudentIdGeneratorService`

### TEMP-INV-05: hasTemporaryStudentId 플래그 정합성

> `hasTemporaryStudentId == true`인 사용자는 반드시 `99`로 시작하는 학번을 가진다. `hasTemporaryStudentId == false`인 사용자는 `99`로 시작하지 않는 학번을 가진다 (단, 탈퇴 후 익명화된 사용자 제외).

- **생성 시**: `User.createWithTemporaryStudentId()` → `hasTemporaryStudentId = true` + `99YYXXXX` 학번
- **변경 시**: `User.updateStudentId()` → `hasTemporaryStudentId = false` + `99`로 시작하지 않는 새 학번
- **위반 시나리오**: 플래그와 학번 접두사가 불일치하면 시스템 데이터 무결성 훼손
- **검증 방법**: 임시 학번 가입 후 `user.isHasTemporaryStudentId() == true && user.getStudentId().startsWith("99")` assertion; 학번 변경 후 양쪽 모두 반전 확인

### TEMP-INV-06: 학번 변경 시 99 접두사 금지

> 임시 학번에서 실제 학번으로 변경할 때, 새 학번은 `99`로 시작할 수 없다.

- **근거**: `99`로 시작하는 학번은 임시 학번 전용 대역. 사용자가 임의로 또 다른 임시 학번을 설정하는 것을 방지
- **검증 계층**: 엔티티 레벨 (`User.updateStudentId()` 내부 검증)
- **위반 시**: `InvalidStudentIdException` → 400 Bad Request
- **검증 방법**: 새 학번 `"99123456"` 으로 변경 시도 → 예외 assertion

### TEMP-INV-07: 임시 학번 사용자만 학번 변경 가능

> 학번 변경 API(`PATCH /api/v1/mypage/student-id`)는 `hasTemporaryStudentId == true`인 사용자만 호출할 수 있다.

- **근거**: 실제 학번이 이미 부여된 사용자는 학번을 변경할 이유가 없음
- **검증 계층**: 서비스 레벨 (`UpdateStudentIdService`)
- **위반 시**: `StudentIdNotTemporaryException` → 400 Bad Request (ErrorCode: `STUDENT_ID_NOT_TEMPORARY`)
- **검증 방법**: `hasTemporaryStudentId == false`인 사용자가 학번 변경 시도 → 예외 assertion

### TEMP-INV-08: 학번 변경 시 중복 검증

> 학번 변경 시 새 학번은 기존 사용자(soft-delete 포함) 중 중복되지 않아야 한다.

- **검증 계층**: 서비스 레벨 (`UpdateStudentIdService`) + DB UNIQUE 제약조건
- **사전조건**: `!userRepository.existsByStudentId(newStudentId)` 및 `countByStudentIdIncludingDeleted(newStudentId) == 0`
- **위반 시**: `DuplicateStudentIdException` → 409 Conflict
- **관련 코드**: `UpdateStudentIdService`, `UserRepository.existsByStudentId()`, `UserRepository.countByStudentIdIncludingDeleted()`

### TEMP-INV-09: 임시 학번 이메일 전송 필수

> 임시 학번으로 회원가입 시, 사용자의 이메일로 임시 학번 안내 메일이 발송된다.

- **발송 시점**: 회원가입 요청 처리 완료 후 (인증 이메일과 별도)
- **발송 방식**: 비동기 (`@Async`) + 재시도 (`@Retryable`, 최대 4회, 지수 백오프)
- **이메일 내용 필수 포함 항목**: 사용자 이름, 임시 학번, 로그인에 사용하라는 안내, 학번 발급 후 마이페이지에서 변경하라는 안내
- **실패 시**: 로그 기록 (`@Recover`), 사용자 가입 자체는 롤백되지 않음 (비동기이므로 독립)
- **관련 코드**: `AuthEmailService.sendTemporaryStudentIdEmail()`, `SmtpAuthEmailService`

### TEMP-INV-10: 학번 변경 시 비밀번호 확인 필수

> 학번 변경 API는 현재 비밀번호를 함께 전달해야 하며, 비밀번호가 일치하지 않으면 거부된다.

- **근거**: 기존 `ChangePhoneNumberService`, `ChangeEmailService` 패턴과 동일한 보안 정책
- **검증 계층**: 서비스 레벨 (`UpdateStudentIdService`)
- **위반 시**: `InvalidCredentialsException` → 401 Unauthorized
- **관련 코드**: `PasswordCredentialRepository`, `PasswordEncoder.matches()`

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. 임시 학번 생명주기

```
┌─────────────────────┐
│ TEMPORARY           │  (hasTemporaryStudentId = true)
│ 학번: 99YYXXXX      │
└──────────┬──────────┘
           │ 사용자가 실제 학번으로 변경
           │ (PATCH /api/v1/mypage/student-id)
           ▼
┌─────────────────────┐
│ PERMANENT           │  (hasTemporaryStudentId = false)
│ 학번: XXXXXXXX      │
└─────────────────────┘
```

| 전이 | 트리거 | 사전조건 | 사후조건 | 관련 코드 |
|------|--------|---------|---------|----------|
| TEMPORARY → PERMANENT | 사용자 학번 변경 요청 | 인증된 사용자, 비밀번호 일치, `hasTemporaryStudentId == true`, 새 학번 `99`로 시작하지 않음, 중복 없음 | `studentId` = 새 학번, `hasTemporaryStudentId = false`, `AccountStatusChangeHistory(STUDENT_ID_UPDATE)` 기록 | `UpdateStudentIdService`, `User.updateStudentId()` |

**금지된 전이 (Invalid Transition)**:

| 시도 | 예상 결과 | 이유 |
|------|----------|------|
| PERMANENT → TEMPORARY | 불가 (API 없음) | 실제 학번을 임시 학번으로 되돌릴 수 없음 |
| PERMANENT → PERMANENT (다른 학번) | `StudentIdNotTemporaryException` | 실제 학번이 이미 부여된 사용자는 학번 변경 불가 |
| TEMPORARY → TEMPORARY (다른 임시 학번) | `InvalidStudentIdException` | 새 학번이 `99`로 시작하면 거부 |

### 2-2. 연도별 시퀀스 상태

```
┌─────────────────────────┐
│ 연도별 시퀀스             │
│ year=26, nextValue=1    │  (초기 상태)
└──────────┬──────────────┘
           │ generateTempStudentId() 호출
           ▼
┌─────────────────────────┐
│ year=26, nextValue=2    │  (1명 발급 후)
└──────────┬──────────────┘
           │ ... 반복
           ▼
┌─────────────────────────┐
│ year=26, nextValue=9999 │  (9998명 발급 후, 마지막 1명 발급 가능)
└──────────┬──────────────┘
           │ generateTempStudentId() 호출
           ▼
┌─────────────────────────┐
│ year=26, nextValue=10000│  (소진 - 다음 호출 시 예외)
└─────────────────────────┘
```

---

## 3. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 3-1. 임시 학번 회원가입 입력값

기존 `PasswordSignupRequest`와 동일한 필드에서 `studentId`가 제거되고, `grade`에 추가 제약이 적용된다.

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 |
|------|-----------|-----------|--------|
| `grade` | `1` | `0`, `2`, `3`, `4`, 음수, null | `0` (무효), `1` (유효), `2` (무효) |
| 현재 월 | 1월, 2월 | 3월~12월 | 2월 28/29일 (유효), 3월 1일 (무효) |
| (기타 필드) | 기존 `PasswordSignupRequest` 검증 기준과 동일 | 동일 | 동일 |

**조합 테스트**:

| # | grade | 현재 월 | 기대 결과 |
|---|:---:|:---:|:---:|
| 1 | 1 | 1월 | 성공 (임시 학번 발급) |
| 2 | 1 | 2월 | 성공 (임시 학번 발급) |
| 3 | 1 | 3월 | 400 (TEMP_STUDENT_ID_NOT_AVAILABLE) |
| 4 | 1 | 12월 | 400 (TEMP_STUDENT_ID_NOT_AVAILABLE) |
| 5 | 2 | 1월 | 400 (grade 제약 위반) |
| 6 | 2 | 2월 | 400 (grade 제약 위반) |
| 7 | 0 | 1월 | 400 (grade 제약 위반) |

### 3-2. 학번 변경 입력값

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 |
|------|-----------|-----------|--------|
| `newStudentId` | 8자리 숫자, `99`로 시작하지 않음 (`"12345678"`) | 7자리, 9자리, 문자 포함, `99`로 시작 (`"99260001"`), null, 빈 문자열 | `"00000000"` (유효), `"98999999"` (유효), `"99000000"` (무효) |
| `password` | 올바른 현재 비밀번호 | 틀린 비밀번호, null, 빈 문자열 | - |
| 사용자 상태 | `hasTemporaryStudentId == true` | `hasTemporaryStudentId == false` | - |

**조합 테스트**:

| # | hasTemporaryStudentId | newStudentId | password | 기대 결과 |
|---|:---:|:---:|:---:|:---:|
| 1 | true | "12345678" | 올바름 | 성공 |
| 2 | true | "99260001" | 올바름 | 400 (99 접두사 금지) |
| 3 | true | "12345678" | 틀림 | 401 (비밀번호 불일치) |
| 4 | true | "1234567" | 올바름 | 400 (형식 불일치) |
| 5 | false | "12345678" | 올바름 | 400 (임시 학번 아님) |
| 6 | true | 기존 사용자 학번 | 올바름 | 409 (중복) |

### 3-3. 시퀀스 경계값

| 항목 | 경계 지점 | 기대 결과 |
|------|----------|----------|
| 순번 최소 | `0001` (연도 첫 발급) | 성공, `99YY0001` |
| 순번 최대 유효 | `9999` | 성공, `99YY9999` |
| 순번 초과 | `10000` (9999명 이후) | `TempStudentIdExhaustedException` (500) |
| 연도 전환 | 2026년 → 2027년 | 새 시퀀스 생성, `99270001`부터 시작 |

---

## 4. 시스템 경계와 책임 분리 (System Boundary & SoC)

### 4-1. 검증 책임 분배

```
┌───────────────────────────────────────────────────────────┐
│ 프론트엔드 (보조 검증, 사용자 경험 최적화)                     │
│  - 현재 월 확인 (1~2월만 UI 노출) ← 우회 가능, 보조적       │
│  - grade == 1 선택 시에만 체크박스 노출                       │
│  - 임시 학번 선택 시 studentId 입력 숨김 + 안내 문구 표시      │
├───────────────────────────────────────────────────────────┤
│ DTO 레벨 (Jakarta Validation)                               │
│  - TemporaryStudentIdSignupRequest: studentId 필드 없음      │
│  - @Min(1) @Max(1): grade 필수 1                            │
│  - 나머지 필드: 기존 PasswordSignupRequest와 동일             │
├───────────────────────────────────────────────────────────┤
│ 서비스 레벨 (TempStudentIdSignupService) - 권위적 검증        │
│  - 1~2월 날짜 검증 (Asia/Seoul) ← 프론트엔드 우회 방어       │
│  - 임시 학번 생성 (TempStudentIdGeneratorService)            │
│  - 이메일/전화번호 중복 검증                                  │
│  - OTHER 교차 검증 (기존 패턴)                               │
├───────────────────────────────────────────────────────────┤
│ 엔티티 레벨 (User.createWithTemporaryStudentId)              │
│  - 학번 형식 검증 (^\d{8}$) ← 임시 학번도 통과               │
│  - hasTemporaryStudentId = true 설정                         │
├───────────────────────────────────────────────────────────┤
│ DB 레벨                                                     │
│  - users_student_id UNIQUE 제약 (최종 안전망)                │
│  - users_has_temporary_student_id BOOLEAN NOT NULL DEFAULT 0 │
│  - temp_student_id_sequences: PESSIMISTIC_WRITE 잠금         │
└───────────────────────────────────────────────────────────┘
```

### 4-2. 프론트엔드 vs 백엔드 검증 이중화

| 검증 항목 | 프론트엔드 | 백엔드 | 비고 |
|----------|:---:|:---:|------|
| 1~2월 제한 | O (UI 숨김) | **O** (서비스) | 백엔드가 권위적, 프론트엔드는 UX 보조 |
| 1학년 제한 | O (체크박스 조건) | **O** (DTO) | DTO `@Max(1)`로 강제 |
| 학번 형식 | - (입력 없음) | **O** (엔티티) | 백엔드가 생성하므로 항상 유효 |
| 중복 검증 | - (학번 입력 없음) | **O** (서비스 + DB) | 시퀀스가 유일성 보장 |

### 4-3. 엔드포인트 분리

| 흐름 | 엔드포인트 | DTO | 서비스 |
|------|----------|-----|--------|
| 일반 회원가입 | `POST /api/v1/auth/password/signup` | `PasswordSignupRequest` | `SignupService` |
| 임시 학번 회원가입 | `POST /api/v1/auth/password/signup/temporary` | `TemporaryStudentIdSignupRequest` | `TempStudentIdSignupService` |
| 학번 변경 | `PATCH /api/v1/mypage/student-id` | `UpdateStudentIdRequest` | `UpdateStudentIdService` |

---

## 5. 외부 의존성 실패 정책 (External Dependency Failure)

### 5-1. 임시 학번 이메일 전송

| 항목 | 정책 | 근거 |
|------|------|------|
| 전송 방식 | 비동기 (`@Async("emailTaskExecutor")`) | 메인 트랜잭션과 독립, 응답 지연 방지 |
| 재시도 | 최대 4회, 지수 백오프 (1분 → 3분 → 9분, 최대 15분) | 기존 `sendVerificationEmail` 패턴과 동일 |
| 최종 실패 시 | `@Recover` 메서드에서 로그 기록, 가입은 유지 | 이메일 실패가 가입을 무효화해서는 안 됨 |
| 멱등성 | 동일 임시 학번을 여러 번 전송해도 부작용 없음 | 안내 이메일은 읽기 전용 정보 |

### 5-2. 시퀀스 DB 접근

| 항목 | 정책 | 근거 |
|------|------|------|
| 동시성 | `PESSIMISTIC_WRITE` (SELECT FOR UPDATE) | 시퀀스 순번 정확성 보장 |
| DB 장애 시 | 트랜잭션 실패 → 회원가입 전체 롤백 | 임시 학번 없이 가입 불가 |
| 잠금 대기 | DB 기본 lock timeout 사용 | 극단적 동시 요청에서만 발생 |

---

## 6. 권한/보안 정책 (RBAC & Authorization)

### 6-1. 접근 제어

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 임시 학번 회원가입 | **O** | - | - | - | - |
| 학번 변경 (본인) | 401 | **O** (임시 학번만) | **O** (임시 학번만) | **O** (임시 학번만) | **O** (임시 학번만) |

### 6-2. 보안 체크리스트

| ID | 검증 항목 | 예상 결과 | 검증 서비스 |
|----|----------|----------|-----------|
| SEC-TEMP-01 | 비밀번호 불일치로 학번 변경 시도 | `InvalidCredentialsException` (401) | `UpdateStudentIdService` |
| SEC-TEMP-02 | 다른 사용자의 학번으로 변경 시도 | `DuplicateStudentIdException` (409) | `UpdateStudentIdService` |
| SEC-TEMP-03 | `hasTemporaryStudentId == false`인 사용자가 학번 변경 시도 | `StudentIdNotTemporaryException` (400) | `UpdateStudentIdService` |
| SEC-TEMP-04 | 3~12월에 임시 학번 회원가입 API 직접 호출 | `TempStudentIdNotAvailableException` (400) | `TempStudentIdSignupService` |
| SEC-TEMP-05 | grade != 1으로 임시 학번 회원가입 시도 | 400 Bad Request | DTO 검증 (`@Max(1)`) |

---

## 7. 테스트 전략 (Test Strategy)

### 7-1. 테스트 계층별 검증 항목

#### 서비스 통합 테스트 (TempStudentIdGeneratorServiceTest)

| 검증 항목 | 불변조건 | 테스트 메서드 |
|----------|---------|-------------|
| 첫 발급 시 `99YY0001` 형식 | TEMP-INV-01 | `generate_FirstOfYear_Returns99YY0001` |
| 순차 증가 확인 | TEMP-INV-04 | `generate_MultipleCalls_ReturnsSequential` |
| 연도 전환 시 새 시퀀스 | TEMP-INV-01 | `generate_NewYear_StartsNewSequence` |
| 9999 초과 시 예외 | TEMP-INV-01 | `generate_Exhausted_ThrowsException` |

#### 서비스 통합 테스트 (TempStudentIdSignupServiceTest)

| 검증 항목 | 불변조건 | 테스트 메서드 |
|----------|---------|-------------|
| 정상 임시 학번 가입 (1월, 1학년) | TEMP-INV-01~05, 09 | `signup_ValidRequest_CreatesUserWithTempId` |
| 응답에 임시 학번 포함 | TEMP-INV-01 | `signup_ValidRequest_ResponseContainsTempId` |
| 3월 요청 시 거부 | TEMP-INV-03 | `signup_InMarch_ThrowsTempIdNotAvailable` |
| 12월 요청 시 거부 | TEMP-INV-03 | `signup_InDecember_ThrowsTempIdNotAvailable` |
| grade 2 요청 시 거부 | TEMP-INV-02 | `signup_Grade2_ThrowsValidationException` |
| 이메일 중복 시 거부 | 기존 INV-06 | `signup_DuplicateEmail_ThrowsDuplicateException` |
| 전화번호 중복 시 거부 | 기존 INV-06 | `signup_DuplicatePhone_ThrowsDuplicateException` |
| hasTemporaryStudentId 플래그 설정 확인 | TEMP-INV-05 | `signup_ValidRequest_SetsTemporaryFlag` |
| 이메일 발송 호출 확인 | TEMP-INV-09 | `signup_ValidRequest_SendsTempIdEmail` |

#### 서비스 통합 테스트 (UpdateStudentIdServiceTest)

| 검증 항목 | 불변조건 | 테스트 메서드 |
|----------|---------|-------------|
| 정상 학번 변경 | TEMP-INV-05~08, 10 | `update_ValidRequest_UpdatesStudentId` |
| 변경 후 플래그 해제 확인 | TEMP-INV-05 | `update_ValidRequest_ClearsTemporaryFlag` |
| 임시 학번 아닌 사용자 거부 | TEMP-INV-07 | `update_NotTemporary_ThrowsException` |
| 99 접두사 학번 거부 | TEMP-INV-06 | `update_StartsWithNinetyNine_ThrowsException` |
| 중복 학번 거부 | TEMP-INV-08 | `update_DuplicateStudentId_ThrowsException` |
| 비밀번호 불일치 거부 | TEMP-INV-10 | `update_WrongPassword_ThrowsException` |
| 형식 불일치 거부 | - | `update_InvalidFormat_ThrowsException` |

### 7-2. 테스트-검증 항목 매핑

| 불변조건 | 커버 테스트 | 상태 |
|---------|-----------|------|
| TEMP-INV-01 (형식) | `TempStudentIdGeneratorServiceTest` 전체 | ✅ 구현 완료 |
| TEMP-INV-02 (1학년) | DTO 검증 + `TempStudentIdSignupServiceTest` | ✅ 구현 완료 |
| TEMP-INV-03 (1~2월) | `TempStudentIdSignupServiceTest` (3월, 12월 케이스) | ✅ 구현 완료 |
| TEMP-INV-04 (유일성) | `TempStudentIdGeneratorServiceTest` (시퀀스 순차 확인) | ✅ 구현 완료 |
| TEMP-INV-05 (플래그 정합성) | `TempStudentIdSignupServiceTest` + `UpdateStudentIdServiceTest` | ✅ 구현 완료 |
| TEMP-INV-06 (99 금지) | `UpdateStudentIdServiceTest` | ✅ 구현 완료 |
| TEMP-INV-07 (임시만 변경) | `UpdateStudentIdServiceTest` | ✅ 구현 완료 |
| TEMP-INV-08 (중복 검증) | `UpdateStudentIdServiceTest` | ✅ 구현 완료 |
| TEMP-INV-09 (이메일 전송) | `TempStudentIdSignupServiceTest` | ✅ 구현 완료 |
| TEMP-INV-10 (비밀번호 확인) | `UpdateStudentIdServiceTest` | ✅ 구현 완료 |

---

## 관련 문서

- [회원가입/승인/강등 검증 기준서](../../verification-criteria.md) - 기존 회원가입 검증 기준 (학번/이메일/비밀번호 등)
- [회원가입 관심 분야/가입 경로 검증 기준서](interests-join-route-verification-criteria.md) - interests/joinRoute 필드 검증 기준
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC) - 용어 및 개념 참조
