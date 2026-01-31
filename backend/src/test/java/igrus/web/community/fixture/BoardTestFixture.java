package igrus.web.community.fixture;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;

import static igrus.web.common.fixture.TestConstants.*;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;

/**
 * Board 도메인 관련 테스트 픽스처 클래스.
 *
 * <p>테스트에서 사용되는 Board 엔티티를 생성하는 팩토리 메서드를 제공합니다.
 */
public final class BoardTestFixture {

    private BoardTestFixture() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ==================== Board 생성 (ID 없음) ====================

    /**
     * 자유게시판(GENERAL) Board를 생성합니다.
     *
     * <p>익명 옵션과 질문 태그 옵션이 활성화되어 있습니다.
     *
     * @return 자유게시판 Board
     */
    public static Board createGeneralBoard() {
        return Board.create(
                BoardCode.GENERAL,
                GENERAL_BOARD_NAME,
                GENERAL_BOARD_DESC,
                true,  // allowsAnonymous
                true,  // allowsQuestionTag
                2      // displayOrder
        );
    }

    /**
     * 공지사항(NOTICES) Board를 생성합니다.
     *
     * <p>익명 옵션과 질문 태그 옵션이 비활성화되어 있습니다.
     *
     * @return 공지사항 Board
     */
    public static Board createNoticesBoard() {
        return Board.create(
                BoardCode.NOTICES,
                NOTICES_BOARD_NAME,
                NOTICES_BOARD_DESC,
                false, // allowsAnonymous
                false, // allowsQuestionTag
                1      // displayOrder
        );
    }

    /**
     * 정보공유(INSIGHT) Board를 생성합니다.
     *
     * <p>익명 옵션과 질문 태그 옵션이 비활성화되어 있습니다.
     *
     * @return 정보공유 Board
     */
    public static Board createInsightBoard() {
        return Board.create(
                BoardCode.INSIGHT,
                INSIGHT_BOARD_NAME,
                INSIGHT_BOARD_DESC,
                false, // allowsAnonymous
                false, // allowsQuestionTag
                3      // displayOrder
        );
    }

    // ==================== Board 생성 (ID 포함) ====================

    /**
     * ID가 설정된 자유게시판(GENERAL) Board를 생성합니다.
     *
     * @return ID가 설정된 자유게시판 Board
     */
    public static Board generalBoard() {
        return withId(createGeneralBoard(), DEFAULT_BOARD_ID);
    }

    /**
     * 지정된 ID가 설정된 자유게시판(GENERAL) Board를 생성합니다.
     *
     * @param id 설정할 ID
     * @return ID가 설정된 자유게시판 Board
     */
    public static Board generalBoard(Long id) {
        return withId(createGeneralBoard(), id);
    }

    /**
     * ID가 설정된 공지사항(NOTICES) Board를 생성합니다.
     *
     * @return ID가 설정된 공지사항 Board
     */
    public static Board noticesBoard() {
        return withId(createNoticesBoard(), NOTICES_BOARD_ID);
    }

    /**
     * 지정된 ID가 설정된 공지사항(NOTICES) Board를 생성합니다.
     *
     * @param id 설정할 ID
     * @return ID가 설정된 공지사항 Board
     */
    public static Board noticesBoard(Long id) {
        return withId(createNoticesBoard(), id);
    }

    /**
     * ID가 설정된 정보공유(INSIGHT) Board를 생성합니다.
     *
     * @return ID가 설정된 정보공유 Board
     */
    public static Board insightBoard() {
        return withId(createInsightBoard(), INSIGHT_BOARD_ID);
    }

    /**
     * 지정된 ID가 설정된 정보공유(INSIGHT) Board를 생성합니다.
     *
     * @param id 설정할 ID
     * @return ID가 설정된 정보공유 Board
     */
    public static Board insightBoard(Long id) {
        return withId(createInsightBoard(), id);
    }

    // ==================== 특수 목적 Board 생성 ====================

    /**
     * 익명 게시가 가능한 Board를 생성합니다.
     *
     * <p>익명 옵션 테스트 시 사용합니다.
     *
     * @return 익명 게시 가능 Board
     */
    public static Board boardAllowingAnonymous() {
        return generalBoard();
    }

    /**
     * 익명 게시가 불가능한 Board를 생성합니다.
     *
     * <p>익명 옵션 테스트 시 사용합니다.
     *
     * @return 익명 게시 불가 Board
     */
    public static Board boardDisallowingAnonymous() {
        return noticesBoard();
    }

    /**
     * 질문 태그가 가능한 Board를 생성합니다.
     *
     * <p>질문 태그 테스트 시 사용합니다.
     *
     * @return 질문 태그 가능 Board
     */
    public static Board boardAllowingQuestion() {
        return generalBoard();
    }

    /**
     * 질문 태그가 불가능한 Board를 생성합니다.
     *
     * <p>질문 태그 테스트 시 사용합니다.
     *
     * @return 질문 태그 불가 Board
     */
    public static Board boardDisallowingQuestion() {
        return insightBoard();
    }
}
