# 관리자 기능 테스트 케이스

**작성일**: 2026-02-07
**버전**: 1.0
**관련 스펙**: [IGRUS_WEB_PRD_V2.md](../../../../docs/feature/common/IGRUS_WEB_PRD_V2.md)
**우선순위**: P2

---

## 1. 개요

관리자 대시보드 통계 API와 회원 관리 API에 대한 테스트 케이스입니다.

- **대시보드**: ADMIN 전용, 오늘 게시글/댓글 수, 이번 주 정회원 승인 수, 대기 중 문의/준회원 수 조회
- **회원 관리**: OPERATOR 이상 목록/상세 조회, ADMIN 전용 권한 변경

---

## 2. 대시보드 통계 조회

### 2.1 통계 조회 (단위 테스트)

| ID | 테스트 케이스 | 예상 결과 | 상태 |
|----|-------------|----------|------|
| DASH-001 | 모든 통계가 정상 반환됨 | 각 통계값 정확히 반환 | ✅ |
| DASH-002 | 데이터 없으면 모든 통계 0 반환 | 모든 필드 0 | ✅ |
| DASH-003 | 문의는 PENDING, 준회원은 ASSOCIATE로 조회 | 올바른 enum 전달 | ✅ |
| DASH-004 | 게시글/댓글은 오늘 시작, 승인 회원은 이번 주 월요일 기준 | 올바른 시간 전달 | ✅ |

### 2.2 대시보드 권한 검증 (통합 테스트)

| ID | 테스트 케이스 | 역할 | 예상 결과 | 상태 |
|----|-------------|------|----------|------|
| DASH-010 | ADMIN 대시보드 조회 성공 | ADMIN | 200 OK | ✅ |
| DASH-011 | OPERATOR 대시보드 조회 거부 | OPERATOR | 403 Forbidden | ✅ |
| DASH-012 | MEMBER 대시보드 조회 거부 | MEMBER | 403 Forbidden | ✅ |
| DASH-013 | 미인증 대시보드 조회 거부 | 미인증 | 403 Forbidden | ✅ |

---

## 3. 회원 목록/상세 조회

### 3.1 목록 조회 권한 검증 (통합 테스트)

| ID | 테스트 케이스 | 역할 | 예상 결과 | 상태 |
|----|-------------|------|----------|------|
| USR-001 | ADMIN 회원 목록 조회 성공 | ADMIN | 200 OK | ✅ |
| USR-002 | OPERATOR 회원 목록 조회 성공 | OPERATOR | 200 OK | ✅ |
| USR-003 | MEMBER 회원 목록 조회 거부 | MEMBER | 403 Forbidden | ✅ |
| USR-004 | 미인증 회원 목록 조회 거부 | 미인증 | 403 Forbidden | ✅ |
| USR-005 | 키워드 검색 성공 | ADMIN | 필터링된 결과 반환 | ✅ |
| USR-006 | 역할 필터 검색 성공 | ADMIN | 필터링된 결과 반환 | ✅ |

### 3.2 상세 조회 (통합 테스트)

| ID | 테스트 케이스 | 역할 | 예상 결과 | 상태 |
|----|-------------|------|----------|------|
| USR-010 | ADMIN 회원 상세 조회 성공 | ADMIN | 200 OK, userId/studentId/name 포함 | ✅ |
| USR-011 | OPERATOR 회원 상세 조회 성공 | OPERATOR | 200 OK | ✅ |
| USR-012 | MEMBER 회원 상세 조회 거부 | MEMBER | 403 Forbidden | ✅ |
| USR-013 | 존재하지 않는 사용자 조회 | ADMIN | 404 Not Found | ✅ |

---

## 4. 회원 권한 변경

### 4.1 권한 변경 (단위 테스트)

| ID | 테스트 케이스 | 예상 결과 | 상태 |
|----|-------------|----------|------|
| ROLE-001 | 정상 권한 변경 성공 | 역할 변경 + UserRoleHistory 저장 | ✅ |
| ROLE-002 | 자기 자신 권한 변경 거부 | SelfRoleChangeException | ✅ |
| ROLE-003 | 동일 역할로 변경 거부 | SameRoleChangeException | ✅ |
| ROLE-004 | 마지막 ADMIN 권한 변경 거부 | LastAdminCannotChangeException | ✅ |
| ROLE-005 | 존재하지 않는 사용자 권한 변경 거부 | UserNotFoundException | ✅ |

### 4.2 권한 변경 (통합 테스트)

| ID | 테스트 케이스 | 역할 | 예상 결과 | 상태 |
|----|-------------|------|----------|------|
| ROLE-010 | ADMIN 권한 변경 성공 | ADMIN | 204 No Content | ✅ |
| ROLE-011 | OPERATOR 권한 변경 거부 (ADMIN 전용) | OPERATOR | 403 Forbidden | ✅ |
| ROLE-012 | MEMBER 권한 변경 거부 | MEMBER | 403 Forbidden | ✅ |
| ROLE-013 | 자기 자신 권한 변경 시 400 | ADMIN | 400 Bad Request (SELF_ROLE_CHANGE_NOT_ALLOWED) | ✅ |
| ROLE-014 | 동일 역할 변경 시 400 | ADMIN | 400 Bad Request (SAME_ROLE_CHANGE) | ✅ |
| ROLE-015 | 마지막 ADMIN 권한 변경 시 400 | ADMIN | 400 Bad Request (LAST_ADMIN_CANNOT_CHANGE) | ✅ |
| ROLE-016 | 존재하지 않는 사용자 권한 변경 시 404 | ADMIN | 404 Not Found | ✅ |

---

## 5. 인가 모델 요약

| 엔드포인트 | ADMIN | OPERATOR | MEMBER |
|-----------|-------|----------|--------|
| `GET /api/v1/admin/dashboard` | 200 | **403** | 403 |
| `GET /api/v1/admin/users` | 200 | 200 | 403 |
| `GET /api/v1/admin/users/{id}` | 200 | 200 | 403 |
| `PUT /api/v1/admin/users/{id}/role` | 204 | **403** | 403 |

---

## 6. 관련 테스트 클래스

| 테스트 클래스 | 유형 | 테스트 수 |
|-------------|------|----------|
| `GetDashboardStatsServiceTest` | 단위 | 4개 |
| `AdminDashboardControllerTest` | 통합 | 4개 |
| `GetUserListServiceTest` | 단위 | - |
| `GetUserDetailServiceTest` | 단위 | - |
| `ChangeUserRoleServiceTest` | 단위 | 5개 |
| `AdminUserControllerTest` | 통합 | 13개 |

---

## 7. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-07 | 최초 작성 (PR #235 리팩토링 시 추가) |
