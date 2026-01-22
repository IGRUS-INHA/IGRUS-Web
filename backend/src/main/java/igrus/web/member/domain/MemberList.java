package igrus.web.member.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// IGRUS 신청 회원 리스트
@Entity
@Table(name = "member_list")
@Getter
@Setter
@NoArgsConstructor
public class MemberList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, length = 8)
    private String studentId; //학번

    @Column(nullable = false, length = 50)
    private String name; //이름

    @Column(nullable = false, length = 10)
    private String semester; //학년 -> 이거 근데 필요 없을지도

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 가입일 -> IGRUS 에 들어온 가입일?

    @PrePersist // 시간 자동 설정
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
