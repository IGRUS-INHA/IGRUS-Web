# 마이페이지 테스트 케이스

**작성일**: 2026-02-05
**버전**: 1.0
**관련 스펙**: IGRUS_WEB_PRD_V2.md 섹션 7
**우선순위**: P1

---

## 1. 개요

마이페이지 기능에 대한 테스트 케이스입니다.
프로필 조회/수정, 비밀번호 변경, 회원 탈퇴, 내 활동(게시글/댓글/행사 신청/좋아요/북마크/문의) 조회를 다룹니다.

---

## 2. 테스트 케이스

### 2.1 프로필 조회 (GetMyProfileService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-001 | 프로필 조회 성공 | 정회원 사용자 존재 | userId로 프로필 조회 | 학번, 이름, 이메일, 학과, 역할, 가입일 포함된 응답 반환 | ✅ |
| MP-003 | 존재하지 않는 사용자 프로필 조회 | 해당 userId의 사용자 없음 | 없는 userId로 프로필 조회 | UserNotFoundException 발생 | ✅ |

### 2.2 내 게시글 목록 조회 (GetMyPostsService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-004 | 내 게시글 목록 조회 성공 | 사용자가 작성한 게시글 2개 존재 | userId와 pageable로 조회 | 게시글 2개가 담긴 Page 반환, 제목/게시판 정보 포함 | ✅ |
| MP-005 | 게시글 없는 경우 빈 페이지 반환 | 사용자가 작성한 게시글 없음 | userId와 pageable로 조회 | 빈 Page 반환, totalElements = 0 | ✅ |

### 2.3 내 댓글 목록 조회 (GetMyCommentsService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-006 | 내 댓글 목록 조회 성공 | 사용자가 작성한 댓글 2개 존재 | userId와 pageable로 조회 | 댓글 2개가 담긴 Page 반환, 게시글 제목 포함 | ✅ |
| MP-007 | 댓글 없는 경우 빈 페이지 반환 | 사용자가 작성한 댓글 없음 | userId와 pageable로 조회 | 빈 Page 반환, totalElements = 0 | ✅ |

### 2.4 내 행사 신청 목록 조회 (EventRegistrationService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-008 | 내 행사 신청 목록 조회 성공 | 사용자가 신청한 행사 2건 존재 | userId로 조회 | 신청 2건이 담긴 List 반환, 행사 제목/상태 포함 | ✅ |
| MP-009 | 신청 없는 경우 빈 리스트 반환 | 사용자가 신청한 행사 없음 | userId로 조회 | 빈 List 반환 | ✅ |

### 2.5 좋아요한 게시글 목록 조회 (GetMyLikedPostsService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-022 | 좋아요한 게시글 목록 조회 성공 | 사용자 존재, 좋아요한 게시글 2개 | userId와 pageable로 조회 | 게시글 2개가 담긴 Page 반환, 게시글 제목/게시판 정보 포함 | ✅ |
| MP-023 | 좋아요한 게시글 없는 경우 빈 페이지 반환 | 사용자 존재, 좋아요한 게시글 없음 | userId와 pageable로 조회 | 빈 Page 반환, totalElements = 0 | ✅ |

### 2.6 북마크한 게시글 목록 조회 (bookmark/GetMyBookmarksService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-025 | 북마크한 게시글 목록 조회 성공 | 사용자 존재, 북마크한 게시글 2개 | userId와 pageable로 조회 | 게시글 2개가 담긴 Page 반환, 게시글 제목/게시판 정보 포함 | ✅ |
| MP-026 | 북마크한 게시글 없는 경우 빈 페이지 반환 | 사용자 존재, 북마크한 게시글 없음 | userId와 pageable로 조회 | 빈 Page 반환, totalElements = 0 | ✅ |

### 2.7 내 문의 목록 조회 (inquiry/GetMyInquiriesService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-028 | 내 문의 목록 조회 성공 | 사용자가 작성한 문의 2건 존재 | userId와 pageable로 조회 | 문의 2건이 담긴 Page 반환, 문의번호/제목/상태 포함 | ✅ |
| MP-029 | 문의 없는 경우 빈 페이지 반환 | 사용자가 작성한 문의 없음 | userId와 pageable로 조회 | 빈 Page 반환, totalElements = 0 | ✅ |

### 2.8 비밀번호 변경 (ChangeMyPasswordService)


| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-010 | 비밀번호 변경 성공 | PasswordCredential 존재, 현재 비밀번호 일치 | 현재 비밀번호 + 새 비밀번호로 변경 요청 | 정상 완료, credential.changePassword 호출됨, 리프레시 토큰 전부 무효화됨 | ✅ |
| MP-011 | 현재 비밀번호 불일치 | PasswordCredential 존재, 현재 비밀번호 불일치 | 틀린 현재 비밀번호로 변경 요청 | InvalidCredentialsException 발생 | ✅ |
| MP-012 | 새 비밀번호 형식 오류 | 현재 비밀번호 일치 | 형식에 맞지 않는 새 비밀번호로 변경 요청 | InvalidPasswordFormatException 발생 | ✅ |
| MP-013 | 존재하지 않는 사용자 비밀번호 변경 | 해당 userId의 PasswordCredential 없음 | 없는 userId로 변경 요청 | UserNotFoundException 발생 | ✅ |
| MP-021 | 현재 비밀번호와 새 비밀번호 동일 | 현재 비밀번호 일치, 새 비밀번호가 현재와 동일 | 같은 비밀번호로 변경 요청 | SamePasswordException 발생 | ✅ |

### 2.9 회원 탈퇴 (WithdrawService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| WD-001 | 회원 탈퇴 성공 | 정회원 존재, 비밀번호 일치 | 비밀번호 + 탈퇴 사유로 탈퇴 요청 | User 상태 WITHDRAWN + soft delete, Credential 상태 WITHDRAWN + soft delete, RefreshToken 무효화, WithdrawalLog 저장 | ✅ |
| WD-002 | 비밀번호 불일치 | 사용자 존재, 비밀번호 불일치 | 틀린 비밀번호로 탈퇴 요청 | InvalidCredentialsException 발생 | ✅ |
| WD-003 | 존재하지 않는 사용자 탈퇴 | 해당 userId의 사용자 없음 | 없는 userId로 탈퇴 요청 | UserNotFoundException 발생 | ✅ |
| WD-004 | PasswordCredential 없는 사용자 탈퇴 | 사용자 존재, PasswordCredential 없음 | 탈퇴 요청 | UserNotFoundException 발생 | ✅ |
| WD-005 | 탈퇴 로그 사유 저장 확인 | 정회원 존재, 비밀번호 일치 | 탈퇴 사유와 함께 탈퇴 요청 | WithdrawalLog에 user, reason 정확히 저장됨 | ✅ |
| WD-006 | 정지 상태 사용자 탈퇴 차단 | 사용자 SUSPENDED 상태 | 탈퇴 요청 | AccountSuspendedException 발생, 비밀번호 검증/탈퇴 처리 수행 안 됨 | ✅ |

### 2.10 프로필 수정 (UpdateMyProfileService)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| MP-014 | 이메일 수정 성공 | 사용자 존재, 새 이메일 미사용 | 새 이메일로 수정 요청 | user.updateEmail 호출됨 | ✅ |
| MP-015 | 전화번호 수정 성공 | 사용자 존재, 새 전화번호 미사용 | 새 전화번호로 수정 요청 | user.updatePhoneNumber 호출됨 | ✅ |
| MP-016 | 이메일 중복 시 예외 | 다른 사용자가 해당 이메일 사용 중 | 중복 이메일로 수정 요청 | DuplicateEmailException 발생 | ✅ |
| MP-017 | 전화번호 중복 시 예외 | 다른 사용자가 해당 전화번호 사용 중 | 중복 전화번호로 수정 요청 | DuplicatePhoneNumberException 발생 | ✅ |
| MP-018 | 존재하지 않는 사용자 프로필 수정 | 해당 userId의 사용자 없음 | 없는 userId로 수정 요청 | UserNotFoundException 발생 | ✅ |
| MP-019 | 기존 이메일과 동일하면 수정 안 함 | 사용자 존재 | 현재와 같은 이메일로 수정 요청 | 중복 체크 없이 정상 완료 | ✅ |
| MP-020 | null 값이면 해당 필드 수정 안 함 | 사용자 존재 | email=null, phoneNumber=null로 요청 | 아무 필드도 수정되지 않음 | ✅ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| FR-MP-001 | 학번, 이름, 이메일, 학과, 역할, 가입일 조회 | MP-001, MP-003 |
| FR-MP-002 | 이메일, 전화번호 수정 | MP-014 ~ MP-020 |
| FR-MP-003 | 비밀번호 변경 (현재 비밀번호 확인 후) | MP-010 ~ MP-013 |
| FR-MP-004 | 내 게시글 목록 조회 | MP-004, MP-005 |
| FR-MP-005 | 내 댓글 목록 조회 | MP-006, MP-007 |
| FR-MP-006 | 신청한 행사 목록 조회 | MP-008, MP-009 |
| FR-MP-007 | 좋아요한 게시글 목록 조회 | MP-022, MP-023 |
| FR-MP-008 | 북마크한 게시글 목록 조회 | MP-025, MP-026 |
| FR-MP-009 | 내 문의 목록 조회 | MP-028, MP-029 |
| FR-MP-010 | 회원 탈퇴 (비밀번호 확인 후, 정지 상태 차단) | WD-001 ~ WD-006 |

---

## 4. 구현된 테스트 클래스

### 4.1 mypage 패키지 Service 테스트

- **GetMyProfileServiceTest** (`user/mypage`) - MP-001, MP-003
- **GetMyPostsServiceTest** (`user/mypage`) - MP-004, MP-005
- **GetMyCommentsServiceTest** (`user/mypage`) - MP-006, MP-007
- **ChangeMyPasswordServiceTest** (`user/mypage`) - MP-010 ~ MP-013, MP-021
- **UpdateMyProfileServiceTest** (`user/mypage`) - MP-014 ~ MP-020

### 4.2 도메인 패키지 Service 테스트 (마이페이지에서 직접 사용)

- **EventRegistrationServiceTest** (`event`) - MP-008, MP-009
- **GetMyLikedPostsServiceTest** (`community/like`) - MP-022, MP-023
- **GetMyBookmarksServiceTest** (`community/bookmark`) - MP-025, MP-026
- **GetMyInquiriesServiceTest** (`inquiry`) - MP-028, MP-029
- **WithdrawServiceTest** (`user/withdrawal`) - WD-001 ~ WD-006

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-05 | - | 최초 작성 |
| 1.1 | 2026-02-05 | - | 회원 탈퇴(WD-001~WD-006) 추가, 비밀번호 변경 시 리프레시 토큰 무효화 반영 |
| 1.2 | 2026-02-05 | - | 좋아요/북마크/문의/행사 신청을 기존 도메인 서비스 직접 사용으로 변경 (방법 B→A), mypage wrapper 서비스 삭제, 테스트 클래스를 mypage/도메인 패키지로 구분 |
