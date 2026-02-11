# 회원 문의 (Member Inquiry) 테스트 케이스

**작성일**: 2026-02-11
**버전**: 1.0
**관련 스펙**: [inquiry-verification-criteria.md](../../../../docs/criteria/inquiry-verification-criteria.md)
**우선순위**: P1

---

## 1. 개요

회원 문의 기능에 대한 테스트 케이스이다. 인증된 사용자(ASSOCIATE 이상)가 JWT 토큰으로 문의를 작성하고, 본인의 문의 목록/상세를 조회할 수 있다. 작성자 이메일과 이름은 User 엔티티에서 자동으로 가져온다.

**대상 엔드포인트**:
- `POST /api/v1/inquiries/member` - 회원 문의 생성 (인증 필수)
- `GET /api/v1/inquiries/my` - 내 문의 목록 조회 (인증 필수)
- `GET /api/v1/inquiries/my/{id}` - 내 문의 상세 조회 (인증 필수, 소유권 검증)

**대상 서비스**:
- `CreateMemberInquiryService`
- `GetMyInquiriesService`
- `GetMyInquiryService`

---

## 2. 테스트 케이스

### 2.1 회원 문의 생성 - 성공

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-M-001 | 유효한 정보로 회원 문의 생성 | ASSOCIATE 사용자 존재 | type=JOIN, title="가입 문의", content="내용" 전송 (JWT 인증) | 201 Created, 문의번호 발급, 상태=PENDING, user FK 연결 | ✅ |
| INQ-M-002 | 첨부파일 포함 회원 문의 생성 | ASSOCIATE 사용자 존재 | 유효한 요청 + 첨부파일 2개 (JWT 인증) | 201 Created, 문의 + 첨부파일 2개 저장 | ⬜ |
| INQ-M-003 | 첨부파일 3개(최대) 포함 생성 | 사용자 존재 | 유효한 요청 + 첨부파일 3개 | 201 Created (INQ-INV-02 경계값) | ⬜ |
| INQ-M-004 | 첨부파일 없이 생성 | 사용자 존재 | 유효한 요청, attachments=[] | 201 Created, 첨부파일 0개 | ⬜ |
| INQ-M-005 | 각 문의 유형별 생성 (5종) | 사용자 존재 | type을 JOIN, EVENT, REPORT, ACCOUNT, OTHER로 각각 생성 | 각 유형으로 정상 생성 | ⬜ |
| INQ-M-006 | 작성자 정보 자동 설정 확인 | User(email="user@inha.edu", name="김철수") | 문의 생성 | getAuthorEmail()="user@inha.edu", getAuthorName()="김철수" 확인 (INQ-INV-06) | ⬜ |

### 2.2 회원 문의 생성 - 실패

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-M-010 | 존재하지 않는 사용자 ID로 생성 | userId=99999 (DB에 없음) | 문의 생성 요청 | UserNotFoundException (INQ-INV-06) | ✅ |
| INQ-M-011 | 문의 유형 누락 | 사용자 존재 | type=null로 요청 | 400 Bad Request, @NotNull 위반 | ⬜ |
| INQ-M-012 | 제목 누락 | 사용자 존재 | title=null 또는 빈 문자열 | 400 Bad Request, @NotBlank 위반 | ⬜ |
| INQ-M-013 | 제목 101자 초과 (경계값) | 사용자 존재 | title=101자 문자열 | 400 Bad Request, @Size(max=100) 위반 | ⬜ |
| INQ-M-014 | 내용 누락 | 사용자 존재 | content=null 또는 빈 문자열 | 400 Bad Request, @NotBlank 위반 | ⬜ |
| INQ-M-015 | 첨부파일 4개 초과 (경계값) | 사용자 존재 | attachments에 4개 포함 | 400 Bad Request, @Size(max=3) 위반 (INQ-INV-02) | ⬜ |
| INQ-M-016 | 비인증 상태에서 생성 시도 | JWT 토큰 없음 | 문의 생성 요청 | 401 Unauthorized | ⬜ |

### 2.3 문의번호 충돌 재시도 (GAP-INQ-03)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-M-020 | 문의번호 1회 충돌 후 재시도 성공 | 1회 DataIntegrityViolationException 발생 설정 | 문의 생성 요청 | 재시도 후 성공, 다른 문의번호 발급 (INQ-INV-01) | ⬜ |
| INQ-M-021 | 문의번호 2회 충돌 후 재시도 성공 | 2회 DataIntegrityViolationException 발생 | 문의 생성 요청 | 3번째 시도에서 성공 (INQ-INV-01) | ⬜ |
| INQ-M-022 | 문의번호 3회 충돌 - 최종 실패 | 3회 모두 실패 | 문의 생성 요청 | InquiryNumberGenerationException, 500 Error | ⬜ |

### 2.4 내 문의 목록 조회

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-M-030 | 내 문의 목록 페이징 조회 | 사용자A의 문의 3건 존재 | GET /my?page=0&size=20 (사용자A JWT) | 200 OK, 3건 반환, totalElements=3 | ✅ |
| INQ-M-031 | 문의 없는 회원의 빈 목록 조회 | 사용자B의 문의 0건 | GET /my?page=0&size=20 (사용자B JWT) | 200 OK, 빈 리스트, totalElements=0 | ⬜ |
| INQ-M-032 | 다른 사용자의 문의는 미포함 | 사용자A 문의 2건, 사용자B 문의 1건 | GET /my (사용자A JWT) | 200 OK, 사용자A의 2건만 반환 | ⬜ |
| INQ-M-033 | 페이징 경계값 확인 | 사용자A의 문의 25건 | GET /my?page=0&size=20 | 200 OK, 20건 반환, totalPages=2, hasNext=true | ⬜ |

### 2.5 내 문의 상세 조회 - 성공

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-M-040 | 본인 문의 상세 조회 | 사용자A의 문의 1건 (id=1) | GET /my/1 (사용자A JWT) | 200 OK, 문의 상세 정보 (제목, 내용, 상태, 유형) | ✅ |
| INQ-M-041 | 첨부파일 포함 문의 상세 조회 | 첨부파일 2개 포함된 사용자A 문의 | GET /my/{id} (사용자A JWT) | 200 OK, attachments에 2개 첨부파일 정보 | ⬜ |
| INQ-M-042 | 답변 포함 문의 상세 조회 | 답변 작성된 사용자A 문의 | GET /my/{id} (사용자A JWT) | 200 OK, reply에 답변 내용 + 답변자 정보 | ⬜ |
| INQ-M-043 | 내부 메모 미노출 확인 | 내부 메모 작성된 사용자A 문의 | GET /my/{id} (사용자A JWT) | 200 OK, 응답에 memos 필드 없음 (회원에게 미노출) | ⬜ |

### 2.6 내 문의 상세 조회 - 실패

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-M-050 | 다른 사용자의 문의 상세 조회 | 사용자A 문의 (id=1) 존재 | GET /my/1 (사용자B JWT) | 404 Not Found, InquiryAccessDeniedException (SEC-INQ-01) | ✅ |
| INQ-M-051 | 존재하지 않는 문의 ID 조회 | - | GET /my/99999 (사용자A JWT) | 404 Not Found, InquiryNotFoundException | ⬜ |
| INQ-M-052 | soft delete된 문의 조회 | 사용자A의 삭제된 문의 | GET /my/{deletedId} (사용자A JWT) | 404 Not Found (INQ-INV-04) | ⬜ |
| INQ-M-053 | 비인증 상태에서 상세 조회 | JWT 토큰 없음 | GET /my/1 | 401 Unauthorized | ⬜ |

### 2.7 이메일 알림 (GAP-INQ-06)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-M-060 | 문의 생성 시 확인 이메일 발송 | test 프로파일 (LoggingInquiryNotificationService) | 회원 문의 생성 | 로그에 문의 접수 확인 이메일 발송 기록 | ⬜ |
| INQ-M-061 | 이메일 수신자는 User의 이메일 | User(email="member@inha.edu") | 회원 문의 생성 | 로그에 수신자 "member@inha.edu" 기록 (request에서가 아닌 User에서 가져옴) | ⬜ |
| INQ-M-062 | 이메일 발송 실패 시 트랜잭션 영향 | SmtpInquiryNotificationService에서 MailException 발생 | 회원 문의 생성 | 문의 생성은 성공/실패? (현재 정책 확인 필요) | ⬜ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| INQ-INV-01 | 문의번호 유일성 보장 | INQ-M-001, INQ-M-020~022 |
| INQ-INV-02 | 첨부파일 최대 3개 제한 | INQ-M-003~004, INQ-M-015 |
| INQ-INV-04 | Soft delete 문의 조회 제외 | INQ-M-052 |
| INQ-INV-06 | 회원 사용자 참조 필수 | INQ-M-006, INQ-M-010 |
| SEC-INQ-01 | 다른 사용자의 문의 접근 거부 | INQ-M-050 |

---

## 4. 구현된 테스트 클래스

### 4.1 서비스 통합 테스트 - 생성
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/create/CreateMemberInquiryServiceTest.java`
- **테스트 범위**: INQ-M-001, INQ-M-010 (생성 성공, 존재하지 않는 사용자)
- **테스트 수**: 2개

### 4.2 서비스 통합 테스트 - 목록 조회
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/read/GetMyInquiriesServiceTest.java`
- **테스트 범위**: INQ-M-030 (목록 조회 성공)
- **테스트 수**: 1개

### 4.3 서비스 통합 테스트 - 상세 조회
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/read/GetMyInquiryServiceTest.java`
- **테스트 범위**: INQ-M-040, INQ-M-050 (상세 조회 성공, 접근 거부)
- **테스트 수**: 2개

### 4.4 미구현 테스트
- 입력 검증 실패 (INQ-M-011~016): 컨트롤러 레벨 테스트 필요
- 문의번호 충돌 재시도 (INQ-M-020~022): Mock 기반 통합 테스트 필요
- 페이징/필터링 상세 (INQ-M-031~033): 서비스 통합 테스트 필요
- 내부 메모 미노출 (INQ-M-043): 응답 구조 검증 테스트 필요
- 이메일 알림 (INQ-M-060~062): 알림 서비스 검증 테스트 필요

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-11 | - | 검증 기준서 기반 최초 작성 |
