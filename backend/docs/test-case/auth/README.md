# 인증 기능 테스트 케이스

**작성일**: 2026-01-23
**버전**: 1.0
**관련 스펙**: [auth-spec.md](../../../../docs/feature/auth/auth-spec.md)

---

## 1. 개요

이 디렉토리는 IGRUS-Web 인증 기능(로그인/회원가입)의 테스트 케이스 문서를 포함합니다. [auth-spec.md](../../../../docs/feature/auth/auth-spec.md)의 User Stories 및 Functional Requirements를 기반으로 작성되었습니다.

---

## 2. 문서 목록

| 문서 | 설명 | 우선순위 | 테스트 케이스 수 |
|------|------|---------|-----------------|
| [registration-test-cases.md](registration-test-cases.md) | 회원가입 테스트 케이스 | P1 | 26개 |
| [login-test-cases.md](login-test-cases.md) | 로그인 테스트 케이스 | P1 | 18개 |
| [token-test-cases.md](token-test-cases.md) | 토큰 갱신 테스트 케이스 | P2 | 15개 |
| [password-reset-test-cases.md](password-reset-test-cases.md) | 비밀번호 재설정 테스트 케이스 | P2 | 15개 |
| [account-recovery-test-cases.md](account-recovery-test-cases.md) | 탈퇴 계정 복구 테스트 케이스 | P3 | 15개 |
| [member-approval-test-cases.md](member-approval-test-cases.md) | 준회원 승인 테스트 케이스 | P2 | 17개 |

**총 테스트 케이스: 106개**

**구현 상태: ✅ 106개 완료 (100%)**

---

## 3. 테스트 케이스 요약

### 3.1 우선순위별 분류

| 우선순위 | 기능 | 테스트 케이스 수 |
|---------|------|-----------------|
| P1 (필수) | 회원가입, 로그인 | 44개 |
| P2 (중요) | 토큰 갱신, 비밀번호 재설정, 준회원 승인 | 47개 |
| P3 (보통) | 탈퇴 계정 복구 | 15개 |

### 3.2 카테고리별 분류

| 카테고리 | 설명 | 테스트 케이스 수 |
|---------|------|-----------------|
| 회원가입 | 개인정보 동의, 필수 정보 입력, 비밀번호 검증, 중복 검사, 이메일 인증 | 26개 |
| 로그인 | 로그인 성공/실패, 계정 상태별 제한, 로그아웃, 다중 디바이스 | 18개 |
| 토큰 갱신 | 토큰 갱신 성공/실패, 계정 상태 변경 시 처리, 토큰 보안 | 15개 |
| 비밀번호 재설정 | 링크 발송, 재설정 성공/실패, 새 비밀번호 검증 | 15개 |
| 탈퇴 계정 복구 | 5일 이내 복구, 5일 경과 후 처리, 재가입 제한 | 15개 |
| 준회원 승인 | 목록 조회, 개별/일괄 승인, 권한 검증, ADMIN 보호 | 17개 |

---

## 4. Functional Requirements 매핑

### 4.1 회원가입 관련 (FR-001 ~ FR-010)

| FR ID | 요구사항 | 관련 테스트 케이스 |
|-------|---------|------------------|
| FR-001 | 개인정보 수집·이용 동의 필수 | REG-001, REG-002 |
| FR-002 | 필수 입력 항목 검증 | REG-010 ~ REG-018 |
| FR-003 | 비밀번호 복잡도 검증 | REG-020 ~ REG-026, PWD-030 ~ PWD-035 |
| FR-004 | 중복 가입 방지 | REG-030 ~ REG-032 |
| FR-005 | 6자리 인증 코드 발송 | REG-040 |
| FR-006 | 인증 코드 10분 유효 | REG-041, REG-042 |
| FR-007 | 인증 코드 재발송 1분 대기 | REG-044, REG-045 |
| FR-008 | 인증 시도 횟수 5회 제한 | REG-043 |
| FR-009 | 인증 완료 시 준회원 등록 | REG-041 |
| FR-010 | 인증 미완료 24시간 후 삭제 | REG-051 |

### 4.2 로그인/인증 관련 (FR-011 ~ FR-016)

| FR ID | 요구사항 | 관련 테스트 케이스 |
|-------|---------|------------------|
| FR-011 | 학번과 비밀번호로 인증 | LOG-001 ~ LOG-014 |
| FR-012 | 이메일 인증 완료 사용자만 로그인 | LOG-012 |
| FR-013 | Access Token(1시간), Refresh Token(7일) | LOG-005, LOG-006, TKN-001, TKN-002 |
| FR-014 | 로그인 시 역할 정보 반환 | LOG-007 |
| FR-015 | Refresh Token으로 Access Token 재발급 | TKN-001 ~ TKN-004 |
| FR-016 | 로그아웃 시 토큰 무효화 | LOG-030 ~ LOG-032 |

### 4.3 비밀번호 재설정 관련 (FR-017 ~ FR-019)

| FR ID | 요구사항 | 관련 테스트 케이스 |
|-------|---------|------------------|
| FR-017 | 학번 입력 시 이메일로 재설정 링크 발송 | PWD-001, PWD-002, PWD-004 |
| FR-018 | 재설정 링크 30분 유효 | PWD-003, PWD-020 |
| FR-019 | 재설정 완료 시 모든 토큰 무효화 | PWD-010, PWD-013, TKN-024 |

### 4.4 계정 상태 관리 관련 (FR-020 ~ FR-025)

| FR ID | 요구사항 | 관련 테스트 케이스 |
|-------|---------|------------------|
| FR-020 | 계정 상태 관리 | LOG-020 ~ LOG-022, REC-003, REC-005 |
| FR-021 | 정지/탈퇴 상태 로그인 차단 | LOG-020 ~ LOG-022, REC-001, REC-002 |
| FR-022 | 정지/탈퇴 시 모든 토큰 무효화 | TKN-020 ~ TKN-023, REC-040 |
| FR-023 | 탈퇴 후 5일 이내 복구 | REC-001 ~ REC-011 |
| FR-024 | 탈퇴한 학번 5일 이내 재가입 차단 | REC-030 ~ REC-033 |
| FR-025 | 탈퇴 후 5일 경과 시 개인정보 삭제 | REC-020, REC-021, REC-022 |

### 4.5 준회원 승인 관련 (FR-026 ~ FR-028)

| FR ID | 요구사항 | 관련 테스트 케이스 |
|-------|---------|------------------|
| FR-026 | 관리자(ADMIN) 준회원 목록 조회 | APR-001 ~ APR-004 |
| FR-027 | 관리자(ADMIN)만 준회원 승인 가능 | APR-010 ~ APR-013, APR-030 ~ APR-034 |
| FR-028 | 개별 승인 및 일괄 승인 제공 | APR-010 ~ APR-024 |

### 4.6 개인정보 보호 관련 (FR-029 ~ FR-031)

| FR ID | 요구사항 | 관련 테스트 케이스 |
|-------|---------|------------------|
| FR-029 | 개인정보 처리방침 링크 제공 | REG-003 |
| FR-030 | 동의 정책 버전 기록 | REG-004 |
| FR-031 | BCrypt 해시 저장 | REG-026 |

---

## 5. 테스트 실행 가이드

### 5.1 단위 테스트 실행

```bash
cd backend

# 전체 인증 테스트 실행
./gradlew test --tests "igrus.web.security.*"

# 개별 테스트 클래스 실행
./gradlew test --tests "igrus.web.security.auth.RegistrationTest"
./gradlew test --tests "igrus.web.security.auth.LoginTest"
./gradlew test --tests "igrus.web.security.jwt.TokenRefreshTest"
```

### 5.2 통합 테스트 실행

```bash
# 통합 테스트 실행
./gradlew integrationTest --tests "igrus.web.security.*"
```

---

## 6. 테스트 케이스 ID 규칙

| 접두사 | 설명 |
|-------|------|
| REG-XXX | 회원가입 (Registration) |
| LOG-XXX | 로그인 (Login) |
| TKN-XXX | 토큰 갱신 (Token) |
| PWD-XXX | 비밀번호 재설정 (Password) |
| REC-XXX | 탈퇴 계정 복구 (Recovery) |
| APR-XXX | 준회원 승인 (Approval) |

---

## 7. 테스트 상태 표기

| 상태 | 설명 |
|------|------|
| 🔲 | 미구현 (Pending) |
| 🔄 | 구현 중 (In Progress) |
| ✅ | 구현 완료 (Completed) |
| ❌ | 실패 (Failed) |
| ⏭️ | 스킵 (Skipped) |

---

## 8. 관련 문서

- [auth-spec.md](../../../../docs/feature/auth/auth-spec.md) - 인증 기능 스펙 문서
- [IGRUS_WEB_PRD_V2.md](../../../../docs/feature/common/IGRUS_WEB_PRD_V2.md) - 제품 요구사항 문서
- [backend/CLAUDE.md](../../../CLAUDE.md) - 백엔드 개발 가이드

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-23 | - | 최초 작성 |
