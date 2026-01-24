# 테스트 케이스 문서

**작성일**: 2026-01-22
**버전**: 1.0

---

## 1. 개요

이 디렉토리는 IGRUS-Web 백엔드의 테스트 케이스 문서를 포함합니다. 현재 구현된 테스트와 PRD 기반 향후 구현 예정 테스트를 정의합니다.

---

## 2. 문서 목록

| 문서 | 설명 | 관련 테스트 파일 |
|------|------|-----------------|
| [user-domain-test-cases.md](user/user-domain-test-cases.md) | User 도메인 관련 테스트 케이스 | `UserRoleHistoryTest.java` |
| [soft-delete-test-cases.md](user/soft-delete-test-cases.md) | Soft Delete 기능 테스트 케이스 | `SoftDeletableEntityTest.java`, `UserRepositorySoftDeleteTest.java` |
| [auth/README.md](auth/README.md) | 인증 기능 테스트 케이스 (회원가입, 로그인, 토큰 등) | 구현 예정 |

---

## 3. 테스트 현황 요약

### 3.1 구현된 테스트

| 카테고리 | 테스트 클래스 | 테스트 케이스 수 | 상태 |
|----------|--------------|-----------------|------|
| User 도메인 | `UserRoleHistoryTest` | 27개 | ✅ 완료 |
| Soft Delete | `SoftDeletableEntityTest` | 7개 | ✅ 완료 |
| Repository 통합 | `UserRepositorySoftDeleteTest` | 7개 | ✅ 완료 |
| 애플리케이션 | `IgrusWebApplicationTests` | 1개 | ✅ 완료 |
| **총계** | **4개 클래스** | **42개** | - |

### 3.2 구현 예정 테스트

| 카테고리 | 예상 테스트 케이스 수 | 우선순위 |
|----------|---------------------|---------|
| 인증 - 회원가입 | 26개 | P1 (높음) |
| 인증 - 로그인 | 18개 | P1 (높음) |
| 인증 - 토큰 갱신 | 15개 | P2 (높음) |
| 인증 - 비밀번호 재설정 | 15개 | P2 (높음) |
| 인증 - 탈퇴 계정 복구 | 15개 | P3 (중간) |
| 인증 - 준회원 승인 | 17개 | P2 (높음) |
| User 도메인 (추가) | 22개 | 높음 |
| PasswordCredential 도메인 | 11개 | 높음 |
| Position 도메인 | 5개 | 중간 |
| 비즈니스 규칙 | 14개 | 높음 |
| Soft Delete (추가) | 10개 | 중간 |
| **총계** | **약 168개** | - |

---

## 4. 테스트 실행 방법

### 4.1 전체 테스트 실행

```bash
cd backend
./gradlew test
```

### 4.2 특정 테스트 클래스 실행

```bash
# UserRoleHistory 테스트
./gradlew test --tests "igrus.web.user.domain.UserRoleHistoryTest"

# SoftDeletableEntity 테스트
./gradlew test --tests "igrus.web.common.domain.SoftDeletableEntityTest"

# Repository 통합 테스트
./gradlew test --tests "igrus.web.user.repository.UserRepositorySoftDeleteTest"
```

### 4.3 테스트 리포트 확인

테스트 실행 후 리포트는 다음 경로에서 확인할 수 있습니다:

```
backend/build/reports/tests/test/index.html
```

---

## 5. 테스트 코드 구조

```
backend/src/test/java/igrus/web/
├── IgrusWebApplicationTests.java        # 애플리케이션 컨텍스트 로드 테스트
├── common/
│   └── domain/
│       └── SoftDeletableEntityTest.java # Soft Delete 도메인 테스트
└── user/
    ├── domain/
    │   └── UserRoleHistoryTest.java     # 역할 변경 이력 도메인 테스트
    └── repository/
        └── UserRepositorySoftDeleteTest.java  # Soft Delete 통합 테스트
```

---

## 6. 테스트 작성 가이드

### 6.1 테스트 명명 규칙

- 메서드명: `methodName_stateUnderTest_expectedBehavior`
- @DisplayName: 한글로 테스트 목적 명시

```java
@Test
@DisplayName("유효한 역할 변경 시 UserRoleHistory 생성 성공")
void create_WithValidRoleChange_ReturnsUserRoleHistory() {
    // ...
}
```

### 6.2 테스트 구조

Given-When-Then 패턴 사용:

```java
@Test
void testExample() {
    // given - 테스트 준비
    User user = createTestUser();

    // when - 테스트 실행
    user.promoteToMember();

    // then - 결과 검증
    assertThat(user.getRole()).isEqualTo(UserRole.MEMBER);
}
```

### 6.3 @Nested 클래스 활용

메서드별로 테스트를 그룹화:

```java
@Nested
@DisplayName("create 메서드")
class CreateTest {
    // create 메서드 관련 테스트들
}

@Nested
@DisplayName("isPromotion 메서드")
class IsPromotionTest {
    // isPromotion 메서드 관련 테스트들
}
```

---

## 7. 관련 문서

- [IGRUS_WEB_PRD_V2.md](../../../docs/feature/common/IGRUS_WEB_PRD_V2.md) - 제품 요구사항 문서
- [backend/CLAUDE.md](../../CLAUDE.md) - 백엔드 개발 가이드

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-22 | - | 최초 작성 |
