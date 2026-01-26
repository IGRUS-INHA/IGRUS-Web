# Tasks: 게시글 (Post) 백엔드 구현

**Input**: `docs/feature/community/post-spec.md`
**Prerequisites**: post-spec.md (기능 명세서), board-spec.md (게시판 기능)
**Tech Stack**: Java 21, Spring Boot 3.5.9, Spring Data JPA, MySQL 8.x, Flyway, AWS S3

**Tests**: 테스트 코드 작성 포함 (backend/CLAUDE.md 개발 규칙에 따름)

**Organization**: User Story 기반으로 구성하여 독립적 구현 및 테스트 가능

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 태스크가 속한 User Story (US1, US2, US3, US4, US5)
- 모든 태스크에 정확한 파일 경로 포함

## Path Conventions

- **Backend**: `backend/src/main/java/igrus/web/board/`
- **Tests**: `backend/src/test/java/igrus/web/board/`
- **Migrations**: `backend/src/main/resources/db/migration/`

---

## Phase 1: Setup (프로젝트 구조 설정)

**Purpose**: 게시글 도메인의 기본 패키지 구조 및 의존성 확인

- [ ] T001 Board 도메인 구현 완료 확인 (board-spec.md 기반 tasks 선행 필수)
- [ ] T002 [P] 게시글 관련 추가 패키지 구조 생성: `board/dto/request/`, `board/dto/response/`
- [ ] T003 [P] Flyway 마이그레이션 버전 확인 및 V7 번호 예약 확인 (V6는 board 사용)

---

## Phase 2: Foundational (핵심 인프라 - 모든 User Story 선행 조건)

**Purpose**: 모든 User Story가 의존하는 핵심 엔티티 및 인프라 구축

**CRITICAL**: 이 페이즈가 완료되어야 User Story 작업 시작 가능

### 2.1 데이터베이스 스키마

- [ ] T004 Flyway 마이그레이션 V7__create_post_tables.sql 작성 in `backend/src/main/resources/db/migration/V7__create_post_tables.sql`
  - posts 테이블: id, board_id(FK), author_id(FK), title(VARCHAR 100), content(TEXT), view_count, is_anonymous, is_question, is_visible_to_associate, deleted, deleted_at, deleted_by, created_at, updated_at, created_by, updated_by
  - post_images 테이블: id, post_id(FK), image_url, display_order, created_at
  - 인덱스: board_id, author_id, created_at, (board_id, is_question), (board_id, deleted)

### 2.2 핵심 도메인 엔티티

- [ ] T005 [P] Post 엔티티 구현 in `backend/src/main/java/igrus/web/board/domain/Post.java`
  - SoftDeletableEntity 상속
  - 필드: board(ManyToOne), author(ManyToOne), title, content, viewCount, isAnonymous, isQuestion, isVisibleToAssociate
  - 정적 팩토리 메서드: createPost(), createAnonymousPost(), createNotice()
  - 비즈니스 메서드: updateContent(), incrementViewCount(), canModify(User), canDelete(User)
  - 제약조건: 제목 100자, 익명/질문 옵션은 자유게시판만, 준회원 공개는 공지사항만

- [ ] T006 [P] PostImage 엔티티 구현 in `backend/src/main/java/igrus/web/board/domain/PostImage.java`
  - BaseEntity 상속
  - 필드: post(ManyToOne), imageUrl, displayOrder
  - 최대 5개 제한 (Post에서 관리)

### 2.3 Repository

- [ ] T007 [P] PostRepository 인터페이스 구현 in `backend/src/main/java/igrus/web/board/repository/PostRepository.java`
  - findByIdAndDeletedFalse(Long id): Optional<Post>
  - findByBoardAndDeletedFalseOrderByCreatedAtDesc(Board board, Pageable pageable): Page<Post>
  - findByBoardAndIsQuestionTrueAndDeletedFalse(Board board, Pageable pageable): Page<Post>
  - searchByTitleOrContent(Board board, String keyword, Pageable pageable): Page<Post>
  - countByAuthorAndCreatedAtAfter(User author, Instant after): long

- [ ] T008 [P] PostImageRepository 인터페이스 구현 in `backend/src/main/java/igrus/web/board/repository/PostImageRepository.java`
  - findByPostOrderByDisplayOrderAsc(Post post): List<PostImage>
  - deleteByPost(Post post): void
  - countByPost(Post post): long

### 2.4 예외 클래스

- [ ] T009 [P] PostNotFoundException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/PostNotFoundException.java`
- [ ] T010 [P] PostAccessDeniedException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/PostAccessDeniedException.java`
- [ ] T011 [P] PostTitleTooLongException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/PostTitleTooLongException.java`
- [ ] T012 [P] PostImageLimitExceededException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/PostImageLimitExceededException.java`
- [ ] T013 [P] PostRateLimitExceededException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/PostRateLimitExceededException.java`
- [ ] T014 [P] InvalidPostOptionException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/InvalidPostOptionException.java`
- [ ] T015 [P] PostDeletedException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/exception/PostDeletedException.java`

- [ ] T016 ErrorCode에 게시글 관련 에러 코드 추가 in `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
  - POST_NOT_FOUND(404, "게시글을 찾을 수 없습니다")
  - POST_ACCESS_DENIED(403, "게시글에 대한 접근 권한이 없습니다")
  - POST_TITLE_TOO_LONG(400, "제목은 100자 이내여야 합니다")
  - POST_IMAGE_LIMIT_EXCEEDED(400, "이미지는 최대 5개까지 첨부 가능합니다")
  - POST_RATE_LIMIT_EXCEEDED(429, "게시글 작성 제한을 초과했습니다 (시간당 20회)")
  - POST_INVALID_ANONYMOUS_OPTION(400, "익명 옵션은 자유게시판에서만 사용 가능합니다")
  - POST_INVALID_QUESTION_OPTION(400, "질문 옵션은 자유게시판에서만 사용 가능합니다")
  - POST_INVALID_VISIBILITY_OPTION(400, "준회원 공개 옵션은 공지사항에서만 사용 가능합니다")
  - POST_DELETED(410, "삭제된 게시글입니다")
  - POST_ANONYMOUS_UNCHANGEABLE(400, "익명 설정은 변경할 수 없습니다")

### 2.5 Rate Limiting 서비스

- [ ] T017 PostRateLimitService 구현 in `backend/src/main/java/igrus/web/board/service/PostRateLimitService.java`
  - checkRateLimit(User user): void (시간당 20회 초과 시 예외)
  - getRemainingPosts(User user): int
  - 내부: 최근 1시간 내 게시글 수 조회

**Checkpoint**: Foundation 완료 - User Story 구현 시작 가능

---

## Phase 3: User Story 1 - 게시글 작성 (Priority: P1) MVP

**Goal**: 정회원이 게시판에서 제목, 내용, 이미지를 포함한 게시글을 작성하고 등록한다

**Independent Test**: 정회원이 자유게시판에서 글을 작성하고 목록에서 확인할 수 있으면 통과

### Acceptance Criteria

1. 정회원이 자유게시판에서 제목과 내용을 입력하고 저장하면 게시글이 등록된다
2. 익명 옵션을 선택하고 저장하면 작성자가 "익명"으로 표시된다
3. 100자를 초과하는 제목 입력 시 저장이 거부된다
4. 5개를 초과하는 이미지 첨부 시 거부된다
5. "질문으로 등록" 옵션 선택 시 게시글에 질문 태그가 표시된다

### Tests for User Story 1

- [ ] T018 [P] [US1] Post 도메인 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/domain/PostTest.java`
  - 게시글 생성 시 필수 필드 검증
  - 제목 100자 초과 시 예외 발생
  - 자유게시판이 아닌 곳에서 익명 옵션 사용 시 예외 발생
  - 자유게시판이 아닌 곳에서 질문 옵션 사용 시 예외 발생
  - 조회수 증가 메서드 동작 확인

- [ ] T019 [P] [US1] PostService 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/service/PostServiceTest.java`
  - 정회원이 일반 게시글 작성 성공
  - 정회원이 익명 게시글 작성 성공 (자유게시판)
  - 정회원이 질문 태그 게시글 작성 성공 (자유게시판)
  - 제목 없이 작성 시 ValidationException 발생
  - 제목 100자 초과 시 PostTitleTooLongException 발생
  - 이미지 5개 초과 시 PostImageLimitExceededException 발생
  - 공지사항에서 익명 옵션 사용 시 InvalidPostOptionException 발생
  - Rate Limit 초과 시 PostRateLimitExceededException 발생

- [ ] T020 [P] [US1] PostController 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/controller/PostControllerCreateTest.java`
  - POST /api/v1/boards/{boardCode}/posts - 정회원이 게시글 작성 성공 201 Created
  - POST /api/v1/boards/{boardCode}/posts - 비인증 사용자 접근 시 401 Unauthorized
  - POST /api/v1/boards/{boardCode}/posts - 준회원이 자유게시판 작성 시도 시 403 Forbidden
  - POST /api/v1/boards/{boardCode}/posts - 제목 없이 요청 시 400 Bad Request
  - POST /api/v1/boards/{boardCode}/posts - 존재하지 않는 게시판 코드 시 404 Not Found

### Implementation for User Story 1

- [ ] T021 [P] [US1] CreatePostRequest DTO 구현 in `backend/src/main/java/igrus/web/board/dto/request/CreatePostRequest.java`
  - 필드: title(@NotBlank, @Size(max=100)), content(@NotBlank), isAnonymous, isQuestion, imageUrls(List, max 5)
  - Bean Validation 적용

- [ ] T022 [P] [US1] PostCreateResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/PostCreateResponse.java`
  - 필드: postId, boardCode, title, createdAt

- [ ] T023 [US1] PostService 게시글 작성 로직 구현 in `backend/src/main/java/igrus/web/board/service/PostService.java`
  - createPost(String boardCode, CreatePostRequest request, User author): PostCreateResponse
  - 권한 검증, Rate Limit 검증, 옵션 유효성 검증 포함
  - 이미지 URL 저장 처리

- [ ] T024 [US1] PostController 게시글 작성 엔드포인트 구현 in `backend/src/main/java/igrus/web/board/controller/PostController.java`
  - POST /api/v1/boards/{boardCode}/posts - 게시글 작성
  - @Valid 적용, 인증 사용자 정보 주입
  - Swagger 어노테이션 (@Operation, @ApiResponse) 추가

- [ ] T025 [US1] GlobalExceptionHandler에 게시글 예외 핸들러 추가 in `backend/src/main/java/igrus/web/common/exception/GlobalExceptionHandler.java`
  - PostNotFoundException → 404 Not Found
  - PostAccessDeniedException → 403 Forbidden
  - PostTitleTooLongException → 400 Bad Request
  - PostImageLimitExceededException → 400 Bad Request
  - PostRateLimitExceededException → 429 Too Many Requests
  - InvalidPostOptionException → 400 Bad Request

**Checkpoint**: User Story 1 완료 - 게시글 작성 기능 독립적으로 테스트 가능

---

## Phase 4: User Story 2 - 게시글 조회 (Priority: P1)

**Goal**: 사용자가 게시글 목록에서 원하는 글을 선택하여 상세 내용을 확인한다

**Independent Test**: 게시판 목록에서 게시글을 선택하여 제목, 내용, 작성자, 조회수, 댓글 수 등을 확인할 수 있으면 통과

### Acceptance Criteria

1. 특정 게시글 선택 시 제목, 내용, 작성자, 작성일, 조회수, 좋아요 수, 댓글 수가 표시된다
2. 게시글 상세 페이지 접근 시 조회수가 1 증가한다
3. 익명 게시글의 작성자는 "익명"으로 표시되고 작성자 ID가 노출되지 않는다
4. 삭제된 게시글 접근 시 "삭제된 게시글입니다" 메시지가 표시된다

### Tests for User Story 2

- [ ] T026 [P] [US2] PostService 조회 로직 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/service/PostServiceReadTest.java`
  - 게시글 목록 조회 시 삭제되지 않은 글만 반환
  - 게시글 목록 조회 시 페이징 동작 확인
  - 게시글 상세 조회 시 조회수 증가
  - 익명 게시글 조회 시 작성자 정보 필터링
  - 삭제된 게시글 조회 시 PostDeletedException 발생
  - 검색어로 목록 조회 시 제목+내용 검색
  - 질문 필터로 목록 조회 시 질문 태그 게시글만 반환

- [ ] T027 [P] [US2] PostController 조회 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/controller/PostControllerReadTest.java`
  - GET /api/v1/boards/{boardCode}/posts - 게시글 목록 조회 성공
  - GET /api/v1/boards/{boardCode}/posts?page=0&size=20 - 페이징 동작 확인
  - GET /api/v1/boards/{boardCode}/posts?keyword=검색어 - 검색 동작 확인
  - GET /api/v1/boards/{boardCode}/posts?questionOnly=true - 필터 동작 확인
  - GET /api/v1/boards/{boardCode}/posts/{postId} - 상세 조회 성공
  - GET /api/v1/boards/{boardCode}/posts/{postId} - 존재하지 않는 게시글 404 Not Found
  - GET /api/v1/boards/{boardCode}/posts/{postId} - 삭제된 게시글 410 Gone

### Implementation for User Story 2

- [ ] T028 [P] [US2] PostListResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/PostListResponse.java`
  - 필드: postId, title, authorName(익명 시 "익명"), isAnonymous, isQuestion, viewCount, likeCount, commentCount, createdAt
  - 정적 팩토리 메서드: from(Post post)

- [ ] T029 [P] [US2] PostDetailResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/PostDetailResponse.java`
  - 필드: postId, boardCode, title, content, authorId(익명 시 null), authorName, isAnonymous, isQuestion, viewCount, likeCount, commentCount, imageUrls, createdAt, updatedAt
  - 정적 팩토리 메서드: from(Post post, boolean isAuthor)

- [ ] T030 [P] [US2] PostListPageResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/PostListPageResponse.java`
  - 필드: posts(List<PostListResponse>), totalElements, totalPages, currentPage, hasNext

- [ ] T031 [US2] PostService 게시글 조회 로직 구현 in `backend/src/main/java/igrus/web/board/service/PostService.java`
  - getPostList(String boardCode, UserRole role, String keyword, Boolean questionOnly, Pageable pageable): PostListPageResponse
  - getPostDetail(String boardCode, Long postId, User currentUser): PostDetailResponse
  - 준회원 공개 필터링 적용 (공지사항)
  - 익명 게시글 작성자 정보 필터링

- [ ] T032 [US2] PostController 게시글 조회 엔드포인트 구현 in `backend/src/main/java/igrus/web/board/controller/PostController.java`
  - GET /api/v1/boards/{boardCode}/posts - 게시글 목록 조회 (페이징, 검색, 필터)
  - GET /api/v1/boards/{boardCode}/posts/{postId} - 게시글 상세 조회
  - Swagger 어노테이션 추가

**Checkpoint**: User Story 2 완료 - 게시글 조회 기능 독립적으로 테스트 가능

---

## Phase 5: User Story 3 - 게시글 수정 (Priority: P2)

**Goal**: 게시글 작성자가 본인이 작성한 글의 제목이나 내용을 수정한다

**Independent Test**: 작성자 본인이 게시글을 수정하고 변경된 내용이 반영되는지 확인

### Acceptance Criteria

1. 본인이 작성한 게시글 상세 페이지에서 수정 버튼 클릭 시 수정 화면이 표시된다
2. 제목과 내용을 변경하고 저장하면 변경된 내용이 반영되고 수정일이 업데이트된다
3. 익명으로 작성한 게시글의 익명 설정은 변경 불가능하다
4. 타인이 작성한 게시글은 수정 버튼이 표시되지 않거나 접근이 거부된다

### Tests for User Story 3

- [ ] T033 [P] [US3] PostService 수정 로직 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/service/PostServiceUpdateTest.java`
  - 본인 게시글 수정 성공
  - 제목, 내용 변경 후 updatedAt 업데이트 확인
  - 익명 설정 변경 시도 시 예외 발생
  - 타인 게시글 수정 시도 시 PostAccessDeniedException 발생
  - 삭제된 게시글 수정 시도 시 PostDeletedException 발생
  - 관리자가 타인 게시글 수정 가능 확인

- [ ] T034 [P] [US3] PostController 수정 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/controller/PostControllerUpdateTest.java`
  - PUT /api/v1/boards/{boardCode}/posts/{postId} - 본인 게시글 수정 성공 200 OK
  - PUT /api/v1/boards/{boardCode}/posts/{postId} - 타인 게시글 수정 시도 403 Forbidden
  - PUT /api/v1/boards/{boardCode}/posts/{postId} - 삭제된 게시글 수정 시도 410 Gone
  - PUT /api/v1/boards/{boardCode}/posts/{postId} - 익명 설정 변경 시도 400 Bad Request

### Implementation for User Story 3

- [ ] T035 [P] [US3] UpdatePostRequest DTO 구현 in `backend/src/main/java/igrus/web/board/dto/request/UpdatePostRequest.java`
  - 필드: title(@NotBlank, @Size(max=100)), content(@NotBlank), isQuestion, imageUrls(List, max 5)
  - 익명 설정은 변경 불가이므로 필드 제외

- [ ] T036 [P] [US3] PostUpdateResponse DTO 구현 in `backend/src/main/java/igrus/web/board/dto/response/PostUpdateResponse.java`
  - 필드: postId, boardCode, title, updatedAt

- [ ] T037 [US3] PostService 게시글 수정 로직 구현 in `backend/src/main/java/igrus/web/board/service/PostService.java`
  - updatePost(String boardCode, Long postId, UpdatePostRequest request, User currentUser): PostUpdateResponse
  - 작성자 본인 또는 관리자만 수정 가능
  - 익명 설정 불변 검증

- [ ] T038 [US3] PostController 게시글 수정 엔드포인트 구현 in `backend/src/main/java/igrus/web/board/controller/PostController.java`
  - PUT /api/v1/boards/{boardCode}/posts/{postId} - 게시글 수정
  - Swagger 어노테이션 추가

**Checkpoint**: User Story 3 완료 - 게시글 수정 기능 독립적으로 테스트 가능

---

## Phase 6: User Story 4 - 게시글 삭제 (Priority: P2)

**Goal**: 게시글 작성자가 본인이 작성한 글을 삭제한다

**Independent Test**: 작성자가 게시글을 삭제하고 목록에서 삭제 처리된 것을 확인

### Acceptance Criteria

1. 본인이 작성한 게시글에서 삭제 버튼 클릭 후 확인하면 게시글이 삭제 처리된다
2. 댓글이 있는 게시글 삭제 시 게시글은 "삭제된 게시글입니다"로 표시되고 댓글은 유지된다
3. 삭제된 게시글은 목록에서 제외되거나 삭제 표시가 된다
4. 관리자는 타인의 게시글도 삭제가 가능하다

### Tests for User Story 4

- [ ] T039 [P] [US4] PostService 삭제 로직 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/service/PostServiceDeleteTest.java`
  - 본인 게시글 삭제 성공 (Soft Delete)
  - 삭제 후 deleted=true, deletedAt, deletedBy 설정 확인
  - 타인 게시글 삭제 시도 시 PostAccessDeniedException 발생
  - 이미 삭제된 게시글 삭제 시도 시 PostDeletedException 발생
  - 관리자가 타인 게시글 삭제 가능 확인

- [ ] T040 [P] [US4] PostController 삭제 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/controller/PostControllerDeleteTest.java`
  - DELETE /api/v1/boards/{boardCode}/posts/{postId} - 본인 게시글 삭제 성공 204 No Content
  - DELETE /api/v1/boards/{boardCode}/posts/{postId} - 타인 게시글 삭제 시도 403 Forbidden
  - DELETE /api/v1/boards/{boardCode}/posts/{postId} - 이미 삭제된 게시글 410 Gone
  - DELETE /api/v1/boards/{boardCode}/posts/{postId} - 관리자 타인 게시글 삭제 성공

### Implementation for User Story 4

- [ ] T041 [US4] PostService 게시글 삭제 로직 구현 in `backend/src/main/java/igrus/web/board/service/PostService.java`
  - deletePost(String boardCode, Long postId, User currentUser): void
  - Soft Delete 적용 (SoftDeletableEntity.delete() 사용)
  - 작성자 본인 또는 관리자만 삭제 가능

- [ ] T042 [US4] PostController 게시글 삭제 엔드포인트 구현 in `backend/src/main/java/igrus/web/board/controller/PostController.java`
  - DELETE /api/v1/boards/{boardCode}/posts/{postId} - 게시글 삭제
  - Swagger 어노테이션 추가

**Checkpoint**: User Story 4 완료 - 게시글 삭제 기능 독립적으로 테스트 가능

---

## Phase 7: User Story 5 - 공지사항 작성 (Priority: P2)

**Goal**: 운영자가 공지사항을 작성하고 준회원 공개 여부를 설정한다

**Independent Test**: OPERATOR가 공지사항을 작성하고 준회원 공개 설정이 정상 동작하는지 확인

### Acceptance Criteria

1. OPERATOR가 공지사항에서 제목과 내용을 입력하고 저장하면 공지사항이 등록된다
2. "준회원에게 공개" 옵션을 선택하면 준회원도 해당 공지를 조회할 수 있다
3. 공지사항 작성 화면에는 익명 옵션이 존재하지 않는다

### Tests for User Story 5

- [ ] T043 [P] [US5] 공지사항 작성 로직 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/service/PostServiceNoticeTest.java`
  - OPERATOR가 공지사항 작성 성공
  - 정회원이 공지사항 작성 시도 시 BoardAccessDeniedException 발생
  - 준회원 공개 옵션 설정 성공
  - 공지사항에서 익명 옵션 사용 시 InvalidPostOptionException 발생
  - 공지사항에서 질문 옵션 사용 시 InvalidPostOptionException 발생

- [ ] T044 [P] [US5] 공지사항 조회 로직 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/service/PostServiceNoticeReadTest.java`
  - 준회원이 공지사항 목록 조회 시 준회원 공개 글만 반환
  - 정회원이 공지사항 목록 조회 시 모든 글 반환
  - 준회원이 비공개 공지사항 상세 조회 시도 시 PostAccessDeniedException 발생

- [ ] T045 [P] [US5] 공지사항 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/controller/PostControllerNoticeTest.java`
  - POST /api/v1/boards/notices/posts - OPERATOR 공지사항 작성 성공
  - POST /api/v1/boards/notices/posts - 정회원 공지사항 작성 시도 403 Forbidden
  - GET /api/v1/boards/notices/posts - 준회원 조회 시 공개 공지만 반환
  - GET /api/v1/boards/notices/posts/{postId} - 준회원이 비공개 공지 조회 403 Forbidden

### Implementation for User Story 5

- [ ] T046 [P] [US5] CreateNoticeRequest DTO 구현 in `backend/src/main/java/igrus/web/board/dto/request/CreateNoticeRequest.java`
  - 필드: title(@NotBlank, @Size(max=100)), content(@NotBlank), isVisibleToAssociate(기본값 false), imageUrls
  - 익명, 질문 옵션 필드 없음

- [ ] T047 [US5] PostService 공지사항 작성 로직 구현 in `backend/src/main/java/igrus/web/board/service/PostService.java`
  - createNotice(CreateNoticeRequest request, User author): PostCreateResponse
  - OPERATOR 이상만 작성 가능 검증
  - 준회원 공개 옵션 처리

- [ ] T048 [US5] PostService 공지사항 조회 시 준회원 필터링 적용 in `backend/src/main/java/igrus/web/board/service/PostService.java`
  - 준회원이 공지사항 조회 시 isVisibleToAssociate=true인 게시글만 반환
  - 상세 조회 시 권한 검증

- [ ] T049 [US5] PostController 공지사항 엔드포인트 통합 in `backend/src/main/java/igrus/web/board/controller/PostController.java`
  - 기존 엔드포인트에서 boardCode=notices일 때 특수 처리
  - 또는 별도 엔드포인트 생성 고려

**Checkpoint**: User Story 5 완료 - 공지사항 작성/조회 기능 독립적으로 테스트 가능

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 전체 기능에 걸친 개선 및 품질 향상

- [ ] T050 [P] SecurityConfig에 게시글 API 경로 권한 설정 추가 in `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`
  - /api/v1/boards/*/posts/** - 인증 필요 (ASSOCIATE 이상)

- [ ] T051 [P] Swagger API 문서 검토 및 보완 in `backend/src/main/java/igrus/web/board/controller/PostController.java`
  - 모든 엔드포인트에 적절한 설명 추가
  - 요청/응답 예시 추가
  - 에러 응답 케이스 문서화

- [ ] T052 [P] 조회수 증가 동시성 처리 검토 in `backend/src/main/java/igrus/web/board/service/PostService.java`
  - 동시 조회 시 조회수 정확성 확보 (낙관적 락 또는 별도 처리)

- [ ] T053 코드 리뷰 및 리팩토링
  - SOLID 원칙 준수 확인
  - N+1 쿼리 문제 점검 (특히 목록 조회 시)
  - 보안 취약점 점검 (OWASP Top 10)
  - XSS 방지 (게시글 내용)

- [ ] T054 전체 테스트 실행 및 커버리지 확인
  - 모든 단위 테스트 통과 확인
  - 모든 통합 테스트 통과 확인
  - 테스트 커버리지 확인

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Board 도메인 구현 완료 필요
- **Phase 2 (Foundational)**: Phase 1 완료 필요 - 모든 User Story 블로킹
- **Phase 3 (US1 - 게시글 작성)**: Phase 2 완료 필요
- **Phase 4 (US2 - 게시글 조회)**: Phase 2 완료 필요, Phase 3와 병렬 가능
- **Phase 5 (US3 - 게시글 수정)**: Phase 3 완료 필요 (게시글이 있어야 수정 가능)
- **Phase 6 (US4 - 게시글 삭제)**: Phase 3 완료 필요 (게시글이 있어야 삭제 가능)
- **Phase 7 (US5 - 공지사항 작성)**: Phase 2 완료 필요, Phase 3/4와 병렬 가능
- **Phase 8 (Polish)**: 모든 User Story 완료 필요

### User Story Dependencies

- **User Story 1 (P1 - 게시글 작성)**: Phase 2 완료 후 시작 - 독립적
- **User Story 2 (P1 - 게시글 조회)**: Phase 2 완료 후 시작 - US1과 병렬 가능
- **User Story 3 (P2 - 게시글 수정)**: US1 완료 필요 (수정할 게시글 필요)
- **User Story 4 (P2 - 게시글 삭제)**: US1 완료 필요 (삭제할 게시글 필요)
- **User Story 5 (P2 - 공지사항 작성)**: Phase 2 완료 후 시작 - 독립적

### Within Each User Story

- 테스트 작성 후 실패 확인 → 구현 → 테스트 통과 순서
- DTO → Service → Controller 순서
- 핵심 기능 → 부가 기능 순서

### Parallel Opportunities

- Phase 2의 T005, T006 엔티티 병렬 구현 가능
- Phase 2의 T007, T008 Repository 병렬 구현 가능
- Phase 2의 T009~T015 예외 클래스 병렬 구현 가능
- Phase 3의 T018, T019, T020 테스트 병렬 작성 가능
- Phase 3의 T021, T022 DTO 병렬 구현 가능
- Phase 4의 T026, T027 테스트 병렬 작성 가능
- Phase 4의 T028, T029, T030 DTO 병렬 구현 가능

---

## Parallel Example: Phase 2 Foundation

```bash
# 엔티티 병렬 구현
Task: "Post 엔티티 구현 in backend/.../Post.java"
Task: "PostImage 엔티티 구현 in backend/.../PostImage.java"

# Repository 병렬 구현
Task: "PostRepository 인터페이스 구현 in backend/.../PostRepository.java"
Task: "PostImageRepository 인터페이스 구현 in backend/.../PostImageRepository.java"

# 예외 클래스 병렬 구현
Task: "PostNotFoundException 예외 클래스 구현"
Task: "PostAccessDeniedException 예외 클래스 구현"
Task: "PostTitleTooLongException 예외 클래스 구현"
...
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료 (CRITICAL)
3. Phase 3: User Story 1 (게시글 작성) 완료
4. Phase 4: User Story 2 (게시글 조회) 완료
5. **STOP and VALIDATE**: 게시글 작성/조회 기능 독립 테스트
6. 배포/데모 가능

### Incremental Delivery

1. Setup + Foundational → Foundation 완료
2. User Story 1 (작성) → 테스트 → 배포/데모
3. User Story 2 (조회) → 테스트 → 배포/데모 (MVP!)
4. User Story 3 (수정) → 테스트 → 배포/데모
5. User Story 4 (삭제) → 테스트 → 배포/데모
6. User Story 5 (공지사항) → 테스트 → 배포/데모
7. 각 스토리가 이전 스토리를 깨뜨리지 않고 가치 추가

### Related Specs

이 태스크는 Post(게시글) 기능만 다룹니다. 다음 관련 기능은 별도 태스크로 구현:
- `board-spec.md` - 게시판 CRUD (선행 필수)
- `comment-spec.md` - 댓글/대댓글 기능
- `like-bookmark-spec.md` - 좋아요/북마크 기능

---

## Notes

- [P] 태스크 = 다른 파일, 의존성 없음
- [Story] 라벨로 User Story 추적 가능
- 각 User Story는 독립적으로 완료 및 테스트 가능
- 테스트 실패 확인 후 구현 시작
- 태스크 또는 논리적 그룹 완료 후 커밋
- Checkpoint에서 스토리 독립 검증 가능
- 모호한 태스크, 같은 파일 충돌, 독립성 깨는 크로스-스토리 의존성 지양
- 이미지 업로드는 외부 스토리지(S3) 사용 - 별도 이미지 업로드 API 필요 (추후 구현)

---

## Summary

| Phase | Task Count | Parallel Tasks |
|-------|------------|----------------|
| Phase 1: Setup | 3 | 2 |
| Phase 2: Foundational | 14 | 11 |
| Phase 3: User Story 1 (작성) | 8 | 5 |
| Phase 4: User Story 2 (조회) | 7 | 5 |
| Phase 5: User Story 3 (수정) | 6 | 4 |
| Phase 6: User Story 4 (삭제) | 4 | 2 |
| Phase 7: User Story 5 (공지사항) | 7 | 4 |
| Phase 8: Polish | 5 | 3 |
| **Total** | **54** | **36** |

**MVP Scope**: Phase 1 + Phase 2 + Phase 3 + Phase 4 = 32 tasks
**Independent Test per Story**: 각 User Story별 독립 테스트 가능
