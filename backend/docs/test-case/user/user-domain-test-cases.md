# User 도메인 테스트 케이스

**작성일**: 2026-01-22
**버전**: 1.0
**관련 PRD**: [IGRUS_WEB_PRD_V2.md](../../../../docs/feature/common/IGRUS_WEB_PRD_V2.md)

---

## 1. 개요

User 도메인 관련 테스트 케이스를 정의합니다. 현재 구현된 테스트와 PRD 기반 향후 구현 예정 테스트를 포함합니다.

### 1.1 테스트 대상

| 클래스 | 설명 | 테스트 파일 |
|--------|------|------------|
| `User` | 사용자 기본정보 엔티티 | (미구현) |
| `UserRole` | 사용자 역할 Enum | (미구현) |
| `UserRoleHistory` | 역할 변경 이력 엔티티 | `UserRoleHistoryTest.java` |
| `PasswordCredential` | 인증 자격증명 엔티티 | (미구현) |
| `Position` | 직책 엔티티 | (미구현) |
| `UserPosition` | 사용자-직책 연결 엔티티 | (미구현) |

---

## 2. UserRoleHistory 테스트 케이스

**테스트 파일**: `src/test/java/igrus/web/user/domain/UserRoleHistoryTest.java`

### 2.1 create 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-001 | 유효한 역할 변경 시 UserRoleHistory 생성 성공 | previousRole, newRole, reason이 유효하면 UserRoleHistory 객체 생성 | ✅ 구현됨 |
| URH-002 | 이전 역할과 새 역할이 동일하면 예외 발생 | 동일 역할로 변경 시 `SameRoleChangeException` 발생 | ✅ 구현됨 |
| URH-003 | 사유 없이도 생성 가능 | reason이 null이어도 정상 생성 | ✅ 구현됨 |

### 2.2 isPromotion 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-004 | 상위 역할로 변경 시 true 반환 | ASSOCIATE→MEMBER, MEMBER→OPERATOR, OPERATOR→ADMIN 등 | ✅ 구현됨 |
| URH-005 | 하위 역할로 변경 시 false 반환 | MEMBER→ASSOCIATE, ADMIN→OPERATOR 등 | ✅ 구현됨 |

### 2.3 isDemotion 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-006 | 하위 역할로 변경 시 true 반환 | MEMBER→ASSOCIATE, OPERATOR→MEMBER, ADMIN→ASSOCIATE 등 | ✅ 구현됨 |
| URH-007 | 상위 역할로 변경 시 false 반환 | ASSOCIATE→MEMBER, MEMBER→ADMIN 등 | ✅ 구현됨 |

### 2.4 isPromotionToAdmin 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-008 | 관리자로 승급 시 true 반환 | ASSOCIATE→ADMIN, MEMBER→ADMIN, OPERATOR→ADMIN | ✅ 구현됨 |
| URH-009 | 관리자가 아닌 역할로 변경 시 false 반환 | ASSOCIATE→MEMBER, MEMBER→OPERATOR 등 | ✅ 구현됨 |

### 2.5 isPromotionToOperator 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-010 | 운영진으로 승급 시 true 반환 | ASSOCIATE→OPERATOR, MEMBER→OPERATOR | ✅ 구현됨 |
| URH-011 | 관리자에서 운영진으로 강등 시 false 반환 | ADMIN→OPERATOR는 승급이 아니므로 false | ✅ 구현됨 |
| URH-012 | 운영진이 아닌 역할로 변경 시 false 반환 | ASSOCIATE→MEMBER, MEMBER→ADMIN 등 | ✅ 구현됨 |

### 2.6 isPromotionToMember 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-013 | 준회원에서 정회원으로 승급 시 true 반환 | ASSOCIATE→MEMBER | ✅ 구현됨 |
| URH-014 | 준회원 외의 역할에서 정회원으로 변경 시 false 반환 | OPERATOR→MEMBER, ADMIN→MEMBER는 강등이므로 false | ✅ 구현됨 |
| URH-015 | 정회원이 아닌 역할로 변경 시 false 반환 | ASSOCIATE→OPERATOR 등 | ✅ 구현됨 |

### 2.7 isDemotionFromAdmin 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-016 | 관리자에서 강등 시 true 반환 | ADMIN→OPERATOR, ADMIN→MEMBER, ADMIN→ASSOCIATE | ✅ 구현됨 |
| URH-017 | 관리자가 아닌 역할에서 변경 시 false 반환 | OPERATOR→MEMBER, MEMBER→ASSOCIATE 등 | ✅ 구현됨 |

### 2.8 isChangeTo 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-018 | 지정한 역할로 변경 시 true 반환 | newRole이 targetRole과 일치 | ✅ 구현됨 |
| URH-019 | 다른 역할로 변경 시 false 반환 | newRole이 targetRole과 불일치 | ✅ 구현됨 |

### 2.9 isChangeFrom 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-020 | 지정한 역할에서 변경 시 true 반환 | previousRole이 sourceRole과 일치 | ✅ 구현됨 |
| URH-021 | 다른 역할에서 변경 시 false 반환 | previousRole이 sourceRole과 불일치 | ✅ 구현됨 |

### 2.10 updateReason 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-022 | 사유 업데이트 성공 | 새로운 reason으로 업데이트 | ✅ 구현됨 |
| URH-023 | null로 사유 업데이트 가능 | reason을 null로 변경 가능 | ✅ 구현됨 |

### 2.11 hasReason 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| URH-024 | 사유가 존재하면 true 반환 | reason이 유효한 문자열일 때 | ✅ 구현됨 |
| URH-025 | 사유가 null이면 false 반환 | reason == null | ✅ 구현됨 |
| URH-026 | 사유가 빈 문자열이면 false 반환 | reason == "" | ✅ 구현됨 |
| URH-027 | 사유가 공백만 있으면 false 반환 | reason == "   " | ✅ 구현됨 |

---

## 3. User 테스트 케이스 (구현 예정)

### 3.1 create 정적 팩토리 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| U-001 | 유효한 정보로 User 생성 성공 | 모든 필수 필드가 유효하면 User 객체 생성 | ⬜ 미구현 |
| U-002 | 생성 시 기본 역할은 ASSOCIATE | 생성된 User의 role은 ASSOCIATE | ⬜ 미구현 |
| U-003 | 학번이 8자리가 아니면 예외 발생 | 학번 형식 검증 | ⬜ 미구현 |
| U-004 | 이메일 형식이 유효하지 않으면 예외 발생 | 이메일 형식 검증 | ⬜ 미구현 |

### 3.2 역할 변경 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| U-005 | promoteToMember 호출 시 MEMBER로 변경 | 준회원→정회원 승급 | ⬜ 미구현 |
| U-006 | promoteToOperator 호출 시 OPERATOR로 변경 | 운영진으로 승급 | ⬜ 미구현 |
| U-007 | promoteToAdmin 호출 시 ADMIN으로 변경 | 관리자로 승급 | ⬜ 미구현 |
| U-008 | demoteToMember 호출 시 MEMBER로 변경 | 정회원으로 강등 | ⬜ 미구현 |
| U-009 | changeRole 호출 시 지정한 역할로 변경 | 임의 역할로 변경 | ⬜ 미구현 |

### 3.3 역할 확인 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| U-010 | isAdmin - ADMIN일 때 true 반환 | 관리자 여부 확인 | ⬜ 미구현 |
| U-011 | isOperator - OPERATOR일 때 true 반환 | 운영진 여부 확인 | ⬜ 미구현 |
| U-012 | isOperatorOrAbove - OPERATOR/ADMIN일 때 true 반환 | 운영진 이상 여부 확인 | ⬜ 미구현 |
| U-013 | isMember - MEMBER일 때 true 반환 | 정회원 여부 확인 | ⬜ 미구현 |
| U-014 | isAssociate - ASSOCIATE일 때 true 반환 | 준회원 여부 확인 | ⬜ 미구현 |

### 3.4 직책 관련 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| U-015 | addPosition으로 직책 추가 성공 | 새 직책 추가 | ⬜ 미구현 |
| U-016 | 중복 직책 추가 시 무시 | 이미 가진 직책 추가 시 중복 방지 | ⬜ 미구현 |
| U-017 | removePosition으로 직책 제거 성공 | 기존 직책 제거 | ⬜ 미구현 |
| U-018 | clearPositions으로 모든 직책 제거 | 전체 직책 초기화 | ⬜ 미구현 |
| U-019 | hasPosition - 해당 직책 보유 시 true 반환 | 직책 보유 여부 확인 | ⬜ 미구현 |
| U-020 | hasAnyPosition - 직책이 있으면 true 반환 | 직책 존재 여부 확인 | ⬜ 미구현 |

### 3.5 프로필 수정 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| U-021 | updateProfile로 프로필 정보 수정 성공 | name, phoneNumber, department 수정 | ⬜ 미구현 |
| U-022 | updateEmail로 이메일 수정 성공 | email 수정 | ⬜ 미구현 |

---

## 4. PasswordCredential 테스트 케이스 (구현 예정)

### 4.1 create 정적 팩토리 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| PC-001 | 유효한 정보로 PasswordCredential 생성 성공 | User와 passwordHash로 생성 | ⬜ 미구현 |
| PC-002 | 생성 시 기본 상태는 ACTIVE | 생성된 credential의 status는 ACTIVE | ⬜ 미구현 |

### 4.2 비밀번호 관련 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| PC-003 | changePassword로 비밀번호 변경 성공 | 새 passwordHash로 변경 | ⬜ 미구현 |

### 4.3 계정 상태 관련 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| PC-004 | activate 호출 시 ACTIVE로 변경 | 계정 활성화 | ⬜ 미구현 |
| PC-005 | suspend 호출 시 SUSPENDED로 변경 | 계정 정지 | ⬜ 미구현 |
| PC-006 | withdraw 호출 시 WITHDRAWN으로 변경 | 계정 탈퇴 | ⬜ 미구현 |
| PC-007 | isActive - ACTIVE일 때 true 반환 | 활성 상태 확인 | ⬜ 미구현 |
| PC-008 | isSuspended - SUSPENDED일 때 true 반환 | 정지 상태 확인 | ⬜ 미구현 |
| PC-009 | isWithdrawn - WITHDRAWN일 때 true 반환 | 탈퇴 상태 확인 | ⬜ 미구현 |

### 4.4 정회원 승인 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| PC-010 | approve 호출 시 승인 정보 기록 | approvedAt, approvedBy 설정 | ⬜ 미구현 |
| PC-011 | isApproved - 승인되었으면 true 반환 | approvedAt != null | ⬜ 미구현 |

---

## 5. Position 테스트 케이스 (구현 예정)

### 5.1 create 정적 팩토리 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| P-001 | 유효한 정보로 Position 생성 성공 | name, imageUrl, displayOrder로 생성 | ⬜ 미구현 |

### 5.2 수정 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| P-002 | updateName으로 직책명 수정 성공 | name 수정 | ⬜ 미구현 |
| P-003 | updateImageUrl로 이미지 URL 수정 성공 | imageUrl 수정 | ⬜ 미구현 |
| P-004 | updateDisplayOrder로 표시 순서 수정 성공 | displayOrder 수정 | ⬜ 미구현 |
| P-005 | update로 전체 정보 수정 성공 | 모든 필드 수정 | ⬜ 미구현 |

---

## 6. PRD 기반 비즈니스 규칙 테스트 케이스 (구현 예정)

### 6.1 회원가입 규칙 (PRD 섹션 1)

| TC-ID | 테스트명 | 설명 | PRD 참조 | 상태 |
|-------|---------|------|---------|------|
| BIZ-001 | 학번은 정확히 8자리 숫자여야 함 | 유효성 검증 | PRD 108행 | ⬜ 미구현 |
| BIZ-002 | 비밀번호는 영문 대/소문자 + 숫자 + 특수문자 조합, 최소 8자 이상 | 비밀번호 정책 | PRD 110행 | ⬜ 미구현 |
| BIZ-003 | 동일 학번 중복 가입 불가 | 중복 검증 | PRD 115행 | ⬜ 미구현 |
| BIZ-004 | 탈퇴 후 5일 이내 동일 학번 재가입 불가 | 재가입 제한 | PRD 12-16행 | ⬜ 미구현 |

### 6.2 역할 권한 규칙 (PRD 섹션 3)

| TC-ID | 테스트명 | 설명 | PRD 참조 | 상태 |
|-------|---------|------|---------|------|
| BIZ-005 | 준회원은 게시글/댓글 작성 불가 | 권한 검증 | PRD 70행 | ⬜ 미구현 |
| BIZ-006 | 준회원은 행사 신청 불가 | 권한 검증 | PRD 71행 | ⬜ 미구현 |
| BIZ-007 | 준회원→정회원 승인은 ADMIN만 가능 | 권한 검증 | PRD 76행 | ⬜ 미구현 |
| BIZ-008 | 회원 정지/강제탈퇴는 ADMIN만 가능 | 권한 검증 | PRD 78행 | ⬜ 미구현 |

### 6.3 계정 상태 규칙 (PRD 섹션 3)

| TC-ID | 테스트명 | 설명 | PRD 참조 | 상태 |
|-------|---------|------|---------|------|
| BIZ-009 | SUSPENDED 상태에서는 로그인 불가 | 상태 검증 | PRD 167행 | ⬜ 미구현 |
| BIZ-010 | WITHDRAWN 상태에서는 로그인 불가 | 상태 검증 | PRD 168행 | ⬜ 미구현 |
| BIZ-011 | 상태 변경 시 모든 활성 토큰 무효화 | 보안 정책 | PRD 179행 | ⬜ 미구현 |

### 6.4 관리자 제한 규칙 (PRD 섹션 8.3)

| TC-ID | 테스트명 | 설명 | PRD 참조 | 상태 |
|-------|---------|------|---------|------|
| BIZ-012 | 자기 자신 정지 불가 | 제한 규칙 | PRD 399행 | ⬜ 미구현 |
| BIZ-013 | 자기 자신 권한 변경 불가 | 제한 규칙 | PRD 400행 | ⬜ 미구현 |
| BIZ-014 | 마지막 ADMIN은 권한 변경 불가 | 제한 규칙 | PRD 401행 | ⬜ 미구현 |

---

## 7. 테스트 실행 방법

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "igrus.web.user.domain.UserRoleHistoryTest"

# 테스트 리포트 확인
# build/reports/tests/test/index.html
```

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-22 | - | 최초 작성 |
