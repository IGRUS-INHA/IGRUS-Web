package igrus.web.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 임시 학번 시퀀스 엔티티.
 * 연도별로 순번을 관리하여 임시 학번을 생성합니다.
 */
@Entity
@Table(name = "temp_student_id_sequences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TempStudentIdSequence {

    private static final int MAX_SEQUENCE_VALUE = 9999;

    @Id
    @Column(name = "temp_student_id_sequences_year")
    private Integer year;

    @Column(name = "temp_student_id_sequences_next_value", nullable = false)
    private Integer nextValue = 1;

    public TempStudentIdSequence(int year) {
        this.year = year;
        this.nextValue = 1;
    }

    /**
     * 현재 순번을 반환하고 다음 순번으로 증가시킵니다.
     *
     * @return 현재 순번 (1~9999)
     * @throws IllegalStateException 순번이 9999를 초과한 경우
     */
    public int getAndIncrement() {
        if (nextValue > MAX_SEQUENCE_VALUE) {
            throw new IllegalStateException("임시 학번 시퀀스가 소진되었습니다: year=" + year);
        }
        int current = nextValue;
        nextValue++;
        return current;
    }
}
