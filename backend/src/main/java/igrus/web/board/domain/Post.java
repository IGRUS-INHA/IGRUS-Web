//package igrus.web.board.domain;
//
//import igrus.web.user.domain.User;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.time.LocalDateTime;
//
//// 게시글
//@Entity
//@Table(name = "post")
//@Getter
//@Setter
//@NoArgsConstructor
//public class Post {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "board_id", nullable = false)
//    private Board board; // 소속 게시판
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "category_id")
//    private Category category; // 카테고리 (nullable = 정보 게시판이 아닌 경우, 특정 게시판만 사용)
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User author; // 작성자
//
//    @Column(name = "title", nullable = false, length = 200)
//    private String title; // 제목
//
//    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
//    private String content; // 내용
//
//    @Column(name = "view_count", nullable = false)
//    private Integer viewCount = 0; // 조회수
//
//    @Column(name = "is_anonymous", nullable = false)
//    private Boolean isAnonymous = false; // 익명 여부
//
//    @Column(name = "is_question", nullable = false)
//    private Boolean isQuestion = false; // 질문 태그 (자유게시판)
//
//    @Column(name = "is_visible_to_associate", nullable = false)
//    private Boolean isVisibleToAssociate = false; // 준회원 공개 여부 (공지사항)
//
//    @Column(name = "is_deleted", nullable = false)
//    private Boolean isDeleted = false; // 삭제 여부 (Soft Delete)
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//}
