//package igrus.web.board.domain;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.time.LocalDateTime;
//
//// 게시판
//@Entity
//@Table(name = "board")
//@Getter
//@Setter
//@NoArgsConstructor
//public class Board {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "code", unique = true, nullable = false, length = 20)
//    private String code; // 게시판 코드 (식별자 : notices, general, insight 고정)
//
//    @Column(name = "name", nullable = false, length = 50)
//    private String name; // 게시판 이름
//
//    @Column(name = "description", length = 200)
//    private String description; // 게시판 설명
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//}
