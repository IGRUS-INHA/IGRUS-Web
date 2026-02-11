# 관리자 문의 관리 (Admin Inquiry) 테스트 케이스

**작성일**: 2026-02-11
**버전**: 1.1
**관련 스펙**: [inquiry-verification-criteria.md](../../../../docs/criteria/inquiry-verification-criteria.md)
**우선순위**: P1

---

## 1. 개요

관리자(OPERATOR/ADMIN) 전용 문의 관리 기능에 대한 테스트 케이스이다. 전체 문의 목록/상세 조회, 상태 변경, 답변 작성/수정, 내부 메모 작성, 문의 삭제(soft delete)를 포함한다.

**대상 엔드포인트**:
- `GET /api/v1/inquiries` - 전체 문의 목록 조회 (OPERATOR/ADMIN)
- `GET /api/v1/inquiries/{id}` - 문의 상세 조회 (OPERATOR/ADMIN)
- `PUT /api/v1/inquiries/{id}/status` - 문의 상태 변경 (OPERATOR/ADMIN)
- `POST /api/v1/inquiries/{id}/reply` - 답변 작성 (OPERATOR/ADMIN)
- `PUT /api/v1/inquiries/{id}/reply` - 답변 수정 (OPERATOR/ADMIN)
- `POST /api/v1/inquiries/{id}/memo` - 내부 메모 작성 (OPERATOR/ADMIN)
- `DELETE /api/v1/inquiries/{id}` - 문의 삭제 (OPERATOR/ADMIN)

**대상 서비스**:
- `GetAllInquiriesService`, `GetInquiryDetailService`
- `UpdateInquiryStatusService`
- `CreateInquiryReplyService`, `UpdateInquiryReplyService`
- `CreateInquiryMemoService`
- `DeleteInquiryService`

---

## 2. 테스트 케이스

### 2.1 전체 문의 목록 조회

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-A-001 | 전체 문의 목록 조회 | 문의 5건 존재 (Guest 2, Member 3) | GET /inquiries?page=0&size=20 | 200 OK, 5건 반환, totalElements=5 | ✅ |
| INQ-A-002 | 문의 유형별 필터 조회 | JOIN 2건, EVENT 1건, REPORT 2건 | GET /inquiries?type=JOIN | 200 OK, JOIN 문의 2건만 반환 | ✅ |
| INQ-A-003 | 문의 상태별 필터 조회 | PENDING 3건, IN_PROGRESS 1건, COMPLETED 1건 | GET /inquiries?status=PENDING | 200 OK, PENDING 문의 3건만 반환 | ✅ |
| INQ-A-004 | 유형+상태 복합 필터 조회 | JOIN/PENDING 1건, JOIN/COMPLETED 1건 | GET /inquiries?type=JOIN&status=PENDING | 200 OK, 1건만 반환 | ✅ |
| INQ-A-005 | 빈 목록 조회 | 문의 0건 | GET /inquiries | 200 OK, 빈 리스트, totalElements=0 | ✅ |
| INQ-A-006 | 페이징 경계값 확인 | 문의 25건 | GET /inquiries?page=0&size=20 | 200 OK, 20건 반환, totalPages=2, hasNext=true | ⬜ |

### 2.2 문의 상세 조회 (관리자)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-A-010 | 비회원 문의 상세 조회 | GuestInquiry 1건 (첨부파일 포함) | GET /inquiries/{id} | 200 OK, 문의 정보 + authorName + authorEmail + isGuest=true + attachments | ✅ |
| INQ-A-011 | 회원 문의 상세 조회 | MemberInquiry 1건 | GET /inquiries/{id} | 200 OK, 문의 정보 + authorName(User) + authorUserId + isGuest=false | ✅ |
| INQ-A-012 | 답변 포함 문의 상세 조회 | 답변 작성된 문의 | GET /inquiries/{id} | 200 OK, reply 필드에 답변 내용 + repliedByName | ⬜ |
| INQ-A-013 | 내부 메모 포함 문의 상세 조회 | 메모 2개 작성된 문의 | GET /inquiries/{id} | 200 OK, memos 필드에 2개 메모 (최신순 정렬) | ⬜ |
| INQ-A-014 | 존재하지 않는 문의 상세 조회 | - | GET /inquiries/99999 | 404 Not Found, InquiryNotFoundException | ✅ |

### 2.3 문의 상태 변경 (GAP-INQ-01, GAP-INQ-05)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-A-020 | PENDING → IN_PROGRESS 상태 변경 | PENDING 문의 | PUT /inquiries/{id}/status, body: {status: "IN_PROGRESS"} | 200 OK, 상태=IN_PROGRESS | ✅ |
| INQ-A-021 | PENDING → COMPLETED 상태 변경 | PENDING 문의 | PUT, body: {status: "COMPLETED"} | 200 OK, 상태=COMPLETED | ✅ |
| INQ-A-022 | IN_PROGRESS → PENDING 되돌리기 (GAP-INQ-05) | IN_PROGRESS 문의 | PUT, body: {status: "PENDING"} | 200 OK, 상태=PENDING | ✅ |
| INQ-A-023 | IN_PROGRESS → COMPLETED 상태 변경 (GAP-INQ-05) | IN_PROGRESS 문의 | PUT, body: {status: "COMPLETED"} | 200 OK, 상태=COMPLETED | ✅ |
| INQ-A-024 | PENDING → PENDING 동일 상태 (멱등성) | PENDING 문의 | PUT, body: {status: "PENDING"} | 200 OK, 상태 변경 없음 (멱등) | ✅ |
| INQ-A-025 | COMPLETED → PENDING 금지 전이 (GAP-INQ-01) | COMPLETED 문의 | PUT, body: {status: "PENDING"} | 400 Bad Request, InvalidStatusTransitionException (INQ-INV-07) | ✅ |
| INQ-A-026 | COMPLETED → IN_PROGRESS 금지 전이 (GAP-INQ-01) | COMPLETED 문의 | PUT, body: {status: "IN_PROGRESS"} | 400 Bad Request, InvalidStatusTransitionException (INQ-INV-07) | ✅ |
| INQ-A-027 | COMPLETED → COMPLETED 동일 상태 (멱등성) | COMPLETED 문의 | PUT, body: {status: "COMPLETED"} | 200 OK, 상태 변경 없음 (멱등) | ✅ |
| INQ-A-028 | IN_PROGRESS → IN_PROGRESS 동일 상태 (멱등성) | IN_PROGRESS 문의 | PUT, body: {status: "IN_PROGRESS"} | 200 OK, 상태 변경 없음 (멱등) | ⬜ |
| INQ-A-029 | 존재하지 않는 문의 상태 변경 | - | PUT /inquiries/99999/status | 404 Not Found, InquiryNotFoundException | ✅ |
| INQ-A-030 | 상태 값 누락 | PENDING 문의 | PUT, body: {status: null} | 400 Bad Request, @NotNull 위반 | ⬜ |

### 2.4 답변 작성

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-A-040 | 답변 작성 성공 | 답변 없는 PENDING 문의, OPERATOR 사용자 | POST /inquiries/{id}/reply, body: {content: "답변 내용"} | 201 Created, 답변 저장, 문의 상태 자동 COMPLETED (INQ-INV-08) | ✅ |
| INQ-A-041 | 답변 작성 시 상태 자동 COMPLETED 전이 | IN_PROGRESS 문의 | 답변 작성 | 문의 상태=COMPLETED 확인 (INQ-INV-08) | ✅ |
| INQ-A-042 | 답변 작성 시 답변자 정보 기록 | OPERATOR(id=10) 사용자 | 답변 작성 | reply.repliedBy.id=10, reply.createdBy=10 | ⬜ |
| INQ-A-043 | 답변 작성 시 이메일 알림 발송 | test 프로파일, GuestInquiry(email="guest@test.com") | 답변 작성 | 로그에 답변 알림 이메일 발송 기록 (수신자: guest@test.com) | ⬜ |
| INQ-A-044 | 이미 답변된 문의에 재답변 시도 | 답변 존재하는 문의 | POST /inquiries/{id}/reply | 409 Conflict, InquiryAlreadyRepliedException (INQ-INV-03) | ✅ |
| INQ-A-045 | 존재하지 않는 문의에 답변 작성 | - | POST /inquiries/99999/reply | 404 Not Found, InquiryNotFoundException | ✅ |
| INQ-A-046 | 답변 내용 누락 | 답변 없는 문의 | POST, body: {content: null} | 400 Bad Request, @NotBlank 위반 | ⬜ |
| INQ-A-047 | 답변 알림 이메일 실패 시 트랜잭션 영향 (GAP-INQ-06) | SmtpInquiryNotificationService에서 MailException 발생 | 답변 작성 | 답변 저장은 성공/실패? (현재 정책 확인 필요) | ⬜ |
| INQ-A-048 | 답변 알림 이메일 수신자 (다형성) | GuestInquiry: request.email, MemberInquiry: user.email | 각각 답변 작성 | getAuthorEmail() 다형성 호출로 정확한 수신자에게 발송 | ⬜ |

### 2.5 답변 수정

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-A-050 | 답변 수정 성공 | 답변 존재하는 문의 | PUT /inquiries/{id}/reply, body: {content: "수정된 답변"} | 200 OK, 답변 내용 업데이트됨 | ✅ |
| INQ-A-051 | 답변 없는 문의에 수정 시도 | 답변 없는 문의 | PUT /inquiries/{id}/reply | 404 Not Found, InquiryReplyNotFoundException | ✅ |
| INQ-A-052 | 답변 수정 후 상태 변경 없음 확인 | COMPLETED 문의의 답변 | 답변 수정 | 문의 상태 COMPLETED 유지 (부작용 없음) | ✅ |
| INQ-A-053 | 답변 수정 시 이메일 알림 미발송 확인 | 답변 존재하는 문의 | 답변 수정 | 답변 수정 시 이메일 발송하지 않음 (생성 시에만 발송) | ⬜ |

### 2.6 내부 메모 작성

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-A-060 | 메모 작성 성공 | 문의 1건 존재, OPERATOR 사용자 | POST /inquiries/{id}/memo, body: {content: "내부 메모"} | 201 Created, 메모 저장, writtenBy=operator | ✅ |
| INQ-A-061 | 동일 문의에 여러 메모 작성 | 문의 1건 + 메모 1건 존재 | 추가 메모 작성 | 201 Created, 메모 2건으로 증가 (제한 없음) | ✅ |
| INQ-A-062 | 존재하지 않는 문의에 메모 작성 | - | POST /inquiries/99999/memo | 404 Not Found, InquiryNotFoundException | ✅ |
| INQ-A-063 | 메모 내용 누락 | 문의 존재 | POST, body: {content: null} | 400 Bad Request, @NotBlank 위반 | ⬜ |

### 2.7 문의 삭제 (Soft Delete)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-A-070 | 문의 soft delete 성공 | 문의 1건 존재, OPERATOR(id=10) | DELETE /inquiries/{id} | 204 No Content, inquiries_deleted=true | ✅ |
| INQ-A-071 | 삭제 감사 이력 확인 | 문의 1건 존재 | soft delete 실행 후 DB 확인 | deletedAt 기록, deletedBy=operatorId | ⬜ |
| INQ-A-072 | 삭제 후 일반 조회에서 제외 | soft delete된 문의 | GET /inquiries (목록 조회) | 삭제된 문의 미포함 (INQ-INV-04) | ✅ |
| INQ-A-073 | 삭제된 문의 재삭제 시도 | soft delete된 문의 | DELETE /inquiries/{deletedId} | 404 Not Found (@SQLRestriction 필터링) | ✅ |
| INQ-A-074 | 삭제된 문의도 문의번호 카운트에 포함 | soft delete된 문의 1건 | 새 문의 생성 시 번호 생성 | 순번이 삭제된 문의를 포함하여 증가 (INQ-INV-01) | ⬜ |
| INQ-A-075 | 존재하지 않는 문의 삭제 | - | DELETE /inquiries/99999 | 404 Not Found, InquiryNotFoundException | ✅ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| INQ-INV-01 | 문의번호 유일성 (삭제 포함 카운트) | INQ-A-074 |
| INQ-INV-03 | 답변 최대 1건 | INQ-A-044 |
| INQ-INV-04 | Soft delete 문의 조회 제외 | INQ-A-072~073 |
| INQ-INV-07 | COMPLETED 종단 상태 | INQ-A-025~028 |
| INQ-INV-08 | 답변 시 자동 완료 | INQ-A-040~041 |
| GAP-INQ-01 | COMPLETED 상태 전이 테스트 | INQ-A-025~027 |
| GAP-INQ-05 | IN_PROGRESS 상태 전이 테스트 | INQ-A-022~023 |
| GAP-INQ-06 | 이메일 알림 실패 영향 | INQ-A-047 |

---

## 4. 구현된 테스트 클래스

### 4.1 서비스 통합 테스트 - 목록 조회
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/read/GetAllInquiriesServiceTest.java`
- **테스트 범위**: INQ-A-001~005 (전체 목록, 유형 필터, 상태 필터, 복합 필터, 빈 목록)
- **테스트 수**: 5개

### 4.2 서비스 통합 테스트 - 상세 조회
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/read/GetInquiryDetailServiceTest.java`
- **테스트 범위**: INQ-A-010~011, INQ-A-014 (비회원/회원 상세 조회, 존재하지 않는 문의)
- **테스트 수**: 3개

### 4.3 서비스 통합 테스트 - 상태 변경
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/manage/UpdateInquiryStatusServiceTest.java`
- **테스트 범위**: INQ-A-020~027, INQ-A-029 (전체 FSM 전이, 멱등성, 금지 전이, 존재하지 않는 문의)
- **테스트 수**: 9개

### 4.4 서비스 통합 테스트 - 답변 작성
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/manage/CreateInquiryReplyServiceTest.java`
- **테스트 범위**: INQ-A-040~041, INQ-A-044~045 (답변 작성 성공, 자동 COMPLETED, 중복 답변, 존재하지 않는 문의)
- **테스트 수**: 4개

### 4.5 서비스 통합 테스트 - 답변 수정
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/manage/UpdateInquiryReplyServiceTest.java`
- **테스트 범위**: INQ-A-050~052 (답변 수정 성공, 답변 없는 문의, 존재하지 않는 문의)
- **테스트 수**: 3개

### 4.6 서비스 통합 테스트 - 메모
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/manage/CreateInquiryMemoServiceTest.java`
- **테스트 범위**: INQ-A-060~062 (메모 작성 성공, 여러 메모, 존재하지 않는 문의)
- **테스트 수**: 3개

### 4.7 서비스 통합 테스트 - 삭제
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/manage/DeleteInquiryServiceTest.java`
- **테스트 범위**: INQ-A-070, INQ-A-072~073, INQ-A-075 (soft delete 성공, 목록 제외, 재삭제, 존재하지 않는 문의)
- **테스트 수**: 4개

### 4.8 미구현 테스트
- 입력 검증 (INQ-A-030, 046, 063): 컨트롤러 레벨 @Valid 검증 테스트 필요
- 이메일 알림 (INQ-A-043, 047~048, 053): 알림 서비스 검증 테스트 필요
- 삭제 감사 이력 (INQ-A-071): DB 레벨 검증 필요
- 답변자 정보 기록 (INQ-A-042): 서비스 통합 테스트 필요
- 페이징 경계값 (INQ-A-006): 대량 데이터 테스트 필요
- IN_PROGRESS 멱등성 (INQ-A-028): 서비스 통합 테스트 필요

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-11 | - | 검증 기준서 기반 최초 작성 |
| 1.1 | 2026-02-11 | - | INQ-A-003~005, 010~011, 014, 021~027, 029, 041, 045, 050~052, 061~062, 072~073, 075 테스트 구현 완료 |
