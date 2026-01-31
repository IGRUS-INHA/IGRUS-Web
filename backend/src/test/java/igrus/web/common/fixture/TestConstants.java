package igrus.web.common.fixture;

import igrus.web.user.domain.Gender;

/**
 * 테스트에서 사용되는 공통 상수를 정의하는 클래스.
 *
 * <p>하드코딩된 테스트 데이터를 중앙화하여 변경에 강건한 테스트를 작성할 수 있도록 지원합니다.
 */
public final class TestConstants {

    private TestConstants() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ==================== User 관련 상수 ====================

    public static final String MEMBER_STUDENT_ID = "20200001";
    public static final String OPERATOR_STUDENT_ID = "20200002";
    public static final String ADMIN_STUDENT_ID = "20200003";
    public static final String ANOTHER_MEMBER_STUDENT_ID = "20200004";

    public static final String DEFAULT_NAME = "테스트유저";
    public static final String OPERATOR_NAME = "운영진유저";
    public static final String ADMIN_NAME = "관리자유저";
    public static final String ANOTHER_MEMBER_NAME = "다른멤버";

    public static final String DEFAULT_EMAIL_DOMAIN = "@inha.edu";
    public static final String DEFAULT_PHONE = "010-1234-5678";
    public static final String DEFAULT_DEPARTMENT = "컴퓨터공학과";
    public static final String DEFAULT_MOTIVATION = "테스트 동기";
    public static final Gender DEFAULT_GENDER = Gender.MALE;
    public static final int DEFAULT_GRADE = 1;

    // ==================== Board 관련 상수 ====================

    public static final String GENERAL_BOARD_NAME = "자유게시판";
    public static final String GENERAL_BOARD_DESC = "자유롭게 이야기를 나눌 수 있는 공간입니다.";

    public static final String NOTICES_BOARD_NAME = "공지사항";
    public static final String NOTICES_BOARD_DESC = "동아리 공지사항을 확인하세요.";

    public static final String INSIGHT_BOARD_NAME = "정보공유";
    public static final String INSIGHT_BOARD_DESC = "유용한 정보를 공유하세요.";

    // ==================== Post 관련 상수 ====================

    public static final String DEFAULT_POST_TITLE = "테스트 제목";
    public static final String DEFAULT_POST_CONTENT = "테스트 내용입니다.";

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_IMAGES = 5;

    // ==================== ID 관련 상수 ====================

    public static final Long DEFAULT_MEMBER_ID = 1L;
    public static final Long DEFAULT_OPERATOR_ID = 2L;
    public static final Long DEFAULT_ADMIN_ID = 3L;
    public static final Long ANOTHER_MEMBER_ID = 4L;

    public static final Long DEFAULT_BOARD_ID = 1L;
    public static final Long NOTICES_BOARD_ID = 2L;
    public static final Long INSIGHT_BOARD_ID = 3L;

    public static final Long DEFAULT_POST_ID = 1L;

    // ==================== 이미지 URL 상수 ====================

    public static final String TEST_IMAGE_URL_PREFIX = "https://example.com/image";
    public static final String TEST_IMAGE_URL_SUFFIX = ".jpg";

    /**
     * 테스트용 이미지 URL을 생성합니다.
     *
     * @param index 이미지 인덱스
     * @return 생성된 이미지 URL
     */
    public static String testImageUrl(int index) {
        return TEST_IMAGE_URL_PREFIX + index + TEST_IMAGE_URL_SUFFIX;
    }
}
