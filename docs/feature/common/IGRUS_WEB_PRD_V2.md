# 아이그루스 시스템 명세서 V2

**버전**: 2.0
**작성일**: 2026-01-22
**상태**: Draft

---

## Clarifications

### Session 2026-01-22 (3차)
- Q: 데이터 모델 정규화? → A: User 테이블을 User(기본정보), UserAuth(인증정보), UserSuspension(정지이력)으로 분리
- 분리 사유: 3NF 위반 해소 (status→suspendedUntil 이행 종속), 보안/책임 분리, 정지 이력 관리

### Session 2026-01-22 (2차)
- Q: 회원가입 방식 변경? → A: 동아리 가입 = 웹 회원가입 동시 진행, MemberList 사전 검증 제거, 누구나 가입 신청 가능
- Q: 이메일 인증 필요 여부? → A: 불필요
- Q: 역할 체계 변경? → A: 비회원 → 준회원(가입신청, 미승인) → 정회원(승인됨) → 운영진 → 관리자 5단계
- Q: 준회원의 시스템 접근 권한? → A: 게시판별로 다른 읽기/쓰기 권한 적용
- Q: 준회원 게시판별 권한? → A: 공지사항(준회원 공개 설정된 글만 읽기) / 자유게시판(접근불가) / 정보공유(접근불가)
- Q: 준회원→정회원 승인 권한? → A: 관리자(ADMIN)만 승인 가능
- Q: 회원가입 필수 입력 정보? → A: 학번, 본명, 비밀번호, 이메일, 전화번호, 학과, 가입 동기
- Q: 준회원 행사 신청 권한? → A: 신청 불가, 정회원 승인 후에만 행사 신청 가능

### Session 2026-01-22 (1차)
- Q: 사용자 계정이 정지/탈퇴될 때 활성 세션(토큰) 처리 방식? → A: 즉시 무효화 - 정지/탈퇴 시 모든 활성 토큰 즉시 폐기
- Q: 게시글/댓글 삭제 처리 방식? → A: Soft Delete - is_deleted=true로 표시, DB 보존, UI에서 "삭제된 게시글/댓글입니다" 표시
- Q: 관리자 역할 분리? → A: ADMIN(최고관리자)과 OPERATOR(운영진) 2단계로 분리
  - OPERATOR 권한: 공지사항 관리, 행사 관리, 문의 처리, 대시보드 통계 조회
  - ADMIN 전용: 회원 권한 변경, 회원 정지/강제탈퇴, 준회원 승인

---

## 개요

### 목적
IGRUS(인하대학교 게임 연구 동아리) 회원들을 위한 웹 기반 커뮤니티 플랫폼 구축. 회원 관리, 게시판 운영, 행사 관리, 문의 처리 기능을 제공하여 동아리 운영 효율화 및 회원 간 소통 활성화를 목표로 한다.

### 대상 사용자
| 사용자 유형 | 역할 코드 | 설명 | 주요 목표 |
|------------|----------|------|----------|
| 비회원 | - | 미가입 사용자 | 회원가입 신청, 문의하기 |
| 준회원 | ASSOCIATE | 가입 신청 완료, 관리자 승인 대기 상태 | 준회원 공개 공지사항 열람, 승인 대기 |
| 정회원 | MEMBER | 관리자 승인 완료된 활성 회원 | 게시글/댓글 작성, 행사 신청, 정보 공유 |
| 운영진 | OPERATOR | 동아리 임원진으로 콘텐츠/행사/문의 관리 권한 보유 | 공지사항 관리, 행사 관리, 문의 처리, 통계 조회 |
| 관리자 | ADMIN | 최고 관리자로 모든 권한 보유 | 준회원 승인, 회원 권한/상태 관리, 시스템 전체 관리 |

### 역할별 권한 요약

| 기능 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|------|-----------|--------|----------|-------|
| 공지사항 읽기 (준회원 공개) | O | O | O | O |
| 공지사항 읽기 (전체) | X | O | O | O |
| 자유게시판/정보공유 접근 | X | O | O | O |
| 게시글/댓글 작성 | X | O | O | O |
| 행사 신청/취소 | X | O | O | O |
| 공지사항 작성/수정/삭제 | X | X | O | O |
| 행사 등록/수정/삭제/조기마감 | X | X | O | O |
| 문의 처리 (상태변경, 메모) | X | X | O | O |
| 대시보드 통계 조회 | X | X | O | O |
| 준회원 → 정회원 승인 | X | X | X | O |
| 회원 권한 변경 | X | X | X | O |
| 회원 정지/강제탈퇴 | X | X | X | O |

---

## 기능 명세

### 1. 회원가입 및 승인

**핵심 흐름**
1. 사용자가 회원가입 정보 입력 (학번, 본명, 비밀번호, 이메일, 전화번호, 학과, 가입 동기)
2. 가입 완료 시 즉시 준회원(ASSOCIATE)으로 등록
3. 준회원 상태에서 로그인 가능 (제한된 기능만 접근)
4. 관리자(ADMIN)가 준회원을 정회원(MEMBER)으로 승인
5. 정회원 전환 후 모든 기능 이용 가능

**상세 규칙**
- 학번: 정확히 8자리 숫자
- 본명: 실명 입력
- 비밀번호: 영문 대/소문자 + 숫자 + 특수문자 조합, 최소 8자 이상
- 이메일: 유효한 이메일 형식, 중복 불가
- 전화번호: 유효한 전화번호 형식
- 학과: 필수 입력
- 가입 동기: 필수 입력
- 동일 학번 중복 가입 불가
- 이메일 인증 불필요 (가입 즉시 로그인 가능)

**준회원 권한**
- 준회원 공개 설정된 공지사항만 읽기 가능
- 자유게시판, 정보공유 게시판 접근 불가
- 행사 신청 불가
- 게시글/댓글 작성 불가

**Acceptance Scenarios**
1. Given 비회원이, When 학번, 본명, 비밀번호, 이메일, 전화번호, 학과, 가입 동기를 입력하여 가입 신청하면, Then 준회원으로 등록되고 즉시 로그인이 가능하다
2. Given 이미 가입된 학번으로 재가입 시도할 때, When 가입 신청을 제출하면, Then "이미 가입된 계정입니다" 메시지가 표시된다
3. Given 준회원 상태에서, When 관리자(ADMIN)가 해당 회원을 승인하면, Then 정회원으로 전환되고 모든 게시판과 행사 신청 기능에 접근할 수 있다
4. Given 준회원 상태에서, When 자유게시판에 접근하면, Then "정회원 승인 후 이용 가능합니다" 메시지가 표시된다

---

### 2. 로그인/인증

**로그인**
- 학번 + 비밀번호로 인증
- 준회원/정회원 모두 가입 즉시 로그인 가능 (이메일 인증 불필요)
- 로그인 시 역할(ASSOCIATE/MEMBER/OPERATOR/ADMIN) 정보 반환

**토큰 관리**
| 토큰 유형 | 유효 기간 | 용도 |
|----------|----------|------|
| Access Token | 1시간 | API 요청 인증 |
| Refresh Token | 7일 | Access Token 재발급 |

**비밀번호 재설정**
1. 사용자가 학번 입력
2. 해당 계정의 이메일로 재설정 링크 발송 (30분 유효)
3. 링크 클릭 후 새 비밀번호 설정
4. 재설정 완료 시 모든 기존 토큰 무효화

---

### 3. 계정 상태

| 상태 | 코드 | 설명 | 로그인 가능 |
|------|------|------|------------|
| Active | ACTIVE | 정상 이용 가능 | O |
| Suspended | SUSPENDED | 관리자에 의해 정지됨 | X |
| Withdrawn | WITHDRAWN | 본인 탈퇴 | X |

**역할 (Role)**
| 역할 | 코드 | 설명 |
|------|------|------|
| 준회원 | ASSOCIATE | 가입 완료, 승인 대기 |
| 정회원 | MEMBER | 관리자 승인 완료 |
| 운영진 | OPERATOR | 콘텐츠/행사/문의 관리 권한 |
| 관리자 | ADMIN | 모든 권한 보유 |

**상태 전환 시 처리**
- Suspended/Withdrawn 전환 시: 모든 활성 토큰 즉시 무효화

---

### 4. 게시판

#### 4.1 게시판 종류

| 게시판 | 코드 | 읽기 권한 | 작성 권한 | 익명 허용 | 질문 태그 |
|--------|------|----------|----------|----------|----------|
| 공지사항 | notices | 준회원: 공개 설정된 글만 / 정회원 이상: 전체 | OPERATOR 이상 | X | X |
| 자유게시판 | general | MEMBER 이상 | MEMBER 이상 | O | O |
| 정보공유 | insight | MEMBER 이상 | MEMBER 이상 | X | X |

#### 4.2 게시글

**작성 규칙**
- 제목: 최대 100자
- 내용: 제한 없음
- 이미지: 최대 5개, 각 10MB 이하
- 익명 설정: 자유게시판에서만 가능, 작성 후 수정 불가
- 질문 태그: 자유게시판에서만 "질문으로 등록" 옵션 선택 가능
- 준회원 공개: 공지사항에서만 "준회원에게 공개" 옵션 선택 가능 (기본값: 비공개)

**수정/삭제**
- 본인 게시글만 수정/삭제 가능 (관리자 예외)
- 삭제 시 Soft Delete (is_deleted=true)
- 삭제된 게시글: UI에 "삭제된 게시글입니다" 표시
- 삭제된 게시글의 댓글: 유지됨 (부모 글이 삭제되어도 댓글 존속)

**Acceptance Scenarios**
1. Given 로그인한 회원이 자유게시판에서, When 제목과 내용을 입력하고 익명 옵션을 선택하여 글을 작성하면, Then 게시글이 등록되고 작성자는 "익명"으로 표시된다
2. Given 게시글에 댓글이 있는 상태에서, When 게시글을 삭제하면, Then 게시글은 "삭제된 게시글입니다"로 표시되고 댓글은 유지된다

#### 4.3 댓글

**작성 규칙**
- 내용: 최대 500자
- 대댓글: 1단계까지만 허용 (댓글의 댓글까지, 대댓글의 대댓글 불가)
- 익명: 해당 게시판이 익명 허용인 경우에만 선택 가능

**삭제**
- 본인 댓글만 삭제 가능 (관리자 예외)
- Soft Delete 적용
- UI에 "삭제된 댓글입니다" 표시
- 대댓글이 있는 댓글 삭제 시: 부모 댓글은 "삭제된 댓글입니다"로 표시, 대댓글은 유지

#### 4.4 좋아요

- 게시글당 1인 1회
- 토글 방식: 좋아요 → 좋아요 취소 → 좋아요 반복 가능
- 본인 게시글에도 좋아요 가능

#### 4.5 북마크

- 게시글당 1인 1회
- 토글 방식
- 북마크한 글 목록은 마이페이지에서 조회

---

### 5. 행사

#### 5.1 운영 기능 (OPERATOR 이상)
- 행사 등록: 제목, 설명, 시작/종료 일시, 장소, 정원, 신청 마감일
- 행사 수정: 모든 필드 수정 가능 (진행 중인 행사도 수정 가능)
- 행사 삭제: 신청자가 있어도 삭제 가능 (삭제 전 경고)
- 조기 마감: 마감일 전에 수동으로 신청 종료
- 신청자 목록: 엑셀 다운로드 지원

#### 5.2 회원 기능 (정회원 이상)
- 신청: 마감 전, 정원 미달 시 신청 가능
- 취소: 마감일 전까지 취소 가능
- 중복 신청 불가
- **준회원은 행사 신청 불가**

#### 5.3 신청 불가 조건
- 정원 초과
- 신청 마감일 경과
- 관리자 조기 마감
- 이미 신청한 행사
- 준회원 (정회원 승인 필요)

**Acceptance Scenarios**
1. Given 정원 50명인 행사에 35명이 신청한 상태에서, When 정회원이 신청하면, Then 신청이 완료되고 현재 신청자 수가 36명으로 업데이트된다
2. Given 정원이 가득 찬 행사에서, When 정회원이 신청하면, Then "정원이 마감되었습니다" 메시지가 표시되고 신청되지 않는다
3. Given 동시에 여러 명이 마지막 정원 1자리에 신청할 때, When 신청 요청이 처리되면, Then 선착순으로 1명만 신청 성공하고 나머지는 정원 마감 메시지를 받는다
4. Given 준회원이 행사에 신청하려 할 때, When 신청 버튼을 클릭하면, Then "정회원 승인 후 신청 가능합니다" 메시지가 표시된다

---

### 6. 문의

**문의 유형**
| 유형 | 코드 | 설명 |
|------|------|------|
| 가입문의 | JOIN | 명단 미등록 등 가입 관련 |
| 행사문의 | EVENT | 행사 관련 문의 |
| 신고 | REPORT | 부적절한 콘텐츠/사용자 신고 |
| 계정문의 | ACCOUNT | 비밀번호, 이메일 등 계정 관련 |
| 기타 | OTHER | 기타 문의 |

**작성 규칙**
- 비로그인 시: 이메일 필수 입력
- 로그인 시: 이메일 선택 (기본값: 계정 이메일)
- 첨부파일: 최대 3개
- 문의 번호: 자동 생성 (INQ-YYYYMMDD#####)

**처리 상태**
| 상태 | 코드 | 설명 |
|------|------|------|
| 접수 | PENDING | 문의 등록 완료 |
| 처리중 | IN_PROGRESS | 관리자 검토 중 |
| 완료 | COMPLETED | 처리 완료 |

**운영 기능 (OPERATOR 이상)**
- 상태 변경
- 내부 메모 추가 (문의자에게 비공개)
- 문의 목록 조회 및 필터링

---

### 7. 마이페이지

**조회 정보**
- 학번
- 본명
- 이메일
- 전화번호
- 학과
- 역할 (준회원/정회원/운영진/관리자)
- 가입일
- 승인일 (정회원 이상)

**수정 가능 정보**
- 이메일
- 전화번호
- 비밀번호: 현재 비밀번호 확인 후 변경

**활동 내역**
- 내 게시글 목록
- 내 댓글 목록
- 좋아요한 게시글 목록
- 북마크한 게시글 목록
- 신청한 행사 목록

**탈퇴**
- 비밀번호 확인 필수
- 탈퇴 사유 입력 (선택)
- 탈퇴 즉시 모든 토큰 무효화
- 계정 상태 WITHDRAWN으로 변경
- 작성한 콘텐츠는 유지 (작성자명은 "탈퇴한 회원"으로 표시)

---

### 8. 관리 기능

#### 8.1 대시보드 통계 (OPERATOR 이상)
- 오늘 방문자 수
- 오늘 게시글 수
- 오늘 댓글 수
- 이번 주 신규 가입자 수
- 대기 중 문의 수
- **승인 대기 준회원 수**

#### 8.2 준회원 승인 (ADMIN 전용)
- 승인 대기 준회원 목록 조회
- 준회원 상세 정보 조회 (학번, 본명, 학과, 가입 동기 등)
- 일괄 승인 / 개별 승인 기능
- 승인 시 즉시 정회원(MEMBER)으로 전환

#### 8.3 회원 관리 (ADMIN 전용)

**회원 목록 조회**
- 회원 목록 조회 및 검색 (OPERATOR 이상)
- 회원 상세 정보 조회 (OPERATOR 이상)

**권한 관리 (ADMIN 전용)**
- 권한 변경: MEMBER ↔ OPERATOR ↔ ADMIN
- 상태 변경: 정지 (기간 설정 가능), 정지 해제
- 강제 탈퇴: 사유 입력 후 처리

**제한 사항**
- 자기 자신 정지/탈퇴 불가
- 자기 자신 권한 변경 불가
- 마지막 ADMIN은 권한 변경 불가

---

## 비기능 요구사항

### 성능
- 게시글 목록 로딩: 3초 이내
- 동시 접속자 100명 이상 지원
- 이미지 업로드: 10MB 이하 파일 10초 이내 완료

### 보안
- 비밀번호: 단방향 해시 저장
- 토큰: 서명 검증 필수
- HTTPS 전용 통신
- SQL Injection, XSS 방지

### 가용성
- 주요 기능 성공률 99% 이상
- 이메일 발송 실패 시 자동 재시도

---

## API 명세

### 인증 `/api/auth`

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /signup | 회원가입 (준회원 등록) |
| POST | /login | 로그인 |
| POST | /logout | 로그아웃 |
| POST | /refresh | 토큰 갱신 |
| POST | /password/reset-request | 비밀번호 재설정 요청 |
| POST | /password/reset | 비밀번호 재설정 |

**POST /signup**
```json
// Request
{
  "studentId": "20231234",
  "name": "홍길동",
  "password": "Pass123!",
  "email": "hong@example.com",
  "phone": "010-1234-5678",
  "department": "컴퓨터공학과",
  "motivation": "게임 개발에 관심이 있어서 가입하고 싶습니다."
}

// Response 201
{
  "id": 1,
  "studentId": "20231234",
  "name": "홍길동",
  "role": "ASSOCIATE",
  "message": "준회원으로 가입되었습니다. 관리자 승인 후 정회원이 됩니다."
}

// Error 409 - 이미 가입됨
{ "code": "AUTH002", "message": "이미 가입된 계정입니다" }
```

**POST /login**
```json
// Request
{ "studentId": "20231234", "password": "Pass123!" }

// Response 200
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "name": "홍길동",
    "role": "ASSOCIATE"  // ASSOCIATE, MEMBER, OPERATOR, ADMIN 중 하나
  }
}

// Error 401 - 인증 실패
{ "code": "AUTH003", "message": "학번 또는 비밀번호가 일치하지 않습니다" }

// Error 403 - 계정 정지
{ "code": "AUTH005", "message": "계정이 정지되었습니다", "suspendedUntil": "2025-02-01T00:00:00Z" }
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

**GET /general** `?page=1&limit=20&sort=latest&search=키워드&isQuestion=true`
```json
// Response 200
{
  "posts": [
    {
      "id": 1,
      "title": "제목",
      "author": "익명",
      "authorId": null,
      "isQuestion": false,
      "viewCount": 45,
      "likeCount": 3,
      "commentCount": 5,
      "createdAt": "2025-01-20T10:00:00Z",
      "isDeleted": false
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 10,
    "totalCount": 200
  }
}
```

**POST /general** (정회원 이상)
```json
// Request
{
  "title": "제목",
  "content": "내용",
  "isAnonymous": true,
  "isQuestion": false,
  "images": [
    { "url": "https://...", "order": 1 }
  ]
}

// Response 201
{
  "id": 1,
  "title": "제목",
  "createdAt": "2025-01-21T09:00:00Z"
}

// Error 400 - 제목 길이 초과
{ "code": "POST003", "message": "제목은 100자 이내여야 합니다" }

// Error 400 - 이미지 개수 초과
{ "code": "POST004", "message": "이미지는 최대 5개까지 첨부 가능합니다" }

// Error 403 - 준회원 접근 불가
{ "code": "POST006", "message": "정회원 승인 후 이용 가능합니다" }
```

**POST /notices** (OPERATOR 이상)
```json
// Request
{
  "title": "공지사항 제목",
  "content": "공지 내용",
  "isVisibleToAssociate": true,  // 준회원 공개 여부
  "images": [
    { "url": "https://...", "order": 1 }
  ]
}

// Response 201
{
  "id": 1,
  "title": "공지사항 제목",
  "isVisibleToAssociate": true,
  "createdAt": "2025-01-21T09:00:00Z"
}
```

**GET /general/:id**
```json
// Response 200
{
  "id": 1,
  "title": "제목",
  "content": "내용",
  "author": "익명",
  "authorId": null,
  "isQuestion": false,
  "viewCount": 46,
  "likeCount": 3,
  "commentCount": 5,
  "images": [
    { "url": "https://...", "order": 1 }
  ],
  "isLiked": false,
  "isBookmarked": true,
  "isDeleted": false,
  "createdAt": "2025-01-20T10:00:00Z",
  "updatedAt": "2025-01-20T10:00:00Z"
}

// Response 200 - 삭제된 게시글
{
  "id": 1,
  "isDeleted": true,
  "message": "삭제된 게시글입니다"
}
```

---

### 댓글 `/api/comments/:board`

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /:board/:postId | 목록 |
| POST | /:board/:postId | 작성 |
| PUT | /:board/:id | 수정 |
| DELETE | /:board/:id | 삭제 |

**GET /general/:postId**
```json
// Response 200
{
  "comments": [
    {
      "id": 1,
      "content": "댓글 내용",
      "author": "홍길동",
      "authorId": 1,
      "isDeleted": false,
      "createdAt": "2025-01-21T09:30:00Z",
      "replies": [
        {
          "id": 2,
          "content": "대댓글 내용",
          "author": "익명",
          "authorId": null,
          "isDeleted": false,
          "createdAt": "2025-01-21T09:35:00Z"
        }
      ]
    },
    {
      "id": 3,
      "isDeleted": true,
      "message": "삭제된 댓글입니다",
      "replies": []
    }
  ]
}
```

**POST /general/:postId**
```json
// Request
{
  "content": "댓글 내용",
  "isAnonymous": false,
  "parentId": null
}

// Response 201
{
  "id": 1,
  "content": "댓글 내용",
  "author": "홍길동",
  "createdAt": "2025-01-21T09:30:00Z"
}

// Error 400 - 대댓글 depth 초과
{ "code": "COMMENT001", "message": "대댓글에는 답글을 달 수 없습니다" }
```

---

### 행사 `/api/events`

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| GET | / | 목록 | 전체 |
| POST | / | 생성 | OPERATOR 이상 |
| GET | /:id | 상세 | 전체 |
| PUT | /:id | 수정 | OPERATOR 이상 |
| DELETE | /:id | 삭제 | OPERATOR 이상 |
| POST | /:id/register | 신청 | MEMBER 이상 (준회원 불가) |
| DELETE | /:id/register | 취소 | MEMBER 이상 (준회원 불가) |
| POST | /:id/close | 조기마감 | OPERATOR 이상 |
| GET | /:id/registrations | 신청자목록 | OPERATOR 이상 |
| GET | /:id/registrations/export | 신청자 엑셀 다운로드 | OPERATOR 이상 |

**GET /** `?status=upcoming&page=1`
```json
// Response 200
{
  "events": [
    {
      "id": 1,
      "title": "신입생 환영회",
      "startDatetime": "2025-02-01T18:00:00Z",
      "endDatetime": "2025-02-01T21:00:00Z",
      "location": "학생회관",
      "capacity": 50,
      "currentCount": 35,
      "registrationDeadline": "2025-01-31T23:59:59Z",
      "status": "UPCOMING",
      "isRegistered": false
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 5,
    "totalCount": 45
  }
}
```

**POST /** (OPERATOR 이상)
```json
// Request
{
  "title": "환영회",
  "description": "설명",
  "startDatetime": "2025-02-01T18:00:00Z",
  "endDatetime": "2025-02-01T21:00:00Z",
  "location": "학생회관",
  "capacity": 50,
  "registrationDeadline": "2025-01-31T23:59:59Z"
}

// Response 201
{
  "id": 1,
  "title": "환영회",
  "status": "UPCOMING",
  "createdAt": "2025-01-15T10:00:00Z"
}
```

**POST /:id/register** (정회원 이상)
```json
// Response 200
{
  "message": "행사 신청이 완료되었습니다",
  "currentCount": 36
}

// Error 400 - 정원 초과
{ "code": "EVENT001", "message": "정원이 마감되었습니다" }

// Error 400 - 기간 종료
{ "code": "EVENT002", "message": "신청 기간이 종료되었습니다" }

// Error 409 - 중복 신청
{ "code": "EVENT003", "message": "이미 신청한 행사입니다" }

// Error 403 - 준회원 접근 불가
{ "code": "EVENT006", "message": "정회원 승인 후 신청 가능합니다" }
```

---

### 문의 `/api/inquiries`

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | / | 작성 | 전체 (비로그인 포함) |
| GET | / | 목록 | OPERATOR 이상 |
| GET | /:id | 상세 | OPERATOR 이상 |
| PUT | /:id/status | 상태변경 | OPERATOR 이상 |
| POST | /:id/memo | 내부메모 추가 | OPERATOR 이상 |

**POST /**
```json
// Request
{
  "type": "JOIN",
  "title": "가입 문의",
  "content": "내용",
  "email": "user@example.com",
  "attachments": [
    { "url": "https://..." }
  ]
}

// Response 201
{
  "id": 1,
  "inquiryNumber": "INQ-2025012100001",
  "status": "PENDING"
}
```

**GET /** (OPERATOR 이상) `?status=PENDING&type=JOIN&page=1`
```json
// Response 200
{
  "inquiries": [
    {
      "id": 1,
      "inquiryNumber": "INQ-2025012100001",
      "type": "JOIN",
      "title": "가입 문의",
      "email": "user@example.com",
      "status": "PENDING",
      "createdAt": "2025-01-21T10:00:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 3,
    "totalCount": 25
  }
}
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

**GET /:userId**
```json
// Response 200
{
  "id": 1,
  "studentId": "20231234",
  "name": "홍길동",
  "email": "hong@example.com",
  "role": "MEMBER",
  "createdAt": "2025-01-01T10:00:00Z"
}
```

**PUT /:userId/password**
```json
// Request
{
  "currentPassword": "Pass123!",
  "newPassword": "NewPass456!"
}

// Response 200
{ "message": "비밀번호가 변경되었습니다" }

// Error 401
{ "code": "AUTH003", "message": "현재 비밀번호가 일치하지 않습니다" }
```

**DELETE /:userId**
```json
// Request
{
  "password": "Pass123!",
  "reason": "탈퇴 사유"
}

// Response 200
{ "message": "탈퇴가 완료되었습니다" }
```

---

### 관리자 `/api/admin`

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| GET | /dashboard | 통계 | OPERATOR 이상 |
| GET | /associates | 승인 대기 준회원 목록 | ADMIN |
| POST | /associates/:id/approve | 준회원 승인 | ADMIN |
| POST | /associates/approve-batch | 준회원 일괄 승인 | ADMIN |
| GET | /users | 회원 목록 | OPERATOR 이상 |
| GET | /users/:id | 회원 상세 | OPERATOR 이상 |
| PUT | /users/:id/role | 권한 변경 | ADMIN |
| PUT | /users/:id/status | 상태 변경 | ADMIN |
| DELETE | /users/:id | 강제 탈퇴 | ADMIN |

**GET /dashboard**
```json
// Response 200
{
  "todayVisitors": 45,
  "todayPosts": 12,
  "todayComments": 35,
  "weeklyNewUsers": 8,
  "pendingInquiries": 5,
  "pendingAssociates": 3
}
```

**GET /associates** (ADMIN 전용)
```json
// Response 200
{
  "associates": [
    {
      "id": 1,
      "studentId": "20231234",
      "name": "홍길동",
      "department": "컴퓨터공학과",
      "motivation": "게임 개발에 관심이 있습니다.",
      "createdAt": "2025-01-21T10:00:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 1,
    "totalCount": 3
  }
}
```

**POST /associates/:id/approve** (ADMIN 전용)
```json
// Response 200
{
  "id": 1,
  "name": "홍길동",
  "role": "MEMBER",
  "message": "정회원으로 승인되었습니다"
}
```

**PUT /users/:id/status**
```json
// Request - 정지
{
  "status": "SUSPENDED",
  "suspendedUntil": "2025-02-01T00:00:00Z",
  "reason": "규칙 위반"
}

// Request - 정지 해제
{
  "status": "ACTIVE"
}

// Response 200
{
  "id": 1,
  "status": "SUSPENDED",
  "message": "모든 활성 토큰이 무효화되었습니다"
}

// Error 400 - 자기 자신 정지 불가
{ "code": "ADMIN002", "message": "본인 계정은 정지할 수 없습니다" }
```

**POST /members/upload** `multipart/form-data`
```
file: [CSV 파일]
semester: "2025-1"
```
```json
// Response 200
{
  "totalRows": 100,
  "added": 25,
  "updated": 70,
  "errors": [
    { "row": 5, "studentId": "2023123", "error": "학번은 8자리여야 합니다" }
  ],
  "inactivated": 15
}
```

---

## 에러 코드

### 인증 관련 (AUTH)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| AUTH002 | 409 | 이미 가입된 계정입니다 |
| AUTH003 | 401 | 학번 또는 비밀번호가 일치하지 않습니다 |
| AUTH005 | 403 | 계정이 정지되었습니다 |
| AUTH007 | 401 | 토큰이 만료되었습니다 |
| AUTH008 | 401 | 유효하지 않은 토큰입니다 |
| AUTH009 | 403 | 정회원 승인 후 이용 가능합니다 |

### 게시글 관련 (POST)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| POST001 | 404 | 게시글을 찾을 수 없습니다 |
| POST002 | 403 | 수정 권한이 없습니다 |
| POST003 | 400 | 제목은 100자 이내여야 합니다 |
| POST004 | 400 | 이미지는 최대 5개까지 첨부 가능합니다 |
| POST005 | 400 | 이미지 크기는 10MB 이하여야 합니다 |
| POST006 | 403 | 정회원 승인 후 이용 가능합니다 |

### 댓글 관련 (COMMENT)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| COMMENT001 | 400 | 대댓글에는 답글을 달 수 없습니다 |
| COMMENT002 | 400 | 댓글은 500자 이내여야 합니다 |
| COMMENT003 | 404 | 댓글을 찾을 수 없습니다 |

### 행사 관련 (EVENT)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| EVENT001 | 400 | 정원이 마감되었습니다 |
| EVENT002 | 400 | 신청 기간이 종료되었습니다 |
| EVENT003 | 409 | 이미 신청한 행사입니다 |
| EVENT004 | 404 | 행사를 찾을 수 없습니다 |
| EVENT005 | 400 | 신청 내역이 없습니다 |
| EVENT006 | 403 | 정회원 승인 후 신청 가능합니다 |

### 관리자 관련 (ADMIN)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| ADMIN001 | 403 | 운영진 이상의 권한이 필요합니다 |
| ADMIN002 | 400 | 본인 계정은 정지할 수 없습니다 |
| ADMIN003 | 400 | 마지막 관리자는 권한을 변경할 수 없습니다 |
| ADMIN004 | 403 | 관리자 권한이 필요합니다 |
| ADMIN005 | 400 | 본인 권한은 변경할 수 없습니다 |

---

## 데이터 모델

### 정규화 설계 원칙

**적용된 정규화:**
- **1NF**: 모든 컬럼이 원자값(Atomic Value)을 가짐
- **2NF**: 모든 비키 속성이 기본키에 완전 함수 종속
- **3NF**: 이행적 종속 제거 (비키 속성 간 종속성 분리)

**테이블 분리 근거:**
| 분리 전 | 분리 후 | 분리 사유 |
|---------|---------|-----------|
| User.password | UserAuth.passwordHash | 프로필 정보와 인증 정보 분리 (보안 및 책임 분리) |
| User.role, status | UserAuth.role, status | 인증/권한 관련 속성 별도 관리 |
| User.suspendedUntil | UserSuspension 테이블 | 정지 이력 관리 및 3NF 위반 해소 (status→suspendedUntil 이행 종속) |
| User.approvedAt | UserAuth.approvedAt | 승인 정보는 인증 도메인에 귀속 |

**테이블 관계:**
```
User (1) ─────── (1) UserAuth
  │
  └─────────────── (N) UserSuspension
  │
  └─────────────── (N) RefreshToken
  │
  └─────────────── (N) Post
  │
  └─────────────── (N) Comment
  │
  └─────────────── (N) EventRegistration
```

---

### User (사용자 기본정보)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| studentId | String(8) | 학번 (Unique) |
| name | String | 본명 |
| email | String | 이메일 (Unique) |
| phone | String | 전화번호 |
| department | String | 학과 |
| motivation | Text | 가입 동기 |
| role | Enum | ASSOCIATE, MEMBER, OPERATOR, ADMIN |
| title | String | 칭호 (nullable) |
| createdAt | DateTime | 가입일 |
| updatedAt | DateTime | 수정일 |

### UserAuth (인증 자격증명)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| userId | Long | 사용자 FK (Unique) |
| passwordHash | String | 비밀번호 해시 |
| status | Enum | ACTIVE, SUSPENDED, WITHDRAWN |
| approvedAt | DateTime | 정회원 승인일 (nullable) |
| approvedBy | Long | 승인 처리자 FK (nullable) |
| createdAt | DateTime | 생성일 |
| updatedAt | DateTime | 수정일 |

### UserSuspension (정지 이력)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| userId | Long | 사용자 FK |
| reason | String | 정지 사유 |
| suspendedAt | DateTime | 정지 시작일 |
| suspendedUntil | DateTime | 정지 종료일 |
| suspendedBy | Long | 정지 처리자 FK |
| liftedAt | DateTime | 해제일 (nullable) |
| liftedBy | Long | 해제 처리자 FK (nullable) |

### Post (게시글)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| board | Enum | notices, general, insight |
| title | String(100) | 제목 |
| content | Text | 내용 |
| authorId | Long | 작성자 FK |
| isAnonymous | Boolean | 익명 여부 |
| isQuestion | Boolean | 질문 태그 |
| isVisibleToAssociate | Boolean | 준회원 공개 여부 (공지사항만) |
| viewCount | Integer | 조회수 |
| isDeleted | Boolean | 삭제 여부 |
| createdAt | DateTime | 작성일 |
| updatedAt | DateTime | 수정일 |

### PostImage (게시글 이미지)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| postId | Long | 게시글 FK |
| url | String | 이미지 URL |
| order | Integer | 순서 |

### Comment (댓글)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| postId | Long | 게시글 FK |
| parentId | Long | 부모 댓글 FK (nullable) |
| authorId | Long | 작성자 FK |
| content | String(500) | 내용 |
| isAnonymous | Boolean | 익명 여부 |
| isDeleted | Boolean | 삭제 여부 |
| createdAt | DateTime | 작성일 |

### Like (좋아요)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| postId | Long | 게시글 FK |
| userId | Long | 사용자 FK |
| createdAt | DateTime | 생성일 |

### Bookmark (북마크)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| postId | Long | 게시글 FK |
| userId | Long | 사용자 FK |
| createdAt | DateTime | 생성일 |

### Event (행사)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| title | String | 제목 |
| description | Text | 설명 |
| startDatetime | DateTime | 시작 일시 |
| endDatetime | DateTime | 종료 일시 |
| location | String | 장소 |
| capacity | Integer | 정원 |
| registrationDeadline | DateTime | 신청 마감일 |
| status | Enum | UPCOMING, ONGOING, CLOSED, CANCELED |
| createdAt | DateTime | 생성일 |
| updatedAt | DateTime | 수정일 |

### EventRegistration (행사 신청)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| eventId | Long | 행사 FK |
| userId | Long | 사용자 FK |
| createdAt | DateTime | 신청일 |

### Inquiry (문의)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| inquiryNumber | String | 문의 번호 |
| type | Enum | JOIN, EVENT, REPORT, ACCOUNT, OTHER |
| title | String | 제목 |
| content | Text | 내용 |
| email | String | 이메일 |
| userId | Long | 사용자 FK (nullable) |
| status | Enum | PENDING, IN_PROGRESS, COMPLETED |
| createdAt | DateTime | 작성일 |
| updatedAt | DateTime | 수정일 |

### InquiryAttachment (문의 첨부파일)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| inquiryId | Long | 문의 FK |
| url | String | 파일 URL |

### InquiryMemo (문의 내부 메모)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| inquiryId | Long | 문의 FK |
| adminId | Long | 관리자 FK |
| content | Text | 메모 내용 |
| createdAt | DateTime | 작성일 |

### RefreshToken
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| token | String | 토큰 값 |
| userId | Long | 사용자 FK |
| expiresAt | DateTime | 만료일 |
| createdAt | DateTime | 생성일 |

---

## 성공 기준

| ID | 기준 | 측정 방법 |
|----|------|----------|
| SC-001 | 사용자가 회원가입을 3분 이내에 완료할 수 있다 | 가입 시작부터 준회원 로그인까지 시간 측정 |
| SC-002 | 게시글 작성이 1분 이내에 완료된다 | 작성 화면 진입부터 게시 완료까지 시간 측정 |
| SC-003 | 게시글 목록이 3초 이내에 표시된다 | 페이지 요청부터 콘텐츠 렌더링까지 시간 측정 |
| SC-004 | 행사 신청/취소가 2번의 클릭으로 완료된다 | UX 플로우 검증 |
| SC-005 | 동시 100명 접속 시 정상 동작한다 | 부하 테스트 |
| SC-006 | 관리자가 회원 상태 변경(승인, 정지 등) 후 즉시(5초 이내) 권한이 반영된다 | 상태 변경 후 권한 검증 시간 측정 |
| SC-007 | 주요 기능 성공률 99% 이상 | 모니터링 대시보드 |
| SC-008 | 준회원 승인 대기 목록이 관리자 대시보드에서 실시간으로 조회 가능하다 | 대시보드 기능 검증 |

---

## 가정 및 제약사항

### 가정
- 이메일 발송 서비스는 외부 서비스를 사용한다 (비밀번호 재설정용)
- 이미지 저장소는 외부 클라우드 스토리지를 사용한다
- 관리자가 준회원 승인을 수동으로 처리한다

### 제약사항
- 학번은 인하대학교 학번 체계(8자리)를 따른다
- 준회원은 관리자 승인 전까지 제한된 기능만 이용 가능
- 행사 신청 시 동시성 처리가 필요함 (Race Condition 방지)

---

## 범위 외 항목

- 실시간 채팅 기능
- 푸시 알림
- 소셜 로그인 (카카오, 구글 등)
- 모바일 앱
- 결제 기능
- 다국어 지원
