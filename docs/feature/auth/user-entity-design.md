# User 도메인 엔티티 설계 문서

**작성일**: 2026-01-22
**상태**: Implemented
**관련 PRD**: [IGRUS_WEB_PRD_V2.md](../common/IGRUS_WEB_PRD_V2.md)

---

## 개요

이 문서는 IGRUS Web 프로젝트의 User 도메인 엔티티 구현을 설명합니다. PRD의 데이터 모델 명세를 기반으로 구현되었으며, 3NF 정규화 원칙과 DDD(Domain-Driven Design) 패턴을 적용했습니다.

---

## 엔티티 구조

```
BaseEntity (공통 감사 필드)
    │
    ├── SoftDeletableEntity (소프트 삭제 지원)
    │       │
    │       ├── User (사용자 기본정보)
    │       ├── PasswordCredential (인증 자격증명)
    │       └── Position (직책)
    │
    ├── UserPosition (User-Position 중간 테이블)
    ├── UserRoleHistory (역할 변경 이력)
    └── UserSuspension (정지 이력)
```

---

## 공통 베이스 엔티티

### BaseEntity

모든 엔티티의 기본이 되는 추상 클래스로, JPA Auditing을 통한 생성/수정 이력을 자동 관리합니다.

**위치**: `igrus.web.common.domain.BaseEntity`

| 필드 | 타입 | 설명 | 비고 |
|------|------|------|------|
| createdAt | LocalDateTime | 생성일시 | 자동 설정, 수정 불가 |
| updatedAt | LocalDateTime | 수정일시 | 자동 갱신 |
| createdBy | Long | 생성자 ID | 자동 설정, 수정 불가 |
| updatedBy | Long | 수정자 ID | 자동 갱신 |

**특징**:
- `@EntityListeners(AuditingEntityListener.class)` 적용
- Spring Data JPA의 `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` 어노테이션 사용

---

### SoftDeletableEntity

소프트 삭제를 지원하는 엔티티의 베이스 클래스입니다.

**위치**: `igrus.web.common.domain.SoftDeletableEntity`

| 필드 | 타입 | 설명 | 비고 |
|------|------|------|------|
| deleted | boolean | 삭제 여부 | 기본값: false |
| deletedAt | LocalDateTime | 삭제일시 | nullable |
| deletedBy | Long | 삭제자 ID | nullable |

**주요 메서드**:
- `delete(Long deletedBy)`: 소프트 삭제 수행
- `restore()`: 삭제 복구
- `isDeleted()`: 삭제 여부 확인

**특징**:
- 하위 엔티티에서 `@SQLRestriction("deleted = false")` 사용하여 자동 필터링

---

## User 도메인 엔티티

### User

사용자 기본 정보를 관리하는 핵심 엔티티입니다.

**위치**: `igrus.web.user.domain.User`
**테이블**: `users`

| 필드 | 컬럼명 | 타입 | 제약조건 | 설명 |
|------|--------|------|----------|------|
| id | users_id | Long | PK, Auto | 사용자 ID |
| studentId | users_student_id | String(8) | Unique, Not Null | 학번 |
| name | users_name | String(50) | Not Null | 본명 |
| email | users_email | String | Unique, Not Null | 이메일 |
| phoneNumber | users_phone_number | String(20) | Unique, Not Null | 전화번호 |
| department | users_department | String(50) | Not Null | 학과 |
| motivation | users_motivation | TEXT | Not Null | 가입 동기 |
| role | users_role | Enum(UserRole) | Not Null | 역할 (기본: ASSOCIATE) |
| userPositions | - | List\<UserPosition\> | - | 직책 목록 (1:N) |

**설계 결정사항**:

1. **role을 User에 배치한 이유** (PRD 참조):
   - role은 "조직 내 사용자의 위치"를 나타내는 프로필 속성
   - 인증(Authentication)이 아닌 인가(Authorization) 개념
   - 대부분의 조회에서 User + role이 함께 필요하여 JOIN 불필요

2. **인증 정보 분리**:
   - 비밀번호, 계정 상태는 `PasswordCredential`로 분리
   - 보안 및 책임 분리 원칙 적용

**주요 메서드**:

```java
// 정적 팩토리 메서드
static User create(String studentId, String name, String email,
                   String phoneNumber, String department, String motivation)

// 역할 변경
void promoteToMember()      // ASSOCIATE → MEMBER
void promoteToOperator()    // → OPERATOR
void promoteToAdmin()       // → ADMIN
void demoteToMember()       // → MEMBER
void changeRole(UserRole role)

// 역할 확인
boolean isAdmin()
boolean isOperator()
boolean isOperatorOrAbove()
boolean isMember()
boolean isAssociate()

// 직책 관리
void addPosition(Position position)
void removePosition(Position position)
void clearPositions()
boolean hasPosition(Position position)
boolean hasAnyPosition()
List<Position> getPositions()

// 프로필 수정
void updateProfile(String name, String phoneNumber, String department)
void updateEmail(String email)
```

---

### PasswordCredential

비밀번호 기반 인증 정보를 관리합니다. PRD의 `UserAuth` 테이블에 대응합니다.

**위치**: `igrus.web.user.domain.PasswordCredential`
**테이블**: `password_credentials`

| 필드 | 컬럼명 | 타입 | 제약조건 | 설명 |
|------|--------|------|----------|------|
| id | password_credentials_id | Long | PK, Auto | ID |
| user | password_credentials_user_id | User | FK, Unique, Not Null | 사용자 (1:1) |
| passwordHash | password_credentials_password_hash | String | Not Null | BCrypt 암호화된 비밀번호 |
| status | password_credentials_status | Enum(UserStatus) | Not Null | 계정 상태 (기본: ACTIVE) |
| approvedAt | password_credentials_approved_at | LocalDateTime | nullable | 정회원 승인일 |
| approvedBy | password_credentials_approved_by | Long | nullable | 승인 처리자 ID |

**설계 결정사항**:

1. **User와 1:1 관계**:
   - 프로필 정보와 인증 자격증명 분리 (보안 및 책임 분리)
   - `@OneToOne(fetch = FetchType.LAZY)` 적용

2. **계정 상태 분리**:
   - 계정 상태는 인증 도메인에 귀속 (로그인 가능 여부 결정)

**주요 메서드**:

```java
// 정적 팩토리 메서드
static PasswordCredential create(User user, String passwordHash)

// 비밀번호 관리
void changePassword(String newPasswordHash)

// 계정 상태 관리
void activate()    // → ACTIVE
void suspend()     // → SUSPENDED
void withdraw()    // → WITHDRAWN

// 상태 확인
boolean isActive()
boolean isSuspended()
boolean isWithdrawn()

// 정회원 승인
void approve(Long approverId)
boolean isApproved()
```

---

### Position

동아리 내 직책 정보를 관리합니다.

**위치**: `igrus.web.user.domain.Position`
**테이블**: `positions`

| 필드 | 컬럼명 | 타입 | 제약조건 | 설명 |
|------|--------|------|----------|------|
| id | positions_id | Long | PK, Auto | 직책 ID |
| name | positions_name | String(20) | Unique, Not Null | 직책명 |
| imageUrl | positions_image_url | String | nullable | 직책 이미지 URL |
| displayOrder | positions_display_order | Integer | nullable | 표시 순서 |
| userPositions | - | List\<UserPosition\> | - | 이 직책을 가진 사용자들 |

**사용 예시**:
- 기술부, 기술부장, 회장, 부회장 등

**주요 메서드**:

```java
// 정적 팩토리 메서드
static Position create(String name, String imageUrl, Integer displayOrder)

// 수정 메서드
void updateName(String name)
void updateImageUrl(String imageUrl)
void updateDisplayOrder(Integer displayOrder)
void update(String name, String imageUrl, Integer displayOrder)

// 조회 메서드
List<User> getUsers()
```

---

### UserPosition

User와 Position의 다대다 관계를 위한 중간 테이블입니다.

**위치**: `igrus.web.user.domain.UserPosition`
**테이블**: `user_positions`

| 필드 | 컬럼명 | 타입 | 제약조건 | 설명 |
|------|--------|------|----------|------|
| id | user_positions_id | Long | PK, Auto | ID |
| user | user_positions_user_id | User | FK, Not Null | 사용자 |
| position | user_positions_position_id | Position | FK, Not Null | 직책 |
| assignedAt | user_positions_assigned_at | LocalDateTime | Not Null | 직책 부여일 |

**특징**:
- BaseEntity 상속 (SoftDeletableEntity 아님)
- 한 유저가 여러 직책 보유 가능 (예: 기술부장 + 기술부)

**주요 메서드**:

```java
// 정적 팩토리 메서드
static UserPosition create(User user, Position position)
static UserPosition create(User user, Position position, LocalDateTime assignedAt)
```

---

### UserRoleHistory

사용자 역할 변경 이력을 기록합니다.

**위치**: `igrus.web.user.domain.UserRoleHistory`
**테이블**: `user_role_history`

| 필드 | 컬럼명 | 타입 | 제약조건 | 설명 |
|------|--------|------|----------|------|
| id | user_role_history_id | Long | PK, Auto | ID |
| user | user_role_history_user_id | User | FK, Not Null | 사용자 |
| previousRole | user_role_history_previous_role | Enum(UserRole) | Not Null | 이전 역할 |
| newRole | user_role_history_new_role | Enum(UserRole) | Not Null | 새 역할 |
| reason | user_role_history_reason | String | nullable | 변경 사유 |

**인덱스**:
- `idx_user_role_history_user_id`: 사용자 ID
- `idx_user_role_history_new_role`: 새 역할
- `idx_user_role_history_created_at`: 생성일시
- `idx_user_role_history_created_by`: 생성자

**주요 메서드**:

```java
// 정적 팩토리 메서드 (같은 역할 변경 시 SameRoleChangeException 발생)
static UserRoleHistory create(User user, UserRole previousRole, UserRole newRole, String reason)

// 역할 변경 유형 확인
boolean isPromotion()           // 승급 여부
boolean isDemotion()            // 강등 여부
boolean isPromotionToAdmin()    // 관리자로 승급
boolean isPromotionToOperator() // 운영진으로 승급
boolean isPromotionToMember()   // 정회원으로 승급
boolean isDemotionFromAdmin()   // 관리자에서 강등

// 특정 역할 변경 확인
boolean isChangeTo(UserRole targetRole)
boolean isChangeFrom(UserRole sourceRole)

// 사유 관리
void updateReason(String reason)
boolean hasReason()
```

---

### UserSuspension

사용자 정지 이력을 관리합니다. PRD의 `UserSuspension` 테이블에 대응합니다.

**위치**: `igrus.web.user.domain.UserSuspension`
**테이블**: `user_suspensions`

| 필드 | 컬럼명 | 타입 | 제약조건 | 설명 |
|------|--------|------|----------|------|
| id | user_suspensions_id | Long | PK, Auto | ID |
| user | user_suspensions_user_id | User | FK, Not Null | 사용자 (N:1) |
| reason | user_suspensions_reason | String(500) | Not Null | 정지 사유 |
| suspendedAt | user_suspensions_suspended_at | LocalDateTime | Not Null | 정지 시작일 |
| suspendedUntil | user_suspensions_suspended_until | LocalDateTime | Not Null | 정지 종료일 |
| suspendedBy | user_suspensions_suspended_by | Long | Not Null | 정지 처리자 ID |
| liftedAt | user_suspensions_lifted_at | LocalDateTime | nullable | 해제일 |
| liftedBy | user_suspensions_lifted_by | Long | nullable | 해제 처리자 ID |

**설계 결정사항**:

1. **User와 1:N 관계**:
   - 한 사용자가 여러 번 정지될 수 있음 (정지 이력 관리)
   - PRD의 3NF 정규화 원칙에 따라 PasswordCredential.status와 분리

2. **정지 기간 관리**:
   - suspendedAt/suspendedUntil로 정지 기간 명확히 정의
   - isActive() 메서드로 현재 유효한 정지 여부 확인

3. **해제 이력 관리**:
   - liftedAt/liftedBy로 정지 해제 정보 기록
   - 해제된 정지도 이력으로 보존

**인덱스**:
- `idx_user_suspensions_user_id`: 사용자 ID
- `idx_user_suspensions_suspended_at`: 정지 시작일
- `idx_user_suspensions_suspended_until`: 정지 종료일
- `idx_user_suspensions_lifted_at`: 해제일

**주요 메서드**:

```java
// 정적 팩토리 메서드
static UserSuspension create(User user, String reason, LocalDateTime suspendedUntil, Long suspendedBy)
static UserSuspension create(User user, String reason, LocalDateTime suspendedAt, LocalDateTime suspendedUntil, Long suspendedBy)

// 해제 관련
void lift(Long liftedBy)
void lift(LocalDateTime liftedAt, Long liftedBy)

// 상태 확인
boolean isLifted()              // 해제 여부
boolean isActive()              // 현재 유효한 정지 여부 (해제되지 않고 기간 내)
boolean isExpired()             // 정지 기간 만료 여부
boolean hasStarted()            // 정지 기간 시작 여부

// 관리 메서드
void updateReason(String reason)
void extendSuspension(LocalDateTime newSuspendedUntil)
```

---

## 열거형 (Enum)

### UserRole

사용자 역할을 정의합니다.

**위치**: `igrus.web.user.domain.UserRole`

| 값 | 설명 | 권한 수준 |
|----|------|----------|
| ASSOCIATE | 준회원 (가입 완료, 승인 대기) | 최소 |
| MEMBER | 정회원 (승인 완료) | 일반 |
| OPERATOR | 운영진 | 관리 |
| ADMIN | 관리자 | 최고 |

**특징**:
- `ordinal()` 값으로 권한 수준 비교 가능 (ASSOCIATE < MEMBER < OPERATOR < ADMIN)

---

### UserStatus

계정 상태를 정의합니다.

**위치**: `igrus.web.user.domain.UserStatus`

| 값 | 설명 | 로그인 가능 |
|----|------|------------|
| ACTIVE | 정상 | O |
| SUSPENDED | 정지됨 | X |
| WITHDRAWN | 탈퇴함 | X |

---

## Repository 인터페이스

### UserRepository

**위치**: `igrus.web.user.repository.UserRepository`

```java
public interface UserRepository extends JpaRepository<User, Long> {
}
```

### PasswordCredentialRepository

**위치**: `igrus.web.user.repository.PasswordCredentialRepository`

```java
public interface PasswordCredentialRepository extends JpaRepository<PasswordCredential, Long> {
}
```

### PositionRepository

**위치**: `igrus.web.user.repository.PositionRepository`

```java
public interface PositionRepository extends JpaRepository<Position, Long> {
}
```

### UserPositionRepository

**위치**: `igrus.web.user.repository.UserPositionRepository`

```java
public interface UserPositionRepository extends JpaRepository<UserPosition, Long> {
}
```

### UserRoleHistoryRepository

**위치**: `igrus.web.user.repository.UserRoleHistoryRepository`

```java
public interface UserRoleHistoryRepository extends JpaRepository<UserRoleHistory, Long> {
}
```

### UserSuspensionRepository

**위치**: `igrus.web.user.repository.UserSuspensionRepository`

```java
public interface UserSuspensionRepository extends JpaRepository<UserSuspension, Long> {

    // 사용자의 모든 정지 이력 조회 (최신순)
    List<UserSuspension> findByUserIdOrderBySuspendedAtDesc(Long userId);

    // 사용자의 현재 유효한 정지 조회
    Optional<UserSuspension> findActiveByUserId(Long userId, LocalDateTime now);

    // 사용자의 현재 유효한 정지 존재 여부 확인
    boolean existsActiveByUserId(Long userId, LocalDateTime now);

    // 만료되었지만 해제 처리되지 않은 정지 이력 조회
    List<UserSuspension> findExpiredButNotLifted(LocalDateTime now);
}
```

---

## ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────────┐
│                           users                                  │
├─────────────────────────────────────────────────────────────────┤
│ users_id (PK)                                                    │
│ users_student_id (UNIQUE)                                        │
│ users_name                                                       │
│ users_email (UNIQUE)                                             │
│ users_phone_number (UNIQUE)                                      │
│ users_department                                                 │
│ users_motivation                                                 │
│ users_role                                                       │
│ users_created_at, users_updated_at                              │
│ users_created_by, users_updated_by                              │
│ users_deleted, users_deleted_at, users_deleted_by               │
└─────────────────────────────────────────────────────────────────┘
          │                    │                     │
          │ 1:1               │ 1:N                 │ 1:N
          ▼                    ▼                     ▼
┌─────────────────────────┐  ┌───────────────────┐  ┌───────────────────┐
│  password_credentials   │  │  user_positions   │  │  user_suspensions │
├─────────────────────────┤  ├───────────────────┤  ├───────────────────┤
│ ...id (PK)              │  │ ...id (PK)        │  │ ...id (PK)        │
│ ...user_id (FK, UNIQUE) │  │ ...user_id (FK)   │  │ ...user_id (FK)   │
│ ...password_hash        │  │ ...position_id    │  │ ...reason         │
│ ...status               │  │ ...assigned_at    │  │ ...suspended_at   │
│ ...approved_at          │  │ (audit fields)    │  │ ...suspended_until│
│ ...approved_by          │  └───────────────────┘  │ ...suspended_by   │
│ (audit & soft delete)   │           │             │ ...lifted_at      │
└─────────────────────────┘           │ N:1         │ ...lifted_by      │
                                      ▼             │ (audit fields)    │
          │                 ┌───────────────────┐   └───────────────────┘
          │ 1:N             │    positions      │
          ▼                 ├───────────────────┤
┌─────────────────────────┐ │ ...id (PK)        │
│   user_role_history     │ │ ...name (UNIQUE)  │
├─────────────────────────┤ │ ...image_url      │
│ ...id (PK)              │ │ ...display_order  │
│ ...user_id (FK)         │ │ (audit/soft del)  │
│ ...previous_role        │ └───────────────────┘
│ ...new_role             │
│ ...reason               │
│ (audit fields)          │
└─────────────────────────┘
```

---

## PRD 대비 구현 현황

| PRD 항목 | 구현 상태 | 비고 |
|----------|----------|------|
| User 기본정보 | ✅ 완료 | `User` 엔티티 |
| UserAuth (인증 자격증명) | ✅ 완료 | `PasswordCredential` 엔티티 |
| UserSuspension (정지 이력) | ✅ 완료 | `UserSuspension` 엔티티 |
| UserRole 열거형 | ✅ 완료 | ASSOCIATE/MEMBER/OPERATOR/ADMIN |
| UserStatus 열거형 | ✅ 완료 | ACTIVE/SUSPENDED/WITHDRAWN |
| 역할 변경 이력 | ✅ 완료 | `UserRoleHistory` 엔티티 |
| Position (직책) | ✅ 완료 | `Position` 엔티티 |
| UserPosition (사용자-직책) | ✅ 완료 | `UserPosition` 중간 테이블 (다대다) |
| 소프트 삭제 | ✅ 완료 | `SoftDeletableEntity` 베이스 클래스 |

---

## 관련 테스트

테스트 코드 위치: `backend/src/test/java/igrus/web/user/`

- `UserTest.java`: User 엔티티 단위 테스트
- `UserPositionTest.java`: UserPosition 관계 테스트
- `UserRoleHistoryTest.java`: 역할 변경 이력 테스트
- `UserSuspensionTest.java`: 정지 이력 테스트

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-01-22 | 최초 작성 | - |
| 2026-01-22 | PRD 동기화 - title(칭호) 제거, Position/UserPosition 구현 현황 반영 | - |
| 2026-01-22 | UserSuspension(정지 이력) 엔티티 구현 및 문서화 | - |
