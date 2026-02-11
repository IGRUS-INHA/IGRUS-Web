# 비회원 문의 (Guest Inquiry) 테스트 케이스

**작성일**: 2026-02-11
**버전**: 1.0
**관련 스펙**: [inquiry-verification-criteria.md](../../../../docs/criteria/inquiry-verification-criteria.md)
**우선순위**: P1

---

## 1. 개요

비회원 문의 기능에 대한 테스트 케이스이다. 비회원은 인증 없이 문의를 작성하고, 문의번호+이메일+비밀번호 조합으로 자신의 문의를 조회할 수 있다.

**대상 엔드포인트**:
- `POST /api/v1/inquiries/guest` - 비회원 문의 생성 (공개)
- `POST /api/v1/inquiries/lookup` - 비회원 문의 조회 (공개)

**대상 서비스**:
- `CreateGuestInquiryService`
- `LookupGuestInquiryService`

---

## 2. 테스트 케이스

### 2.1 비회원 문의 생성 - 성공

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-G-001 | 유효한 정보로 비회원 문의 생성 | - | type=JOIN, title="가입 문의", content="내용", email="guest@test.com", name="홍길동", password="pass123" 전송 | 201 Created, 문의번호(INQ-YYYYMMDD#####) 발급, 상태=PENDING | ✅ |
| INQ-G-002 | 첨부파일 1개 포함 문의 생성 | - | 유효한 요청 + 첨부파일 1개(fileUrl, fileName, fileSize) | 201 Created, 문의 + 첨부파일 1개 저장 | ✅ |
| INQ-G-003 | 첨부파일 2개 포함 문의 생성 | - | 유효한 요청 + 첨부파일 2개 | 201 Created, 문의 + 첨부파일 2개 저장 | ✅ |
| INQ-G-004 | 첨부파일 3개(최대) 포함 문의 생성 | - | 유효한 요청 + 첨부파일 3개 | 201 Created, 문의 + 첨부파일 3개 저장 (INQ-INV-02 경계값) | ⬜ |
| INQ-G-005 | 첨부파일 없이 문의 생성 | - | 유효한 요청, attachments=[] | 201 Created, 첨부파일 0개 | ⬜ |
| INQ-G-006 | 각 문의 유형별 생성 (5종) | - | type을 JOIN, EVENT, REPORT, ACCOUNT, OTHER로 각각 생성 | 각 유형으로 정상 생성 | ⬜ |
| INQ-G-007 | 비밀번호 BCrypt 해싱 저장 확인 | - | password="plaintext"로 문의 생성 후 DB 확인 | passwordHash가 BCrypt 형식($2a$...)으로 저장, 원문과 다름 (INQ-INV-05) | ⬜ |

### 2.2 비회원 문의 생성 - 입력 검증 실패

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-G-010 | 문의 유형 누락 | - | type=null로 요청 | 400 Bad Request, @NotNull 위반 | ⬜ |
| INQ-G-011 | 제목 누락 | - | title=null 또는 빈 문자열로 요청 | 400 Bad Request, @NotBlank 위반 | ⬜ |
| INQ-G-012 | 제목 100자 (최대, 경계값) | - | title=100자 문자열로 요청 | 201 Created, 정상 생성 | ⬜ |
| INQ-G-013 | 제목 101자 초과 (경계값) | - | title=101자 문자열로 요청 | 400 Bad Request, @Size(max=100) 위반 | ⬜ |
| INQ-G-014 | 내용 누락 | - | content=null 또는 빈 문자열로 요청 | 400 Bad Request, @NotBlank 위반 | ⬜ |
| INQ-G-015 | 이메일 누락 | - | email=null 또는 빈 문자열로 요청 | 400 Bad Request, @NotBlank 위반 | ⬜ |
| INQ-G-016 | 잘못된 이메일 형식 | - | email="invalid-email"로 요청 | 400 Bad Request, @Email 위반 | ⬜ |
| INQ-G-017 | 이름 누락 | - | name=null 또는 빈 문자열로 요청 | 400 Bad Request, @NotBlank 위반 | ⬜ |
| INQ-G-018 | 이름 50자 (최대, 경계값) | - | name=50자 문자열로 요청 | 201 Created, 정상 생성 | ⬜ |
| INQ-G-019 | 이름 51자 초과 (경계값) | - | name=51자 문자열로 요청 | 400 Bad Request, @Size(max=50) 위반 | ⬜ |
| INQ-G-020 | 비밀번호 누락 | - | password=null 또는 빈 문자열로 요청 | 400 Bad Request, @NotBlank 위반 (INQ-INV-05) | ⬜ |
| INQ-G-021 | 첨부파일 4개 초과 (경계값) | - | attachments에 4개 첨부파일 포함 | 400 Bad Request, @Size(max=3) 위반 (INQ-INV-02) | ⬜ |
| INQ-G-022 | 첨부파일 URL 형식 불일치 | - | fileUrl="not-a-url"로 요청 | 400 Bad Request, @Pattern 위반 | ⬜ |
| INQ-G-023 | 첨부파일 크기 0 | - | fileSize=0으로 요청 | 400 Bad Request, @Positive 위반 | ⬜ |
| INQ-G-024 | 첨부파일 크기 음수 | - | fileSize=-1로 요청 | 400 Bad Request, @Positive 위반 | ⬜ |
| INQ-G-025 | 첨부파일 파일명 256자 초과 | - | fileName=256자 문자열로 요청 | 400 Bad Request, @Size(max=255) 위반 | ⬜ |

### 2.3 문의번호 충돌 재시도 (GAP-INQ-03)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-G-030 | 문의번호 1회 충돌 후 재시도 성공 | InquiryPersistenceExecutor가 1회 DataIntegrityViolationException 발생하도록 설정 | 문의 생성 요청 | 재시도 후 성공, 다른 문의번호 발급 (INQ-INV-01) | ⬜ |
| INQ-G-031 | 문의번호 2회 충돌 후 재시도 성공 | 2회 DataIntegrityViolationException 발생 | 문의 생성 요청 | 3번째 시도에서 성공 (INQ-INV-01) | ⬜ |
| INQ-G-032 | 문의번호 3회 충돌 - 최종 실패 | 3회 모두 DataIntegrityViolationException 발생 | 문의 생성 요청 | InquiryNumberGenerationException 발생, 500 Internal Server Error | ⬜ |

### 2.4 비회원 문의 조회 - 성공

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-G-040 | 올바른 정보로 비회원 문의 조회 | 비회원 문의 1건 생성됨 (inquiryNumber, email, password) | 문의번호+이메일+비밀번호로 조회 | 200 OK, 문의 상세 정보 반환 (제목, 내용, 상태, 첨부파일 등) | ✅ |
| INQ-G-041 | 첨부파일 포함 문의 조회 | 첨부파일 2개 포함된 비회원 문의 | 조회 요청 | 200 OK, attachments에 2개 첨부파일 정보 포함 | ⬜ |
| INQ-G-042 | 답변 포함 문의 조회 | 답변이 작성된 비회원 문의 | 조회 요청 | 200 OK, reply에 답변 내용 + 답변자 정보 포함 | ⬜ |
| INQ-G-043 | 내부 메모 미노출 확인 | 내부 메모가 작성된 비회원 문의 | 조회 요청 | 200 OK, 응답에 memos 필드 없음 (비회원에게 미노출) | ⬜ |

### 2.5 비회원 문의 조회 - 실패

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-G-050 | 존재하지 않는 문의번호로 조회 | - | inquiryNumber="INQ-99999999-00000"으로 조회 | 404 Not Found, InquiryNotFoundException | ✅ |
| INQ-G-051 | 이메일 불일치로 조회 (GAP-INQ-02) | 비회원 문의 (email=a@test.com) | 동일 문의번호 + email=b@test.com으로 조회 | 404 Not Found, InquiryNotFoundException (존재 여부 미노출) | ⬜ |
| INQ-G-052 | 비밀번호 불일치로 조회 | 비회원 문의 (password=correct) | 동일 문의번호 + 이메일 + password=wrong으로 조회 | 401 Unauthorized, InquiryInvalidPasswordException | ✅ |
| INQ-G-053 | 문의번호 누락으로 조회 | - | inquiryNumber=null로 조회 | 400 Bad Request, @NotBlank 위반 | ⬜ |
| INQ-G-054 | 이메일 누락으로 조회 | - | email=null로 조회 | 400 Bad Request, @NotBlank 위반 | ⬜ |
| INQ-G-055 | 비밀번호 누락으로 조회 | - | password=null로 조회 | 400 Bad Request, @NotBlank 위반 | ⬜ |
| INQ-G-056 | soft delete된 문의 조회 | soft delete된 비회원 문의 | 올바른 정보로 조회 | 404 Not Found (INQ-INV-04, @SQLRestriction 필터링) | ⬜ |
| INQ-G-057 | 회원 문의를 비회원 조회로 시도 | 회원 문의 1건 존재 | 회원 문의의 문의번호로 비회원 조회 | 404 Not Found (GuestInquiry 타입 불일치) | ⬜ |

### 2.6 이메일 알림 (GAP-INQ-06)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-G-060 | 문의 생성 시 확인 이메일 발송 | test 프로파일 (LoggingInquiryNotificationService) | 비회원 문의 생성 | 로그에 문의 접수 확인 이메일 발송 기록 | ⬜ |
| INQ-G-061 | 이메일 발송 실패 시 문의 생성 트랜잭션 영향 | SmtpInquiryNotificationService에서 MailException 발생하도록 설정 | 비회원 문의 생성 | 문의 생성은 성공/실패? (현재 정책 확인 필요) | ⬜ |
| INQ-G-062 | 이메일 수신자 확인 | test 프로파일 | email="recipient@test.com"으로 문의 생성 | 로그에 수신자 "recipient@test.com" 기록 | ⬜ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| INQ-INV-01 | 문의번호 유일성 보장 | INQ-G-001, INQ-G-030~032 |
| INQ-INV-02 | 첨부파일 최대 3개 제한 | INQ-G-004~005, INQ-G-021 |
| INQ-INV-04 | Soft delete 문의 조회 제외 | INQ-G-056 |
| INQ-INV-05 | 비회원 비밀번호 해시 필수 | INQ-G-007, INQ-G-020 |
| SEC-INQ-02 | 이메일 불일치 시 존재 여부 미노출 | INQ-G-051 |
| SEC-INQ-03 | 비밀번호 불일치 시 인증 실패 | INQ-G-052 |

---

## 4. 구현된 테스트 클래스

### 4.1 서비스 통합 테스트 - 생성
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/create/CreateGuestInquiryServiceTest.java`
- **테스트 범위**: INQ-G-001~003 (비회원 문의 생성 성공, 첨부파일 포함)
- **테스트 수**: 2개

### 4.2 서비스 통합 테스트 - 조회
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/read/LookupGuestInquiryServiceTest.java`
- **테스트 범위**: INQ-G-040, INQ-G-050, INQ-G-052 (정상 조회, 문의번호 불일치, 비밀번호 불일치)
- **테스트 수**: 3개

### 4.3 미구현 테스트
- 입력 검증 실패 (INQ-G-010~025): 컨트롤러 레벨 테스트 필요
- 문의번호 충돌 재시도 (INQ-G-030~032): Mock 기반 통합 테스트 필요
- 이메일 불일치 조회 (INQ-G-051): 서비스 통합 테스트 필요
- 이메일 알림 (INQ-G-060~062): 알림 서비스 검증 테스트 필요

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-11 | - | 검증 기준서 기반 최초 작성 |
