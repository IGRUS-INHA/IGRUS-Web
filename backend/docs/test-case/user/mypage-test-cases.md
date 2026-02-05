# 마이페이지 테스트 케이스

**작성일**: 2026-02-05
**버전**: 1.0
**관련 스펙**: IGRUS_WEB_PRD_V2.md 섹션 7
**우선순위**: P1

---

## 1. 개요

마이페이지 기능에 대한 테스트 케이스입니다.
프로필 조회/수정, 비밀번호 변경, 내 활동(게시글/댓글/행사 신청) 조회를 다룹니다.

---

## 2. 테스트 케이스

### 2.1 프로필 조회 (GetMyProfileService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-001 | 프로필 조회 성공 | 정회원 사용자 존재, PasswordCredential에 approvedAt 있음 | userId로 프로필 조회 | 학번, 이름, 이메일, 학과, 역할, 가입일, 승인일 포함된 응답 반환 | ⬜ |
| MP-002 | 승인일 없는 프로필 조회 | 준회원 사용자 존재, PasswordCredential에 approvedAt 없음 | userId로 프로필 조회 | approvedAt이 null인 응답 반환 | ⬜ |
| MP-003 | 존재하지 않는 사용자 프로필 조회 | 해당 userId의 사용자 없음 | 없는 userId로 프로필 조회 | UserNotFoundException 발생 | ⬜ |

### 2.2 내 게시글 목록 조회 (GetMyPostsService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-004 | 내 게시글 목록 조회 성공 | 사용자가 작성한 게시글 2개 존재 | userId와 pageable로 조회 | 게시글 2개가 담긴 Page 반환, 제목/게시판 정보 포함 | ⬜ |
| MP-005 | 게시글 없는 경우 빈 페이지 반환 | 사용자가 작성한 게시글 없음 | userId와 pageable로 조회 | 빈 Page 반환, totalElements = 0 | ⬜ |

### 2.3 내 댓글 목록 조회 (GetMyCommentsService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-006 | 내 댓글 목록 조회 성공 | 사용자가 작성한 댓글 2개 존재 | userId와 pageable로 조회 | 댓글 2개가 담긴 Page 반환, 게시글 제목 포함 | ⬜ |
| MP-007 | 댓글 없는 경우 빈 페이지 반환 | 사용자가 작성한 댓글 없음 | userId와 pageable로 조회 | 빈 Page 반환, totalElements = 0 | ⬜ |

### 2.4 내 행사 신청 목록 조회 (GetMyRegistrationsService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-008 | 내 행사 신청 목록 조회 성공 | 사용자가 신청한 행사 2건 존재 | userId로 조회 | 신청 2건이 담긴 List 반환, 행사 제목/상태 포함 | ⬜ |
| MP-009 | 신청 없는 경우 빈 리스트 반환 | 사용자가 신청한 행사 없음 | userId로 조회 | 빈 List 반환 | ⬜ |

### 2.5 비밀번호 변경 (ChangeMyPasswordService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-010 | 비밀번호 변경 성공 | PasswordCredential 존재, 현재 비밀번호 일치 | 현재 비밀번호 + 새 비밀번호로 변경 요청 | 정상 완료, credential.changePassword 호출됨 | ⬜ |
| MP-011 | 현재 비밀번호 불일치 | PasswordCredential 존재, 현재 비밀번호 불일치 | 틀린 현재 비밀번호로 변경 요청 | InvalidCredentialsException 발생 | ⬜ |
| MP-012 | 새 비밀번호 형식 오류 | 현재 비밀번호 일치 | 형식에 맞지 않는 새 비밀번호로 변경 요청 | InvalidPasswordFormatException 발생 | ⬜ |
| MP-013 | 존재하지 않는 사용자 비밀번호 변경 | 해당 userId의 PasswordCredential 없음 | 없는 userId로 변경 요청 | UserNotFoundException 발생 | ⬜ |
| MP-021 | 현재 비밀번호와 새 비밀번호 동일 | 현재 비밀번호 일치, 새 비밀번호가 현재와 동일 | 같은 비밀번호로 변경 요청 | SamePasswordException 발생 | ⬜ |

### 2.6 프로필 수정 (UpdateMyProfileService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-014 | 이메일 수정 성공 | 사용자 존재, 새 이메일 미사용 | 새 이메일로 수정 요청 | user.updateEmail 호출됨 | ⬜ |
| MP-015 | 전화번호 수정 성공 | 사용자 존재, 새 전화번호 미사용 | 새 전화번호로 수정 요청 | user.updatePhoneNumber 호출됨 | ⬜ |
| MP-016 | 이메일 중복 시 예외 | 다른 사용자가 해당 이메일 사용 중 | 중복 이메일로 수정 요청 | DuplicateEmailException 발생 | ⬜ |
| MP-017 | 전화번호 중복 시 예외 | 다른 사용자가 해당 전화번호 사용 중 | 중복 전화번호로 수정 요청 | DuplicatePhoneNumberException 발생 | ⬜ |
| MP-018 | 존재하지 않는 사용자 프로필 수정 | 해당 userId의 사용자 없음 | 없는 userId로 수정 요청 | UserNotFoundException 발생 | ⬜ |
| MP-019 | 기존 이메일과 동일하면 수정 안 함 | 사용자 존재 | 현재와 같은 이메일로 수정 요청 | 중복 체크 없이 정상 완료 | ⬜ |
| MP-020 | null 값이면 해당 필드 수정 안 함 | 사용자 존재 | email=null, phoneNumber=null로 요청 | 아무 필드도 수정되지 않음 | ⬜ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| FR-MP-001 | 학번, 이름, 이메일, 학과, 역할, 가입일, 승인일 조회 | MP-001, MP-002, MP-003 |
| FR-MP-002 | 이메일, 전화번호 수정 | MP-014 ~ MP-020 |
| FR-MP-003 | 비밀번호 변경 (현재 비밀번호 확인 후) | MP-010 ~ MP-013 |
| FR-MP-004 | 내 게시글 목록 조회 | MP-004, MP-005 |
| FR-MP-005 | 내 댓글 목록 조회 | MP-006, MP-007 |
| FR-MP-006 | 신청한 행사 목록 조회 | MP-008, MP-009 |

---

## 4. 구현된 테스트 클래스

### 4.1 Service 테스트

- **GetMyProfileServiceTest** - MP-001 ~ MP-003
- **GetMyPostsServiceTest** - MP-004, MP-005
- **GetMyCommentsServiceTest** - MP-006, MP-007
- **GetMyRegistrationsServiceTest** - MP-008, MP-009
- **ChangeMyPasswordServiceTest** - MP-010 ~ MP-013
- **UpdateMyProfileServiceTest** - MP-014 ~ MP-020

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-05 | - | 최초 작성 |
