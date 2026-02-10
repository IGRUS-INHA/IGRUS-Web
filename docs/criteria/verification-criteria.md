# 회원가입 / 승인 / 강등 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-02-10
> **Scope**: 회원가입(Signup), 이메일 인증(Email Verification), 준회원 승인/거절(Approval/Rejection), 역할 변경/강등(Role Change/Demotion)
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)

## 목적

이 문서는 회원가입/승인/강등 도메인에서 **반드시 지켜져야 하는 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 도메인에 직접 관련된 6개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 비즈니스 로직의 핵심 계약 정의 |
| 2 | 상태 모델 | 역할/상태/결정의 유한 상태 머신 검증 |
| 3 | 입력 도메인 분할과 경계값 | 회원가입 입력값, 인증 시도 횟수 등의 경계 검증 |
| 4 | 권한/보안 정책 | RBAC 기반 접근 제어 검증 |
| 5 | 관측 가능성 | 감사 이력, 이벤트, 토큰 무효화 추적 |
| 6 | 테스트 전략 | 테스트-검증 항목 매핑 및 누락 식별 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

### INV-01: Active AssociateDecision 유일성

> 한 사용자에 대해 `active = true`인 `AssociateDecision`은 최대 1개만 존재한다.

- **사전조건**: 새 결정 생성 전, 기존 active 결정이 있으면 `deactivate()` 호출
- **사후조건**: `associateDecisionRepository.findByUserIdAndActiveTrue(userId)` 결과가 0개 또는 1개
- **위반 시**: 승인/거절/강등 결과가 중복 적용되어 상태 불일치 발생
- **관련 코드**:
  - `ApproveAssociateService:56-57` - 타입 무관하게 기존 active 비활성화
  - `RejectAssociateService:48-55` - DEMOTED만 비활성화, 그 외는 예외
  - `ChangeUserRoleService:51-52` - 강등 시 기존 active 비활성화
- **검증 방법**: 모든 승인/거절/강등 작업 후 active 결정 수가 1개인지 assertion

### INV-02: MEMBER 이상 역할과 APPROVED 이력의 정합성

> `UserRole`이 MEMBER, OPERATOR, ADMIN인 사용자는 반드시 `AssociateDecisionType.APPROVED` 이력이 1개 이상 존재한다.

- **사전조건**: `user.isAssociate() == true` (승인 대상)
- **사후조건**: 승인 완료 후 `AssociateDecision(type=APPROVED)` 레코드 존재
- **예외**: 시스템 초기 데이터로 직접 삽입된 ADMIN (마이그레이션 데이터)
- **관련 코드**: `ApproveAssociateService:62`, `BulkApproveAssociatesService:78`

### INV-03: 이력 보존

> `AssociateDecision` 레코드는 삭제되지 않는다. 비활성화(`active = false`)만 가능하다.

- **사후조건**: 전체 레코드 수 >= active 레코드 수 (항상)
- **위반 시**: 감사 추적 불가
- **검증 방법**: 승인/거절/강등 사이클 후 전체 이력 수 확인
- **관련 테스트**: `DemotionReapprovalFlowTest:multipleCycles_allHistoryPreserved`

### INV-04: PENDING_VERIFICATION 상태-역할 제약

> `UserStatus.PENDING_VERIFICATION` 상태의 사용자는 `UserRole.ASSOCIATE` 역할만 가질 수 있다.

- **근거**: 이메일 인증이 완료되지 않은 사용자는 승인 대상이 될 수 없음
- **관련 코드**: `User.create()` - 역할 ASSOCIATE, 상태 PENDING_VERIFICATION으로 고정
- **위반 시나리오**: 이메일 미인증 상태에서 직접 승인 시도

### INV-05: 마지막 ADMIN 보호

> 시스템에 ADMIN이 1명만 남은 경우, 해당 ADMIN의 역할을 변경할 수 없다.

- **사전조건**: `ValidateNotLastAdminService.validateNotLastAdmin(targetUserId)` 검증 통과
- **위반 시**: 시스템에 ADMIN이 0명이 되어 관리 기능 접근 불가
- **관련 코드**: `ChangeUserRoleService:42`

### INV-06: 학번/이메일/전화번호 유일성

> `users` 테이블에서 학번(`users_student_id`), 이메일(`users_email`), 전화번호(`users_phone_number`)는 각각 고유하다.

- **검증 계층**: DB unique 제약조건 + 서비스 레벨 중복 검사
- **관련 코드**: `SignupService:106-116` (서비스 검증), `User` 엔티티의 `unique = true` (DB 검증)
- **위반 시 예외**: `DuplicateStudentIdException`, `DuplicateEmailException`, `DuplicatePhoneNumberException`
- **특이사항**: 탈퇴 사용자의 학번/이메일은 `anonymizeForCleanup()`으로 suffix가 추가되어 충돌 방지

### INV-07: REJECTED/DEMOTED 결정의 사유 필수

> `AssociateDecisionType`이 REJECTED 또는 DEMOTED인 결정은 반드시 `reason` 필드가 존재한다.

- **관련 코드**:
  - `AssociateDecision.reject(user, decidedBy, reason)` - reason 파라미터 필수
  - `AssociateDecision.demote(user, decidedBy, reason)` - reason 파라미터 필수
  - `AssociateDecision.approve(user, decidedBy)` - reason 없음 (정상)
- **검증 방법**: REJECTED/DEMOTED 타입의 레코드에서 `reason != null` assertion

### INV-08: 역할 변경 시 이력 기록 필수

> 사용자의 역할이 변경될 때 반드시 `UserRoleHistory` 레코드가 생성된다.

- **관련 코드**:
  - `ApproveAssociateService:65-71` - 승인 시 이력 기록
  - `BulkApproveAssociatesService:81-87` - 일괄 승인 시 이력 기록
  - `ChangeUserRoleService:57-63` - 역할 변경 시 이력 기록
- **주의**: 거절(`RejectAssociateService`)은 역할이 변경되지 않으므로 이력 기록 없음 (정상)

### INV-09: 역할 변경 시 리프레시 토큰 만료

> 사용자의 역할이 변경되면 해당 사용자의 모든 리프레시 토큰이 만료된다.

- **목적**: 이전 역할 기반의 권한으로 발급된 토큰이 계속 사용되는 것을 방지
- **관련 코드**:
  - `ApproveAssociateService:73` - 승인 시 토큰 만료
  - `BulkApproveAssociatesService:89` - 일괄 승인 시 토큰 만료
  - `ChangeUserRoleService:65` - 역할 변경 시 토큰 만료
- **주의**: 거절은 역할 변경이 없으므로 토큰 만료 없음 (정상)

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. 계정 상태 전이 (UserStatus FSM)

```
┌──────────────────────┐
│ PENDING_VERIFICATION │
└──────────┬───────────┘
           │ 이메일 인증 성공
           ▼
      ┌─────────┐     관리자 정지      ┌───────────┐
      │  ACTIVE  │ ──────────────────> │ SUSPENDED │
      └────┬─────┘ <────────────────── └───────────┘
           │         정지 해제
           │ 탈퇴
           ▼
      ┌───────────┐
      │ WITHDRAWN │  (종단 상태)
      └───────────┘
```

| 전이 | 트리거 | 사전조건 | 사후조건 | 관련 코드 |
|------|--------|---------|---------|----------|
| PENDING → ACTIVE | 이메일 인증 성공 | 유효한 인증 코드, 만료 전, 시도 횟수 미초과 | `User.status = ACTIVE`, `PasswordCredential.verified = true` | `VerifyEmailService:71-83` |
| ACTIVE → SUSPENDED | 관리자 정지 | ADMIN 권한 | `User.status = SUSPENDED`, 토큰 만료 | `User.suspend()` |
| SUSPENDED → ACTIVE | 정지 해제 | ADMIN 권한 | `User.status = ACTIVE` | `User.activate()` |
| ACTIVE → WITHDRAWN | 탈퇴 | 본인 요청 또는 ADMIN 강제 | `User.status = WITHDRAWN`, soft delete | `User.withdraw()` |

**금지된 전이 (Invalid Transition)**:

| 시도 | 예상 결과 | 이유 |
|------|----------|------|
| PENDING_VERIFICATION → SUSPENDED | 거부 | 인증 전 사용자는 정지 대상이 아님 |
| PENDING_VERIFICATION → WITHDRAWN | 거부 | 인증 전 사용자는 탈퇴 대상이 아님 |
| WITHDRAWN → 어떤 상태든 | 거부 | 탈퇴는 종단 상태 |
| SUSPENDED → WITHDRAWN | 거부 | 정지 해제 후 탈퇴해야 함 |

### 2-2. 역할 전이 (UserRole FSM)

```
┌───────────┐     승인      ┌────────┐
│ ASSOCIATE │ ────────────> │ MEMBER │
└───────────┘ <──────────── └───┬────┘
                  강등           │
                            관리자 변경
                                │
                           ┌────▼─────┐
                           │ OPERATOR │
                           └────┬─────┘
                                │
                            관리자 변경
                                │
                           ┌────▼─────┐
                           │  ADMIN   │
                           └──────────┘
```

| 전이 | 트리거 | 사전조건 | 사후조건 |
|------|--------|---------|---------|
| ASSOCIATE → MEMBER | 승인 | ADMIN 권한, 대상이 ASSOCIATE | `user.role = MEMBER`, APPROVED 결정, 이력 기록, 토큰 만료 |
| MEMBER → ASSOCIATE | 강등 | ADMIN 권한, 자기 자신 아님 | `user.role = ASSOCIATE`, DEMOTED 결정, 이력 기록, 토큰 만료 |
| MEMBER ↔ OPERATOR | 관리자 변경 | ADMIN 권한, 자기 자신 아님 | `user.role` 변경, 이력 기록, 토큰 만료 |
| OPERATOR ↔ ADMIN | 관리자 변경 | ADMIN 권한, 자기 자신 아님, 마지막 ADMIN 아님 | `user.role` 변경, 이력 기록, 토큰 만료 |

**금지된 전이**:

| 시도 | 예상 결과 | 이유 |
|------|----------|------|
| ASSOCIATE → OPERATOR/ADMIN | 코드상 가능하나 비즈니스 규칙 위반 | 승인(MEMBER) 단계를 거쳐야 함 |
| 자기 자신 역할 변경 | `SelfRoleChangeException` | 권한 남용 방지 |
| 마지막 ADMIN 강등 | `ValidateNotLastAdminService` 차단 | 시스템 관리 불가 방지 |

### 2-3. AssociateDecision 전이 매트릭스

사용자의 현재 상태별로 승인/거절/강등 시 어떤 결과가 발생하는지 정의한다.

| 현재 active 결정 | 승인 (Approve) | 거절 (Reject, 개별) | 거절 (Reject, 일괄) | 강등 (Demote) |
|:---:|:---:|:---:|:---:|:---:|
| **없음** | APPROVED 생성, role → MEMBER | REJECTED 생성, role 유지 | REJECTED 생성, role 유지 | N/A (ASSOCIATE 아님) |
| **APPROVED** | N/A (이미 MEMBER) | N/A (이미 MEMBER) | N/A (이미 MEMBER) | APPROVED 비활성화 → DEMOTED 생성, role → ASSOCIATE |
| **REJECTED** | REJECTED 비활성화 → APPROVED 생성, role → MEMBER | `AssociateAlreadyDecidedException` | skip (실패 목록) | N/A (ASSOCIATE이므로 강등 불가) |
| **DEMOTED** | DEMOTED 비활성화 → APPROVED 생성, role → MEMBER | DEMOTED 비활성화 → REJECTED 생성, role 유지 | skip (실패 목록) | N/A (이미 ASSOCIATE) |

**주의사항**:
- 개별 거절(`RejectAssociateService`)과 일괄 거절(`BulkRejectAssociatesService`)의 DEMOTED 처리 방식이 다름
  - 개별: DEMOTED를 비활성화하고 새 REJECTED 생성 (허용)
  - 일괄: active 결정이 있으면 무조건 skip (DEMOTED 포함)
- 개별 승인과 일괄 승인은 동일한 방식 (타입 무관 비활성화 후 APPROVED 생성)
- `ChangeUserRoleService`는 `AdminRoleValidator`를 사용하지 않음 - ADMIN 권한 검증이 컨트롤러(Spring Security) 레벨에서 수행됨. 반면 `ApproveAssociateService`, `RejectAssociateService`는 서비스 내부에서 `AdminRoleValidator`로 직접 검증

---

## 3. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 3-1. 회원가입 입력값

입력값은 2단계로 검증된다: **DTO 레벨** (Jakarta Validation) → **엔티티 레벨** (User.create 내부 검증).

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 | DTO 검증 | 엔티티 검증 |
|------|-----------|-----------|--------|---------|-----------|
| `studentId` | 8자리 숫자 (`"12345678"`) | 7자리 (`"1234567"`), 9자리 (`"123456789"`), 문자 포함 (`"1234567a"`), null, 빈 문자열 | `"00000000"` (최소), `"99999999"` (최대) | `@NotBlank`, `@Pattern("^\\d{8}$")` | `validateStudentId()` |
| `name` | 1~50자 문자열 | null, 빈 문자열, 51자 이상 | 1자 (최소), 50자 (최대), 51자 (초과) | `@NotBlank`, `@Size(max=50)` | - |
| `email` | 유효한 이메일 형식 | @없음, 도메인 없음, null, 빈 문자열 | - | `@NotBlank`, `@Email` | `validateEmail()` |
| `password` | 영문+숫자 포함 8~72자 | 숫자만, 영문만, 7자, 73자, null | 8자 (최소), 72자 (최대) | `@NotBlank`, `@Size(min=8, max=72)`, `@Pattern` | - |
| `phoneNumber` | `000-0000-0000` 형식 | 형식 불일치 (`"01012345678"`), null, 빈 문자열 | - | `@NotBlank`, `@Pattern("^\\d{3}-\\d{4}-\\d{4}$")` | `validatePhoneNumber()` |
| `department` | 1~50자 문자열 | null, 빈 문자열, 51자 이상 | 50자 (최대) | `@NotBlank`, `@Size(max=50)` | - |
| `motivation` | 비어있지 않은 문자열 | null, 빈 문자열 | - | `@NotBlank` | - |
| `wishes` | `Wish` enum 리스트 (0~10개) | 11개 이상, 유효하지 않은 enum 값 | 0개 (최소), 10개 (최대) | `@Size(max=10)` | - |
| `gender` | `MALE` 또는 `FEMALE` | null, 유효하지 않은 값 | - | `@NotNull` | - |
| `grade` | 1 이상 정수 | 0, 음수, null | 1 (최소) | `@NotNull`, `@Min(1)` | `validateGrade()` |
| `privacyConsent` | `true` | `false`, null | - | `@NotNull`, `@AssertTrue` | - |

**중복 검증 동치류**:

| 필드 | 유효 (유일) | 무효 (중복) | 예외 |
|------|-----------|-----------|------|
| 학번 | DB에 존재하지 않는 학번 | 이미 등록된 학번 | `DuplicateStudentIdException` |
| 이메일 | DB에 존재하지 않는 이메일 | 이미 등록된 이메일 | `DuplicateEmailException` |
| 전화번호 | DB에 존재하지 않는 번호 | 이미 등록된 번호 | `DuplicatePhoneNumberException` |

### 3-2. 이메일 인증 경계값

| 항목 | 유효 범위 | 경계 지점 | 설정값 |
|------|----------|----------|--------|
| 인증 코드 시도 횟수 | 1 ~ `maxAttempts` | `maxAttempts`회 (마지막 유효), `maxAttempts + 1`회 (초과) | 기본 5회 (`app.mail.verification-max-attempts`) |
| 인증 코드 유효 시간 | 생성 ~ 만료 전 | 만료 직전 (유효), 만료 시점 (만료) | 기본 10분 (`app.mail.verification-code-expiry`) |

**특이 동작**:
- 인증 코드 불일치 시, 시도 횟수 증가는 **별도 트랜잭션**(`EmailVerificationAttemptService.incrementAttempts()`)으로 처리
  - 목적: 메인 트랜잭션 롤백 시에도 시도 횟수가 보존되도록
- 인증 코드 비교에 `MessageDigest.isEqual()` 사용 (Timing Attack 방지)

### 3-3. 일괄 처리 경계값

| 항목 | 유효 범위 | 경계 지점 | 예외 |
|------|----------|----------|------|
| `userIds` 리스트 크기 | 1개 이상 | 0개 (빈 리스트), null | `BulkApprovalEmptyException`, `BulkRejectionEmptyException` |
| 처리 결과 | 0 ~ N명 성공 | 전원 성공, 전원 실패, 부분 성공 | 예외 없음 (부분 성공 패턴) |

---

## 4. 권한/보안 정책 (RBAC & Authorization)

### 4-1. 역할별 접근 제어 매트릭스

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 회원가입 | O | - | - | - | - |
| 이메일 인증 | O | - | - | - | - |
| 준회원 개별 승인 | 401 | 403 | 403 | 403 | **O** |
| 준회원 개별 거절 | 401 | 403 | 403 | 403 | **O** |
| 준회원 일괄 승인 | 401 | 403 | 403 | 403 | **O** |
| 준회원 일괄 거절 | 401 | 403 | 403 | 403 | **O** |
| 역할 변경 | 401 | 403 | 403 | 403 | **O** |
| 대기 목록 조회 | 401 | 403 | 403 | 403 | **O** |
| 거절 목록 조회 | 401 | 403 | 403 | 403 | **O** |
| 강등 목록 조회 | 401 | 403 | 403 | 403 | **O** |

### 4-2. 권한 검증 체크리스트

| ID | 검증 항목 | 예상 결과 | 검증 서비스 |
|----|----------|----------|-----------|
| SEC-01 | ADMIN이 아닌 사용자가 승인 시도 | `AdminRequiredException` (403) | `AdminRoleValidator` |
| SEC-02 | ADMIN이 아닌 사용자가 거절 시도 | `AdminRequiredException` (403) | `AdminRoleValidator` |
| SEC-03 | ADMIN이 아닌 사용자가 역할 변경 시도 | 403 Forbidden | Spring Security (컨트롤러 레벨) |
| SEC-04 | ADMIN이 자기 자신의 역할 변경 시도 | `SelfRoleChangeException` | `ChangeUserRoleService:38-40` |
| SEC-05 | 마지막 ADMIN의 역할 변경 시도 | `LastAdminCannotChangeException` | `ValidateNotLastAdminService:38-41` |
| SEC-06 | 비인가 접근이 상태를 변경하지 않는지 (부작용 없음) | DB 변경 없음 | 트랜잭션 롤백 확인 |

### 4-3. 보안 주의사항

- **Timing Attack 방지**: 이메일 인증 코드 비교에 `MessageDigest.isEqual()` 사용 (`VerifyEmailService:62-64`)
- **비밀번호 저장**: BCrypt 인코딩 (`PasswordEncoder.encode()`)
- **토큰 무효화**: 역할 변경 시 즉시 리프레시 토큰 만료로 이전 권한 세션 차단

---

## 5. 관측 가능성 (Observability & Audit)

AI 생성 코드의 신뢰성은 **"문제 발생 시 원인을 추적할 수 있는가"**로 측정된다.

### 5-1. 감사 이력 (Audit Trail)

| 이벤트 | 저장소 | 기록 내용 | 관련 코드 |
|--------|--------|---------|----------|
| 역할 변경 (승인) | `UserRoleHistory` | 이전 역할, 새 역할, 변경 사유 | `ApproveAssociateService:65-71` |
| 역할 변경 (일괄 승인) | `UserRoleHistory` | 이전 역할, 새 역할, 변경 사유 | `BulkApproveAssociatesService:81-87` |
| 역할 변경 (관리자) | `UserRoleHistory` | 이전 역할, 새 역할, 변경 사유 | `ChangeUserRoleService:57-63` |
| 승인 결정 | `AssociateDecision` | type=APPROVED, decidedBy, decidedAt | `AssociateDecision.approve()` |
| 거절 결정 | `AssociateDecision` | type=REJECTED, reason, decidedBy, decidedAt | `AssociateDecision.reject()` |
| 강등 결정 | `AssociateDecision` | type=DEMOTED, reason, decidedBy, decidedAt | `AssociateDecision.demote()` |

### 5-2. 이벤트 발행

| 이벤트 | 발행 조건 | 포함 정보 | 관련 코드 |
|--------|---------|----------|----------|
| `AccountStatusChangeEvent` | 승인 | userId, approverId, APPROVAL, 이전→새 역할, 사유 | `ApproveAssociateService:76-80` |
| `AccountStatusChangeEvent` | 일괄 승인 | userId, approverId, APPROVAL, 이전→새 역할, 사유 | `BulkApproveAssociatesService:91-95` |
| `AccountStatusChangeEvent` | 역할 변경 | userId, currentUserId, ROLE_CHANGE, 이전→새 역할, 사유 | `ChangeUserRoleService:68-72` |

**누락 확인**: 거절(`RejectAssociateService`, `BulkRejectAssociatesService`)은 `AccountStatusChangeEvent`를 발행하지 않음. 역할이 변경되지 않으므로 현재 설계상 의도된 동작이나, 감사 추적 관점에서 이벤트 추가를 고려할 수 있음.

### 5-3. 로그 메시지

| 서비스 | 시작 로그 | 완료 로그 | 실패 로그 |
|--------|---------|---------|---------|
| `ApproveAssociateService` | `개별 승인 요청: userId, approverId` | `개별 승인 완료: userId, previousRole, newRole` | 토큰 만료 로그 별도 |
| `RejectAssociateService` | `개별 거절 요청: userId, rejectorId` | `개별 거절 완료: userId, reason` | - |
| `BulkApproveAssociatesService` | `일괄 승인 요청: userIds, approverId` | `일괄 승인 완료: approvedCount, failedCount` | `일괄 승인 중 개별 사용자 처리 실패` |
| `BulkRejectAssociatesService` | `일괄 거절 요청: userIds, rejectorId` | `일괄 거절 완료: rejectedCount, failedCount` | `일괄 거절 중 개별 사용자 처리 실패` |
| `ChangeUserRoleService` | `회원 권한 변경 요청` | `회원 권한 변경 완료` | 토큰 만료 로그 별도 |
| `SignupService` | `회원가입 요청: email` | `회원가입 완료, 이메일 인증 대기` | - |
| `VerifyEmailService` | `이메일 인증 요청: email` | `이메일 인증 완료: email` | - |

### 5-4. 토큰 무효화 추적

| 트리거 | 토큰 만료 여부 | 관련 코드 |
|--------|:---:|----------|
| 개별 승인 | O | `ApproveAssociateService:73` |
| 일괄 승인 | O (각 사용자별) | `BulkApproveAssociatesService:89` |
| 개별 거절 | X (역할 불변) | - |
| 일괄 거절 | X (역할 불변) | - |
| 역할 변경 (강등 포함) | O | `ChangeUserRoleService:65` |

---

## 6. 테스트 전략 (Test Strategy)

### 6-1. 현재 테스트 현황

| 테스트 클래스 | 상속 | 테스트 수 | 범위 |
|-------------|------|---------|------|
| `ApproveAssociateServiceTest` | `ServiceIntegrationTestBase` | 기본 승인, 권한 검증, 예외 케이스 | 개별 승인 |
| `RejectAssociateServiceTest` | `ServiceIntegrationTestBase` | 기본 거절, 권한 검증, 예외 케이스 | 개별 거절 |
| `DemotionReapprovalFlowTest` | `ServiceIntegrationTestBase` | 13개 | 승인→강등→재승인 사이클, 다중 사이클, 목록 반영, 일괄 처리 |
| `BulkApproveAssociatesServiceTest` | `ServiceIntegrationTestBase` | 빈 리스트, 부분 성공 | 일괄 승인 |
| `BulkRejectAssociatesServiceTest` | `ServiceIntegrationTestBase` | 빈 리스트, 부분 성공 | 일괄 거절 |

테스트 기반: `ServiceIntegrationTestBase` (non-transactional, 각 서비스 호출이 독립 트랜잭션).

### 6-2. 테스트-검증 항목 매핑

#### 불변조건 커버리지

| 불변조건 | 커버 테스트 | 상태 |
|---------|-----------|------|
| INV-01 (active 유일성) | `DemotionReapprovalFlowTest` 전체 (active 수 assertion) | **커버됨** |
| INV-02 (MEMBER↔APPROVED 정합성) | `ApproveAssociateServiceTest`, `DemotionReapprovalFlowTest` | **커버됨** |
| INV-03 (이력 보존) | `DemotionReapprovalFlowTest:multipleCycles_allHistoryPreserved` | **커버됨** |
| INV-04 (PENDING 상태-역할 제약) | - | **누락** |
| INV-05 (마지막 ADMIN 보호) | `ChangeUserRoleService` 관련 테스트 | 확인 필요 |
| INV-06 (학번/이메일 유일성) | `SignupService` 관련 테스트 | 확인 필요 |
| INV-07 (REJECTED/DEMOTED 사유 필수) | - | **누락** (팩토리 메서드 시그니처로 강제되나, null 검증 없음) |
| INV-08 (역할 변경 시 이력 기록) | 간접적 커버 | **명시적 테스트 누락** |
| INV-09 (역할 변경 시 토큰 만료) | 간접적 커버 | **명시적 테스트 누락** |

#### 상태 전이 커버리지

| 전이 | 커버 테스트 | 상태 |
|------|-----------|------|
| 결정없음 → APPROVED | `ApproveAssociateServiceTest` | **커버됨** |
| 결정없음 → REJECTED | `RejectAssociateServiceTest` | **커버됨** |
| APPROVED → DEMOTED | `DemotionReapprovalFlowTest:approve_thenDemote_createsActiveDemotedRecord` | **커버됨** |
| DEMOTED → APPROVED (재승인) | `DemotionReapprovalFlowTest:demotedUser_reapproved_becomesMember` | **커버됨** |
| DEMOTED → REJECTED (재거절) | `DemotionReapprovalFlowTest:demotedUser_rejected_throwsException` | **불일치** (테스트는 예외 기대, 코드는 허용) |
| REJECTED → APPROVED (재승인) | `DemotionReapprovalFlowTest:rejectedUser_reapproved_becomesMember` | **커버됨** |
| REJECTED → REJECTED (재거절) | `DemotionReapprovalFlowTest:rejectedUser_rerejected_throwsException` | **커버됨** |
| OPERATOR → ASSOCIATE | `DemotionReapprovalFlowTest:operatorToAssociate_createsDemotedRecord` | **커버됨** |
| ADMIN → ASSOCIATE | `DemotionReapprovalFlowTest:adminToAssociate_createsDemotedRecord` | **커버됨** |
| 다중 사이클 | `DemotionReapprovalFlowTest:multipleCycles_allHistoryPreserved` | **커버됨** |

#### 권한 검증 커버리지

| 검증 | 커버 테스트 | 상태 |
|------|-----------|------|
| SEC-01 (비ADMIN 승인) | `ApproveAssociateServiceTest` | **커버됨** |
| SEC-02 (비ADMIN 거절) | `RejectAssociateServiceTest` | **커버됨** |
| SEC-04 (자기 자신 역할 변경) | `ChangeUserRoleService` 테스트 | 확인 필요 |
| SEC-05 (마지막 ADMIN 보호) | `ChangeUserRoleService` 테스트 | 확인 필요 |
| SEC-06 (비인가 접근 부작용 없음) | - | **누락** |

### 6-3. 발견된 불일치

| ID | 내용 | 심각도 | 상태 |
|----|------|--------|------|
| DISC-01 | `DemotionReapprovalFlowTest:demotedUser_rejected_throwsException` 테스트는 DEMOTED 유저 거절 시 예외를 기대하지만, 현재 `RejectAssociateService` 코드는 DEMOTED를 비활성화하고 새 REJECTED를 생성함 | **높음** | 미해결 |
| DISC-02 | `BulkRejectAssociatesService`는 active 결정이 있는 유저를 무조건 skip하는 반면, `RejectAssociateService`는 DEMOTED만 허용하는 비대칭 동작 | **중간** | 의도적 설계인지 확인 필요 |

---

## 관련 문서

- [auth-spec.md](../feature/auth/auth-spec.md) - 기능 스펙 (FR-026~FR-041)
- [user-entity-design.md](../feature/auth/user-entity-design.md) - 엔티티 설계
- [ADR: AssociateDecision 엔티티 분리](../adr/v20260207-associate-decision-entity-separation.md) - 아키텍처 결정
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC) - 용어 및 개념 참조
