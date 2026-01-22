package igrus.web.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 사이트 가입 유저
@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 8)
    private String studentId; //학번

    @Column(nullable = false, length = 50)
    private String name; //본명

    @Column(nullable = false)
    private String password; //비번

    @Column(nullable = false)
    private String email; //이메일

    @Column(nullable = false)
    private Boolean emailVerified = false; //이메일 인증 여부


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)//계정 상태
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)//권한
    private UserRole role = UserRole.MEMBER;

    @Column
    private String title; //칭호 -> ENUM ?안만들어도 될까

    @Column
    private LocalDateTime suspendedUntil; //정지 해제 일시 - 관리자가 관리

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;//가입일

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
