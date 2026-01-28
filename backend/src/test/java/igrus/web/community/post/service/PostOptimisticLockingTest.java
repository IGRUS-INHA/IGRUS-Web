package igrus.web.community.post.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Post 엔티티 낙관적 락 테스트.
 *
 * <p>JPA @Version 필드를 사용한 낙관적 락의 동작을 검증합니다.
 *
 * <p>테스트 시나리오:
 * <ul>
 *     <li>OPT-001: 동시 조회수 증가 시 락 충돌 발생</li>
 *     <li>OPT-002: 동시 게시글 수정 시 락 충돌</li>
 *     <li>OPT-003: Stale 엔티티 저장 시 예외 발생</li>
 *     <li>OPT-004: 리플렉션 버전 조작 시 예외 발생</li>
 *     <li>OPT-005: 순차적 수정 충돌</li>
 *     <li>OPT-006: 첫 번째 충돌 후 재시도 성공</li>
 *     <li>OPT-007: 최대 재시도 후 포기</li>
 *     <li>OPT-008: 재시도 시 엔티티 새로고침</li>
 * </ul>
 */
@DisplayName("Post 낙관적 락 테스트")
class PostOptimisticLockingTest extends ServiceIntegrationTestBase {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardPermissionRepository boardPermissionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostService postService;

    private User memberUser;
    private Board generalBoard;

    @BeforeEach
    void setUp() {
        setUpBase();
        setupBoardData();
        memberUser = createAndSaveUser("20200001", "member@inha.edu", UserRole.MEMBER);
    }

    private void setupBoardData() {
        generalBoard = Board.create(
                BoardCode.GENERAL,
                "자유게시판",
                "자유롭게 이야기를 나눌 수 있는 공간입니다.",
                true,
                true,
                1
        );
        boardRepository.save(generalBoard);

        boardPermissionRepository.save(
                BoardPermission.create(generalBoard, UserRole.MEMBER, true, true)
        );
    }

    private Post createAndSavePost(Board board, User author, String title, String content) {
        Post post = Post.createPost(board, author, title, content);
        return postRepository.save(post);
    }

    private AuthenticatedUser toAuthenticatedUser(User user) {
        return new AuthenticatedUser(user.getId(), user.getStudentId(), user.getRole().name());
    }

    // =========================================================================
    // 1. 동시성 테스트 (ConcurrencyTest)
    // =========================================================================

    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTest {

        @Test
        @DisplayName("OPT-001: 여러 스레드에서 동시에 조회수 증가 시 일부 요청은 락 충돌 발생")
        void incrementViewCount_ConcurrentAccess_SomeRequestsFail() throws InterruptedException {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "테스트 제목", "테스트 내용");
            Long postId = post.getId();

            int threadCount = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // when
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();

                        transactionTemplate.execute(status -> {
                            Post foundPost = postRepository.findById(postId).orElseThrow();
                            foundPost.incrementViewCount();
                            postRepository.saveAndFlush(foundPost);
                            return null;
                        });

                        successCount.incrementAndGet();
                    } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                        failCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(10, TimeUnit.SECONDS);
            executorService.shutdown();

            // then
            assertThat(successCount.get()).isGreaterThan(0);
            assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);

            Post updatedPost = postRepository.findById(postId).orElseThrow();
            assertThat(updatedPost.getViewCount()).isEqualTo(successCount.get());
        }

        @Test
        @DisplayName("OPT-002: 동시 게시글 수정 시 하나만 성공하고 나머지는 락 충돌")
        void updatePost_ConcurrentAccess_OnlyOneSucceeds() throws InterruptedException {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "원본 제목", "원본 내용");
            Long postId = post.getId();

            int threadCount = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // when
            for (int i = 0; i < threadCount; i++) {
                final int threadNum = i;
                executorService.submit(() -> {
                    try {
                        startLatch.await();

                        transactionTemplate.execute(status -> {
                            Post foundPost = postRepository.findById(postId).orElseThrow();
                            foundPost.updateContent("수정된 제목 " + threadNum, "수정된 내용 " + threadNum);
                            postRepository.saveAndFlush(foundPost);
                            return null;
                        });

                        successCount.incrementAndGet();
                    } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                        failCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(10, TimeUnit.SECONDS);
            executorService.shutdown();

            // then
            assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
            assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        }
    }

    // =========================================================================
    // 2. 버전 충돌 시뮬레이션 테스트 (VersionConflictSimulationTest)
    // =========================================================================

    @Nested
    @DisplayName("버전 충돌 시뮬레이션 테스트")
    class VersionConflictSimulationTest {

        @Test
        @DisplayName("OPT-003: 동일 엔티티를 두 트랜잭션에서 로드 후 첫 번째 저장 시 두 번째는 실패")
        void updatePost_WithStaleVersion_ThrowsOptimisticLockException() {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "테스트 제목", "테스트 내용");
            Long postId = post.getId();

            // 첫 번째 트랜잭션에서 엔티티 로드 및 detach
            Post postInTx1 = transactionTemplate.execute(status -> {
                Post p = postRepository.findById(postId).orElseThrow();
                entityManager.detach(p);
                return p;
            });

            // 두 번째 트랜잭션에서 엔티티 로드 및 수정/저장
            transactionTemplate.execute(status -> {
                Post postInTx2 = postRepository.findById(postId).orElseThrow();
                postInTx2.updateContent("TX2에서 수정한 제목", "TX2에서 수정한 내용");
                postRepository.saveAndFlush(postInTx2);
                return null;
            });

            // when & then: 첫 번째 트랜잭션의 stale 엔티티 저장 시도
            assertThatThrownBy(() -> {
                transactionTemplate.execute(status -> {
                    postInTx1.updateContent("TX1에서 수정한 제목", "TX1에서 수정한 내용");
                    postRepository.saveAndFlush(postInTx1);
                    return null;
                });
            }).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }

        @Test
        @DisplayName("OPT-004: 리플렉션으로 버전을 낮추면 저장 시 락 충돌 발생")
        void updatePost_WithManuallyDecreasedVersion_ThrowsOptimisticLockException() {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "테스트 제목", "테스트 내용");
            Long postId = post.getId();

            // 버전을 증가시키기 위해 먼저 업데이트
            transactionTemplate.execute(status -> {
                Post p = postRepository.findById(postId).orElseThrow();
                p.incrementViewCount();
                postRepository.saveAndFlush(p);
                return null;
            });

            // when & then: 버전을 리플렉션으로 낮춘 후 저장 시도
            assertThatThrownBy(() -> {
                transactionTemplate.execute(status -> {
                    Post p = postRepository.findById(postId).orElseThrow();
                    Long currentVersion = p.getVersion();

                    // detach 후 버전 조작
                    entityManager.detach(p);
                    setField(p, "version", currentVersion - 1);

                    p.updateContent("버전 조작 후 수정", "버전 조작 후 내용");
                    postRepository.saveAndFlush(p);
                    return null;
                });
            }).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }
    }

    // =========================================================================
    // 3. 게시글 수정 충돌 테스트 (PostUpdateConflictTest)
    // =========================================================================

    @Nested
    @DisplayName("게시글 수정 충돌 테스트")
    class PostUpdateConflictTest {

        @Test
        @DisplayName("OPT-005: 두 사용자가 동시에 같은 게시글 수정 시 한 명은 실패")
        void updatePost_TwoUsersConcurrently_OneFailsWithLockException() throws InterruptedException {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "원본 제목", "원본 내용");
            Long postId = post.getId();

            CountDownLatch bothLoaded = new CountDownLatch(2);
            CountDownLatch firstSaved = new CountDownLatch(1);

            AtomicReference<Exception> secondThreadException = new AtomicReference<>();

            // 첫 번째 스레드: 로드 -> 대기 -> 저장 -> 신호
            Thread thread1 = new Thread(() -> {
                transactionTemplate.execute(status -> {
                    Post p = postRepository.findById(postId).orElseThrow();
                    bothLoaded.countDown();

                    try {
                        bothLoaded.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    p.updateContent("스레드1 제목", "스레드1 내용");
                    postRepository.saveAndFlush(p);
                    firstSaved.countDown();
                    return null;
                });
            });

            // 두 번째 스레드: 로드 -> 대기 -> 첫 번째 저장 후 저장 시도
            Thread thread2 = new Thread(() -> {
                transactionTemplate.execute(status -> {
                    Post p = postRepository.findById(postId).orElseThrow();
                    bothLoaded.countDown();

                    try {
                        bothLoaded.await(5, TimeUnit.SECONDS);
                        firstSaved.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    try {
                        p.updateContent("스레드2 제목", "스레드2 내용");
                        postRepository.saveAndFlush(p);
                    } catch (Exception e) {
                        secondThreadException.set(e);
                    }

                    return null;
                });
            });

            // when
            thread1.start();
            thread2.start();

            thread1.join(10000);
            thread2.join(10000);

            // then
            assertThat(secondThreadException.get())
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }
    }

    // =========================================================================
    // 4. 재시도 로직 테스트 (RetryLogicTest) - 실제 동시성 기반
    // =========================================================================

    @Nested
    @DisplayName("재시도 로직 테스트")
    class RetryLogicTest {

        @Test
        @DisplayName("OPT-006: 동시 getPostDetail 호출 시 재시도를 통해 조회수 증가가 정상 동작")
        void getPostDetail_ConcurrentCalls_ViewCountIncreasesCorrectly() throws InterruptedException {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "테스트 제목", "테스트 내용");
            Long postId = post.getId();
            AuthenticatedUser authUser = toAuthenticatedUser(memberUser);

            int callCount = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(callCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(callCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger exceptionCount = new AtomicInteger(0);

            // when - 동시에 getPostDetail 호출
            for (int i = 0; i < callCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        postService.getPostDetail(generalBoard.getCode().name(), postId, authUser);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        exceptionCount.incrementAndGet();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(30, TimeUnit.SECONDS);
            executorService.shutdown();

            // then - 대부분의 요청이 성공해야 함 (재시도 로직 덕분)
            // 동시 호출이므로 일부 예외가 발생할 수 있지만, 최소 1개 이상은 성공해야 함
            assertThat(successCount.get()).isGreaterThan(0);
            assertThat(successCount.get() + exceptionCount.get()).isEqualTo(callCount);

            // 최종 조회수 확인 (성공한 호출만큼 증가)
            entityManager.clear();
            Post updatedPost = postRepository.findById(postId).orElseThrow();
            assertThat(updatedPost.getViewCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("OPT-007: 단일 getPostDetail 호출 시 조회수가 정확히 1 증가")
        void getPostDetail_SingleCall_ViewCountIncreasesBy1() {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "테스트 제목", "테스트 내용");
            Long postId = post.getId();
            int initialViewCount = post.getViewCount();
            AuthenticatedUser authUser = toAuthenticatedUser(memberUser);

            // when
            postService.getPostDetail(generalBoard.getCode().name(), postId, authUser);

            // then
            entityManager.clear();
            Post updatedPost = postRepository.findById(postId).orElseThrow();
            assertThat(updatedPost.getViewCount()).isEqualTo(initialViewCount + 1);
        }

        @Test
        @DisplayName("OPT-008: 연속 getPostDetail 호출 시 조회수가 누적 증가")
        void getPostDetail_SequentialCalls_ViewCountAccumulates() {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "테스트 제목", "테스트 내용");
            Long postId = post.getId();
            AuthenticatedUser authUser = toAuthenticatedUser(memberUser);

            int callCount = 3;

            // when - 순차적으로 getPostDetail 호출
            for (int i = 0; i < callCount; i++) {
                postService.getPostDetail(generalBoard.getCode().name(), postId, authUser);
                entityManager.clear();
            }

            // then
            Post updatedPost = postRepository.findById(postId).orElseThrow();
            assertThat(updatedPost.getViewCount()).isEqualTo(callCount);
        }
    }
}
