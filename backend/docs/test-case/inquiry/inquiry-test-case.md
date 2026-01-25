# Inquiry(문의) 도메인 테스트 케이스

## 개요

PRD V2 기반 문의 기능의 테스트 케이스를 정의합니다.

## 테스트 범위

### 1. 도메인 단위 테스트

#### 1.1 Inquiry 엔티티

| ID | 테스트 케이스 | 입력 | 기대 결과 | 카테고리 |
|----|--------------|------|----------|---------|
| INQ-D-001 | 비회원 문의 생성 성공 | 유효한 정보 | Inquiry 객체 생성, 상태=PENDING | 정상 |
| INQ-D-002 | 회원 문의 생성 성공 | 유효한 정보 + User | Inquiry 객체 생성, user 연결 | 정상 |
| INQ-D-003 | 비회원 문의 식별 | - | isGuestInquiry=true, isMemberInquiry=false | 정상 |
| INQ-D-004 | 회원 문의 식별 | - | isGuestInquiry=false, isMemberInquiry=true | 정상 |
| INQ-D-005 | 상태 변경 (changeStatus) | IN_PROGRESS | 상태 변경됨 | 정상 |
| INQ-D-006 | 처리 시작 (startProcessing) | - | 상태=IN_PROGRESS | 정상 |
| INQ-D-007 | 완료 (complete) | - | 상태=COMPLETED | 정상 |
| INQ-D-008 | 첨부파일 추가 (1~3개) | 유효한 첨부파일 | 정상 추가 | 정상 |
| INQ-D-009 | 첨부파일 추가 (4개 이상) | 4번째 첨부파일 | InquiryMaxAttachmentsExceededException | 예외 |
| INQ-D-010 | 첨부파일 목록 불변성 | getAttachments().add() | UnsupportedOperationException | 예외 |
| INQ-D-011 | 답변 없음 확인 | - | hasReply=false | 정상 |
| INQ-D-012 | 답변 설정 | InquiryReply | hasReply=true, reply 연결 | 정상 |
| INQ-D-013 | 메모 추가 | InquiryMemo | memos 리스트에 추가 | 정상 |
| INQ-D-014 | 소유권 확인 (본인) | 동일 userId | isOwnedByUser=true | 정상 |
| INQ-D-015 | 소유권 확인 (타인) | 다른 userId | isOwnedByUser=false | 정상 |
| INQ-D-016 | 소유권 확인 (비회원) | 어떤 userId | isOwnedByUser=false | 정상 |

#### 1.2 InquiryAttachment 엔티티

| ID | 테스트 케이스 | 입력 | 기대 결과 | 카테고리 |
|----|--------------|------|----------|---------|
| INQ-A-001 | 첨부파일 생성 성공 | URL, 파일명, 크기 | 객체 생성 | 정상 |

#### 1.3 InquiryReply 엔티티

| ID | 테스트 케이스 | 입력 | 기대 결과 | 카테고리 |
|----|--------------|------|----------|---------|
| INQ-R-001 | 답변 생성 성공 | 내용, 운영자 | 객체 생성 | 정상 |
| INQ-R-002 | 답변 수정 | 새 내용 | 내용 변경됨 | 정상 |

#### 1.4 InquiryMemo 엔티티

| ID | 테스트 케이스 | 입력 | 기대 결과 | 카테고리 |
|----|--------------|------|----------|---------|
| INQ-M-001 | 메모 생성 성공 | 내용, 운영자 | 객체 생성 | 정상 |

### 2. 서비스 단위/통합 테스트

#### 2.1 InquiryNumberGenerator

| ID | 테스트 케이스 | 입력 | 기대 결과 | 카테고리 |
|----|--------------|------|----------|---------|
| ING-001 | 첫 번째 문의 번호 생성 | count=0 | INQ-YYYYMMDD00001 | 정상 |
| ING-002 | 10번째 문의 번호 생성 | count=9 | INQ-YYYYMMDD00010 | 정상 |
| ING-003 | 100번째 문의 번호 생성 | count=99 | INQ-YYYYMMDD00100 | 정상 |
| ING-004 | INQ- 접두사 확인 | - | INQ-로 시작 | 정상 |
| ING-005 | 문의 번호 길이 | - | 17자 | 정상 |

#### 2.2 InquiryService

**비회원 문의 생성**

| ID | 테스트 케이스 | 입력 | 기대 결과 | 카테고리 |
|----|--------------|------|----------|---------|
| IS-001 | 비회원 문의 생성 성공 | 유효한 요청 | 문의 생성, 번호 발급 | 정상 |
| IS-002 | 첨부파일 포함 생성 | 유효한 요청 + 첨부파일 2개 | 문의 + 첨부파일 저장 | 정상 |

**회원 문의 생성**

| ID | 테스트 케이스 | 입력 | 기대 결과 | 카테고리 |
|----|--------------|------|----------|---------|
| IS-003 | 회원 문의 생성 성공 | 유효한 요청 + userId | 문의 생성, user 연결 | 정상 |
| IS-004 | 존재하지 않는 사용자 | 유효한 요청 + 없는 userId | UserNotFoundException | 예외 |

**비회원 문의 조회**

| ID | 테스트 케이스 | 입력 | 기대 결과 | 카테고리 |
|----|--------------|------|----------|---------|
| IS-005 | 올바른 정보로 조회 | 문의번호, 이메일, 비밀번호 | 문의 정보 반환 | 정상 |
| IS-006 | 틀린 비밀번호 | 문의번호, 이메일, 틀린 비밀번호 | InquiryInvalidPasswordException | 예외 |
| IS-007 | 존재하지 않는 문의 | 잘못된 문의번호 | InquiryNotFoundException | 예외 |

**내 문의 조회**

| ID | 테스트 케이스 | 입력 | 기대 결과 | 카테고리 |
|----|--------------|------|----------|---------|
| IS-008 | 내 문의 목록 조회 | userId, pageable | 해당 유저의 문의 목록 | 정상 |
| IS-009 | 내 문의 상세 조회 | inquiryId, userId | 문의 상세 정보 | 정상 |
| IS-010 | 다른 사용자 문의 조회 | inquiryId, 다른 userId | InquiryNotFoundException | 예외 |

**관리자 기능**

| ID | 테스트 케이스 | 입력 | 기대 결과 | 카테고리 |
|----|--------------|------|----------|---------|
| IS-011 | 전체 문의 목록 조회 | pageable | 전체 문의 목록 | 정상 |
| IS-012 | 유형별 필터링 | type=JOIN, pageable | 가입 문의만 반환 | 정상 |
| IS-013 | 상태별 필터링 | status=PENDING, pageable | 대기중 문의만 반환 | 정상 |
| IS-014 | 상태 변경 | inquiryId, IN_PROGRESS | 상태 변경됨 | 정상 |
| IS-015 | 존재하지 않는 문의 상태 변경 | 잘못된 inquiryId | InquiryNotFoundException | 예외 |
| IS-016 | 답변 작성 성공 | inquiryId, 답변내용, operatorId | 답변 저장, 상태=COMPLETED | 정상 |
| IS-017 | 중복 답변 작성 | 이미 답변된 inquiryId | InquiryAlreadyRepliedException | 예외 |
| IS-018 | 내부 메모 작성 | inquiryId, 메모내용, operatorId | 메모 저장 | 정상 |
| IS-019 | 문의 소프트 삭제 | inquiryId, operatorId | deleted=true | 정상 |

### 3. API 통합 테스트

#### 3.1 공개 API

| ID | 테스트 케이스 | Method | Path | 기대 결과 | 카테고리 |
|----|--------------|--------|------|----------|---------|
| API-001 | 비회원 문의 작성 | POST | /api/v1/inquiries | 201 Created | 정상 |
| API-002 | 회원 문의 작성 | POST | /api/v1/inquiries (X-User-Id 헤더) | 201 Created | 정상 |
| API-003 | 유효성 검증 실패 | POST | /api/v1/inquiries (빈 제목) | 400 Bad Request | 예외 |
| API-004 | 비회원 문의 조회 | POST | /api/v1/inquiries/lookup | 200 OK | 정상 |
| API-005 | 비회원 문의 조회 실패 (비밀번호) | POST | /api/v1/inquiries/lookup | 401 Unauthorized | 예외 |

#### 3.2 회원 API

| ID | 테스트 케이스 | Method | Path | 기대 결과 | 카테고리 |
|----|--------------|--------|------|----------|---------|
| API-006 | 내 문의 목록 | GET | /api/v1/inquiries/my | 200 OK | 정상 |
| API-007 | 내 문의 상세 | GET | /api/v1/inquiries/my/{id} | 200 OK | 정상 |
| API-008 | 타인 문의 접근 | GET | /api/v1/inquiries/my/{id} | 404 Not Found | 예외 |

#### 3.3 관리자 API

| ID | 테스트 케이스 | Method | Path | 기대 결과 | 카테고리 |
|----|--------------|--------|------|----------|---------|
| API-009 | 전체 문의 목록 | GET | /api/v1/inquiries | 200 OK | 정상 |
| API-010 | 문의 상세 조회 | GET | /api/v1/inquiries/{id} | 200 OK | 정상 |
| API-011 | 상태 변경 | PUT | /api/v1/inquiries/{id}/status | 200 OK | 정상 |
| API-012 | 답변 작성 | POST | /api/v1/inquiries/{id}/reply | 201 Created | 정상 |
| API-013 | 중복 답변 | POST | /api/v1/inquiries/{id}/reply | 409 Conflict | 예외 |
| API-014 | 메모 작성 | POST | /api/v1/inquiries/{id}/memo | 201 Created | 정상 |
| API-015 | 문의 삭제 | DELETE | /api/v1/inquiries/{id} | 204 No Content | 정상 |

### 4. 엣지 케이스

| ID | 테스트 케이스 | 설명 | 기대 결과 |
|----|--------------|------|----------|
| EDGE-001 | 빈 첨부파일 목록 | attachments = [] | 문의 생성 성공 (첨부파일 없음) |
| EDGE-002 | 최대 길이 제목 | title = 100자 | 문의 생성 성공 |
| EDGE-003 | 제목 초과 | title = 101자 | 400 Bad Request |
| EDGE-004 | 동시 답변 작성 | 동시에 2명이 답변 작성 | 1명만 성공, 1명은 예외 |
| EDGE-005 | 삭제된 문의 접근 | deleted=true인 문의 조회 | 404 Not Found |
| EDGE-006 | 빈 문자열 비밀번호 | password = "" | 400 Bad Request |
| EDGE-007 | 특수문자 포함 이메일 | guest+test@test.com | 문의 생성 성공 |

## 테스트 실행 방법

```bash
# 전체 테스트 실행
./gradlew test

# Inquiry 관련 테스트만 실행
./gradlew test --tests "igrus.web.inquiry.*"

# 도메인 단위 테스트만 실행
./gradlew test --tests "igrus.web.inquiry.domain.*"

# 서비스 통합 테스트만 실행
./gradlew test --tests "igrus.web.inquiry.service.*"
```

## 변경 이력

| 날짜 | 버전 | 작성자 | 변경 내용 |
|------|------|-------|----------|
| 2024-01-23 | 1.0 | Claude | 초기 작성 |
