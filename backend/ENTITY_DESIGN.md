# 엔티티 설계 계획서

**기준 문서**: IGRUS_WEB_PRD_V2.md
**작성일**: 2026-01-22

---

## 주요 변경점 (V1 → V2)

| 항목 | V1 | V2 |
|------|----|----|
| 가입 검증 | MemberList로 사전 검증 | 누구나 가입 가능, 관리자 승인 |
| 역할 체계 | MEMBER, ADMIN | ASSOCIATE, MEMBER, OPERATOR, ADMIN |
| User 테이블 | 단일 테이블 | User + UserAuth + UserSuspension 분리 |
| 회원가입 정보 | 학번, 이름, 비밀번호, 이메일 | + 전화번호, 학과, 가입 동기 |
| 이메일 인증 | 필요 | 불필요 |

---

## 패키지 구조

```
igrus.web/
├── user/
│   ├── domain/
│   │   ├── User.java
│   │   ├── UserAuth.java
│   │   ├── UserSuspension.java
│   │   ├── UserRole.java          (enum)
│   │   └── UserStatus.java        (enum)
│   ├── repository/
│   └── ...
│
├── auth/
│   ├── domain/
│   │   └── RefreshToken.java
│   └── ...
│
├── post/
│   ├── domain/
│   │   ├── Post.java
│   │   ├── PostImage.java
│   │   ├── Like.java
│   │   ├── Bookmark.java
│   │   └── BoardType.java         (enum)
│   └── ...
│
├── comment/
│   ├── domain/
│   │   └── Comment.java
│   └── ...
│
├── event/
│   ├── domain/
│   │   ├── Event.java
│   │   ├── EventRegistration.java
│   │   └── EventStatus.java       (enum)
│   └── ...
│
├── inquiry/
│   ├── domain/
│   │   ├── Inquiry.java
│   │   ├── InquiryAttachment.java
│   │   ├── InquiryMemo.java
│   │   ├── InquiryType.java       (enum)
│   │   └── InquiryStatus.java     (enum)
│   └── ...
│
└── common/
    └── ...
```

---

## 엔티티 목록

### 1. User (사용자 기본정보)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| studentId | String(8) | UK, Not Null | 학번 |
| name | String(50) | Not Null | 본명 |
| email | String | UK, Not Null | 이메일 |
| phone | String(20) | Not Null | 전화번호 |
| department | String(50) | Not Null | 학과 |
| motivation | Text | Not Null | 가입 동기 |
| role | Enum | Not Null, Default ASSOCIATE | 역할 |
| title | String | Nullable | 칭호 |
| createdAt | DateTime | Not Null | 가입일 |
| updatedAt | DateTime | Not Null | 수정일 |

**관계**:
- UserAuth (1:1)
- UserSuspension (1:N)
- Post (1:N)
- Comment (1:N)
- Like (1:N)
- Bookmark (1:N)
- EventRegistration (1:N)
- RefreshToken (1:N)

---

### 2. UserAuth (인증 자격증명)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| userId | Long | UK, FK, Not Null | User FK |
| passwordHash | String | Not Null | 비밀번호 해시 |
| status | Enum | Not Null, Default ACTIVE | 계정 상태 |
| approvedAt | DateTime | Nullable | 정회원 승인일 |
| approvedBy | Long | FK, Nullable | 승인 처리자 |
| createdAt | DateTime | Not Null | 생성일 |
| updatedAt | DateTime | Not Null | 수정일 |

---

### 3. UserSuspension (정지 이력)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| userId | Long | FK, Not Null | User FK |
| reason | String | Not Null | 정지 사유 |
| suspendedAt | DateTime | Not Null | 정지 시작일 |
| suspendedUntil | DateTime | Not Null | 정지 종료일 |
| suspendedBy | Long | FK, Not Null | 정지 처리자 |
| liftedAt | DateTime | Nullable | 해제일 |
| liftedBy | Long | FK, Nullable | 해제 처리자 |

---

### 4. RefreshToken

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| token | String | UK, Not Null | 토큰 값 |
| userId | Long | FK, Not Null | User FK |
| expiresAt | DateTime | Not Null | 만료일 |
| createdAt | DateTime | Not Null | 생성일 |

---

### 5. Post (게시글)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| board | Enum | Not Null | 게시판 종류 |
| title | String(100) | Not Null | 제목 |
| content | Text | Not Null | 내용 |
| authorId | Long | FK, Not Null | 작성자 |
| isAnonymous | Boolean | Not Null, Default false | 익명 여부 |
| isQuestion | Boolean | Not Null, Default false | 질문 태그 |
| isVisibleToAssociate | Boolean | Not Null, Default false | 준회원 공개 (공지사항만) |
| viewCount | Integer | Not Null, Default 0 | 조회수 |
| isDeleted | Boolean | Not Null, Default false | 삭제 여부 |
| createdAt | DateTime | Not Null | 작성일 |
| updatedAt | DateTime | Not Null | 수정일 |

---

### 6. PostImage (게시글 이미지)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| postId | Long | FK, Not Null | Post FK |
| url | String | Not Null | 이미지 URL |
| displayOrder | Integer | Not Null | 순서 |

---

### 7. Comment (댓글)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| postId | Long | FK, Not Null | Post FK |
| parentId | Long | FK, Nullable | 부모 댓글 (대댓글용) |
| authorId | Long | FK, Not Null | 작성자 |
| content | String(500) | Not Null | 내용 |
| isAnonymous | Boolean | Not Null, Default false | 익명 여부 |
| isDeleted | Boolean | Not Null, Default false | 삭제 여부 |
| createdAt | DateTime | Not Null | 작성일 |

---

### 8. Like (좋아요)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| postId | Long | FK, Not Null | Post FK |
| userId | Long | FK, Not Null | User FK |
| createdAt | DateTime | Not Null | 생성일 |

**제약**: (postId, userId) Unique

---

### 9. Bookmark (북마크)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| postId | Long | FK, Not Null | Post FK |
| userId | Long | FK, Not Null | User FK |
| createdAt | DateTime | Not Null | 생성일 |

**제약**: (postId, userId) Unique

---

### 10. Event (행사)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| title | String(100) | Not Null | 제목 |
| description | Text | Not Null | 설명 |
| startDatetime | DateTime | Not Null | 시작 일시 |
| endDatetime | DateTime | Not Null | 종료 일시 |
| location | String(200) | Not Null | 장소 |
| capacity | Integer | Not Null | 정원 |
| registrationDeadline | DateTime | Not Null | 신청 마감일 |
| status | Enum | Not Null, Default UPCOMING | 행사 상태 |
| createdAt | DateTime | Not Null | 생성일 |
| updatedAt | DateTime | Not Null | 수정일 |

---

### 11. EventRegistration (행사 신청)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| eventId | Long | FK, Not Null | Event FK |
| userId | Long | FK, Not Null | User FK |
| createdAt | DateTime | Not Null | 신청일 |

**제약**: (eventId, userId) Unique

---

### 12. Inquiry (문의)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| inquiryNumber | String | UK, Not Null | 문의 번호 |
| type | Enum | Not Null | 문의 유형 |
| title | String(100) | Not Null | 제목 |
| content | Text | Not Null | 내용 |
| email | String | Not Null | 이메일 |
| userId | Long | FK, Nullable | User FK (비로그인 시 null) |
| status | Enum | Not Null, Default PENDING | 처리 상태 |
| createdAt | DateTime | Not Null | 작성일 |
| updatedAt | DateTime | Not Null | 수정일 |

---

### 13. InquiryAttachment (문의 첨부파일)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| inquiryId | Long | FK, Not Null | Inquiry FK |
| url | String | Not Null | 파일 URL |

---

### 14. InquiryMemo (문의 내부 메모)

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 기본키 |
| inquiryId | Long | FK, Not Null | Inquiry FK |
| adminId | Long | FK, Not Null | 관리자 FK |
| content | Text | Not Null | 메모 내용 |
| createdAt | DateTime | Not Null | 작성일 |

---

## Enum 목록

### UserRole
```java
ASSOCIATE,   // 준회원 (가입 완료, 승인 대기)
MEMBER,      // 정회원 (승인 완료)
OPERATOR,    // 운영진
ADMIN        // 관리자
```

### UserStatus
```java
ACTIVE,      // 정상
SUSPENDED,   // 정지
WITHDRAWN    // 탈퇴
```

### BoardType
```java
NOTICE,      // 공지사항
GENERAL,     // 자유게시판
INSIGHT      // 정보공유
```

### EventStatus
```java
UPCOMING,    // 예정
ONGOING,     // 진행중
CLOSED,      // 마감
CANCELED     // 취소
```

### InquiryType
```java
JOIN,        // 가입문의
EVENT,       // 행사문의
REPORT,      // 신고
ACCOUNT,     // 계정문의
OTHER        // 기타
```

### InquiryStatus
```java
PENDING,     // 접수
IN_PROGRESS, // 처리중
COMPLETED    // 완료
```

---

## 구현 우선순위

### Phase 1: 인증/회원 (로그인 기능)
1. UserRole (enum)
2. UserStatus (enum)
3. User
4. UserAuth
5. RefreshToken
6. UserRepository
7. UserAuthRepository
8. RefreshTokenRepository

### Phase 2: 게시판
9. BoardType (enum)
10. Post
11. PostImage
12. Comment
13. Like
14. Bookmark

### Phase 3: 행사
15. EventStatus (enum)
16. Event
17. EventRegistration

### Phase 4: 문의
18. InquiryType (enum)
19. InquiryStatus (enum)
20. Inquiry
21. InquiryAttachment
22. InquiryMemo

### Phase 5: 관리
23. UserSuspension

---

## 삭제 대상

V2에서 더 이상 필요 없는 항목:
- ~~MemberList~~ (사전 검증 제거됨)
- ~~emailVerified~~ (이메일 인증 불필요)

---

## 다음 단계

1. 기존 User.java, MemberList.java 삭제/수정
2. Phase 1 엔티티부터 순차 구현
3. Repository 생성
4. JWT/Security 설정
5. AuthService, AuthController 구현
