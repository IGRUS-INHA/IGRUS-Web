# 댓글 좋아요 수 조회 - 배치 쿼리 방식 선택

## 배경

댓글 목록 조회 시 각 댓글의 좋아요 수와 현재 사용자의 좋아요 여부를 조회해야 합니다.
현재 구현은 루프 내에서 댓글마다 개별 쿼리를 실행하여 비효율적입니다.

```java
// 현재 구현 - CommentService.getCommentsByPostId()
for (Comment comment : allComments) {
    long likeCount = commentLikeRepository.countByCommentId(comment.getId());  // N번 쿼리
    boolean isLikedByMe = commentLikeRepository.existsByCommentIdAndUserId(...);  // N번 쿼리
}
```

댓글 N개 조회 시 2N개의 추가 쿼리가 발생하여 성능 저하가 우려됩니다.

> **참고**: 이 문제는 JPA Lazy Loading으로 인한 N+1 문제가 아닙니다.
> 루프 내에서 명시적으로 집계/조회 쿼리를 호출하는 "루프 내 다중 쿼리" 문제입니다.

## 선택지

1. **비정규화**: Comment 엔티티에 `likeCount` 필드를 추가하고 좋아요 추가/삭제 시 동기화
2. **배치 쿼리**: IN 절을 사용하여 한 번에 여러 댓글의 좋아요 정보를 조회
3. **캐시**: Redis 등에 좋아요 수를 캐싱

## 결정

- **배치 쿼리 방식** 선택

## 결정 이유

### 1. 구현 복잡성 대비 효과

| 방식 | 쿼리 수 | 구현 복잡성 | 동시성 이슈 |
|------|---------|-------------|-------------|
| 현재 (개별 쿼리) | 2N | 낮음 | 없음 |
| 비정규화 | 0 | 높음 | 있음 |
| 배치 쿼리 | 2 | 낮음 | 없음 |
| 캐시 | 0~2 | 높음 | 있음 |

배치 쿼리는 2N개 쿼리를 2개로 줄이면서 구현 복잡성이 낮고 동시성 이슈가 없습니다.

### 2. 비정규화의 부담

비정규화 시 발생하는 문제:

- **동시성 처리 필요**: 여러 사용자가 동시에 좋아요/취소 시 lost update 가능
  - `@Version` 낙관적 락 또는 비관적 락 필요
  - 락 충돌 시 재시도 로직 필요
- **데이터 일관성**: `comment_likes` 테이블과 `likeCount` 필드 간 불일치 가능성
  - 트랜잭션 실패 시 복구 로직 필요
  - 정기적인 동기화 배치 작업 고려 필요
- **코드 복잡성**: 좋아요 추가/삭제 시 Comment 엔티티 수정 로직 추가 필요

### 3. 프로젝트 규모에 적합

- 동아리 웹사이트 수준의 트래픽에서 배치 쿼리로 충분한 성능 확보 가능
- 과도한 최적화(비정규화, 캐시)는 복잡성만 증가시킴
- 추후 트래픽 증가 시 비정규화나 캐시로 전환 가능

### 4. 배치 쿼리 구현 예시

```java
// Repository
@Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl " +
       "WHERE cl.comment.id IN :commentIds GROUP BY cl.comment.id")
List<Object[]> countLikesByCommentIds(@Param("commentIds") List<Long> commentIds);

@Query("SELECT cl.comment.id FROM CommentLike cl " +
       "WHERE cl.comment.id IN :commentIds AND cl.user.id = :userId")
List<Long> findLikedCommentIdsByUser(@Param("commentIds") List<Long> commentIds,
                                      @Param("userId") Long userId);

// Service
Map<Long, Long> likeCounts = getLikeCountsMap(commentIds);
Set<Long> likedCommentIds = getLikedCommentIds(commentIds, currentUserId);
```

## 고려한 대안

### 비정규화 방식

**장점:**
- 조회 시 추가 쿼리 불필요 (최고 성능)
- 좋아요 수 기준 정렬/필터링에 인덱스 활용 가능

**채택하지 않은 이유:**
- 동시성 처리를 위한 락 메커니즘 필요
- 데이터 일관성 유지 로직 복잡
- 현재 트래픽 수준에서 과도한 최적화

### 캐시 방식 (Redis)

**장점:**
- 매우 빠른 조회 성능
- DB 부하 감소

**채택하지 않은 이유:**
- Redis 인프라 추가 필요
- 캐시 무효화 전략 필요
- 프로젝트 규모 대비 과도한 인프라

## 결과

- CommentLikeRepository에 배치 조회 메서드 추가
- CommentService.getCommentsByPostId() 리팩토링
- 2N+1 쿼리 → 3개 쿼리로 감소 (댓글 조회 1 + 좋아요 수 1 + 좋아요 여부 1)

## 후속 조치

- [ ] CommentLikeRepository에 배치 조회 메서드 추가
- [ ] CommentService.getCommentsByPostId() 리팩토링
- [ ] 성능 테스트 (댓글 100개 기준 응답 시간 측정)
- [ ] 추후 트래픽 증가 시 비정규화 재검토

## 비정규화 전환 기준

다음 조건 충족 시 비정규화 검토:

1. 댓글 조회 API 응답 시간이 200ms 이상
2. DB 부하로 인한 성능 저하 발생
3. 좋아요 수 기준 정렬/필터링 요구사항 추가
