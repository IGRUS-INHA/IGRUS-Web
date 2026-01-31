# Tasks: 댓글 좋아요 조회 배치 쿼리 리팩토링

**관련 ADR**: `docs/adr/v20260128-comment_like_batch_query.md`
**대상 파일**: `CommentService.java`, `CommentLikeRepository.java`
**목적**: 루프 내 다중 쿼리 문제 해결 (2N+1 쿼리 → 3개 쿼리)

---

## 현재 문제

```java
// CommentService.getCommentsByPostId() - 현재 구현
for (Comment comment : allComments) {
    long likeCount = commentLikeRepository.countByCommentId(comment.getId());  // N번
    boolean isLikedByMe = commentLikeRepository.existsByCommentIdAndUserId(...);  // N번
}
```

원본
```java
for (Comment comment : allComments) {
    if (comment.isReply()) {
        replies.add(comment);
    } else {
        long likeCount = commentLikeRepository.countByCommentId(comment.getId());
        boolean isLikedByMe = currentUserId != null &&
                commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId);
        parentCommentMap.put(comment.getId(), CommentWithRepliesResponse.from(comment, likeCount, isLikedByMe));
    }
}
```

- 댓글 N개 조회 시 **2N개 추가 쿼리** 발생
- 댓글이 많아질수록 성능 저하

---

## 목표

| 항목 | Before | After |
|------|--------|-------|
| 쿼리 수 | 2N+1 | 3 |
| 구조 변경 | - | Repository 메서드 추가 |
| 동시성 이슈 | 없음 | 없음 |

---

## Tasks

### Phase 1: Repository 메서드 추가

- [ ] T001 CommentLikeRepository에 배치 좋아요 수 조회 메서드 추가
  - 파일: `backend/src/main/java/igrus/web/community/comment/repository/CommentLikeRepository.java`
  - 메서드: `countLikesByCommentIds(List<Long> commentIds)`
  - 반환: `List<Object[]>` (commentId, count)
  - JPQL:
    ```java
    @Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl " +
           "WHERE cl.comment.id IN :commentIds GROUP BY cl.comment.id")
    List<Object[]> countLikesByCommentIds(@Param("commentIds") List<Long> commentIds);
    ```

- [ ] T002 CommentLikeRepository에 배치 좋아요 여부 조회 메서드 추가
  - 파일: `backend/src/main/java/igrus/web/community/comment/repository/CommentLikeRepository.java`
  - 메서드: `findLikedCommentIdsByUser(List<Long> commentIds, Long userId)`
  - 반환: `List<Long>` (사용자가 좋아요한 commentId 목록)
  - JPQL:
    ```java
    @Query("SELECT cl.comment.id FROM CommentLike cl " +
           "WHERE cl.comment.id IN :commentIds AND cl.user.id = :userId")
    List<Long> findLikedCommentIdsByUser(@Param("commentIds") List<Long> commentIds,
                                          @Param("userId") Long userId);
    ```

### Phase 2: Service 리팩토링

- [ ] T003 CommentService에 헬퍼 메서드 추가
  - 파일: `backend/src/main/java/igrus/web/community/comment/service/CommentService.java`
  - 추가할 메서드:
    ```java
    private Map<Long, Long> getLikeCountsMap(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentLikeRepository.countLikesByCommentIds(commentIds)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]
            ));
    }

    private Set<Long> getLikedCommentIds(List<Long> commentIds, Long userId) {
        if (commentIds.isEmpty() || userId == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(
            commentLikeRepository.findLikedCommentIdsByUser(commentIds, userId)
        );
    }
    ```

- [ ] T004 CommentService.getCommentsByPostId() 리팩토링
  - 파일: `backend/src/main/java/igrus/web/community/comment/service/CommentService.java`
  - 변경 내용:
    1. 모든 댓글 ID 수집
    2. 배치 쿼리로 좋아요 정보 조회
    3. Map/Set에서 조회하여 DTO 생성
  - 리팩토링 후 코드:
    ```java
    public CommentListResponse getCommentsByPostId(Long postId, Long currentUserId) {
        // ... 기존 검증 로직 ...

        List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        // 모든 댓글 ID 수집
        List<Long> commentIds = allComments.stream()
            .map(Comment::getId)
            .collect(Collectors.toList());

        // 배치 쿼리로 좋아요 정보 조회
        Map<Long, Long> likeCounts = getLikeCountsMap(commentIds);
        Set<Long> likedCommentIds = getLikedCommentIds(commentIds, currentUserId);

        // DTO 변환 시 Map/Set에서 조회
        for (Comment comment : allComments) {
            long likeCount = likeCounts.getOrDefault(comment.getId(), 0L);
            boolean isLikedByMe = likedCommentIds.contains(comment.getId());
            // ... DTO 생성 ...
        }
        // ...
    }
    ```

### Phase 3: 테스트

- [ ] T005 배치 쿼리 Repository 메서드 단위 테스트
  - 파일: `backend/src/test/java/igrus/web/community/comment/repository/CommentLikeRepositoryTest.java`
  - 테스트 케이스:
    - 여러 댓글의 좋아요 수 조회 성공
    - 좋아요 없는 댓글은 결과에 미포함 확인
    - 빈 리스트 입력 시 빈 결과 반환
    - 사용자가 좋아요한 댓글 ID 목록 조회 성공

- [ ] T006 리팩토링된 Service 메서드 테스트
  - 파일: `backend/src/test/java/igrus/web/community/comment/service/CommentServiceTest.java`
  - 기존 테스트 케이스 통과 확인
  - 쿼리 수 검증 (선택적 - DataJpaTest에서 쿼리 로그 확인)

### Phase 4: 검증

- [ ] T007 전체 테스트 실행
  - 기존 테스트 모두 통과 확인
  - `./gradlew test --tests "igrus.web.community.comment.*"`

- [ ] T008 수동 테스트
  - 댓글 목록 조회 API 호출
  - 쿼리 로그 확인 (3개 쿼리인지 검증)
  - 응답 데이터 정상 확인

---

## 의존성

```
T001 ──┬──> T003 ──> T004 ──> T005 ──> T007 ──> T008
T002 ──┘                  └──> T006 ──┘
```

- T001, T002: 병렬 가능
- T003: T001, T002 완료 후
- T004: T003 완료 후
- T005, T006: T004 완료 후 병렬 가능
- T007: T005, T006 완료 후
- T008: T007 완료 후

---

## 예상 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `CommentLikeRepository.java` | 배치 조회 메서드 2개 추가 |
| `CommentService.java` | 헬퍼 메서드 2개 추가, getCommentsByPostId() 리팩토링 |
| `CommentLikeRepositoryTest.java` | 테스트 케이스 추가 (신규 파일 가능) |
| `CommentServiceTest.java` | 기존 테스트 검증 |

---

## 롤백 계획

리팩토링 실패 시:
1. Repository 메서드는 추가만 하므로 기존 코드 영향 없음
2. Service 메서드 변경 부분만 롤백
3. 기존 for-loop 방식으로 복원

---

## 완료 기준

- [ ] 모든 기존 테스트 통과
- [ ] 댓글 조회 시 쿼리 수 3개 이하
- [ ] 응답 데이터 정확성 검증
