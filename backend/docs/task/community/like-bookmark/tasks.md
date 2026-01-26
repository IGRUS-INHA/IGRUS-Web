# Tasks: 좋아요/북마크 (Like/Bookmark) 백엔드 구현

**Input**: `docs/feature/community/like-bookmark-spec.md`
**Prerequisites**: Post 기능 구현 완료 필요 (`post-spec.md` 기반)
**Tech Stack**: Java 21, Spring Boot 3.5.9, Spring Data JPA, MySQL 8.x, Flyway

**Tests**: 테스트 코드 작성 포함 (backend/CLAUDE.md 개발 규칙에 따름)

**Organization**: User Story 기반으로 구성하여 독립적 구현 및 테스트 가능

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 태스크가 속한 User Story (US1, US2, US3)
- 모든 태스크에 정확한 파일 경로 포함

## Path Conventions

- **Backend**: `backend/src/main/java/igrus/web/board/`
- **Tests**: `backend/src/test/java/igrus/web/board/`
- **Migrations**: `backend/src/main/resources/db/migration/`

---

## Phase 1: Setup (프로젝트 구조 설정)

**Purpose**: 좋아요/북마크 관련 패키지 구조 및 Flyway 마이그레이션 버전 확인

- [ ] T001 좋아요/북마크 관련 패키지 구조 확인 및 필요시 생성: `board/domain/`, `board/repository/`, `board/service/`, `board/dto/`, `board/exception/`
- [ ] T002 [P] Flyway 마이그레이션 최신 버전 확인 및 다음 버전 번호 예약

---

## Phase 2: Foundational (핵심 인프라 - 모든 User Story 선행 조건)

**Purpose**: 좋아요/북마크 기능의 핵심 엔티티 및 인프라 구축

**CRITICAL**: 이 페이즈가 완료되어야 User Story 작업 시작 가능

**Prerequisites**: Post 엔티티가 구현되어 있어야 함 (post-spec.md 기반)

### 2.1 데이터베이스 스키마

- [ ] T003 Flyway 마이그레이션 파일 작성 (likes, bookmarks 테이블) in `backend/src/main/resources/db/migration/V{N}__create_like_bookmark_tables.sql`
  - likes 테이블: id, post_id(FK), user_id(FK), created_at
  - UNIQUE 제약: (post_id, user_id)
  - bookmarks 테이블: id, post_id(FK), user_id(FK), created_at
  - UNIQUE 제약: (post_id, user_id)

- [ ] T004 Post 테이블에 like_count 컬럼 추가 마이그레이션 in `backend/src/main/resources/db/migration/V{N+1}__add_like_count_to_posts.sql`
  - like_count INT NOT NULL DEFAULT 0

### 2.2 핵심 도메인 엔티티

- [ ] T005 [P] Like 엔티티 구현 in `backend/src/main/java/igrus/web/board/domain/Like.java`
  - BaseEntity 상속
  - 필드: post(ManyToOne, LAZY), user(ManyToOne, LAZY)
  - @Table uniqueConstraints = (post_id, user_id)
  - 정적 팩토리 메서드: create(Post post, User user)
  - createdAt은 Instant 타입 사용

- [ ] T006 [P] Bookmark 엔티티 구현 in `backend/src/main/java/igrus/web/board/domain/Bookmark.java`
  - BaseEntity 상속
  - 필드: post(ManyToOne, LAZY), user(ManyToOne, LAZY)
  - @Table uniqueConstraints = (post_id, user_id)
  - 정적 팩토리 메서드: create(Post post, User user)
  - createdAt은 Instant 타입 사용

- [ ] T007 Post 엔티티에 likeCount 필드 추가 in `backend/src/main/java/igrus/web/board/domain/Post.java`
  - 필드: likeCount (Integer, default 0)
  - 메서드: incrementLikeCount(), decrementLikeCount()

### 2.3 Repository

- [ ] T008 [P] LikeRepository 인터페이스 구현 in `backend/src/main/java/igrus/web/board/repository/LikeRepository.java`
  - findByPostAndUser(Post post, User user): Optional<Like>
  - existsByPostAndUser(Post post, User user): boolean
  - deleteByPostAndUser(Post post, User user): void
  - findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable): Page<Like>
  - countByPost(Post post): long

- [ ] T009 [P] BookmarkRepository 인터페이스 구현 in `backend/src/main/java/igrus/web/board/repository/BookmarkRepository.java`
  - findByPostAndUser(Post post, User user): Optional<Bookmark>
  - existsByPostAndUser(Post post, User user): boolean
  - deleteByPostAndUser(Post post, User user): void
  - findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable): Page<Bookmark>

### 2.4 예외 클래스

- [ ] T010 [P] PostNotFoundException 예외 클래스 구현 (없으면 생성) in `backend/src/main/java/igrus/web/board/exception/PostNotFoundException.java`
- [ ] T011 [P] PostDeletedException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/PostDeletedException.java`
- [ ] T012 [P] ErrorCode에 좋아요/북마크 관련 에러 코드 추가 in `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
  - POST_NOT_FOUND, POST_DELETED, LIKE_ALREADY_EXISTS, LIKE_NOT_FOUND, BOOKMARK_ALREADY_EXISTS, BOOKMARK_NOT_FOUND

**Checkpoint**: Foundation 완료 - User Story 구현 시작 가능

---

## Phase 3: User Story 1 - 게시글 좋아요 (Priority: P2) MVP

**Goal**: 정회원이 마음에 드는 게시글에 좋아요를 눌러 관심을 표현한다

**Independent Test**: 게시글에 좋아요를 누르고 좋아요 수가 증가하는지 확인

### Acceptance Criteria

1. 정회원이 게시글 상세 페이지에서 좋아요 버튼을 클릭하면 좋아요가 추가되고 좋아요 수가 1 증가한다
2. 이미 좋아요한 게시글에서 좋아요 버튼을 다시 클릭하면 좋아요가 취소되고 좋아요 수가 1 감소한다
3. 본인이 작성한 게시글에도 좋아요가 가능하다
4. 좋아요 상태인 게시글에서 상세 페이지를 조회하면 좋아요 버튼이 활성화 상태로 표시된다

### Tests for User Story 1

- [ ] T013 [P] [US1] LikeService 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/service/LikeServiceTest.java`
  - 좋아요 토글 - 좋아요 추가 성공 및 likeCount 증가
  - 좋아요 토글 - 좋아요 취소 성공 및 likeCount 감소
  - 본인 게시글에 좋아요 가능
  - 삭제된 게시글에 좋아요 시도 시 PostDeletedException 발생
  - 존재하지 않는 게시글에 좋아요 시도 시 PostNotFoundException 발생

- [ ] T014 [P] [US1] LikeController 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/controller/LikeControllerTest.java`
  - POST /api/v1/posts/{postId}/likes - 좋아요 토글 성공 (201 Created / 204 No Content)
  - POST /api/v1/posts/{postId}/likes - 비인증 사용자 접근 시 401 Unauthorized
  - POST /api/v1/posts/{postId}/likes - 준회원 접근 시 403 Forbidden
  - POST /api/v1/posts/{postId}/likes - 존재하지 않는 게시글 시 404 Not Found
  - GET /api/v1/posts/{postId}/likes/status - 좋아요 상태 조회 성공

### Implementation for User Story 1

- [ ] T015 [US1] LikeService 구현 in `backend/src/main/java/igrus/web/board/service/LikeService.java`
  - toggleLike(Long postId, Long userId): LikeToggleResponse (좋아요 추가/취소 토글)
  - isLikedByUser(Long postId, Long userId): boolean
  - 좋아요 추가 시 Post.likeCount 증가
  - 좋아요 취소 시 Like 레코드 Hard Delete 및 Post.likeCount 감소
  - 삭제된 게시글 체크 로직 포함

- [ ] T016 [P] [US1] LikeToggleResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/LikeToggleResponse.java`
  - 필드: liked (boolean), likeCount (int)

- [ ] T017 [P] [US1] LikeStatusResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/LikeStatusResponse.java`
  - 필드: liked (boolean), likeCount (int)

- [ ] T018 [US1] LikeController 구현 in `backend/src/main/java/igrus/web/board/controller/LikeController.java`
  - POST /api/v1/posts/{postId}/likes - 좋아요 토글
  - GET /api/v1/posts/{postId}/likes/status - 좋아요 상태 조회
  - Swagger 어노테이션 (@Operation, @ApiResponse) 추가

- [ ] T019 [US1] SecurityConfig에 좋아요 API 경로 권한 설정 추가 in `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`
  - /api/v1/posts/*/likes - REGULAR 이상 권한 필요

**Checkpoint**: User Story 1 완료 - 좋아요 기능 독립적으로 테스트 가능

---

## Phase 4: User Story 2 - 게시글 북마크 (Priority: P2)

**Goal**: 정회원이 나중에 다시 보고 싶은 게시글을 북마크하여 저장한다

**Independent Test**: 게시글을 북마크하고 마이페이지 북마크 목록에서 확인

### Acceptance Criteria

1. 정회원이 게시글 상세 페이지에서 북마크 버튼을 클릭하면 북마크가 추가되고 버튼이 활성화 상태로 변경된다
2. 이미 북마크한 게시글에서 북마크 버튼을 다시 클릭하면 북마크가 취소되고 버튼이 비활성화 상태로 변경된다
3. 북마크한 게시글이 있는 상태에서 마이페이지의 북마크 목록에 접근하면 북마크한 게시글 목록이 표시된다
4. 북마크한 게시글이 삭제되었을 때 북마크 목록을 조회하면 해당 게시글은 "삭제된 게시글"로 표시되거나 목록에서 제외된다

### Tests for User Story 2

- [ ] T020 [P] [US2] BookmarkService 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/service/BookmarkServiceTest.java`
  - 북마크 토글 - 북마크 추가 성공
  - 북마크 토글 - 북마크 취소 성공
  - 삭제된 게시글에 북마크 시도 시 PostDeletedException 발생
  - 존재하지 않는 게시글에 북마크 시도 시 PostNotFoundException 발생
  - 북마크 목록 조회 시 삭제된 게시글 필터링 확인

- [ ] T021 [P] [US2] BookmarkController 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/controller/BookmarkControllerTest.java`
  - POST /api/v1/posts/{postId}/bookmarks - 북마크 토글 성공
  - POST /api/v1/posts/{postId}/bookmarks - 비인증 사용자 접근 시 401 Unauthorized
  - POST /api/v1/posts/{postId}/bookmarks - 준회원 접근 시 403 Forbidden
  - GET /api/v1/posts/{postId}/bookmarks/status - 북마크 상태 조회 성공
  - GET /api/v1/users/me/bookmarks - 본인 북마크 목록 조회 성공

### Implementation for User Story 2

- [ ] T022 [US2] BookmarkService 구현 in `backend/src/main/java/igrus/web/board/service/BookmarkService.java`
  - toggleBookmark(Long postId, Long userId): BookmarkToggleResponse (북마크 추가/취소 토글)
  - isBookmarkedByUser(Long postId, Long userId): boolean
  - getMyBookmarks(Long userId, Pageable pageable): Page<BookmarkedPostResponse>
  - 북마크 취소 시 Bookmark 레코드 Hard Delete
  - 삭제된 게시글 체크 로직 포함

- [ ] T023 [P] [US2] BookmarkToggleResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/BookmarkToggleResponse.java`
  - 필드: bookmarked (boolean)

- [ ] T024 [P] [US2] BookmarkStatusResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/BookmarkStatusResponse.java`
  - 필드: bookmarked (boolean)

- [ ] T025 [P] [US2] BookmarkedPostResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/BookmarkedPostResponse.java`
  - 필드: postId, title, boardCode, boardName, authorName, createdAt, isDeleted, deletedMessage (optional)

- [ ] T026 [US2] BookmarkController 구현 in `backend/src/main/java/igrus/web/board/controller/BookmarkController.java`
  - POST /api/v1/posts/{postId}/bookmarks - 북마크 토글
  - GET /api/v1/posts/{postId}/bookmarks/status - 북마크 상태 조회
  - GET /api/v1/users/me/bookmarks - 본인 북마크 목록 조회
  - Swagger 어노테이션 추가

- [ ] T027 [US2] SecurityConfig에 북마크 API 경로 권한 설정 추가 in `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`
  - /api/v1/posts/*/bookmarks - REGULAR 이상 권한 필요
  - /api/v1/users/me/bookmarks - REGULAR 이상 권한 필요

**Checkpoint**: User Story 2 완료 - 북마크 기능 독립적으로 테스트 가능

---

## Phase 5: User Story 3 - 마이페이지 좋아요 목록 (Priority: P3)

**Goal**: 사용자가 본인이 좋아요한 게시글 목록을 마이페이지에서 확인한다

**Independent Test**: 마이페이지에서 좋아요한 게시글 목록이 표시되는지 확인

### Acceptance Criteria

1. 좋아요한 게시글이 있는 상태에서 마이페이지의 좋아요 목록에 접근하면 좋아요한 게시글 목록이 최신순으로 표시된다
2. 좋아요한 게시글이 삭제되었을 때 좋아요 목록을 조회하면 해당 게시글은 "삭제된 게시글"로 표시되거나 목록에서 제외된다

### Tests for User Story 3

- [ ] T028 [P] [US3] 좋아요 목록 조회 테스트 작성 in `backend/src/test/java/igrus/web/board/service/LikeServiceTest.java`
  - 좋아요 목록 조회 성공 - 최신순 정렬 확인
  - 좋아요한 게시글 삭제 시 목록에서 처리 확인

- [ ] T029 [P] [US3] 좋아요 목록 API 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/controller/LikeControllerTest.java`
  - GET /api/v1/users/me/likes - 좋아요 목록 조회 성공
  - GET /api/v1/users/me/likes - 비인증 사용자 접근 시 401 Unauthorized

### Implementation for User Story 3

- [ ] T030 [US3] LikeService에 좋아요 목록 조회 메서드 추가 in `backend/src/main/java/igrus/web/board/service/LikeService.java`
  - getMyLikes(Long userId, Pageable pageable): Page<LikedPostResponse>
  - 삭제된 게시글 필터링/표시 처리

- [ ] T031 [P] [US3] LikedPostResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/LikedPostResponse.java`
  - 필드: postId, title, boardCode, boardName, authorName, likeCount, createdAt, isDeleted, deletedMessage (optional)

- [ ] T032 [US3] LikeController에 좋아요 목록 조회 엔드포인트 추가 in `backend/src/main/java/igrus/web/board/controller/LikeController.java`
  - GET /api/v1/users/me/likes - 본인 좋아요 목록 조회
  - Swagger 어노테이션 추가

- [ ] T033 [US3] SecurityConfig에 좋아요 목록 API 권한 설정 추가 in `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`
  - /api/v1/users/me/likes - REGULAR 이상 권한 필요

**Checkpoint**: User Story 3 완료 - 좋아요 목록 조회 기능 독립적으로 테스트 가능

---

## Phase 6: Cross-Cutting Concerns (공통 관심사)

**Purpose**: 게시글 상세 조회 시 좋아요/북마크 상태 통합 및 Rate Limiting

### 게시글 상세 조회 통합

- [ ] T034 Post 상세 조회 응답에 좋아요/북마크 상태 포함 in `backend/src/main/java/igrus/web/board/dto/response/PostDetailResponse.java`
  - 필드 추가: liked (boolean), bookmarked (boolean), likeCount (int)

- [ ] T035 PostService 상세 조회 메서드에 좋아요/북마크 상태 조회 로직 추가 in `backend/src/main/java/igrus/web/board/service/PostService.java`
  - getPostDetail(Long postId, Long userId): PostDetailResponse 수정
  - LikeService.isLikedByUser() 호출
  - BookmarkService.isBookmarkedByUser() 호출

### Rate Limiting

- [ ] T036 [P] 좋아요/북마크 API Rate Limiting 설정 (사용자당 분당 30회) in `backend/src/main/java/igrus/web/common/config/RateLimitConfig.java`
  - /api/v1/posts/*/likes, /api/v1/posts/*/bookmarks 경로에 적용

---

## Phase 7: Polish & Final Validation

**Purpose**: 전체 기능에 걸친 개선 및 품질 향상

- [ ] T037 [P] Swagger API 문서 검토 및 보완 - 모든 좋아요/북마크 관련 엔드포인트
  - 요청/응답 예시 추가
  - 에러 응답 케이스 문서화
  - FR-011 (좋아요 수만 공개, 좋아요한 사용자 목록 비공개) 명시
  - FR-012 (북마크 정보는 본인만 조회 가능) 명시

- [ ] T038 GlobalExceptionHandler에 좋아요/북마크 예외 핸들러 추가 in `backend/src/main/java/igrus/web/common/exception/GlobalExceptionHandler.java`
  - PostNotFoundException → 404 Not Found
  - PostDeletedException → 410 Gone 또는 400 Bad Request

- [ ] T039 코드 리뷰 및 리팩토링
  - SOLID 원칙 준수 확인
  - N+1 쿼리 문제 점검
  - 동시성 처리 확인 (동시에 여러 사용자가 좋아요 시 likeCount 정확성)
  - 보안 취약점 점검 (OWASP Top 10)

- [ ] T040 전체 테스트 실행 및 커버리지 확인
  - 모든 단위 테스트 통과 확인
  - 모든 통합 테스트 통과 확인
  - 테스트 커버리지 확인

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 의존성 없음 - 즉시 시작 가능
- **Phase 2 (Foundational)**: Phase 1 완료 필요, **Post 기능 구현 완료 필수**
- **Phase 3 (US1 - 좋아요)**: Phase 2 완료 필요
- **Phase 4 (US2 - 북마크)**: Phase 2 완료 필요, Phase 3와 병렬 가능
- **Phase 5 (US3 - 좋아요 목록)**: Phase 3 완료 필요
- **Phase 6 (Cross-Cutting)**: Phase 3, 4 완료 필요
- **Phase 7 (Polish)**: 모든 User Story 완료 필요

### External Dependencies

- **Post 기능**: 이 태스크는 Post 기능이 구현되어 있어야 시작 가능
  - Post 엔티티
  - PostRepository
  - PostService

### User Story Dependencies

- **User Story 1 (P2 - 좋아요)**: Phase 2 완료 후 시작 - 독립적
- **User Story 2 (P2 - 북마크)**: Phase 2 완료 후 시작 - US1과 병렬 가능
- **User Story 3 (P3 - 좋아요 목록)**: US1 완료 후 시작 (LikeService 확장)

### Within Each User Story

- 테스트 작성 후 실패 확인 → 구현 → 테스트 통과 순서
- DTO → Service → Controller 순서
- 핵심 기능 → 부가 기능 순서

### Parallel Opportunities

- Phase 2의 T005, T006 엔티티 병렬 구현 가능
- Phase 2의 T008, T009 Repository 병렬 구현 가능
- Phase 2의 T010, T011, T012 예외 클래스 병렬 구현 가능
- Phase 3의 T013, T014 테스트 병렬 작성 가능
- Phase 3의 T016, T017 DTO 병렬 구현 가능
- Phase 4의 T020, T021 테스트 병렬 작성 가능
- Phase 4의 T023, T024, T025 DTO 병렬 구현 가능
- **Phase 3 (US1)과 Phase 4 (US2) 전체를 병렬 진행 가능**

---

## Parallel Example: User Story 1 & 2

```bash
# US1과 US2를 병렬로 시작 가능 (Phase 2 완료 후)

# Team A - User Story 1 (좋아요)
Task: "LikeService 단위 테스트 작성 in backend/.../LikeServiceTest.java"
Task: "LikeController 통합 테스트 작성 in backend/.../LikeControllerTest.java"

# Team B - User Story 2 (북마크) - 동시 진행
Task: "BookmarkService 단위 테스트 작성 in backend/.../BookmarkServiceTest.java"
Task: "BookmarkController 통합 테스트 작성 in backend/.../BookmarkControllerTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료 (CRITICAL - Post 의존성 확인)
3. Phase 3: User Story 1 (좋아요) 완료
4. **STOP and VALIDATE**: 좋아요 토글 기능 독립 테스트
5. 배포/데모 가능 (MVP!)

### Incremental Delivery

1. Setup + Foundational → Foundation 완료
2. User Story 1 (좋아요) → 테스트 → 배포/데모 (MVP!)
3. User Story 2 (북마크) → 테스트 → 배포/데모
4. User Story 3 (좋아요 목록) → 테스트 → 배포/데모
5. Cross-Cutting + Polish → 최종 배포
6. 각 스토리가 이전 스토리를 깨뜨리지 않고 가치 추가

### Related Specs

이 태스크는 좋아요/북마크 기능만 다룹니다. 다음 관련 기능은 별도 태스크로 구현:
- `board-spec.md` - 게시판 관리
- `post-spec.md` - 게시글 CRUD (선행 의존성)
- `comment-spec.md` - 댓글/대댓글 기능

---

## Notes

- [P] 태스크 = 다른 파일, 의존성 없음
- [Story] 라벨로 User Story 추적 가능
- 각 User Story는 독립적으로 완료 및 테스트 가능
- 테스트 실패 확인 후 구현 시작
- 태스크 또는 논리적 그룹 완료 후 커밋
- Checkpoint에서 스토리 독립 검증 가능
- **좋아요 취소 시 Hard Delete** (레코드 완전 삭제)
- **북마크 취소 시 Hard Delete** (레코드 완전 삭제)
- **동시성 주의**: 여러 사용자가 동시에 좋아요 시 likeCount 정확성 보장 필요

---

## Summary

| Phase | Task Count | Parallel Tasks |
|-------|------------|----------------|
| Phase 1: Setup | 2 | 1 |
| Phase 2: Foundational | 10 | 7 |
| Phase 3: User Story 1 (좋아요) | 7 | 4 |
| Phase 4: User Story 2 (북마크) | 8 | 4 |
| Phase 5: User Story 3 (좋아요 목록) | 6 | 2 |
| Phase 6: Cross-Cutting | 3 | 1 |
| Phase 7: Polish | 4 | 1 |
| **Total** | **40** | **20** |

**MVP Scope**: Phase 1 + Phase 2 + Phase 3 (User Story 1) = 19 tasks
**Independent Test per Story**: 각 User Story별 독립 테스트 가능
**Parallel Story Execution**: US1, US2 병렬 진행 가능

---

## Requirements Traceability

| Requirement | Task(s) |
|-------------|---------|
| FR-001: 게시글당 1인 1회 좋아요 | T005, T008 (UNIQUE 제약) |
| FR-002: 좋아요 토글 방식 | T015 |
| FR-003: 본인 게시글 좋아요 가능 | T015 |
| FR-004: 게시글당 1인 1회 북마크 | T006, T009 (UNIQUE 제약) |
| FR-005: 북마크 토글 방식 | T022 |
| FR-006: 북마크 목록 마이페이지 조회 | T026 |
| FR-007: 좋아요 목록 마이페이지 조회 | T032 |
| FR-008: 삭제된 게시글 좋아요/북마크 불가 | T015, T022 |
| FR-009: 게시글 상세 조회 시 좋아요/북마크 상태 포함 | T034, T035 |
| FR-010: 좋아요 수 표시 | T007, T015, T016 |
| FR-011: 좋아요 사용자 목록 비공개 | API 설계 (목록 미제공) |
| FR-012: 북마크 정보 본인만 조회 | T026, T027 |
| FR-013: Rate Limiting (분당 30회) | T036 |
| FR-014: 비회원/준회원 버튼 표시 + 권한 안내 | T019, T027 (권한 체크) |
