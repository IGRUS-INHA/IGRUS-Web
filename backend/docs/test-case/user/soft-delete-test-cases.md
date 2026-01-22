# Soft Delete 테스트 케이스

**작성일**: 2026-01-22
**버전**: 1.0
**관련 PRD**: [IGRUS_WEB_PRD_V2.md](../../../../docs/feature/common/IGRUS_WEB_PRD_V2.md)

---

## 1. 개요

Soft Delete 기능 관련 테스트 케이스를 정의합니다. 개인정보보호법 제21조에 따른 탈퇴 후 5일 이내 복구 기능 및 데이터 보존 정책을 지원합니다.

### 1.1 테스트 대상

| 클래스 | 설명 | 테스트 파일 |
|--------|------|------------|
| `SoftDeletableEntity` | Soft Delete 기능을 제공하는 추상 클래스 | `SoftDeletableEntityTest.java` |
| `UserRepository` | Soft Delete 필터링이 적용된 Repository | `UserRepositorySoftDeleteTest.java` |

### 1.2 PRD 관련 항목

- **탈퇴 후 복구 가능 기간**: 5일 이내 (PRD 11-15행, 355-358행)
- **개인정보 파기 기한**: 탈퇴일로부터 5일 이내 (PRD 21행, 362행)
- **게시글/댓글 삭제 처리**: Soft Delete, is_deleted=true (PRD 42행, 205-207행)

---

## 2. SoftDeletableEntity 테스트 케이스

**테스트 파일**: `src/test/java/igrus/web/common/domain/SoftDeletableEntityTest.java`

### 2.1 delete 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| SD-001 | 삭제 호출 시 deleted가 true로 변경되고 삭제 정보가 기록된다 | delete(deletedBy) 호출 시 deleted=true, deletedAt 설정, deletedBy 설정 | ✅ 구현됨 |
| SD-002 | 삭제자 ID 없이 삭제 호출 가능 | delete(null) 호출 시 deletedBy는 null로 유지 | ✅ 구현됨 |

### 2.2 restore 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| SD-003 | 복원 호출 시 deleted가 false로 변경되고 삭제 정보가 초기화된다 | restore() 호출 시 deleted=false, deletedAt=null, deletedBy=null | ✅ 구현됨 |
| SD-004 | 삭제되지 않은 엔티티에 복원 호출해도 정상 동작 | 이미 deleted=false인 엔티티에 restore() 호출해도 오류 없음 | ✅ 구현됨 |

### 2.3 isDeleted 메서드

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| SD-005 | 생성 직후에는 false 반환 | 새로 생성된 엔티티의 deleted 기본값은 false | ✅ 구현됨 |
| SD-006 | 삭제 후에는 true 반환 | delete() 호출 후 isDeleted()는 true | ✅ 구현됨 |
| SD-007 | 복원 후에는 false 반환 | restore() 호출 후 isDeleted()는 false | ✅ 구현됨 |

---

## 3. UserRepository Soft Delete 통합 테스트 케이스

**테스트 파일**: `src/test/java/igrus/web/user/repository/UserRepositorySoftDeleteTest.java`

### 3.1 기본 조회 (Soft Delete 필터링)

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| UR-001 | 삭제되지 않은 사용자는 findById로 조회 가능 | @SQLRestriction에 의해 deleted=false인 사용자만 조회 | ✅ 구현됨 |
| UR-002 | Soft Delete된 사용자는 findById로 조회되지 않음 | deleted=true인 사용자는 기본 조회에서 제외 | ✅ 구현됨 |
| UR-003 | Soft Delete된 사용자는 findByEmail로 조회되지 않음 | 이메일로 조회 시에도 Soft Delete 필터링 적용 | ✅ 구현됨 |
| UR-004 | Soft Delete된 사용자는 existsByEmail로 false 반환 | 존재 여부 확인 시에도 Soft Delete 필터링 적용 | ✅ 구현됨 |

### 3.2 삭제된 데이터 포함 조회

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| UR-005 | findByIdIncludingDeleted로 Soft Delete된 사용자도 조회 가능 | Native Query로 @SQLRestriction 우회 | ✅ 구현됨 |
| UR-006 | findByEmailIncludingDeleted로 Soft Delete된 사용자도 조회 가능 | 이메일로 삭제된 사용자 조회 (관리자/복구용) | ✅ 구현됨 |

### 3.3 복원 테스트

| TC-ID | 테스트명 | 설명 | 상태 |
|-------|---------|------|------|
| UR-007 | 복원된 사용자는 기본 조회로 다시 조회 가능 | restore() 후 deleted=false가 되어 기본 조회 가능 | ✅ 구현됨 |

---

## 4. Soft Delete 적용 엔티티 목록

| 엔티티 | 테이블명 | Soft Delete 컬럼 | 설명 |
|--------|----------|-----------------|------|
| `User` | `users` | `users_deleted` | 사용자 기본정보 |
| `PasswordCredential` | `password_credentials` | `password_credentials_deleted` | 인증 자격증명 |
| `Position` | `positions` | `positions_deleted` | 직책 |
| `UserPosition` | `user_positions` | - | 사용자-직책 연결 (현재 미적용) |

---

## 5. 추가 구현 예정 테스트 케이스

### 5.1 탈퇴 복구 관련 (PRD 섹션 7)

| TC-ID | 테스트명 | 설명 | PRD 참조 | 상태 |
|-------|---------|------|---------|------|
| SD-101 | 탈퇴 후 5일 이내 복구 가능 | withdrawnAt + 5일 이내면 복구 허용 | PRD 355행 | ⬜ 미구현 |
| SD-102 | 탈퇴 후 5일 경과 시 복구 불가 | withdrawnAt + 5일 초과면 복구 거부 | PRD 358행 | ⬜ 미구현 |
| SD-103 | 탈퇴한 학번으로 5일 이내 재가입 불가 | 재가입 제한 검증 | PRD 357행 | ⬜ 미구현 |
| SD-104 | 탈퇴 후 5일 경과 시 동일 학번 재가입 가능 | 재가입 허용 검증 | PRD 358행 | ⬜ 미구현 |

### 5.2 게시글/댓글 Soft Delete (PRD 섹션 4)

| TC-ID | 테스트명 | 설명 | PRD 참조 | 상태 |
|-------|---------|------|---------|------|
| SD-201 | 삭제된 게시글은 "삭제된 게시글입니다" 표시 | UI 응답 검증 | PRD 206행 | ⬜ 미구현 |
| SD-202 | 삭제된 게시글의 댓글은 유지됨 | 부모 글 삭제 시 댓글 보존 | PRD 207행 | ⬜ 미구현 |
| SD-203 | 대댓글이 있는 댓글 삭제 시 부모 댓글 표시 변경 | "삭제된 댓글입니다" 표시, 대댓글 유지 | PRD 224행 | ⬜ 미구현 |

### 5.3 개인정보 파기 관련 (PRD 섹션 7, 비기능 요구사항)

| TC-ID | 테스트명 | 설명 | PRD 참조 | 상태 |
|-------|---------|------|---------|------|
| SD-301 | 탈퇴 후 5일 경과 시 개인정보 영구 삭제 | 스케줄러에 의한 Hard Delete | PRD 362행 | ⬜ 미구현 |
| SD-302 | 파기 예외 항목(문의 내역)은 3년 보관 | 전자상거래법 준수 | PRD 365행 | ⬜ 미구현 |
| SD-303 | 파기 예외 항목(로그인 기록)은 3개월 보관 | 통신비밀보호법 준수 | PRD 366행 | ⬜ 미구현 |
| SD-304 | 탈퇴한 회원의 게시글/댓글은 익명화 처리 후 보존 | 작성자 연결 해제 | PRD 367행 | ⬜ 미구현 |

---

## 6. @SQLRestriction 동작 검증

### 6.1 JPA 조회 메서드별 필터링 여부

| 메서드 | Soft Delete 필터링 | 비고 |
|--------|-------------------|------|
| `findById()` | O | @SQLRestriction 적용 |
| `findByXxx()` | O | 파생 쿼리도 적용 |
| `existsByXxx()` | O | 존재 확인도 적용 |
| `findAll()` | O | 목록 조회도 적용 |
| `count()` | O | 카운트도 적용 |
| Native Query | X | @SQLRestriction 우회 가능 |

---

## 7. 테스트 실행 방법

```bash
# Soft Delete 관련 테스트만 실행
./gradlew test --tests "*SoftDelete*"

# SoftDeletableEntity 테스트 실행
./gradlew test --tests "igrus.web.common.domain.SoftDeletableEntityTest"

# UserRepository Soft Delete 통합 테스트 실행
./gradlew test --tests "igrus.web.user.repository.UserRepositorySoftDeleteTest"
```

---

## 8. 테스트 환경 설정

### 8.1 테스트 프로파일

통합 테스트는 `@ActiveProfiles("test")`를 사용하여 테스트 환경에서 실행됩니다.

### 8.2 데이터 정리

테스트 간 격리를 위해 `@BeforeEach`에서 Native Query로 모든 데이터 삭제:

```java
@BeforeEach
void setUp() {
    entityManager.createNativeQuery("DELETE FROM user_positions").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM user_role_history").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM password_credentials").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
    entityManager.flush();
    entityManager.clear();
}
```

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-22 | - | 최초 작성 |
