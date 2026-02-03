package igrus.web.community.board.service.permission;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.exception.BoardReadDeniedException;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckReadPermissionService 단위 테스트")
class CheckReadPermissionServiceTest {

    @Mock
    private CanReadBoardService canReadBoardService;

    @InjectMocks
    private CheckReadPermissionService checkReadPermissionService;

    private Board noticeBoard;
    private Board freeBoard;

    @BeforeEach
    void setUp() {
        noticeBoard = Board.create(BoardCode.NOTICES, "공지사항", "공지사항 게시판", false, false, 1);
        freeBoard = Board.create(BoardCode.GENERAL, "자유게시판", "자유게시판", false, false, 2);
    }

    @DisplayName("읽기 권한이 있는 경우 예외가 발생하지 않음")
    @Test
    void checkReadPermission_WithValidPermission_NoException() {
        // given
        given(canReadBoardService.canRead(noticeBoard, UserRole.ASSOCIATE)).willReturn(true);

        // when & then
        checkReadPermissionService.checkReadPermission(noticeBoard, UserRole.ASSOCIATE);
    }

    @DisplayName("읽기 권한 없는 접근 시 BoardReadDeniedException 발생")
    @Test
    void checkReadPermission_WithoutPermission_ThrowsBoardReadDeniedException() {
        // given
        given(canReadBoardService.canRead(freeBoard, UserRole.ASSOCIATE)).willReturn(false);

        // getRequiredRoleForRead 내부에서 호출되는 canRead를 위한 추가 stubbing
        for (UserRole role : UserRole.values()) {
            if (role != UserRole.ASSOCIATE) {
                given(canReadBoardService.canRead(freeBoard, role)).willReturn(false);
            }
        }

        // when & then
        assertThatThrownBy(() -> checkReadPermissionService.checkReadPermission(freeBoard, UserRole.ASSOCIATE))
                .isInstanceOf(BoardReadDeniedException.class);
    }

    @DisplayName("권한 정보가 없는 경우 BoardReadDeniedException 발생")
    @Test
    void checkReadPermission_WithNoPermissionRecord_ThrowsBoardReadDeniedException() {
        // given
        given(canReadBoardService.canRead(noticeBoard, UserRole.ASSOCIATE)).willReturn(false);

        // getRequiredRoleForRead 내부에서 호출되는 canRead를 위한 추가 stubbing
        for (UserRole role : UserRole.values()) {
            if (role != UserRole.ASSOCIATE) {
                given(canReadBoardService.canRead(noticeBoard, role)).willReturn(false);
            }
        }

        // when & then
        assertThatThrownBy(() -> checkReadPermissionService.checkReadPermission(noticeBoard, UserRole.ASSOCIATE))
                .isInstanceOf(BoardReadDeniedException.class);
    }
}
