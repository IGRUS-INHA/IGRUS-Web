# 아이그루스 시스템 명세서

---

## Clarifications

### Session 2026-01-22
- Q: 사용자 계정이 정지/탈퇴될 때 활성 세션(토큰) 처리 방식? → A: 즉시 무효화 - 정지/탈퇴 시 모든 활성 토큰 즉시 폐기
- Q: 게시글/댓글 삭제 처리 방식? → A: Soft Delete - is_deleted=true로 표시, DB 보존, UI에서 "삭제된 게시글/댓글입니다" 표시
- Q: 이메일 발송 실패 시 처리 방식? → A: 요청 성공 + 재시도 - 요청은 성공, 이메일은 백그라운드 재시도, "이메일 재발송" 버튼 제공

---

## 기능 명세

### 회원가입
- 학번(8자리) + 본명으로 MemberList 검증
- 미등록자는 가입 불가 → 문의하기 안내
- 동일 학번 중복 가입 불가
- 비밀번호: 영문+숫자+특수문자 8자 이상
- 이메일 본인 인증 (인증 링크 발송)
- 이메일 발송 실패 시: 가입 요청은 성공, 백그라운드 재시도, "이메일 재발송" 버튼 제공

### 로그인/인증
- 학번 + 비밀번호 로그인
- JWT: Access Token 1시간, Refresh Token 7일
- 비밀번호 재설정: 이메일 링크 발송 (30분 유효)
- 재설정 이메일 발송 실패 시: 요청은 성공, 백그라운드 재시도, "이메일 재발송" 버튼 제공

### 계정 상태
| 상태 | 설명 |
|------|------|
| Active | 정상 이용 |
| Inactive | 명단 미포함, 로그인 불가 |
| Suspended | 관리자 정지, 기존 활성 토큰 즉시 무효화 |
| Withdrawn | 본인 탈퇴, 기존 활성 토큰 즉시 무효화 |

### 게시판

| 게시판 | 작성 권한 | 익명 |
|--------|----------|------|
| 공지사항 (notices) | 관리자 | X |
| 자유게시판 (general) | 회원 | O |
| 정보공유 (insight) | 회원 | X |

**게시글**
- 제목 100자, 이미지 최대 5개(각 10MB)
- 익명 설정은 수정 불가
- 자유게시판에서 "질문으로 등록" 옵션 선택 가능
- 삭제 시 Soft Delete (is_deleted=true), UI에 "삭제된 게시글입니다" 표시

**댓글**
- 최대 500자
- 대댓글 1단계까지
- 삭제 시 Soft Delete (is_deleted=true), UI에 "삭제된 댓글입니다" 표시

**좋아요**
- 게시글당 1인 1회, 취소 가능

**북마크**
- 게시글당 1인 1회, 취소 가능

### 행사
- 관리자: 등록/수정/삭제/조기마감/신청자 엑셀 다운로드
- 회원: 신청/취소 (마감일 전까지)
- 정원 초과, 마감일 경과, 수동 마감 시 신청 불가

### 문의
- 유형: 가입문의, 행사문의, 신고, 계정문의, 기타
- 비로그인 시 이메일 필수
- 첨부파일 최대 3개
- 관리자: 상태 변경(접수→처리중→완료), 내부 메모

### 마이페이지
- 조회: 학번, 본명, 이메일, 가입일, 칭호
- 수정: 이메일, 비밀번호
- 활동내역: 내 게시글/댓글/좋아요/북마크/신청행사
- 탈퇴: 비밀번호 확인 후 처리

### 관리자
- 대시보드: 방문자, 게시글, 댓글, 가입자, 대기문의 통계
- 회원관리: 권한변경, 정지, 강제탈퇴, 칭호설정
- 명단관리: CSV 업로드, 비활성 처리

---

## API 명세

### 인증 `/api/auth`

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /signup | 회원가입 |
| POST | /verify-email | 이메일 인증 |
| POST | /login | 로그인 |
| POST | /logout | 로그아웃 |
| POST | /refresh | 토큰 갱신 |
| POST | /password/reset-request | 비밀번호 재설정 요청 |
| POST | /password/reset | 비밀번호 재설정 |

**POST /signup**
```json
// Request
{ "studentId": "20231234", "name": "홍길동", "password": "Pass123!", "email": "hong@example.com" }

// Response 201
{ "id": 1, "studentId": "20231234", "name": "홍길동", "message": "인증 이메일이 발송되었습니다" }
```

**POST /login**
```json
// Request
{ "studentId": "20231234", "password": "Pass123!" }

// Response 200
{ "accessToken": "eyJ...", "refreshToken": "eyJ...", "expiresIn": 3600, "user": { "id": 1, "name": "홍길동", "role": "MEMBER" } }
```

---

### 게시글 `/api/posts/:board`

> board: `notices`, `general`, `insight`

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /:board | 목록 조회 |
| POST | /:board | 작성 |
| GET | /:board/:id | 상세 조회 |
| PUT | /:board/:id | 수정 |
| DELETE | /:board/:id | 삭제 |
| POST | /:board/:id/like | 좋아요 |
| DELETE | /:board/:id/like | 좋아요 취소 |
| POST | /:board/:id/bookmark | 북마크 |
| DELETE | /:board/:id/bookmark | 북마크 취소 |

**GET /general** `?page=1&limit=20&sort=latest&search=키워드`
```json
// Response 200
{
  "posts": [
    { "id": 1, "title": "제목", "author": "익명", "isQuestion": false, "viewCount": 45, "likeCount": 3, "commentCount": 5, "createdAt": "2025-01-20T10:00:00Z" }
  ],
  "pagination": { "currentPage": 1, "totalPages": 10, "totalCount": 200 }
}
```

**POST /general**
```json
// Request
{ "title": "제목", "content": "내용", "isAnonymous": true, "isQuestion": false, "images": [{ "url": "https://...", "order": 1 }] }

// Response 201
{ "id": 1, "title": "제목", "createdAt": "2025-01-21T09:00:00Z" }
```

---

### 댓글 `/api/comments/:board`

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /:board/:postId | 목록 |
| POST | /:board/:postId | 작성 |
| PUT | /:board/:id | 수정 |
| DELETE | /:board/:id | 삭제 |

**POST /general/:postId**
```json
// Request
{ "content": "댓글 내용", "isAnonymous": false, "parentId": null }

// Response 201
{ "id": 1, "content": "댓글 내용", "author": "홍길동", "createdAt": "2025-01-21T09:30:00Z" }
```

---

### 행사 `/api/events`

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | / | 목록 |
| POST | / | 생성 (관리자) |
| GET | /:id | 상세 |
| PUT | /:id | 수정 (관리자) |
| DELETE | /:id | 삭제 (관리자) |
| POST | /:id/register | 신청 |
| DELETE | /:id/register | 취소 |
| POST | /:id/close | 조기마감 (관리자) |
| GET | /:id/registrations | 신청자목록 (관리자) |

**GET /** `?status=upcoming&page=1`
```json
// Response 200
{
  "events": [
    { "id": 1, "title": "신입생 환영회", "startDatetime": "2025-02-01T18:00:00Z", "location": "학생회관", "capacity": 50, "currentCount": 35, "status": "UPCOMING" }
  ]
}
```

**POST /** (관리자)
```json
// Request
{ "title": "환영회", "description": "설명", "startDatetime": "2025-02-01T18:00:00Z", "endDatetime": "2025-02-01T21:00:00Z", "location": "학생회관", "capacity": 50, "registrationDeadline": "2025-01-31T23:59:59Z" }
```

---

### 문의 `/api/inquiries`

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | / | 작성 |
| GET | / | 목록 (관리자) |
| GET | /:id | 상세 (관리자) |
| PUT | /:id/status | 상태변경 (관리자) |

**POST /**
```json
// Request
{ "type": "JOIN", "title": "가입 문의", "content": "내용", "email": "user@example.com", "attachments": [{ "url": "https://..." }] }

// Response 201
{ "id": 1, "inquiryNumber": "INQ-2025012100001", "status": "PENDING" }
```

---

### 마이페이지 `/api/users/:userId`

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /:userId | 내 정보 |
| PUT | /:userId | 정보 수정 |
| PUT | /:userId/password | 비밀번호 변경 |
| POST | /:userId/logout | 로그아웃 |
| DELETE | /:userId | 탈퇴 |
| GET | /:userId/posts | 내 게시글 |
| GET | /:userId/comments | 내 댓글 |
| GET | /:userId/likes | 좋아요 목록 |
| GET | /:userId/bookmarks | 북마크 목록 |
| GET | /:userId/events | 신청 행사 |

**PUT /:userId/password**
```json
// Request
{ "currentPassword": "Pass123!", "newPassword": "NewPass456!" }
```

**DELETE /:userId**
```json
// Request
{ "password": "Pass123!", "reason": "탈퇴 사유" }
```

---

### 관리자 `/api/admin`

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /dashboard | 통계 |
| GET | /users | 회원 목록 |
| GET | /users/:id | 회원 상세 |
| PUT | /users/:id/role | 권한 변경 |
| PUT | /users/:id/status | 상태 변경 |
| PUT | /users/:id/title | 칭호 설정 |
| DELETE | /users/:id | 강제 탈퇴 |
| POST | /members/upload | 명단 업로드 |

**GET /dashboard**
```json
// Response 200
{ "todayVisitors": 45, "todayPosts": 12, "todayComments": 35, "weeklyNewUsers": 8, "pendingInquiries": 5 }
```

**PUT /users/:id/status**
```json
// Request
{ "status": "SUSPENDED", "suspendedUntil": "2025-02-01T00:00:00Z", "reason": "규칙 위반" }
```

**POST /members/upload** `multipart/form-data`
```
file: [CSV 파일]
semester: "2025-1"
```

---

## 에러 코드

| 코드 | 메시지 |
|------|--------|
| AUTH001 | 등록된 회원 정보가 없습니다 |
| AUTH002 | 이미 가입된 계정입니다 |
| AUTH003 | 학번 또는 비밀번호가 일치하지 않습니다 |
| AUTH004 | 해당 학기 활동 회원이 아닙니다 |
| AUTH005 | 계정이 정지되었습니다 |
| AUTH006 | 이메일 인증이 필요합니다 |
| POST001 | 게시글을 찾을 수 없습니다 |
| POST002 | 수정 권한이 없습니다 |
| EVENT001 | 정원이 마감되었습니다 |
| EVENT002 | 신청 기간이 종료되었습니다 |
| EVENT003 | 이미 신청한 행사입니다 |
| ADMIN001 | 관리자 권한이 필요합니다 |
