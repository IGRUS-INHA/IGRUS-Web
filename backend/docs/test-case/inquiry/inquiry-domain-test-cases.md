# 문의 도메인 엔티티 (Inquiry Domain) 테스트 케이스

**작성일**: 2026-02-11
**버전**: 1.1
**관련 스펙**: [inquiry-verification-criteria.md](../../../../docs/criteria/inquiry-verification-criteria.md)
**우선순위**: P2

---

## 1. 개요

문의 도메인 엔티티에 대한 순수 단위 테스트 케이스이다. Spring 컨텍스트 없이 순수 Java + Mockito로 검증하며, 엔티티 생성, 상태 전이 FSM, 첨부파일 관리, 답변/메모 관리, 소유권 확인, 문의번호 생성 로직을 포함한다.

**대상 클래스**:
- `Inquiry` (abstract), `GuestInquiry`, `MemberInquiry`
- `InquiryStatus` (FSM)
- `InquiryAttachment`, `InquiryReply`, `InquiryMemo`
- `InquiryNumberGenerator`

---

## 2. 테스트 케이스

### 2.1 GuestInquiry 엔티티

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-D-001 | GuestInquiry 팩토리 메서드로 생성 | - | `GuestInquiry.create(inquiryNumber, type, title, content, email, name, passwordHash)` | 객체 생성, 모든 필드 초기화, status=PENDING | ✅ |
| INQ-D-002 | isGuestInquiry() 반환값 확인 | GuestInquiry 생성됨 | `isGuestInquiry()` 호출 | true 반환 | ✅ |
| INQ-D-003 | getAuthorName() 반환값 확인 | GuestInquiry(name="홍길동") | `getAuthorName()` 호출 | "홍길동" 반환 | ✅ |
| INQ-D-004 | getAuthorEmail() 반환값 확인 | GuestInquiry(email="guest@test.com") | `getAuthorEmail()` 호출 | "guest@test.com" 반환 | ✅ |
| INQ-D-005 | getAuthorUserId() null 반환 확인 | GuestInquiry 생성됨 | `getAuthorUserId()` 호출 | null 반환 (비회원은 userId 없음) | ✅ |
| INQ-D-006 | 비밀번호 해시 저장 확인 | GuestInquiry(passwordHash="$2a$...") | 엔티티 필드 확인 | passwordHash 필드에 해시값 저장됨 (INQ-INV-05) | ✅ |

### 2.2 MemberInquiry 엔티티

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-D-010 | MemberInquiry 팩토리 메서드로 생성 | User mock 존재 | `MemberInquiry.create(inquiryNumber, type, title, content, user)` | 객체 생성, user FK 연결, status=PENDING (INQ-INV-06) | ✅ |
| INQ-D-011 | isGuestInquiry() false 반환 | MemberInquiry 생성됨 | `isGuestInquiry()` 호출 | false 반환 | ✅ |
| INQ-D-012 | getAuthorName() User 이름 반환 | User(name="김철수") mock | `getAuthorName()` 호출 | "김철수" 반환 (User 위임) | ✅ |
| INQ-D-013 | getAuthorEmail() User 이메일 반환 | User(email="user@inha.edu") mock | `getAuthorEmail()` 호출 | "user@inha.edu" 반환 (User 위임) | ✅ |
| INQ-D-014 | getAuthorUserId() User ID 반환 | User(id=42L) mock | `getAuthorUserId()` 호출 | 42L 반환 | ✅ |
| INQ-D-015 | isOwnedByUser() 본인 확인 | MemberInquiry + User(id=42L) | `isOwnedByUser(42L)` 호출 | true 반환 | ✅ |
| INQ-D-016 | isOwnedByUser() 타인 거부 | MemberInquiry + User(id=42L) | `isOwnedByUser(99L)` 호출 | false 반환 | ✅ |
| INQ-D-017 | GuestInquiry.isOwnedByUser() 항상 false | GuestInquiry 생성됨 | `isOwnedByUser(anyId)` 호출 | false 반환 (비회원은 소유권 없음) | ✅ |

### 2.3 상태 전이 FSM (GAP-INQ-01, GAP-INQ-05)

#### 2.3.1 canTransitionTo() 검증

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-D-020 | PENDING → IN_PROGRESS 전이 가능 | - | `PENDING.canTransitionTo(IN_PROGRESS)` | true | ✅ |
| INQ-D-021 | PENDING → COMPLETED 전이 가능 | - | `PENDING.canTransitionTo(COMPLETED)` | true | ✅ |
| INQ-D-022 | PENDING → PENDING 동일 상태 허용 | - | `PENDING.canTransitionTo(PENDING)` | true (멱등성) | ✅ |
| INQ-D-023 | IN_PROGRESS → PENDING 전이 가능 (GAP-INQ-05) | - | `IN_PROGRESS.canTransitionTo(PENDING)` | true | ✅ |
| INQ-D-024 | IN_PROGRESS → COMPLETED 전이 가능 (GAP-INQ-05) | - | `IN_PROGRESS.canTransitionTo(COMPLETED)` | true | ✅ |
| INQ-D-025 | IN_PROGRESS → IN_PROGRESS 동일 상태 허용 | - | `IN_PROGRESS.canTransitionTo(IN_PROGRESS)` | true (멱등성) | ✅ |
| INQ-D-026 | COMPLETED → COMPLETED 동일 상태 허용 | - | `COMPLETED.canTransitionTo(COMPLETED)` | true (멱등성) | ✅ |
| INQ-D-027 | COMPLETED → PENDING 전이 불가 (GAP-INQ-01) | - | `COMPLETED.canTransitionTo(PENDING)` | false (INQ-INV-07 종단 상태) | ✅ |
| INQ-D-028 | COMPLETED → IN_PROGRESS 전이 불가 (GAP-INQ-01) | - | `COMPLETED.canTransitionTo(IN_PROGRESS)` | false (INQ-INV-07 종단 상태) | ✅ |

#### 2.3.2 changeStatus() / startProcessing() / complete() 검증

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-D-029 | changeStatus() 유효한 전이 | PENDING 문의 | `inquiry.changeStatus(IN_PROGRESS)` | status=IN_PROGRESS | ✅ |
| INQ-D-030 | changeStatus() 무효한 전이 시 예외 | COMPLETED 문의 | `inquiry.changeStatus(PENDING)` | InvalidStatusTransitionException (INQ-INV-07) | ✅ |
| INQ-D-031 | startProcessing() 호출 | PENDING 문의 | `inquiry.startProcessing()` | status=IN_PROGRESS | ✅ |
| INQ-D-032 | complete() 호출 | PENDING 또는 IN_PROGRESS 문의 | `inquiry.complete()` | status=COMPLETED (INQ-INV-08) | ✅ |
| INQ-D-033 | 생성 직후 기본 상태 확인 | 새로 생성된 문의 | status 확인 | PENDING | ✅ |

### 2.4 첨부파일 관리

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-D-040 | 첨부파일 1개 추가 | 문의 생성됨 | `inquiry.addAttachment(attachment)` | attachments.size()=1 | ✅ |
| INQ-D-041 | 첨부파일 3개(최대) 추가 | 문의 생성됨 | 3회 `addAttachment()` 호출 | attachments.size()=3, 정상 (INQ-INV-02 경계값) | ✅ |
| INQ-D-042 | 첨부파일 4개(초과) 추가 시 예외 | 첨부파일 3개 추가된 문의 | 4번째 `addAttachment()` 호출 | InquiryMaxAttachmentsExceededException (INQ-INV-02) | ✅ |
| INQ-D-043 | getAttachments() 불변 리스트 반환 | 첨부파일 추가된 문의 | `inquiry.getAttachments().add(newAttachment)` | UnsupportedOperationException (방어적 복사) | ✅ |
| INQ-D-044 | InquiryAttachment.create() 성공 | - | `InquiryAttachment.create(fileUrl, fileName, fileSize)` | 객체 생성, 모든 필드 초기화 | ✅ |
| INQ-D-045 | 첨부파일 없는 문의의 리스트 | 문의 생성 직후 | `inquiry.getAttachments()` | 빈 리스트 반환, size()=0 | ✅ |

### 2.5 답변 관리

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-D-050 | hasReply() 답변 없을 때 | 문의 생성 직후 | `inquiry.hasReply()` | false | ✅ |
| INQ-D-051 | setReply() 후 hasReply() | 문의 생성됨 | `inquiry.setReply(reply)` 후 `inquiry.hasReply()` | true | ✅ |
| INQ-D-052 | InquiryReply.create() 성공 | Inquiry, User(operator) 존재 | `InquiryReply.create(content, inquiry, operator)` | 객체 생성, 필드 초기화 | ✅ |
| INQ-D-053 | InquiryReply.updateContent() 성공 | 답변 존재 | `reply.updateContent("수정된 내용")` | content="수정된 내용" | ✅ |
| INQ-D-054 | 답변 설정 후 getReply() 확인 | `setReply(reply)` 호출됨 | `inquiry.getReply()` | 설정한 reply 객체 반환 | ✅ |

### 2.6 메모 관리

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-D-060 | addMemo() 메모 추가 | 문의 생성됨 | `inquiry.addMemo(memo)` | memos.size()=1 | ✅ |
| INQ-D-061 | 여러 메모 추가 (제한 없음) | 문의 생성됨 | 5회 `addMemo()` 호출 | memos.size()=5 (답변과 달리 제한 없음) | ⬜ |
| INQ-D-062 | InquiryMemo.create() 성공 | Inquiry, User(operator) 존재 | `InquiryMemo.create(content, inquiry, operator)` | 객체 생성, 필드 초기화 | ✅ |
| INQ-D-063 | 메모 없는 문의의 리스트 | 문의 생성 직후 | `inquiry.getMemos()` | 빈 리스트 반환 | ✅ |

### 2.7 문의번호 생성 (InquiryNumberGenerator)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-D-070 | 첫 번째 문의번호 생성 | 당일 문의 0건 (count=0) | `generator.generate()` | INQ-YYYYMMDD00001 형식 | ✅ |
| INQ-D-071 | 10번째 문의번호 생성 | 당일 문의 9건 (count=9) | `generator.generate()` | INQ-YYYYMMDD00010 (5자리 패딩) | ✅ |
| INQ-D-072 | 100번째 문의번호 생성 | count=99 | `generator.generate()` | INQ-YYYYMMDD00100 | ✅ |
| INQ-D-073 | INQ- 접두사 확인 | - | `generator.generate()` | "INQ-"로 시작 | ✅ |
| INQ-D-074 | 문의번호 총 길이 확인 | - | `generator.generate()` | 17자 (INQ-YYYYMMDD#####) | ✅ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| INQ-INV-01 | 문의번호 유일성 | INQ-D-070~074 |
| INQ-INV-02 | 첨부파일 최대 3개 | INQ-D-040~045 |
| INQ-INV-03 | 답변 최대 1건 | INQ-D-050~054 (hasReply 검증) |
| INQ-INV-05 | 비회원 비밀번호 필수 | INQ-D-006 |
| INQ-INV-06 | 회원 사용자 참조 필수 | INQ-D-010~017 |
| INQ-INV-07 | COMPLETED 종단 상태 | INQ-D-027~028, INQ-D-030 |
| INQ-INV-08 | 답변 시 자동 완료 | INQ-D-032 |
| GAP-INQ-01 | COMPLETED 상태 전이 테스트 | INQ-D-027~028, INQ-D-030 |
| GAP-INQ-05 | IN_PROGRESS 상태 전이 테스트 | INQ-D-023~024 |

---

## 4. 구현된 테스트 클래스

### 4.1 Inquiry 도메인 단위 테스트
- **파일**: `backend/src/test/java/igrus/web/inquiry/domain/InquiryTest.java`
- **테스트 범위**: INQ-D-001~006, INQ-D-010~017, INQ-D-020~033, INQ-D-040~045, INQ-D-050~054, INQ-D-060, INQ-D-063
- **테스트 수**: 39개

### 4.2 InquiryAttachment 단위 테스트
- **파일**: `backend/src/test/java/igrus/web/inquiry/domain/InquiryAttachmentTest.java`
- **테스트 범위**: INQ-D-044
- **테스트 수**: 1개

### 4.3 InquiryReply 단위 테스트
- **파일**: `backend/src/test/java/igrus/web/inquiry/domain/InquiryReplyTest.java`
- **테스트 범위**: INQ-D-052~053
- **테스트 수**: 2개

### 4.4 InquiryMemo 단위 테스트
- **파일**: `backend/src/test/java/igrus/web/inquiry/domain/InquiryMemoTest.java`
- **테스트 범위**: INQ-D-062
- **테스트 수**: 1개

### 4.5 InquiryNumberGenerator 단위 테스트
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/support/InquiryNumberGeneratorTest.java`
- **테스트 범위**: INQ-D-070~074
- **테스트 수**: 5개

### 4.6 미구현 테스트
- INQ-D-061 (여러 메모 추가 도메인 레벨): 서비스 레벨에서 INQ-A-061로 간접 검증됨

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-11 | - | 검증 기준서 기반 최초 작성 |
| 1.1 | 2026-02-11 | - | INQ-D-006, 014, 020~028, 030, 045, 054, 063 테스트 구현 완료 |
