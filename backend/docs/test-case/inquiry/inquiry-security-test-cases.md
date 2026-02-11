# 문의 보안/RBAC (Inquiry Security) 테스트 케이스

**작성일**: 2026-02-11
**버전**: 1.0
**관련 스펙**: [inquiry-verification-criteria.md](../../../../docs/criteria/inquiry-verification-criteria.md)
**우선순위**: P2

---

## 1. 개요

문의 도메인의 컨트롤러 레벨 보안 테스트 케이스이다. Spring Security의 `@PreAuthorize` 어노테이션 기반 RBAC 접근 제어, 소유권 검증, 비밀번호 보안, 내부 데이터 노출 방지를 검증한다.

**현재 상태**: 컨트롤러 레벨 테스트가 전무한 상태 (GAP-INQ-04). 이 문서의 모든 테스트 케이스가 구현되면 GAP-INQ-04가 해소된다.

**테스트 인프라**:
- `@WebMvcTest` 또는 `ControllerIntegrationTestBase` (MockMvc)
- 역할별 인증 토큰 생성
- 각 엔드포인트의 HTTP 상태 코드 검증

**검증 기준서 참조**: 섹션 6 (권한/보안 정책), RBAC 매트릭스

---

## 2. 테스트 케이스

### 2.1 공개 API 접근 (비인증 허용)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-SEC-001 | 비인증 사용자의 비회원 문의 생성 | JWT 토큰 없음 | POST /api/v1/inquiries/guest (유효한 body) | 201 Created (공개 API) | ⬜ |
| INQ-SEC-002 | 비인증 사용자의 비회원 문의 조회 | JWT 토큰 없음 | POST /api/v1/inquiries/lookup (유효한 body) | 200 OK (공개 API) | ⬜ |

### 2.2 비인증 접근 차단 (401 Unauthorized)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-SEC-010 | 비인증 - 회원 문의 생성 | JWT 토큰 없음 | POST /api/v1/inquiries/member | 401 Unauthorized | ⬜ |
| INQ-SEC-011 | 비인증 - 내 문의 목록 조회 | JWT 토큰 없음 | GET /api/v1/inquiries/my | 401 Unauthorized | ⬜ |
| INQ-SEC-012 | 비인증 - 내 문의 상세 조회 | JWT 토큰 없음 | GET /api/v1/inquiries/my/1 | 401 Unauthorized | ⬜ |
| INQ-SEC-013 | 비인증 - 전체 문의 목록 | JWT 토큰 없음 | GET /api/v1/inquiries | 401 Unauthorized | ⬜ |
| INQ-SEC-014 | 비인증 - 답변 작성 | JWT 토큰 없음 | POST /api/v1/inquiries/1/reply | 401 Unauthorized | ⬜ |
| INQ-SEC-015 | 비인증 - 문의 삭제 | JWT 토큰 없음 | DELETE /api/v1/inquiries/1 | 401 Unauthorized | ⬜ |

### 2.3 일반 회원 접근 제한 (403 Forbidden) (GAP-INQ-04)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-SEC-020 | ASSOCIATE - 전체 문의 목록 조회 | ASSOCIATE JWT | GET /api/v1/inquiries | 403 Forbidden (SEC-INQ-04) | ⬜ |
| INQ-SEC-021 | MEMBER - 전체 문의 목록 조회 | MEMBER JWT | GET /api/v1/inquiries | 403 Forbidden (SEC-INQ-04) | ⬜ |
| INQ-SEC-022 | ASSOCIATE - 문의 상세 조회 (관리자) | ASSOCIATE JWT | GET /api/v1/inquiries/1 | 403 Forbidden | ⬜ |
| INQ-SEC-023 | MEMBER - 답변 작성 | MEMBER JWT | POST /api/v1/inquiries/1/reply | 403 Forbidden (SEC-INQ-05) | ⬜ |
| INQ-SEC-024 | ASSOCIATE - 답변 수정 | ASSOCIATE JWT | PUT /api/v1/inquiries/1/reply | 403 Forbidden | ⬜ |
| INQ-SEC-025 | MEMBER - 상태 변경 | MEMBER JWT | PUT /api/v1/inquiries/1/status | 403 Forbidden | ⬜ |
| INQ-SEC-026 | ASSOCIATE - 내부 메모 작성 | ASSOCIATE JWT | POST /api/v1/inquiries/1/memo | 403 Forbidden | ⬜ |
| INQ-SEC-027 | MEMBER - 문의 삭제 | MEMBER JWT | DELETE /api/v1/inquiries/1 | 403 Forbidden (SEC-INQ-06) | ⬜ |

### 2.4 관리자 접근 허용 (OPERATOR/ADMIN)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-SEC-030 | OPERATOR - 전체 문의 목록 조회 | OPERATOR JWT, 문의 존재 | GET /api/v1/inquiries | 200 OK | ⬜ |
| INQ-SEC-031 | ADMIN - 전체 문의 목록 조회 | ADMIN JWT, 문의 존재 | GET /api/v1/inquiries | 200 OK | ⬜ |
| INQ-SEC-032 | OPERATOR - 답변 작성 | OPERATOR JWT, 답변 없는 문의 | POST /api/v1/inquiries/{id}/reply | 201 Created | ⬜ |
| INQ-SEC-033 | ADMIN - 문의 삭제 | ADMIN JWT, 문의 존재 | DELETE /api/v1/inquiries/{id} | 204 No Content | ⬜ |
| INQ-SEC-034 | OPERATOR - 상태 변경 | OPERATOR JWT, PENDING 문의 | PUT /api/v1/inquiries/{id}/status | 200 OK | ⬜ |

### 2.5 인증된 사용자 API 접근

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-SEC-035 | ASSOCIATE - 회원 문의 생성 | ASSOCIATE JWT | POST /api/v1/inquiries/member (유효한 body) | 201 Created | ⬜ |
| INQ-SEC-036 | MEMBER - 회원 문의 생성 | MEMBER JWT | POST /api/v1/inquiries/member (유효한 body) | 201 Created | ⬜ |
| INQ-SEC-037 | OPERATOR - 회원 문의 생성 | OPERATOR JWT | POST /api/v1/inquiries/member (유효한 body) | 201 Created | ⬜ |
| INQ-SEC-038 | ADMIN - 회원 문의 생성 | ADMIN JWT | POST /api/v1/inquiries/member (유효한 body) | 201 Created | ⬜ |

### 2.6 소유권 검증 (Ownership Verification)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-SEC-040 | 회원 A가 본인 문의 상세 조회 | 회원A의 문의 존재 | GET /my/{id} (회원A JWT) | 200 OK (SEC-INQ-01) | ⬜ |
| INQ-SEC-041 | 회원 B가 회원 A의 문의 상세 조회 | 회원A의 문의 존재 | GET /my/{id} (회원B JWT) | 403 Forbidden, InquiryAccessDeniedException (SEC-INQ-01) | ⬜ |
| INQ-SEC-042 | 비회원 이메일 불일치로 조회 | GuestInquiry(email=a@t.com) | POST /lookup (email=b@t.com) | 404 Not Found (SEC-INQ-02, 존재 여부 미노출) | ⬜ |
| INQ-SEC-043 | 비회원 비밀번호 불일치로 조회 | GuestInquiry(password=correct) | POST /lookup (password=wrong) | 401 Unauthorized (SEC-INQ-03) | ⬜ |
| INQ-SEC-044 | 회원 A의 문의가 회원 B 목록에 미포함 | 회원A 문의 2건, 회원B 문의 1건 | GET /my (회원B JWT) | 회원B의 1건만 반환, 회원A 문의 미포함 | ⬜ |

### 2.7 보안 검증 (Security Assurance)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INQ-SEC-050 | 비회원 비밀번호 BCrypt 해싱 확인 | - | 비회원 문의 생성 후 DB의 passwordHash 확인 | BCrypt 형식($2a$...)으로 저장, 원문과 불일치 (INQ-INV-05) | ⬜ |
| INQ-SEC-051 | 비회원 조회 응답에 내부 메모 미포함 | 메모 작성된 비회원 문의 | POST /lookup (정상 인증) | 응답 JSON에 memos 필드 없음 | ⬜ |
| INQ-SEC-052 | 회원 상세 조회 응답에 내부 메모 미포함 | 메모 작성된 회원 문의 | GET /my/{id} (본인 JWT) | 응답 JSON에 memos 필드 없음 | ⬜ |
| INQ-SEC-053 | soft delete된 문의 접근 불가 | soft delete된 문의 | GET /my/{id} 또는 GET /inquiries/{id} | 404 Not Found (INQ-INV-04) | ⬜ |
| INQ-SEC-054 | 비인가 접근 시 DB 부작용 없음 | PENDING 문의, MEMBER JWT | PUT /inquiries/{id}/status (403 예상) | DB에 상태 변경 없음 (SEC-INQ-07) | ⬜ |
| INQ-SEC-055 | 비인가 삭제 시도 후 문의 존재 확인 | 문의 존재, ASSOCIATE JWT | DELETE /inquiries/{id} (403 예상) | 문의 여전히 존재, deleted=false (SEC-INQ-07) | ⬜ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| INQ-INV-04 | Soft delete 문의 접근 제외 | INQ-SEC-053 |
| INQ-INV-05 | 비회원 비밀번호 해시 필수 | INQ-SEC-050 |
| SEC-INQ-01 | 다른 사용자의 문의 접근 거부 | INQ-SEC-041, INQ-SEC-044 |
| SEC-INQ-02 | 이메일 불일치 시 존재 여부 미노출 | INQ-SEC-042 |
| SEC-INQ-03 | 비밀번호 불일치 시 인증 실패 | INQ-SEC-043 |
| SEC-INQ-04 | ASSOCIATE/MEMBER의 관리자 API 접근 차단 | INQ-SEC-020~022 |
| SEC-INQ-05 | ASSOCIATE/MEMBER의 답변 작성 차단 | INQ-SEC-023~024 |
| SEC-INQ-06 | ASSOCIATE/MEMBER의 문의 삭제 차단 | INQ-SEC-027 |
| SEC-INQ-07 | 비인가 접근 시 부작용 없음 | INQ-SEC-054~055 |
| GAP-INQ-04 | 컨트롤러 RBAC 검증 테스트 부재 | INQ-SEC-010~034 (전체) |

---

## 4. 구현된 테스트 클래스

### 4.1 컨트롤러 테스트

**현재 미구현** - 컨트롤러 레벨 테스트가 전무한 상태이다 (GAP-INQ-04).

### 4.2 서비스 테스트에서의 간접 검증
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/read/GetMyInquiryServiceTest.java`
  - INQ-SEC-041의 서비스 레벨 검증 (접근 거부)
- **파일**: `backend/src/test/java/igrus/web/inquiry/service/read/LookupGuestInquiryServiceTest.java`
  - INQ-SEC-043의 서비스 레벨 검증 (비밀번호 불일치)

### 4.3 구현 권장 사항

컨트롤러 테스트 구현 시 참고할 기존 패턴:

```java
// ControllerIntegrationTestBase 활용
@AutoConfigureMockMvc
@SpringBootTest
class InquiryControllerSecurityTest extends ControllerIntegrationTestBase {

    @Nested
    @DisplayName("비인증 접근 차단")
    class UnauthenticatedAccessTest {
        // INQ-SEC-010 ~ INQ-SEC-015
    }

    @Nested
    @DisplayName("일반 회원 접근 제한")
    class MemberAccessRestrictionTest {
        // INQ-SEC-020 ~ INQ-SEC-027
    }

    @Nested
    @DisplayName("관리자 접근 허용")
    class AdminAccessPermissionTest {
        // INQ-SEC-030 ~ INQ-SEC-034
    }
}
```

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-11 | - | 검증 기준서 기반 최초 작성 |
