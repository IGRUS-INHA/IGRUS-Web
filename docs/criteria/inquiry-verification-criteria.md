# 문의 (Inquiry) 검증 기준서

> **Status**: Review Completed
> **Last Updated**: 2026-02-24
> **Scope**: 비회원 문의(GuestInquiry), 회원 문의(MemberInquiry), 문의 조회(Lookup), 답변/메모(Reply/Memo), 상태 관리(Status), 삭제(Delete)
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)

## 목적

이 문서는 문의 도메인에서 **반드시 지켜져야 하는 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 도메인에 직접 관련된 8개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 문의번호 유일성, 첨부파일 제한, 답변 1건 제한 등 핵심 계약 |
| 2 | 상태 모델 | InquiryStatus FSM (PENDING → IN_PROGRESS → COMPLETED) |
| 3 | 시스템 경계와 책임 분리 | Guest/Member 상속 구조, 알림 서비스, 트랜잭션 격리 |
| 4 | 외부 의존성 실패 정책 | 이메일 발송 실패, 문의번호 충돌 재시도 |
| 5 | 입력 도메인 분할과 경계값 | Guest/Member 입력 검증, 첨부파일 경계값 |
| 6 | 권한/보안 정책 | RBAC, 본인 소유권 검증, 비회원 비밀번호 BCrypt |
| 7 | 관측 가능성 | 서비스별 로그, 이메일 발송 추적, soft delete 감사 |
| 8 | 테스트 전략 | 테스트-검증 항목 매핑, 커버리지 현황 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

### INQ-INV-01: 문의번호 유일성

> `inquiries_inquiry_number`는 시스템 전체에서 유일하다.

- **사전조건**: `InquiryNumberGenerator.generate()`가 날짜 기반 순번 생성 (`INQ-YYYYMMDD#####`)
- **사후조건**: DB UNIQUE 제약조건 + 재시도 로직으로 보장
- **위반 시**: `DataIntegrityViolationException` 발생, 최대 3회 재시도 후 `InquiryNumberGenerationException`
- **관련 코드**:
  - `InquiryNumberGenerator:24-29` - 문의번호 생성 (날짜 prefix + 순번)
  - `InquiryPersistenceExecutor:33-40` - `REQUIRES_NEW` 트랜잭션으로 격리
  - `CreateGuestInquiryService:44-81` - 재시도 루프 (3회)
  - `CreateMemberInquiryService:48-85` - 재시도 루프 (3회)
- **검증 방법**: 동시 생성 시나리오에서 문의번호 중복 없이 모두 성공하는지 확인

### INQ-INV-02: 첨부파일 최대 3개 제한

> 하나의 문의에 첨부파일은 최대 3개까지만 가능하다.

- **검증 계층**: DTO 레벨 (`@Size(max = 3)`) + 엔티티 레벨 (`Inquiry.MAX_ATTACHMENTS = 3`)
- **위반 시**: DTO 검증 실패 또는 `InquiryMaxAttachmentsExceededException`
- **관련 코드**:
  - `Inquiry:34` - `MAX_ATTACHMENTS = 3` 상수 정의
  - `Inquiry:102-108` - `addAttachment()` 메서드의 크기 검증
  - `CreateGuestInquiryRequest:53-55` - `@Size(max = 3)` DTO 검증
  - `CreateMemberInquiryRequest:38-40` - `@Size(max = 3)` DTO 검증
- **검증 방법**: 3개 첨부 시 성공, 4개 첨부 시 예외 assertion

### INQ-INV-03: 문의당 답변 최대 1건

> 하나의 문의에는 답변(`InquiryReply`)이 최대 1건만 존재한다.

- **검증 계층**: DB UNIQUE 제약 (`inquiry_replies_inquiry_id` UNIQUE) + 서비스 레벨 검증 (`inquiry.hasReply()`)
- **위반 시**: `InquiryAlreadyRepliedException`
- **관련 코드**:
  - `InquiryReply` - `@OneToOne` 관계, `inquiry_replies_inquiry_id` UNIQUE 컬럼
  - `CreateInquiryReplyService:42-44` - 기존 답변 존재 시 예외 발생
  - `Inquiry:115-117` - `hasReply()` null 체크
- **검증 방법**: 답변 작성 후 동일 문의에 재답변 시 `InquiryAlreadyRepliedException` assertion

### INQ-INV-04: Soft Delete된 문의는 일반 조회에서 제외

> `inquiries_deleted = true`인 문의는 모든 JPA 조회에서 자동 필터링된다.

- **근거**: `@SQLRestriction("inquiries_deleted = false")` (Inquiry:20)
- **사후조건**: soft delete된 문의는 `findById()`, `findAll()` 등 일반 쿼리에서 반환되지 않음
- **예외**: 네이티브 쿼리 (통계, 문의번호 생성용 `countByInquiryNumberPrefix`)는 soft delete 무시
- **관련 코드**:
  - `Inquiry:20` - `@SQLRestriction` 적용
  - `DeleteInquiryService:27-31` - `inquiry.delete(operatorId)` 호출
  - `InquiryNumberGenerator:26` - `countByInquiryNumberPrefix()` 네이티브 쿼리 (삭제 포함)
- **검증 방법**: 문의 삭제 후 조회 시 `InquiryNotFoundException` 발생 확인

### INQ-INV-05: 비회원 문의는 비밀번호 해시 필수

> `GuestInquiry`는 반드시 `guest_inquiries_password_hash` 값이 존재한다.

- **검증 계층**: DB NOT NULL 제약 + DTO `@NotBlank` + 엔티티 팩토리 메서드 파라미터
- **관련 코드**:
  - `GuestInquiry:21-22` - `@Column(nullable = false)` passwordHash
  - `CreateGuestInquiryRequest:48-50` - `@NotBlank` password
  - `CreateGuestInquiryService:42` - `passwordEncoder.encode()` BCrypt 해싱
- **위반 시나리오**: 비밀번호 없이 비회원 문의 생성 시 DTO 검증 실패

### INQ-INV-06: 회원 문의는 유효한 사용자 참조 필수

> `MemberInquiry`는 반드시 존재하는 `User`를 참조한다.

- **검증 계층**: DB NOT NULL + FK 제약 + 서비스 레벨 조회
- **관련 코드**:
  - `MemberInquiry:16-18` - `@ManyToOne` User, `nullable = false`
  - `CreateMemberInquiryService:45-46` - `userRepository.findById()` 후 `UserNotFoundException`
- **위반 시**: `UserNotFoundException`

### INQ-INV-07: COMPLETED 상태는 종단 상태

> `InquiryStatus.COMPLETED`에서는 다른 상태로 전이할 수 없다.

- **근거**: `InquiryStatus.canTransitionTo()` - COMPLETED → false (InquiryStatus:36)
- **예외**: 동일 상태 전이 (COMPLETED → COMPLETED)는 멱등성 보장으로 허용
- **위반 시**: `InvalidStatusTransitionException`
- **관련 코드**:
  - `InquiryStatus:33-37` - `canTransitionTo()` switch 문
  - `Inquiry:86-91` - `changeStatus()` 검증 후 예외

### INQ-INV-08: 답변 작성 시 상태 자동 완료

> 답변 작성 시 문의 상태가 자동으로 `COMPLETED`로 전이된다.

- **근거**: `CreateInquiryReplyService:51` - `inquiry.complete()` 호출
- **사후조건**: 답변 존재하는 문의의 상태는 반드시 `COMPLETED`
- **관련 코드**:
  - `CreateInquiryReplyService:49-51` - reply 설정 + complete() 호출
- **검증 방법**: 답변 작성 후 문의 상태가 COMPLETED인지 assertion

### INQ-INV-09: 문의 상태 변경 감사 이력 (신규)

> 문의 상태가 변경될 때 `InquiryStatusChangeHistory`에 변경 이력이 기록된다.

- **기록 대상 변경 유형** (`InquiryChangeType`):
  - `STATUS_CHANGED` — 관리자가 수동으로 상태 변경 (`UpdateInquiryStatusService`)
  - `REPLY_COMPLETED` — 답변 작성으로 인한 자동 완료 (`CreateInquiryReplyService`)
- **기록하지 않는 변경**: 멱등성 전이 (동일 상태 → 동일 상태)는 실제 변경이 아니므로 기록하지 않음
- **사후조건**: 변경 유형, 이전/이후 상태값, 변경자 정보, 학번(비정규화)이 감사 이력에 기록됨
- **구현 방식**: `@EventListener` + `REQUIRES_NEW` 독립 트랜잭션. 이력 저장 실패가 비즈니스 로직에 영향 없음 (try-catch 격리)
- **설계 결정**: FK 없이 ID만 저장 (soft-delete/탈퇴 후에도 이력 영구 보존), `changedByStudentId` 비정규화
- **관련 코드** `(현재 구현 일치)`:
  - `InquiryStatusChangeHistory` — 감사 이력 엔티티 (`BaseEntity` 상속)
  - `InquiryChangeType` — 변경 유형 enum
  - `InquiryStatusChangeEvent` — Spring 이벤트 record
  - `RecordInquiryStatusChangeService` — `@EventListener` + `REQUIRES_NEW` TransactionTemplate 리스너
  - `UpdateInquiryStatusService` — `STATUS_CHANGED` 이벤트 발행
  - `CreateInquiryReplyService` — `REPLY_COMPLETED` 이벤트 발행
- **검증 방법**: 상태 변경 후 `inquiry_status_change_histories` 테이블에 이력 레코드 존재 확인

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. 문의 상태 전이 (InquiryStatus FSM)

```
┌─────────┐     changeStatus     ┌─────────────┐
│ PENDING │ ───────────────────> │ IN_PROGRESS │
└────┬────┘ <─────────────────── └──────┬──────┘
     │         changeStatus             │
     │                                  │
     │         changeStatus             │ changeStatus
     └──────────────┐                   │
                    ▼                   ▼
              ┌───────────┐
              │ COMPLETED │  (종단 상태)
              └───────────┘
```

| 전이 | 트리거 | 사전조건 | 사후조건 | 관련 코드 |
|------|--------|---------|---------|----------|
| PENDING → IN_PROGRESS | 관리자 상태 변경 | OPERATOR/ADMIN 권한 | `inquiry.status = IN_PROGRESS` | `UpdateInquiryStatusService:28-32` |
| PENDING → COMPLETED | 관리자 상태 변경 또는 답변 작성 | OPERATOR/ADMIN 권한 | `inquiry.status = COMPLETED` | `UpdateInquiryStatusService:28-32`, `CreateInquiryReplyService:51` |
| IN_PROGRESS → PENDING | 관리자 상태 변경 (되돌리기) | OPERATOR/ADMIN 권한 | `inquiry.status = PENDING` | `UpdateInquiryStatusService:28-32` |
| IN_PROGRESS → COMPLETED | 관리자 상태 변경 또는 답변 작성 | OPERATOR/ADMIN 권한 | `inquiry.status = COMPLETED` | `UpdateInquiryStatusService:28-32`, `CreateInquiryReplyService:51` |
| 동일 상태 → 동일 상태 | 관리자 상태 변경 | OPERATOR/ADMIN 권한 | 변경 없음 (멱등) | `InquiryStatus:29-30` |

**금지된 전이 (Invalid Transition)**:

| 시도 | 예상 결과 | 이유 |
|------|----------|------|
| COMPLETED → PENDING | `InvalidStatusTransitionException` | 완료된 문의는 상태 되돌림 불가 |
| COMPLETED → IN_PROGRESS | `InvalidStatusTransitionException` | 완료된 문의는 상태 되돌림 불가 |

### 2-2. 편의 메서드에 의한 상태 전이

`Inquiry`는 `changeStatus()` 외에 `complete()`와 `startProcessing()` 편의 메서드를 제공한다. 두 메서드 모두 내부적으로 `changeStatus()`를 호출하여 FSM 검증을 거친다.

| 메서드 | 동작 | FSM 검증 | 관련 코드 |
|--------|------|:---:|----------|
| `changeStatus(newStatus)` | 직접 상태 전이 | O | `Inquiry:86-91` |
| `complete()` | `changeStatus(COMPLETED)` 호출 | O | `Inquiry:97-99` |
| `startProcessing()` | `changeStatus(IN_PROGRESS)` 호출 | O | `Inquiry:93-95` |

- **답변 작성 시**: `CreateInquiryReplyService:51`에서 `inquiry.complete()` 호출 → `changeStatus(COMPLETED)` → FSM 검증 통과 후 상태 변경
- **주의**: COMPLETED 상태의 문의에 답변을 작성하는 경우, `hasReply()` 검증에 의해 `InquiryAlreadyRepliedException`으로 차단됨 (상태 전이 이전에 차단)

---

## 3. 시스템 경계와 책임 분리 (System Boundary & SoC)

### 3-1. Guest/Member 상속 구조

문의 엔티티는 JOINED 상속 전략으로 Guest/Member를 분리한다.

```
                ┌──────────────────────┐
                │   Inquiry (abstract) │  inquiries 테이블
                │  - inquiryNumber     │
                │  - type, status      │
                │  - title, content    │
                └──────┬───────────────┘
                       │
            ┌──────────┴──────────┐
            │                     │
   ┌────────▼─────────┐  ┌───────▼──────────┐
   │   GuestInquiry   │  │  MemberInquiry   │
   │  guest_inquiries  │  │ member_inquiries │
   │  - email          │  │  - user (FK)     │
   │  - name           │  │                  │
   │  - passwordHash   │  │                  │
   └──────────────────┘  └──────────────────┘
```

| 구분 | GuestInquiry | MemberInquiry |
|------|-------------|---------------|
| 인증 | 불필요 (공개 API) | JWT 인증 필수 |
| 작성자 이메일 | 요청에서 직접 입력 | 사용자 엔티티에서 가져옴 |
| 본인 확인 | 문의번호 + 이메일 + 비밀번호 | JWT userId 기반 소유권 확인 |
| 컨트롤러 | `GuestInquiryController` | `MemberInquiryController` |
| 서비스 | `CreateGuestInquiryService` | `CreateMemberInquiryService` |

### 3-2. 알림 서비스 경계 (Notification Boundary)

이메일 알림은 Strategy 패턴으로 분리되어 있다.

| 컴포넌트 | 책임 | 신뢰 경계 |
|---------|------|----------|
| `InquiryNotificationService` (인터페이스) | 알림 발송 계약 정의 | 내부 |
| `LoggingInquiryNotificationService` | 개발 환경 로깅 | 내부 |
| `SmtpInquiryNotificationService` | SMTP 이메일 발송 | **외부** (메일 서버) |

- **장애 허용성**: 알림 발송 실패는 문의 생성/답변 트랜잭션에 영향을 주지 않음 (try-catch로 격리, 실패 시 `log.error`로 기록)
- **관련 코드**: `CreateGuestInquiryService:66-74`, `CreateMemberInquiryService:68-76`, `CreateInquiryReplyService:55-64`

### 3-3. 트랜잭션 격리 (InquiryPersistenceExecutor)

문의 저장은 `REQUIRES_NEW` 전파 속성으로 별도 트랜잭션에서 실행된다.

| 구분 | 설명 |
|------|------|
| 목적 | 문의번호 충돌 시 호출자의 영속성 컨텍스트 오염 방지 |
| 전파 | `Propagation.REQUIRES_NEW` (`InquiryPersistenceExecutor:33`) |
| 효과 | `DataIntegrityViolationException` 발생 시 격리된 트랜잭션만 롤백, 호출자 트랜잭션은 유지 |
| 주의 | 내부 트랜잭션이 커밋된 후 외부에서 예외 발생 시, 이미 저장된 문의는 롤백되지 않음 |

---

## 4. 외부 의존성 실패 정책 (External Dependency Failure Policy)

### 4-1. 문의번호 충돌 재시도

| 항목 | 설정값 |
|------|-------|
| 재시도 횟수 | 최대 3회 (`MAX_INQUIRY_NUMBER_RETRIES = 3`) |
| 재시도 트리거 | `DataIntegrityViolationException` (UNIQUE 제약 위반) |
| 재시도 전략 | 즉시 재시도 (지수 백오프 없음) |
| 최종 실패 | `InquiryNumberGenerationException` |
| 멱등성 | 각 재시도마다 새 문의번호 생성, 이전 실패 트랜잭션은 롤백됨 |

**관련 코드**: `CreateGuestInquiryService:44-81`, `CreateMemberInquiryService:48-85`

### 4-2. 이메일 알림 발송

| 항목 | 발송 시점 | 수신자 | 관련 코드 |
|------|----------|--------|----------|
| 문의 접수 확인 | 문의 생성 직후 | 문의자 이메일 | `CreateGuestInquiryService:66-74`, `CreateMemberInquiryService:68-76` |
| 답변 알림 | 답변 작성 직후 | 문의자 이메일 (`inquiry.getAuthorEmail()`) | `CreateInquiryReplyService:55-64` |

- **실패 정책**: 알림 발송 실패 시 try-catch로 격리, `log.error`로 기록 후 문의 생성/답변 성공 응답 반환. 별도 재시도/DLQ 없음.
- **타임아웃**: SMTP 서버 응답 타임아웃 설정 확인 필요

---

## 5. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 5-1. 비회원 문의 생성 입력값 (CreateGuestInquiryRequest)

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 | DTO 검증 |
|------|-----------|-----------|--------|---------|
| `type` | `JOIN`, `EVENT`, `REPORT`, `ACCOUNT`, `OTHER` | null, 유효하지 않은 값 | - | `@NotNull` |
| `title` | 1~100자 문자열 | null, 빈 문자열, 101자 이상 | 1자 (최소), 100자 (최대), 101자 (초과) | `@NotBlank`, `@Size(max=100)` |
| `content` | 비어있지 않은 문자열 | null, 빈 문자열 | - | `@NotBlank` |
| `email` | 유효한 이메일 형식 | null, 빈 문자열, @없음 | - | `@NotBlank`, `@Email` |
| `name` | 1~50자 문자열 | null, 빈 문자열, 51자 이상 | 1자 (최소), 50자 (최대), 51자 (초과) | `@NotBlank`, `@Size(max=50)` |
| `password` | 비어있지 않은 문자열 | null, 빈 문자열 | - | `@NotBlank` |
| `attachments` | 0~3개 `AttachmentInfo` 리스트 | 4개 이상 | 0개 (최소), 3개 (최대), 4개 (초과) | `@Size(max=3)` |

### 5-2. 회원 문의 생성 입력값 (CreateMemberInquiryRequest)

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 | DTO 검증 |
|------|-----------|-----------|--------|---------|
| `type` | `JOIN`, `EVENT`, `REPORT`, `ACCOUNT`, `OTHER` | null, 유효하지 않은 값 | - | `@NotNull` |
| `title` | 1~100자 문자열 | null, 빈 문자열, 101자 이상 | 1자 (최소), 100자 (최대) | `@NotBlank`, `@Size(max=100)` |
| `content` | 비어있지 않은 문자열 | null, 빈 문자열 | - | `@NotBlank` |
| `attachments` | 0~3개 `AttachmentInfo` 리스트 | 4개 이상 | 0개 (최소), 3개 (최대), 4개 (초과) | `@Size(max=3)` |

**회원 문의에는 이메일/이름/비밀번호 필드가 없음** - 사용자 엔티티에서 가져옴.

### 5-3. 첨부파일 입력값 (AttachmentInfo)

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 | DTO 검증 |
|------|-----------|-----------|--------|---------|
| `fileUrl` | 유효한 HTTP/HTTPS URL | null, 빈 문자열, 형식 불일치 | - | `@NotBlank`, `@Pattern("^https?://...")` |
| `fileName` | 1~255자 문자열 | null, 빈 문자열, 256자 이상 | 1자 (최소), 255자 (최대) | `@NotBlank`, `@Size(max=255)` |
| `fileSize` | 양수 (1 이상) | null, 0, 음수 | 1 (최소) | `@NotNull`, `@Positive` |

### 5-4. 비회원 문의 조회 입력값 (GuestInquiryLookupRequest)

| 필드 | 유효 동치류 | 무효 동치류 | DTO 검증 |
|------|-----------|-----------|---------|
| `inquiryNumber` | 존재하는 문의번호 | null, 빈 문자열, 존재하지 않는 번호 | `@NotBlank` |
| `email` | 등록된 이메일과 일치 | null, 빈 문자열, 이메일 불일치 | `@NotBlank`, `@Email` |
| `password` | 등록 시 비밀번호와 일치 | null, 빈 문자열, 비밀번호 불일치 | `@NotBlank` |

**조회 검증 순서**:
1. 문의번호 + 이메일로 문의 조회 (`InquiryNotFoundException` 가능)
2. 비밀번호 BCrypt 비교 (`InquiryInvalidPasswordException` 가능)

### 5-5. 문의번호 경계값

| 항목 | 형식 | 예시 |
|------|------|------|
| 정상 | `INQ-YYYYMMDD#####` | `INQ-2026021100001` |
| 일별 최대 순번 | 99999 | `INQ-2026021199999` |
| 소프트 삭제된 문의 포함 카운트 | 네이티브 쿼리로 삭제 포함 | 번호 공백 없음 보장 |

**문의번호 길이**: `INQ-` (4자) + `YYYYMMDD` (8자) + `#####` (5자) = **총 17자** (`Inquiry:41` - `@Column(length = 20)`으로 여유 확보)

---

## 6. 권한/보안 정책 (RBAC & Authorization)

### 6-1. 역할별 접근 제어 매트릭스

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 비회원 문의 작성 | **O** | - | - | - | - |
| 비회원 문의 조회 (lookup) | **O** | - | - | - | - |
| 회원 문의 작성 | 401 | **O** | **O** | **O** | **O** |
| 내 문의 목록 조회 | 401 | **O** | **O** | **O** | **O** |
| 내 문의 상세 조회 | 401 | **O** (본인만) | **O** (본인만) | **O** (본인만) | **O** (본인만) |
| 전체 문의 목록 조회 | 401 | 403 | 403 | **O** | **O** |
| 문의 상세 조회 (관리자) | 401 | 403 | 403 | **O** | **O** |
| 문의 상태 변경 | 401 | 403 | 403 | **O** | **O** |
| 답변 작성 | 401 | 403 | 403 | **O** | **O** |
| 답변 수정 | 401 | 403 | 403 | **O** | **O** |
| 내부 메모 작성 | 401 | 403 | 403 | **O** | **O** |
| 문의 삭제 (soft) | 401 | 403 | 403 | **O** | **O** |

### 6-2. 소유권 검증 (Ownership Verification)

| ID | 검증 항목 | 예상 결과 | 관련 코드 |
|----|----------|----------|----------|
| SEC-INQ-01 | 회원이 다른 사용자의 문의 상세 조회 시도 | `InquiryAccessDeniedException` | `GetMyInquiryService:30-31` |
| SEC-INQ-02 | 비회원이 이메일 불일치로 문의 조회 시도 | `InquiryNotFoundException` | `LookupGuestInquiryService:33-36` |
| SEC-INQ-03 | 비회원이 잘못된 비밀번호로 문의 조회 시도 | `InquiryInvalidPasswordException` | `LookupGuestInquiryService:38-40` |

**보안 설계**:
- 비회원 문의 조회 실패 시 문의 존재 여부를 노출하지 않음: 이메일 불일치 시 `InquiryNotFoundException` (문의번호+이메일 복합 조회)
- 비밀번호는 BCrypt 해싱 저장 (`passwordEncoder.encode()`), 조회 시 `passwordEncoder.matches()`로 비교

### 6-3. 권한 검증 체크리스트

| ID | 검증 항목 | 예상 결과 | 검증 서비스 |
|----|----------|----------|-----------|
| SEC-INQ-04 | ASSOCIATE/MEMBER가 전체 문의 목록 조회 시도 | 403 Forbidden | Spring Security `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")` |
| SEC-INQ-05 | ASSOCIATE/MEMBER가 답변 작성 시도 | 403 Forbidden | Spring Security (컨트롤러 레벨) |
| SEC-INQ-06 | ASSOCIATE/MEMBER가 문의 삭제 시도 | 403 Forbidden | Spring Security (컨트롤러 레벨) |
| SEC-INQ-07 | 비인가 접근이 상태를 변경하지 않는지 (부작용 없음) | DB 변경 없음 | 트랜잭션 롤백 확인 |

### 6-4. 보안 주의사항

- **비밀번호 저장**: BCrypt 인코딩 (`CreateGuestInquiryService:42`)
- **내부 메모 노출 방지**: `InquiryMemo`는 관리자 상세 조회(`GetInquiryDetailService`)에서만 반환, 회원/비회원 조회 응답에는 포함되지 않음
- **soft delete 후 접근**: `@SQLRestriction`에 의해 삭제된 문의는 자동 필터링

---

## 7. 관측 가능성 (Observability & Audit)

### 7-1. 서비스별 로그 메시지

| 서비스 | 시작/완료 로그 | 실패 로그 |
|--------|-------------|---------|
| `CreateGuestInquiryService` | `비회원 문의 생성: inquiryNumber, email` | `문의 번호 중복 발생, 재시도 중: attempt`, `문의 접수 확인 이메일 발송 실패: inquiryNumber, email` |
| `CreateMemberInquiryService` | `회원 문의 생성: inquiryNumber, userId` | `문의 번호 중복 발생, 재시도 중: attempt`, `문의 접수 확인 이메일 발송 실패: inquiryNumber, userId` |
| `CreateInquiryReplyService` | `문의 답변 작성: inquiryId, operatorId` | `답변 알림 이메일 발송 실패: inquiryId, email` |
| `UpdateInquiryStatusService` | `문의 상태 변경: inquiryId, newStatus` | - |
| `DeleteInquiryService` | `문의 삭제: inquiryId, deletedBy` | - |

### 7-2. 이메일 발송 추적

| 이벤트 | 발송 여부 | 수신자 소스 | 관련 코드 |
|--------|:---:|----------|----------|
| 비회원 문의 접수 확인 | O | `request.getEmail()` | `CreateGuestInquiryService:66-74` |
| 회원 문의 접수 확인 | O | `user.getEmail()` | `CreateMemberInquiryService:68-76` |
| 답변 알림 | O | `inquiry.getAuthorEmail()` (다형성) | `CreateInquiryReplyService:55-64` |

### 7-3. Soft Delete 감사 이력

| 필드 | 저장 내용 | 관련 컬럼 |
|------|---------|----------|
| 삭제 여부 | `true` | `inquiries_deleted` |
| 삭제 시각 | 삭제 시점 타임스탬프 | `inquiries_deleted_at` |
| 삭제자 | 운영자 ID | `inquiries_deleted_by` |

### 7-4. 상태 변경 감사 이력 (`inquiry_status_change_histories`)

문의 상태가 변경될 때 `InquiryStatusChangeHistory` 엔티티에 이력을 기록한다 (INQ-INV-09).

| 필드 | 저장 내용 | 컬럼명 |
|------|---------|--------|
| 문의 ID | 대상 문의 | `inquiry_status_change_histories_inquiry_id` |
| 변경자 ID | 운영자 ID | `inquiry_status_change_histories_changed_by_id` |
| 변경자 학번 | 운영자 학번 (비정규화) | `inquiry_status_change_histories_changed_by_student_id` |
| 변경 유형 | `InquiryChangeType` enum | `inquiry_status_change_histories_change_type` |
| 이전 값 | 변경 전 상태명 | `inquiry_status_change_histories_previous_value` |
| 이후 값 | 변경 후 상태명 | `inquiry_status_change_histories_new_value` |
| 생성 시각 | 이력 기록 시각 | `inquiry_status_change_histories_created_at` |

- **구현 방식**: `@EventListener` + `REQUIRES_NEW` TransactionTemplate (`RecordInquiryStatusChangeService`)
- **FK 없음**: soft-delete/탈퇴 후에도 이력 영구 보존
- **인덱스**: `inquiry_id`, `changed_by_id`, `change_type`, `created_at`

---

## 8. 테스트 전략 (Test Strategy)

### 8-1. 현재 테스트 현황

**도메인 단위 테스트** (순수 Java, Mock 사용):

| 테스트 클래스 | 테스트 수 | 범위 |
|-------------|---------|------|
| `InquiryTest` | 17개 | GuestInquiry 생성(3), MemberInquiry 생성(3), 상태 변경(3), 첨부파일 관리(4), 답변 관리(2), 메모 관리(2), 소유권 확인(3) -- 중첩 클래스 구조 |
| `InquiryReplyTest` | 2개 | 답변 생성, 내용 수정 |
| `InquiryMemoTest` | 1개 | 메모 생성 |
| `InquiryAttachmentTest` | 1개 | 첨부파일 생성 |

**서비스 통합 테스트** (`@SpringBootTest`, non-transactional):

| 테스트 클래스 | 테스트 수 | 범위 |
|-------------|---------|------|
| `CreateGuestInquiryServiceTest` | 2개 | 비회원 문의 생성, 첨부파일 포함 생성 |
| `CreateMemberInquiryServiceTest` | 2개 | 회원 문의 생성, 사용자 없음 예외 |
| `CreateInquiryReplyServiceTest` | 2개 | 답변 작성 성공, 중복 답변 예외 |
| `CreateInquiryMemoServiceTest` | 1개 | 메모 작성 |
| `UpdateInquiryStatusServiceTest` | 7개 | 유효한 상태 전이(4), 멱등성 전이(2), 금지된 전이(2), 예외(1) |
| `DeleteInquiryServiceTest` | 1개 | soft delete 후 조회 불가, 삭제 포함 카운트 검증 |
| `GetAllInquiriesServiceTest` | 5개 | 전체 목록 조회, 유형별 필터링, 상태별 필터링, 복합 필터링, 빈 목록 |
| `GetMyInquiriesServiceTest` | 2개 | 내 문의 목록 조회, 빈 목록 조회 |
| `GetMyInquiryServiceTest` | 2개 | 내 문의 상세 조회, 다른 사용자 접근 시 예외 |
| `LookupGuestInquiryServiceTest` | 3개 | 정상 조회, 비밀번호 불일치, 문의번호 불일치 |

**지원 컴포넌트 테스트**:

| 테스트 클래스 | 테스트 수 | 범위 |
|-------------|---------|------|
| `InquiryNumberGeneratorTest` | 5개 | 문의번호 생성 로직, 순번 증가, prefix/길이 검증 |

### 8-2. 테스트-검증 항목 매핑

#### 불변조건 커버리지

| 불변조건 | 커버 테스트 | 상태 |
|---------|-----------|------|
| INQ-INV-01 (문의번호 유일성) | `InquiryNumberGeneratorTest` | **부분 커버** (생성 로직만, 동시성 미검증) |
| INQ-INV-02 (첨부파일 최대 3개) | `InquiryTest:addAttachment_UpTo3Attachments_Success`, `addAttachment_MoreThan3Attachments_ThrowsException` | **커버됨** |
| INQ-INV-03 (답변 최대 1건) | `CreateInquiryReplyServiceTest:createReply_WhenAlreadyReplied_ThrowsException` | **커버됨** |
| INQ-INV-04 (soft delete 필터링) | `DeleteInquiryServiceTest:deleteInquiry_WithValidId_SoftDeletes` | **커버됨** (`findById` 빈 결과 + `countByIdIncludingDeleted` = 1 검증) |
| INQ-INV-05 (비회원 비밀번호 필수) | `InquiryTest:createGuestInquiry_WithValidInfo_ReturnsInquiry` | **간접 커버** (팩토리 메서드 시그니처로 강제) |
| INQ-INV-06 (회원 사용자 참조 필수) | `CreateMemberInquiryServiceTest:createMemberInquiry_WithInvalidUserId_ThrowsException` | **커버됨** |
| INQ-INV-07 (COMPLETED 종단 상태) | `UpdateInquiryStatusServiceTest:INQ-A-025,026` | **커버됨** (COMPLETED → PENDING/IN_PROGRESS 금지 검증) |
| INQ-INV-08 (답변 시 자동 완료) | `CreateInquiryReplyServiceTest:createReply_WithValidRequest_Success` (line 114) | **커버됨** |
| INQ-INV-09 (상태 변경 감사 이력) | - | **부분 커버** (이벤트 발행 로직 존재, 전용 통합 테스트 미작성) |

#### 상태 전이 커버리지

| 전이 | 커버 테스트 | 상태 |
|------|-----------|------|
| PENDING → IN_PROGRESS | `UpdateInquiryStatusServiceTest:updateInquiryStatus_WithValidStatus_Success` | **커버됨** |
| PENDING → COMPLETED | `InquiryTest:complete_ChangesStatusToCompleted` | **커버됨** (도메인 테스트) |
| IN_PROGRESS → PENDING | `UpdateInquiryStatusServiceTest:INQ-A-022` | **커버됨** |
| IN_PROGRESS → COMPLETED | `UpdateInquiryStatusServiceTest:INQ-A-023` | **커버됨** |
| COMPLETED → PENDING (금지) | `UpdateInquiryStatusServiceTest:INQ-A-025` | **커버됨** |
| COMPLETED → IN_PROGRESS (금지) | `UpdateInquiryStatusServiceTest:INQ-A-026` | **커버됨** |

#### 소유권/보안 커버리지

| 검증 | 커버 테스트 | 상태 |
|------|-----------|------|
| SEC-INQ-01 (다른 사용자 문의 조회) | `GetMyInquiryServiceTest:getMyInquiry_WithDifferentUserId_ThrowsException` | **커버됨** |
| SEC-INQ-02 (이메일 불일치 조회) | `LookupGuestInquiryServiceTest:lookupGuestInquiry_WithInvalidInquiryNumber_ThrowsException` | **부분 커버** (번호 불일치만, 이메일 불일치 미검증) |
| SEC-INQ-03 (비밀번호 불일치 조회) | `LookupGuestInquiryServiceTest:lookupGuestInquiry_WithWrongPassword_ThrowsException` | **커버됨** |
| SEC-INQ-04~06 (RBAC 접근 제어) | - | **누락** (컨트롤러 레벨 테스트 없음) |
| SEC-INQ-07 (비인가 접근 부작용 없음) | - | **누락** |

### 8-3. 발견된 누락 및 개선 사항

| ID | 내용 | 심각도 | 상태 |
|----|------|--------|------|
| GAP-INQ-01 | COMPLETED 상태에서 다른 상태로의 전이 시도에 대한 통합 테스트 부재 | **중간** | **해결됨** (INQ-A-025, INQ-A-026) |
| GAP-INQ-02 | 비회원 문의 조회 시 이메일 불일치 케이스 테스트 부재 | **낮음** | 미해결 |
| GAP-INQ-03 | 문의번호 동시 생성 시 충돌 재시도 로직 통합 테스트 부재 | **중간** | 미해결 |
| GAP-INQ-04 | 컨트롤러 레벨 RBAC 검증 테스트 (MockMvc) 부재 | **중간** | 미해결 |
| GAP-INQ-05 | IN_PROGRESS → PENDING/COMPLETED 상태 전이 통합 테스트 부재 | **낮음** | **해결됨** (INQ-A-022, INQ-A-023) |
| GAP-INQ-06 | 이메일 알림 발송 실패 시 문의 생성 트랜잭션에 미치는 영향 미검증 | **중간** | **해결됨** (try-catch 격리 적용) |

---

## 관련 문서

- [IGRUS_WEB_PRD_V2.md](../feature/common/IGRUS_WEB_PRD_V2.md) - PRD 문의 섹션 (6. 문의)
- [회원가입/승인/강등 검증 기준서](./verification-criteria.md) - 동일 형식의 기존 검증 기준서
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC) - 용어 및 개념 참조
