package igrus.web.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 사용자 인증 정보 (비밀번호, 계정 상태, 승인 정보)
@Entity
@Table(name = "user_auth")
@Getter
@Setter
@NoArgsConstructor
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 기본키

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;
    // 사용자 외래키

    @Column(nullable = false)
    private String passwordHash;
    // 비밀번호 해시 (BCrypt 암호화)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
    // 계정 상태

    @Column
    private LocalDateTime approvedAt;
    // 정회원 승인일 : 준회원 -> 정회원 전환 시 기록

    @Column
    private Long approvedBy;
    // 승인 처리자의 ID

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // 생성일 (자동 설정, 수정 불가)

    @Column(nullable = false)
    private LocalDateTime updatedAt;
    // 수정일

}
