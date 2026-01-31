package igrus.web.common.fixture;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * 테스트에서 엔티티의 ID를 설정하기 위한 유틸리티 클래스.
 *
 * <p>JPA 엔티티의 ID 필드는 일반적으로 setter가 없으므로,
 * 테스트에서 ReflectionTestUtils를 사용하여 ID를 설정해야 합니다.
 * 이 클래스는 해당 로직을 중앙화하여 필드명 변경 시 한 곳만 수정하면 되도록 합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * User user = User.create(...);
 * TestEntityIdAssigner.assignId(user, 1L);
 *
 * // 또는 메서드 체이닝 사용
 * User user = TestEntityIdAssigner.withId(User.create(...), 1L);
 * }</pre>
 */
public final class TestEntityIdAssigner {

    private static final String ID_FIELD_NAME = "id";

    private TestEntityIdAssigner() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 엔티티에 ID를 설정합니다.
     *
     * @param entity ID를 설정할 엔티티
     * @param id     설정할 ID 값
     */
    public static void assignId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, ID_FIELD_NAME, id);
    }

    /**
     * 엔티티에 ID를 설정하고 엔티티를 반환합니다.
     *
     * <p>메서드 체이닝을 지원하여 더 간결한 코드 작성이 가능합니다.
     *
     * @param entity ID를 설정할 엔티티
     * @param id     설정할 ID 값
     * @param <T>    엔티티 타입
     * @return ID가 설정된 엔티티
     */
    public static <T> T withId(T entity, Long id) {
        assignId(entity, id);
        return entity;
    }
}
