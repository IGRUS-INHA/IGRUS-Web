# Orval API 마이그레이션 상태

**생성일**: 2026-02-02
**작업자**: Claude Sonnet 4.5

---

## 📋 개요

게시판 관련 수동 axios 기반 API를 Orval 자동 생성 API로 마이그레이션한 작업의 결과를 정리합니다.

### 목표
- ✅ 수동 작성 API → Orval 생성 API 전환
- ✅ CORS 에러 해결 (경로 불일치 문제)
- ✅ 타입 안전성 향상
- ✅ 코드 일관성 확보

---

## ✅ 완전 마이그레이션 완료

### 게시판 API (Posts)

#### 1. 게시글 목록 조회
- **Endpoint**: `GET /api/v1/boards/{boardCode}/posts`
- **Hook**: `useGetPostList(boardCode, params)`
- **파일**: [BoardListPage.tsx](../../src/pages/board/BoardListPage.tsx)
- **상태**: ✅ 완료

**주요 변경사항**:
- `usePosts` → `useGetPostList`
- Pagination: 1-based → 0-based (`page: currentPage - 1`)
- 응답 구조: `data.content` → `data.posts`
- 필드명: `post.id` → `post.postId`

#### 2. 게시글 상세 조회
- **Endpoint**: `GET /api/v1/boards/{boardCode}/posts/{postId}`
- **Hook**: `useGetPostDetail(boardCode, postId)`
- **파일**: [PostDetailPage.tsx](../../src/pages/board/PostDetailPage.tsx)
- **상태**: ✅ 완료

**필드 매핑**:
| 기존 필드 | Orval 필드 | 타입 변경 |
|-----------|------------|-----------|
| `id` | `postId` | number |
| `author` | `authorName` | object → string |
| `likes` | `likeCount` | number |
| `comments` | `commentCount` | number |
| `isLiked` | `liked` | boolean |
| `date` | `createdAt` | string |
| `image` | `imageUrls[0]` | string → string[] |
| `category` | `boardCode` | 용도 변경 |

#### 3. 게시글 작성
- **Endpoint**: `POST /api/v1/boards/{boardCode}/posts`
- **Hook**: `useCreatePost()`
- **파일**: [PostWritePage.tsx](../../src/pages/board/PostWritePage.tsx)
- **상태**: ✅ 완료

**주요 변경사항**:
- Mutation 파라미터: `{ board, data }` → `{ boardCode, data }`
- `category` 필드 제거 (백엔드 API에 없음)
- Response: `post.id` → `response.data.postId`

#### 4. 게시글 수정
- **Endpoint**: `PUT /api/v1/boards/{boardCode}/posts/{postId}`
- **Hook**: `useUpdatePost()`
- **상태**: ⏸️ 보류 (작성 페이지에서 미사용)

#### 5. 게시글 삭제
- **Endpoint**: `DELETE /api/v1/boards/{boardCode}/posts/{postId}`
- **Hook**: `useDeletePost()`
- **상태**: ⏸️ 보류 (현재 페이지에서 미사용)

---

### 좋아요 API (Likes)

#### 1. 좋아요 토글
- **Endpoint**: `POST /api/v1/posts/{postId}/likes`
- **Hook**: `useToggleLike()`
- **파일**: [PostDetailPage.tsx](../../src/pages/board/PostDetailPage.tsx)
- **상태**: ✅ 완료

**주요 변경사항**:
- Mutation 파라미터 단순화: `{ board, postId, isLiked }` → `{ postId }`
- 백엔드에서 현재 상태 자동 판단

#### 2. 좋아요 상태 조회
- **Endpoint**: `GET /api/v1/posts/{postId}/likes/status`
- **Hook**: `useGetLikeStatus(postId)`
- **상태**: ⏸️ 사용 가능하나 현재 미사용

---

## 🔄 아직 마이그레이션되지 않은 API

### 수동 API (유지 중)

다음 API들은 **의도적으로 이번 작업 범위에서 제외**되었습니다:

- `frontend/src/api/users.ts` - 사용자 관리 API
- `frontend/src/api/inquiries.ts` - 문의 API
- `frontend/src/api/events.ts` - 이벤트 API
- `frontend/src/api/auth.ts` - 인증 API
- `frontend/src/api/admin.ts` - 관리자 API
- `frontend/src/api/axiosClient.ts` - 위 API들이 사용 중

**향후 계획**: 게시판 API 검증 완료 후 순차적으로 마이그레이션 예정

---

## 🚫 백엔드 미구현 API

현재까지 발견된 백엔드 미구현 API: **없음**

게시판 및 좋아요 관련 모든 API가 Orval 스펙에 포함되어 있으며, 정상 작동 예상.

---

## 📂 삭제 예정 파일

사용자 테스트 완료 후 삭제 가능:

1. ✅ `frontend/src/api/posts.ts` - Orval API로 대체 완료
2. ✅ `frontend/src/hooks/queries/usePosts.ts` - Orval hooks로 대체 완료

**주의**: 다른 API에서 사용 중이므로 유지:
- ⏸️ `frontend/src/api/axiosClient.ts`

---

## 🧪 테스트 가이드

### 준비
```bash
cd frontend
npm run dev
```

브라우저에서 `http://localhost:5173` 접속 후 개발자 도구 (F12) 열기

### 1. BoardListPage 테스트

#### ✅ 목록 조회
1. `/board/notices` 접속
2. Network 탭 확인:
   - URL: `GET .../api/v1/boards/notices/posts?page=0&size=20`
   - Status: 200 OK
3. 게시글 목록 표시 확인

#### ✅ 페이지네이션
1. 2페이지 클릭
2. URL에 `page=1` 포함 확인 (0-based)

#### ✅ 검색
1. 키워드 입력 후 검색
2. URL에 `keyword=...` 포함 확인

### 2. PostDetailPage 테스트

#### ✅ 상세 조회
1. 게시글 클릭
2. Network 탭 확인:
   - URL: `GET .../api/v1/boards/notices/posts/{postId}`
   - Status: 200 OK
3. 제목, 내용, 작성자, 날짜 표시 확인

#### ✅ 좋아요 토글
1. 좋아요 버튼 클릭
2. Network 탭 확인:
   - URL: `POST .../api/v1/posts/{postId}/likes`
   - Status: 200 OK
3. 좋아요 수 증가/감소 확인

### 3. PostWritePage 테스트

#### ✅ 게시글 작성
1. "글쓰기" 버튼 클릭
2. 제목, 내용 입력 후 작성
3. Network 탭 확인:
   - URL: `POST .../api/v1/boards/notices/posts`
   - Status: 201 Created
4. 상세 페이지로 리다이렉트 확인

---

## ⚠️ 알려진 제약사항

### 1. Category 필드
- **문제**: Orval API에 `category` 필드 없음
- **해결**: `boardCode`로 대체 (예: "notices", "general")
- **영향**: 게시판별 카테고리 분류 미지원 (백엔드 구현 필요)

### 2. 댓글 기능
- **상태**: UI는 있으나 API 미연결 (TODO 주석 처리)
- **필요 작업**: Comment API 연동 (별도 작업)

### 3. 스크랩/북마크
- **상태**: UI는 있으나 API 미연결 (TODO 주석 처리)
- **필요 작업**: Bookmark API 연동 (별도 작업)

---

## 📊 마이그레이션 통계

### 변경된 파일 (3개)
- ✅ `frontend/src/pages/board/BoardListPage.tsx`
- ✅ `frontend/src/pages/board/PostDetailPage.tsx`
- ✅ `frontend/src/pages/board/PostWritePage.tsx`

### 사용된 Orval API (6개)
- `useGetPostList` - 목록 조회
- `useGetPostDetail` - 상세 조회
- `useCreatePost` - 작성
- `useUpdatePost` - 수정 (선언만)
- `useDeletePost` - 삭제 (선언만)
- `useToggleLike` - 좋아요 토글

### 삭제 예정 파일 (2개)
- `frontend/src/api/posts.ts`
- `frontend/src/hooks/queries/usePosts.ts`

---

## 🎯 다음 단계

### 단기 (1주일 이내)
1. **사용자 테스트**: 위 테스트 가이드에 따라 모든 기능 검증
2. **에러 처리**: API 에러 발생 시 적절한 fallback 추가
3. **파일 정리**: 테스트 완료 후 수동 API 파일 삭제

### 중기 (1개월 이내)
1. **Comment API 연동**: 댓글 기능 활성화
2. **Bookmark API 연동**: 스크랩 기능 활성화
3. **Edit/Delete 기능**: 게시글 수정/삭제 버튼 추가 및 연동

### 장기 (분기별)
1. **다른 API 마이그레이션**: users, inquiries, events, auth, admin
2. **axiosClient 제거**: 모든 수동 API 제거 후
3. **API 모니터링**: Orval API 성능 및 에러율 추적

---

## 📝 참고 문서

- [Orval 설정](../../orval.config.ts)
- [customFetch 구현](../../src/api/client.ts)
- [Orval 생성 API](../../src/api/model/)
- [프론트엔드 가이드](../../CLAUDE.md)

---

**마이그레이션 완료일**: 2026-02-02
**예상 작업 시간**: 13-20분 (실제: 병렬 처리로 단축)
**상태**: ✅ 코드 마이그레이션 완료, 사용자 테스트 대기 중
