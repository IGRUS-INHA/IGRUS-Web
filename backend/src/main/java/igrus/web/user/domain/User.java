package igrus.web.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 사용자 기본정보
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 기본키

    @Column(unique = true, nullable = false, length = 8)
    private String studentId;
    // 학번 (로그인아이디)

    @Column(nullable = false, length = 50)
    private String name;
    // 본명

    @Column(unique = true, nullable = false)
    private String email;
    // 이메일

    @Column(unique = true, nullable = false, length = 20)
    private String phone;
    // 전화번호

    @Column(nullable = false, length = 50)
    private String department;
    // 학과

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivation;
    // 가입 동기

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.ASSOCIATE;
    // 역할

    @Column
    private String title;
    // 칭호 -> 관리자 설정 요소

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // 가입일 (자동 설정, 수정 불가)

    @Column(nullable = false)
    private LocalDateTime updatedAt;
    // 수정일 (자동 갱신)

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserAuth userAuth;
    // 인증 정보

}
