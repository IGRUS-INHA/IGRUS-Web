# Tasks: 댓글 (Comment) 백엔드 구현

**Input**: `docs/feature/community/comment-spec.md`
**Prerequisites**: comment-spec.md (기능 명세서), Board/Post 기능 구현 완료 필요
**Tech Stack**: Java 21, Spring Boot 3.5.9, Spring Data JPA, MySQL 8.x, Flyway

**Tests**: 테스트 코드 작성 포함 (backend/CLAUDE.md 개발 규칙에 따름)

**Organization**: User Story 기반으로 구성하여 독립적 구현 및 테스트 가능

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 태스크가 속한 User Story (US1, US2, US3, US4, US5, US6, US7)
- 모든 태스크에 정확한 파일 경로 포함

## Path Conventions

- **Backend**: `backend/src/main/java/igrus/web/board/comment/`
- **Tests**: `backend/src/test/java/igrus/web/board/comment/`
- **Migrations**: `backend/src/main/resources/db/migration/`

## User Stories Overview

| Story | Priority | Title |
|-------|----------|-------|
| US1 | P1 | 댓글 작성 |
| US2 | P1 | 대댓글 작성 |
| US3 | P1 | 댓글 조회 |
| US4 | P2 | 댓글 삭제 |
| US5 | P2 | 댓글 좋아요 |
| US6 | P2 | 댓글 신고 |
| US7 | P3 | 댓글 멘션(@) |

---

## Phase 1: Setup (프로젝트 구조 설정)

**Purpose**: 댓글 도메인의 기본 패키지 구조 설정

- [ ] T001 댓글 도메인 패키지 구조 생성: `board/comment/domain/`, `board/comment/repository/`, `board/comment/service/`, `board/comment/controller/`, `board/comment/dto/`, `board/comment/exception/`
- [ ] T002 [P] Flyway 마이그레이션 버전 확인 (V6 이후 사용 가능 여부 확인)

---

## Phase 2: Foundational (핵심 인프라 - 모든 User Story 선행 조건)

**Purpose**: 모든 User Story가 의존하는 핵심 엔티티 및 인프라 구축

**CRITICAL**: 이 페이즈가 완료되어야 User Story 작업 시작 가능

**Prerequisites**: Board, Post 도메인 구현 완료 필요

### 2.1 데이터베이스 스키마

- [ ] T003 Flyway 마이그레이션 VX__create_comment_tables.sql 작성 in `backend/src/main/resources/db/migration/VX__create_comment_tables.sql`
  - comments 테이블: id, post_id(FK), parent_comment_id(FK, nullable), author_id(FK), content(VARCHAR 500), is_anonymous, is_deleted, deleted_at, deleted_by, created_at, updated_at
  - comment_likes 테이블: id, comment_id(FK), user_id(FK), created_at, UNIQUE(comment_id, user_id)
  - comment_reports 테이블: id, comment_id(FK), reporter_id(FK), reason(TEXT), status(ENUM: PENDING, RESOLVED, DISMISSED), resolved_at, resolved_by, created_at
  - 인덱스: comments(post_id), comments(parent_comment_id), comments(author_id), comment_likes(user_id)

### 2.2 핵심 도메인 엔티티

- [ ] T004 [P] Comment 엔티티 구현 in `backend/src/main/java/igrus/web/board/comment/domain/Comment.java`
  - SoftDeletableEntity 상속
  - 필드: post(ManyToOne), parentComment(ManyToOne, nullable), author(ManyToOne), content(max 500자), isAnonymous
  - 자기 참조 관계 설정 (대댓글)
  - 비즈니스 메서드: isReply(), canReplyTo(), validateContent()

- [ ] T005 [P] CommentLike 엔티티 구현 in `backend/src/main/java/igrus/web/board/comment/domain/CommentLike.java`
  - BaseEntity 상속
  - 필드: comment(ManyToOne), user(ManyToOne)
  - 복합 유니크 제약: (comment_id, user_id)

- [ ] T006 [P] CommentReport 엔티티 구현 in `backend/src/main/java/igrus/web/board/comment/domain/CommentReport.java`
  - BaseEntity 상속
  - 필드: comment(ManyToOne), reporter(ManyToOne), reason, status(ReportStatus), resolvedAt, resolvedBy

- [ ] T007 [P] ReportStatus enum 구현 in `backend/src/main/java/igrus/web/board/comment/domain/ReportStatus.java`
  - PENDING, RESOLVED, DISMISSED

### 2.3 Repository

- [ ] T008 [P] CommentRepository 인터페이스 구현 in `backend/src/main/java/igrus/web/board/comment/repository/CommentRepository.java`
  - findByPostIdOrderByCreatedAtAsc(Long postId): List<Comment>
  - findByPostIdAndDeletedFalseOrderByCreatedAtAsc(Long postId): List<Comment>
  - findByParentCommentId(Long parentCommentId): List<Comment>
  - countByPostIdAndDeletedFalse(Long postId): Long
  - existsByIdAndAuthorId(Long id, Long authorId): boolean

- [ ] T009 [P] CommentLikeRepository 인터페이스 구현 in `backend/src/main/java/igrus/web/board/comment/repository/CommentLikeRepository.java`
  - findByCommentIdAndUserId(Long commentId, Long userId): Optional<CommentLike>
  - existsByCommentIdAndUserId(Long commentId, Long userId): boolean
  - countByCommentId(Long commentId): Long
  - deleteByCommentIdAndUserId(Long commentId, Long userId): void

- [ ] T010 [P] CommentReportRepository 인터페이스 구현 in `backend/src/main/java/igrus/web/board/comment/repository/CommentReportRepository.java`
  - findByStatus(ReportStatus status): List<CommentReport>
  - findByCommentId(Long commentId): List<CommentReport>
  - existsByCommentIdAndReporterId(Long commentId, Long reporterId): boolean

### 2.4 예외 클래스

- [ ] T011 [P] CommentNotFoundException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/comment/exception/CommentNotFoundException.java`
- [ ] T012 [P] CommentAccessDeniedException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/comment/exception/CommentAccessDeniedException.java`
- [ ] T013 [P] InvalidCommentException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/comment/exception/InvalidCommentException.java`
- [ ] T014 [P] CommentLikeException 예외 클래스 구현 in `backend/src/main/java/igrus/web/board/comment/exception/CommentLikeException.java`
- [ ] T015 [P] ErrorCode에 댓글 관련 에러 코드 추가 in `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
  - COMMENT_NOT_FOUND, COMMENT_ACCESS_DENIED, COMMENT_CONTENT_TOO_LONG, COMMENT_CONTENT_EMPTY
  - REPLY_TO_REPLY_NOT_ALLOWED, POST_DELETED_CANNOT_COMMENT, ANONYMOUS_NOT_ALLOWED
  - CANNOT_LIKE_OWN_COMMENT, ALREADY_LIKED_COMMENT, LIKE_NOT_FOUND
  - ALREADY_REPORTED_COMMENT, INVALID_REPORT_REASON

**Checkpoint**: Foundation 완료 - User Story 구현 시작 가능

---

## Phase 3: User Story 1 - 댓글 작성 (Priority: P1) MVP

**Goal**: 정회원이 게시글에 의견이나 정보를 담은 댓글을 작성한다

**Independent Test**: 정회원이 게시글에 댓글을 작성하고 댓글 목록에서 확인할 수 있으면 테스트 통과

### Acceptance Criteria

1. 정회원이 게시글 상세 페이지에서 댓글 내용을 입력하고 등록하면 댓글이 추가됨
2. 자유게시판에서 익명 옵션 선택 시 작성자가 "익명"으로 표시됨
3. 500자 초과 시 작성 거부 및 에러 메시지 표시
4. 정보공유 게시판에서는 익명 옵션이 존재하지 않음
5. 삭제된 게시글에는 댓글 작성 불가

### Tests for User Story 1

- [ ] T016 [P] [US1] CommentService 댓글 작성 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/service/CommentServiceCreateTest.java`
  - 정회원이 일반 댓글 작성 성공
  - 익명 허용 게시판에서 익명 댓글 작성 성공
  - 익명 비허용 게시판에서 익명 댓글 작성 시 실패
  - 500자 초과 내용으로 작성 시 InvalidCommentException 발생
  - 빈 내용으로 작성 시 InvalidCommentException 발생
  - 삭제된 게시글에 댓글 작성 시 실패

- [ ] T017 [P] [US1] CommentController 댓글 작성 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/controller/CommentControllerCreateTest.java`
  - POST /api/v1/posts/{postId}/comments - 인증된 사용자가 댓글 작성 성공 (201 Created)
  - POST /api/v1/posts/{postId}/comments - 비인증 사용자가 접근 시 401 Unauthorized
  - POST /api/v1/posts/{postId}/comments - 준회원이 접근 시 403 Forbidden
  - POST /api/v1/posts/{postId}/comments - 500자 초과 시 400 Bad Request
  - POST /api/v1/posts/{postId}/comments - 존재하지 않는 게시글에 작성 시 404 Not Found

### Implementation for User Story 1

- [ ] T018 [P] [US1] CreateCommentRequest DTO 구현 in `backend/src/main/java/igrus/web/board/comment/dto/request/CreateCommentRequest.java`
  - 필드: content(@NotBlank, @Size(max=500)), isAnonymous
  - Bean Validation 적용

- [ ] T019 [P] [US1] CommentResponse DTO 구현 in `backend/src/main/java/igrus/web/board/comment/dto/response/CommentResponse.java`
  - 필드: id, postId, content, authorId, authorName, isAnonymous, isDeleted, likeCount, isLikedByMe, createdAt
  - 익명인 경우 authorId=null, authorName="익명"으로 변환

- [ ] T020 [US1] CommentService 댓글 작성 로직 구현 in `backend/src/main/java/igrus/web/board/comment/service/CommentService.java`
  - createComment(Long postId, CreateCommentRequest request, Long userId): CommentResponse
  - 게시글 존재 여부 및 삭제 상태 검증
  - 익명 허용 게시판 검증 (BoardPermissionService 연동)
  - 내용 길이 검증 (500자)

- [ ] T021 [US1] CommentController 댓글 작성 엔드포인트 구현 in `backend/src/main/java/igrus/web/board/comment/controller/CommentController.java`
  - POST /api/v1/posts/{postId}/comments - 댓글 작성
  - Swagger 어노테이션 (@Operation, @ApiResponse) 추가
  - @Valid 적용

- [ ] T022 [US1] SecurityConfig에 댓글 API 경로 권한 설정 추가 in `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`
  - /api/v1/posts/*/comments/** - 인증 필요 (REGULAR 이상)

**Checkpoint**: User Story 1 완료 - 댓글 작성 기능 독립적으로 테스트 가능

---

## Phase 4: User Story 2 - 대댓글 작성 (Priority: P1)

**Goal**: 사용자가 특정 댓글에 답글(대댓글)을 작성하여 대화를 이어간다

**Independent Test**: 댓글에 대댓글을 작성하고 해당 댓글 하위에 표시되는지 확인

### Acceptance Criteria

1. 댓글에서 답글 버튼 클릭하고 내용 입력하여 등록하면 해당 댓글 하위에 대댓글 표시
2. 대댓글에는 답글을 달 수 없음 (1단계까지만 허용)
3. 익명 허용 게시판에서는 대댓글도 익명 작성 가능

### Tests for User Story 2

- [ ] T023 [P] [US2] CommentService 대댓글 작성 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/service/CommentServiceReplyTest.java`
  - 댓글에 대댓글 작성 성공
  - 대댓글에 답글 작성 시 InvalidCommentException 발생
  - 삭제된 댓글에 대댓글 작성 시 실패
  - 익명 허용 게시판에서 익명 대댓글 작성 성공

- [ ] T024 [P] [US2] CommentController 대댓글 작성 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/controller/CommentControllerReplyTest.java`
  - POST /api/v1/posts/{postId}/comments/{commentId}/replies - 대댓글 작성 성공 (201 Created)
  - POST /api/v1/posts/{postId}/comments/{commentId}/replies - 대댓글에 답글 시도 시 400 Bad Request

### Implementation for User Story 2

- [ ] T025 [US2] CommentService 대댓글 작성 로직 구현 in `backend/src/main/java/igrus/web/board/comment/service/CommentService.java`
  - createReply(Long postId, Long parentCommentId, CreateCommentRequest request, Long userId): CommentResponse
  - 부모 댓글 존재 여부 검증
  - 부모 댓글이 대댓글이 아닌지 검증 (depth 1단계 제한)
  - 부모 댓글 삭제 여부와 무관하게 대댓글 작성 가능 (기존 대댓글 유지 정책)

- [ ] T026 [US2] CommentController 대댓글 작성 엔드포인트 구현 in `backend/src/main/java/igrus/web/board/comment/controller/CommentController.java`
  - POST /api/v1/posts/{postId}/comments/{commentId}/replies - 대댓글 작성
  - Swagger 어노테이션 추가

**Checkpoint**: User Story 2 완료 - 대댓글 작성 기능 독립적으로 테스트 가능

---

## Phase 5: User Story 3 - 댓글 조회 (Priority: P1)

**Goal**: 사용자가 게시글의 모든 댓글과 대댓글을 확인한다

**Independent Test**: 게시글 상세 페이지에서 댓글과 대댓글이 계층 구조로 표시되는지 확인

### Acceptance Criteria

1. 게시글 상세 페이지 로드 시 모든 댓글과 대댓글이 계층 구조로 표시
2. 삭제된 댓글은 "삭제된 댓글입니다"로 표시
3. 삭제된 댓글의 대댓글은 정상 표시되고 부모 댓글만 "삭제된 댓글입니다"로 표시
4. 댓글 목록은 등록순(오래된 댓글 먼저)으로 정렬
5. 익명 댓글은 작성자 정보가 "익명"으로 표시 (관리자는 실제 작성자 확인 가능)

### Tests for User Story 3

- [ ] T027 [P] [US3] CommentService 댓글 조회 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/service/CommentServiceReadTest.java`
  - 게시글의 모든 댓글 조회 성공 (계층 구조)
  - 삭제된 댓글이 "삭제된 댓글입니다"로 표시
  - 삭제된 댓글의 대댓글이 정상 표시
  - 익명 댓글의 작성자 정보가 숨겨짐
  - 댓글이 등록순으로 정렬됨

- [ ] T028 [P] [US3] CommentController 댓글 조회 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/controller/CommentControllerReadTest.java`
  - GET /api/v1/posts/{postId}/comments - 댓글 목록 조회 성공 (200 OK)
  - GET /api/v1/posts/{postId}/comments - 계층 구조로 반환됨 확인
  - GET /api/v1/posts/{postId}/comments - 존재하지 않는 게시글 조회 시 404 Not Found

### Implementation for User Story 3

- [ ] T029 [P] [US3] CommentListResponse DTO 구현 in `backend/src/main/java/igrus/web/board/comment/dto/response/CommentListResponse.java`
  - 필드: comments(List<CommentWithRepliesResponse>), totalCount
  - 계층 구조 (댓글 + 대댓글 리스트)

- [ ] T030 [P] [US3] CommentWithRepliesResponse DTO 구현 in `backend/src/main/java/igrus/web/board/comment/dto/response/CommentWithRepliesResponse.java`
  - CommentResponse 확장
  - 필드: replies(List<CommentResponse>)
  - 삭제된 댓글 처리 (content="삭제된 댓글입니다", authorId=null, authorName=null)

- [ ] T031 [US3] CommentService 댓글 조회 로직 구현 in `backend/src/main/java/igrus/web/board/comment/service/CommentService.java`
  - getCommentsByPostId(Long postId, Long currentUserId): CommentListResponse
  - 계층 구조로 변환 (부모 댓글 + 대댓글 그룹핑)
  - 등록순 정렬 (createdAt ASC)
  - 삭제된 댓글 처리 (대댓글 있으면 "삭제된 댓글입니다"로 표시, 없으면 제외 가능)
  - 익명 댓글 작성자 정보 마스킹

- [ ] T032 [US3] CommentController 댓글 조회 엔드포인트 구현 in `backend/src/main/java/igrus/web/board/comment/controller/CommentController.java`
  - GET /api/v1/posts/{postId}/comments - 댓글 목록 조회
  - Swagger 어노테이션 추가

**Checkpoint**: User Story 3 완료 - 댓글 조회 기능 독립적으로 테스트 가능

---

## Phase 6: User Story 4 - 댓글 삭제 (Priority: P2)

**Goal**: 댓글 작성자가 본인이 작성한 댓글을 삭제한다

**Independent Test**: 작성자가 댓글을 삭제하고 "삭제된 댓글입니다"로 표시되는지 확인

### Acceptance Criteria

1. 본인이 작성한 댓글에서 삭제 버튼 클릭 시 "삭제된 댓글입니다"로 변경
2. 대댓글이 있는 댓글 삭제 시 부모 댓글은 "삭제된 댓글입니다"로 표시되고 대댓글은 유지
3. 타인이 작성한 댓글에는 삭제 버튼이 표시되지 않거나 접근 거부
4. 관리자는 타인의 댓글도 삭제 가능

### Tests for User Story 4

- [ ] T033 [P] [US4] CommentService 댓글 삭제 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/service/CommentServiceDeleteTest.java`
  - 본인 댓글 삭제 성공 (Soft Delete)
  - 대댓글이 있는 댓글 삭제 시 "삭제된 댓글입니다"로 표시, 대댓글 유지
  - 대댓글이 없는 댓글 삭제 성공
  - 타인 댓글 삭제 시 CommentAccessDeniedException 발생
  - 관리자가 타인 댓글 삭제 성공

- [ ] T034 [P] [US4] CommentController 댓글 삭제 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/controller/CommentControllerDeleteTest.java`
  - DELETE /api/v1/posts/{postId}/comments/{commentId} - 본인 댓글 삭제 성공 (204 No Content)
  - DELETE /api/v1/posts/{postId}/comments/{commentId} - 타인 댓글 삭제 시 403 Forbidden
  - DELETE /api/v1/posts/{postId}/comments/{commentId} - 관리자가 타인 댓글 삭제 성공
  - DELETE /api/v1/posts/{postId}/comments/{commentId} - 존재하지 않는 댓글 삭제 시 404 Not Found

### Implementation for User Story 4

- [ ] T035 [US4] CommentService 댓글 삭제 로직 구현 in `backend/src/main/java/igrus/web/board/comment/service/CommentService.java`
  - deleteComment(Long postId, Long commentId, Long userId, UserRole role): void
  - 본인 댓글 또는 관리자 권한 검증
  - Soft Delete 적용 (SoftDeletableEntity.delete() 호출)
  - 대댓글 유무와 관계없이 부모 댓글만 삭제 처리

- [ ] T036 [US4] CommentController 댓글 삭제 엔드포인트 구현 in `backend/src/main/java/igrus/web/board/comment/controller/CommentController.java`
  - DELETE /api/v1/posts/{postId}/comments/{commentId} - 댓글 삭제
  - Swagger 어노테이션 추가

**Checkpoint**: User Story 4 완료 - 댓글 삭제 기능 독립적으로 테스트 가능

---

## Phase 7: User Story 5 - 댓글 좋아요 (Priority: P2)

**Goal**: 사용자가 댓글에 좋아요를 표시하고 취소할 수 있다

**Independent Test**: 댓글에 좋아요를 누르고 좋아요 수가 증가/감소하는지 확인

### Acceptance Criteria

1. 좋아요 수는 모든 사용자에게 공개
2. 좋아요 취소 가능
3. 본인 댓글에는 좋아요 불가
4. 중복 좋아요 불가

### Tests for User Story 5

- [ ] T037 [P] [US5] CommentLikeService 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/service/CommentLikeServiceTest.java`
  - 댓글 좋아요 성공
  - 좋아요 취소 성공
  - 본인 댓글에 좋아요 시 CommentLikeException 발생
  - 이미 좋아요한 댓글에 중복 좋아요 시 CommentLikeException 발생
  - 좋아요하지 않은 댓글 취소 시 LikeNotFoundException 발생

- [ ] T038 [P] [US5] CommentLikeController 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/controller/CommentLikeControllerTest.java`
  - POST /api/v1/comments/{commentId}/likes - 좋아요 성공 (201 Created)
  - DELETE /api/v1/comments/{commentId}/likes - 좋아요 취소 성공 (204 No Content)
  - POST /api/v1/comments/{commentId}/likes - 본인 댓글 좋아요 시 400 Bad Request

### Implementation for User Story 5

- [ ] T039 [US5] CommentLikeService 구현 in `backend/src/main/java/igrus/web/board/comment/service/CommentLikeService.java`
  - likeComment(Long commentId, Long userId): void
  - unlikeComment(Long commentId, Long userId): void
  - getLikeCount(Long commentId): Long
  - hasUserLiked(Long commentId, Long userId): boolean
  - 본인 댓글 좋아요 방지 검증

- [ ] T040 [US5] CommentLikeController 구현 in `backend/src/main/java/igrus/web/board/comment/controller/CommentLikeController.java`
  - POST /api/v1/comments/{commentId}/likes - 좋아요
  - DELETE /api/v1/comments/{commentId}/likes - 좋아요 취소
  - Swagger 어노테이션 추가

**Checkpoint**: User Story 5 완료 - 댓글 좋아요 기능 독립적으로 테스트 가능

---

## Phase 8: User Story 6 - 댓글 신고 (Priority: P2)

**Goal**: 사용자가 부적절한 댓글을 신고하고 관리자가 검토할 수 있다

**Independent Test**: 댓글 신고 후 관리자 검토 대기열에 추가되는지 확인

### Acceptance Criteria

1. 신고 접수 시 관리자 검토 대기열에 추가
2. 관리자가 수동으로 처리 (승인/반려)
3. 동일 사용자가 동일 댓글 중복 신고 불가

### Tests for User Story 6

- [ ] T041 [P] [US6] CommentReportService 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/service/CommentReportServiceTest.java`
  - 댓글 신고 성공
  - 중복 신고 시 실패
  - 관리자가 신고 처리 (RESOLVED) 성공
  - 관리자가 신고 반려 (DISMISSED) 성공
  - PENDING 상태 신고 목록 조회 성공

- [ ] T042 [P] [US6] CommentReportController 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/controller/CommentReportControllerTest.java`
  - POST /api/v1/comments/{commentId}/reports - 신고 성공 (201 Created)
  - POST /api/v1/comments/{commentId}/reports - 중복 신고 시 400 Bad Request
  - GET /api/v1/admin/comment-reports - 관리자 신고 목록 조회 성공
  - PATCH /api/v1/admin/comment-reports/{reportId} - 신고 처리 성공

### Implementation for User Story 6

- [ ] T043 [P] [US6] CreateCommentReportRequest DTO 구현 in `backend/src/main/java/igrus/web/board/comment/dto/request/CreateCommentReportRequest.java`
  - 필드: reason(@NotBlank)

- [ ] T044 [P] [US6] CommentReportResponse DTO 구현 in `backend/src/main/java/igrus/web/board/comment/dto/response/CommentReportResponse.java`
  - 필드: id, commentId, reporterId, reason, status, createdAt, resolvedAt

- [ ] T045 [P] [US6] UpdateReportStatusRequest DTO 구현 in `backend/src/main/java/igrus/web/board/comment/dto/request/UpdateReportStatusRequest.java`
  - 필드: status(ReportStatus)

- [ ] T046 [US6] CommentReportService 구현 in `backend/src/main/java/igrus/web/board/comment/service/CommentReportService.java`
  - reportComment(Long commentId, CreateCommentReportRequest request, Long reporterId): CommentReportResponse
  - getPendingReports(): List<CommentReportResponse>
  - updateReportStatus(Long reportId, UpdateReportStatusRequest request, Long adminId): void
  - 중복 신고 방지 검증

- [ ] T047 [US6] CommentReportController 구현 in `backend/src/main/java/igrus/web/board/comment/controller/CommentReportController.java`
  - POST /api/v1/comments/{commentId}/reports - 댓글 신고
  - GET /api/v1/admin/comment-reports - 신고 목록 조회 (관리자)
  - PATCH /api/v1/admin/comment-reports/{reportId} - 신고 처리 (관리자)
  - Swagger 어노테이션 추가

- [ ] T048 [US6] SecurityConfig에 관리자 API 경로 권한 설정 추가 in `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`
  - /api/v1/admin/comment-reports/** - OPERATOR 이상

**Checkpoint**: User Story 6 완료 - 댓글 신고 기능 독립적으로 테스트 가능

---

## Phase 9: User Story 7 - 댓글 멘션(@) (Priority: P3)

**Goal**: 사용자가 @사용자명으로 다른 정회원을 멘션하여 알림을 보낼 수 있다

**Independent Test**: 댓글에서 @사용자명으로 멘션하면 해당 사용자에게 알림이 발송되는지 확인

### Acceptance Criteria

1. @사용자명으로 정회원 이상의 사용자 멘션 가능
2. 멘션된 사용자에게 인앱 알림 발송
3. 멘션된 사용자에게 이메일 알림 발송

### Tests for User Story 7

- [ ] T049 [P] [US7] CommentMentionService 단위 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/service/CommentMentionServiceTest.java`
  - 댓글 내용에서 멘션 파싱 성공 (@username 추출)
  - 유효한 정회원 사용자 멘션 시 알림 발송
  - 존재하지 않는 사용자 멘션 시 무시
  - 준회원 멘션 시 무시

- [ ] T050 [P] [US7] 멘션 알림 통합 테스트 작성 in `backend/src/test/java/igrus/web/board/comment/integration/CommentMentionIntegrationTest.java`
  - 댓글 작성 시 멘션된 사용자에게 알림 발송 확인

### Implementation for User Story 7

- [ ] T051 [US7] CommentMentionService 구현 in `backend/src/main/java/igrus/web/board/comment/service/CommentMentionService.java`
  - extractMentions(String content): List<String> - @username 패턴 추출
  - processMentions(Comment comment, List<String> mentionedUsernames): void
  - 정회원 이상 사용자만 멘션 대상
  - 알림 서비스 연동 (인앱 알림 + 이메일)

- [ ] T052 [US7] CommentService에 멘션 처리 로직 통합 in `backend/src/main/java/igrus/web/board/comment/service/CommentService.java`
  - 댓글/대댓글 작성 시 CommentMentionService 호출
  - 비동기 처리 고려 (@Async)

**Checkpoint**: User Story 7 완료 - 댓글 멘션 기능 독립적으로 테스트 가능

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: 전체 기능에 걸친 개선 및 품질 향상

- [ ] ~~T053 [P] Rate Limiting 설정 추가~~ (추후 구현으로 연기 - FR-011 Deferred)

- [ ] T054 [P] GlobalExceptionHandler에 댓글 예외 핸들러 추가 in `backend/src/main/java/igrus/web/common/exception/GlobalExceptionHandler.java`
  - CommentNotFoundException → 404 Not Found
  - CommentAccessDeniedException → 403 Forbidden
  - InvalidCommentException → 400 Bad Request
  - CommentLikeException → 400 Bad Request

- [ ] T055 [P] Swagger API 문서 검토 및 보완
  - 모든 엔드포인트에 적절한 설명 추가
  - 요청/응답 예시 추가
  - 에러 응답 케이스 문서화

- [ ] T056 코드 리뷰 및 리팩토링
  - SOLID 원칙 준수 확인
  - N+1 쿼리 문제 점검 (댓글 + 대댓글 + 좋아요 수 조회)
  - 보안 취약점 점검 (OWASP Top 10)

- [ ] T057 전체 테스트 실행 및 커버리지 확인
  - 모든 단위 테스트 통과 확인
  - 모든 통합 테스트 통과 확인
  - 테스트 커버리지 확인

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 의존성 없음 - 즉시 시작 가능
- **Phase 2 (Foundational)**: Phase 1 완료 필요, Board/Post 기능 구현 완료 필요 - 모든 User Story 블로킹
- **Phase 3 (US1)**: Phase 2 완료 필요
- **Phase 4 (US2)**: Phase 3 완료 필요 (댓글 작성 기능 필요)
- **Phase 5 (US3)**: Phase 3 완료 필요 (조회할 댓글 필요)
- **Phase 6 (US4)**: Phase 3 완료 필요
- **Phase 7 (US5)**: Phase 3 완료 필요, Phase 3/4/5와 병렬 가능
- **Phase 8 (US6)**: Phase 3 완료 필요, Phase 3/4/5와 병렬 가능
- **Phase 9 (US7)**: Phase 3 완료 필요, 알림 시스템 연동 필요
- **Phase 10 (Polish)**: 모든 User Story 완료 필요

### User Story Dependencies

- **User Story 1 (P1)**: Phase 2 완료 후 시작 - 독립적 (MVP)
- **User Story 2 (P1)**: US1 완료 후 시작 - 댓글 존재 필요
- **User Story 3 (P1)**: US1 완료 후 시작 - 댓글 존재 필요, US2와 병렬 가능
- **User Story 4 (P2)**: US1 완료 후 시작 - 댓글 존재 필요
- **User Story 5 (P2)**: US1 완료 후 시작 - 댓글 존재 필요, US4와 병렬 가능
- **User Story 6 (P2)**: US1 완료 후 시작 - 댓글 존재 필요, US4/5와 병렬 가능
- **User Story 7 (P3)**: US1 완료 후 시작 - 알림 시스템 필요

### Within Each User Story

- 테스트 작성 후 실패 확인 → 구현 → 테스트 통과 순서
- DTO → Service → Controller 순서
- 핵심 기능 → 부가 기능 순서

### Parallel Opportunities

- Phase 2의 T004, T005, T006, T007 병렬 실행 가능
- Phase 2의 T008, T009, T010 병렬 실행 가능
- Phase 2의 T011~T015 예외 클래스 병렬 구현 가능
- Phase 3의 T016, T017 테스트 병렬 작성 가능
- Phase 3의 T018, T019 DTO 병렬 구현 가능
- US4, US5, US6은 US1 완료 후 병렬 작업 가능

---

## Parallel Example: Phase 2 Foundation

```bash
# 엔티티 병렬 구현
Task: "Comment 엔티티 구현 in backend/.../Comment.java"
Task: "CommentLike 엔티티 구현 in backend/.../CommentLike.java"
Task: "CommentReport 엔티티 구현 in backend/.../CommentReport.java"
Task: "ReportStatus enum 구현 in backend/.../ReportStatus.java"

# Repository 병렬 구현
Task: "CommentRepository 인터페이스 구현 in backend/.../CommentRepository.java"
Task: "CommentLikeRepository 인터페이스 구현 in backend/.../CommentLikeRepository.java"
Task: "CommentReportRepository 인터페이스 구현 in backend/.../CommentReportRepository.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료 (CRITICAL)
3. Phase 3: User Story 1 완료 (댓글 작성)
4. **STOP and VALIDATE**: 댓글 작성 기능 독립 테스트
5. 배포/데모 가능

### Core Features (US1 + US2 + US3 + US4)

1. Setup + Foundational → Foundation 완료
2. User Story 1 → 테스트 → (MVP!)
3. User Story 2 (대댓글) → 테스트
4. User Story 3 (조회) → 테스트
5. User Story 4 (삭제) → 테스트
6. 핵심 댓글 기능 완료

### Full Features

1. Core Features 완료
2. User Story 5 (좋아요) → 테스트 → 배포/데모
3. User Story 6 (신고) → 테스트 → 배포/데모
4. User Story 7 (멘션) → 테스트 → 배포/데모
5. 전체 기능 완료

### Related Specs

이 태스크는 Comment(댓글) 기능만 다룹니다. 다음 관련 기능은 별도 태스크로 구현:
- `board-spec.md` - 게시판 기능 (선행 구현 필요)
- `post-spec.md` - 게시글 CRUD (선행 구현 필요)
- `like-bookmark-spec.md` - 게시글 좋아요/북마크 기능

---

## Notes

- [P] 태스크 = 다른 파일, 의존성 없음
- [Story] 라벨로 User Story 추적 가능
- 각 User Story는 독립적으로 완료 및 테스트 가능
- 테스트 실패 확인 후 구현 시작
- 태스크 또는 논리적 그룹 완료 후 커밋
- Checkpoint에서 스토리 독립 검증 가능
- Board/Post 기능이 선행 구현되어야 함
- 모호한 태스크, 같은 파일 충돌, 독립성 깨는 크로스-스토리 의존성 지양

---

## Summary

| Phase | Task Count | Parallel Tasks |
|-------|------------|----------------|
| Phase 1: Setup | 2 | 1 |
| Phase 2: Foundational | 13 | 11 |
| Phase 3: US1 - 댓글 작성 | 7 | 4 |
| Phase 4: US2 - 대댓글 작성 | 4 | 2 |
| Phase 5: US3 - 댓글 조회 | 6 | 4 |
| Phase 6: US4 - 댓글 삭제 | 4 | 2 |
| Phase 7: US5 - 댓글 좋아요 | 4 | 2 |
| Phase 8: US6 - 댓글 신고 | 8 | 5 |
| Phase 9: US7 - 댓글 멘션 | 4 | 2 |
| Phase 10: Polish | 4 | 2 |
| **Total** | **56** | **35** |

**MVP Scope**: Phase 1 + Phase 2 + Phase 3 (US1) = 22 tasks
**Core Features Scope**: Phase 1-6 (US1-4) = 36 tasks
**Independent Test per Story**: 각 User Story별 독립 테스트 가능
