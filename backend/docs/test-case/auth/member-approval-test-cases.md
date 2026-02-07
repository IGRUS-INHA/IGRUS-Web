# 준회원 승인/거절 테스트 케이스

**작성일**: 2026-01-23
**버전**: 1.1
**관련 스펙**: [auth-spec.md](../../../../docs/feature/auth/auth-spec.md)
**우선순위**: P2

---

## 1. 개요

준회원 승인/거절 기능에 대한 테스트 케이스입니다. 관리자(ADMIN)가 준회원을 정회원으로 승인하거나 부적합한 가입 요청을 거절하는 과정을 검증합니다. 거절된 사용자는 ASSOCIATE 역할이 유지되며, 승인 대기 목록에서만 제외됩니다.

---

## 2. 테스트 케이스

### 2.1 준회원 목록 조회

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| APR-001 | 관리자 준회원 목록 조회 성공 | ADMIN 역할로 로그인 | 승인 대기 준회원 목록 조회 요청 | 준회원 목록 반환 (학번, 본명, 학과, 가입 동기 등 상세 정보 포함) | ✅ |
| APR-002 | 준회원 상세 정보 표시 | 준회원 목록 조회 성공 | 각 준회원 정보 확인 | 학번, 본명, 학과, 가입 동기, 가입일 등 표시 | ✅ |
| APR-003 | 준회원이 없는 경우 빈 목록 | 승인 대기 준회원 없음 | 준회원 목록 조회 요청 | 빈 목록 반환 (적절한 메시지 표시) | ✅ |
| APR-004 | 목록 페이지네이션 | 다수의 준회원 존재 | 페이지별 목록 조회 | 페이지네이션 적용된 목록 반환 | ✅ |
| APR-005 | 거절된 준회원은 승인 대기 목록에서 제외 | 거절된 준회원 존재 | 승인 대기 목록 조회 | 거절된 준회원은 목록에 미포함 | ✅ |
| APR-006 | 승인된 준회원은 승인 대기 목록에서 제외 | 승인된 준회원 존재 | 승인 대기 목록 조회 | 승인된 준회원은 목록에 미포함 | ✅ |

### 2.2 개별 승인

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| APR-010 | 관리자 개별 승인 성공 | ADMIN 역할로 로그인, 준회원 존재 | 특정 준회원 선택 후 승인 버튼 클릭 | 해당 사용자 역할이 MEMBER로 변경, AssociateDecision에 APPROVED 기록 | ✅ |
| APR-011 | 승인 후 역할 변경 확인 | 개별 승인 완료 | 승인된 사용자로 로그인 | 역할이 MEMBER로 표시 | ✅ |
| APR-012 | 승인 결정 기록 | 개별 승인 완료 | AssociateDecision 확인 | APPROVED 타입, 승인자 ID, 승인일시 기록 | ✅ |
| APR-013 | 역할 변경 감사 이력 기록 | 개별 승인 완료 | UserRoleHistory 확인 | ASSOCIATE → MEMBER 변경 이력 기록 | ✅ |

### 2.3 일괄 승인

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| APR-020 | 다수 준회원 일괄 승인 성공 | ADMIN 역할로 로그인, 5명의 준회원 존재 | 5명 모두 선택 후 일괄 승인 실행 | 5명 모두 MEMBER로 변경 | ✅ |
| APR-021 | 일부 준회원 선택 후 일괄 승인 | 10명의 준회원 존재 | 3명 선택 후 일괄 승인 | 선택된 3명만 MEMBER로 변경, 나머지 7명은 ASSOCIATE 유지 | ✅ |
| APR-022 | 일괄 승인 시 각각 AssociateDecision 기록 | 일괄 승인 완료 | 각 사용자 AssociateDecision 확인 | 각 사용자별 APPROVED 결정 개별 기록 | ✅ |
| APR-023 | 일괄 승인 시 역할 변경 이력 개별 기록 | 일괄 승인 완료 | UserRoleHistory 확인 | 각 사용자별 역할 변경 이력 개별 기록 | ✅ |
| APR-024 | 선택 없이 일괄 승인 시도 | 아무도 선택하지 않음 | 일괄 승인 버튼 클릭 | 일괄 승인 버튼 비활성화 또는 경고 메시지 | ✅ |

### 2.4 권한 검증

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| APR-030 | 운영진 승인 시도 시 거부 | OPERATOR 역할로 로그인 | 준회원 승인 시도 | "관리자 권한이 필요합니다" 메시지 표시, 403 Forbidden | ✅ |
| APR-031 | 정회원 승인 시도 시 거부 | MEMBER 역할로 로그인 | 준회원 승인 API 호출 시도 | 403 Forbidden | ✅ |
| APR-032 | 준회원 승인 시도 시 거부 | ASSOCIATE 역할로 로그인 | 준회원 승인 API 호출 시도 | 403 Forbidden | ✅ |
| APR-033 | 비로그인 상태 승인 시도 시 거부 | 비로그인 상태 | 준회원 승인 API 호출 시도 | 401 Unauthorized | ✅ |
| APR-034 | 운영진 목록 조회 시도 시 거부 | OPERATOR 역할로 로그인 | 준회원 목록 조회 시도 | 403 Forbidden | ✅ |

### 2.5 ADMIN 권한 보호

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| APR-040 | 마지막 ADMIN 권한 변경 시도 | 시스템에 ADMIN이 1명만 존재 | 해당 ADMIN 역할 변경 시도 | "마지막 관리자는 권한을 변경할 수 없습니다" 메시지 표시, 변경 거부 | ✅ |
| APR-041 | 여러 ADMIN 존재 시 권한 변경 가능 | 시스템에 ADMIN이 2명 이상 존재 | 한 명의 ADMIN 역할 변경 | 정상적으로 역할 변경됨 | ✅ |

### 2.6 개별 거절

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REJ-010 | 관리자 개별 거절 성공 | ADMIN 역할로 로그인, 준회원 존재 | 준회원 선택 후 거절 사유 입력, 거절 실행 | AssociateDecision에 REJECTED 기록, 거절 사유/거절자/거절일 저장 | ✅ |
| REJ-011 | 거절 후 역할 ASSOCIATE 유지 | 개별 거절 완료 | 거절된 사용자 역할 확인 | 역할이 ASSOCIATE로 유지됨 | ✅ |
| REJ-012 | 거절 사유 누락 시 400 반환 | ADMIN 역할로 로그인 | 거절 사유 없이 거절 시도 | 400 Bad Request 반환 | ✅ |
| REJ-013 | 존재하지 않는 사용자 거절 시 404 반환 | ADMIN 역할로 로그인 | 존재하지 않는 사용자 ID로 거절 시도 | 404 Not Found 반환 | ✅ |

### 2.7 일괄 거절

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REJ-020 | 다수 준회원 일괄 거절 성공 | ADMIN 역할로 로그인, 2명의 준회원 존재 | 2명 선택 후 거절 사유 입력, 일괄 거절 실행 | rejectedCount=2, failedCount=0, totalRequested=2 반환 | ✅ |
| REJ-021 | 부분 성공 시 결과 반환 | 유효/무효 사용자 혼합 | 혼합 목록으로 일괄 거절 | rejectedCount + failedCount = totalRequested | ✅ |
| REJ-022 | 빈 목록으로 일괄 거절 시 400 반환 | ADMIN 역할로 로그인 | 빈 목록으로 일괄 거절 시도 | 400 Bad Request 반환 | ✅ |
| REJ-023 | null 목록으로 일괄 거절 시 400 반환 | ADMIN 역할로 로그인 | null 목록으로 일괄 거절 시도 | 400 Bad Request 반환 | ✅ |

### 2.8 거절된 준회원 목록 조회

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REJ-030 | 관리자 거절 목록 조회 성공 | ADMIN 역할로 로그인, 거절된 준회원 존재 | 거절된 준회원 목록 조회 | 거절 사유, 거절일, 거절자 정보 포함 목록 반환 | ✅ |
| REJ-031 | 거절된 준회원이 없는 경우 빈 목록 | 거절된 준회원 없음 | 거절된 준회원 목록 조회 | 빈 목록 반환 | ✅ |
| REJ-032 | 일반 사용자 거절 목록 조회 시 403 반환 | MEMBER 역할로 로그인 | 거절된 준회원 목록 조회 시도 | 403 Forbidden | ✅ |

### 2.9 거절 권한 검증 및 엣지 케이스

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REJ-040 | 비관리자 거절 시도 시 AdminRequiredException | MEMBER 역할로 로그인 | 준회원 거절 시도 | AdminRequiredException 발생 | ✅ |
| REJ-041 | 존재하지 않는 처리자 ID로 거절 시도 | 존재하지 않는 처리자 ID | 준회원 거절 시도 | UserNotFoundException 발생 | ✅ |
| REJ-042 | MEMBER 사용자 거절 시도 시 UserNotAssociateException | ADMIN 역할로 로그인, MEMBER 사용자 존재 | MEMBER 사용자 거절 시도 | UserNotAssociateException 발생 | ✅ |
| REJ-043 | 이미 처리된 준회원 거절 시도 시 AssociateAlreadyDecidedException | 이미 거절/승인된 준회원 | 다시 거절 시도 | AssociateAlreadyDecidedException 발생 | ✅ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| FR-026 | 관리자(ADMIN) 승인 대기 준회원 목록 조회 (승인/거절된 준회원 제외) | APR-001 ~ APR-006 |
| FR-027 | 관리자(ADMIN)만 준회원 승인 가능 | APR-010 ~ APR-013, APR-030 ~ APR-034 |
| FR-028 | 개별 승인 및 일괄 승인 제공 | APR-010 ~ APR-024 |
| FR-037 | 관리자(ADMIN)만 준회원 거절 가능 (거절 사유 필수) | REJ-010 ~ REJ-013, REJ-040 ~ REJ-043 |
| FR-038 | 개별 거절 및 일괄 거절 제공 | REJ-010 ~ REJ-023 |
| FR-039 | 거절된 준회원 ASSOCIATE 역할 유지, 승인 대기 목록에서만 제외 | REJ-011, APR-005 |
| FR-040 | 거절된 준회원 목록 조회 기능 | REJ-030 ~ REJ-032 |
| Edge Case | 마지막 ADMIN 권한 변경 불가 | APR-040, APR-041 |

---

## 4. 예상 테스트 클래스 구조

```java
// 서비스 통합 테스트
class ApproveAssociateServiceTest {
    @Nested class IndividualApprovalTest { /* APR-010 ~ APR-013 */ }
    @Nested class AuthorizationTest { /* APR-030 ~ APR-031 */ }
    @Nested class EdgeCaseTest { /* MEMBER 승인 시도, 존재하지 않는 사용자 등 */ }
}

class BulkApproveAssociatesServiceTest {
    @Nested class BulkApprovalTest { /* APR-020 ~ APR-024 */ }
}

class RejectAssociateServiceTest {
    @Nested class IndividualRejectionTest { /* REJ-010 ~ REJ-011 */ }
    @Nested class AuthorizationTest { /* REJ-040 ~ REJ-041 */ }
    @Nested class EdgeCaseTest { /* REJ-042 ~ REJ-043 */ }
}

class BulkRejectAssociatesServiceTest {
    @Nested class BulkRejectionTest { /* REJ-020 ~ REJ-023 */ }
}

class GetRejectedAssociatesServiceTest {
    @Nested class GetRejectedTest { /* REJ-030 ~ REJ-031 */ }
}

// 컨트롤러 통합 테스트
class AdminMemberControllerTest {
    @Nested class GetPendingAssociatesTest { /* APR-001 ~ APR-006 */ }
    @Nested class ApproveAssociateTest { /* APR-010 ~ APR-013 */ }
    @Nested class BulkApprovalTest { /* APR-020 ~ APR-024 */ }
    @Nested class RejectAssociateTest { /* REJ-010 ~ REJ-013 */ }
    @Nested class BulkRejectionTest { /* REJ-020 ~ REJ-022 */ }
    @Nested class GetRejectedAssociatesTest { /* REJ-030 ~ REJ-032, APR-005 */ }
}
```

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-23 | - | 최초 작성 |
| 1.1 | 2026-02-07 | Claude | 준회원 거절 기능 테스트 케이스 추가 (REJ-010 ~ REJ-043), 승인 테스트 케이스 보정 (AssociateDecision 엔티티 반영), APR-005/APR-006 추가 |
