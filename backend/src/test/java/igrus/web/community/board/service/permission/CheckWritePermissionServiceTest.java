package igrus.web.community.board.service.permission;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.exception.BoardWriteDeniedException;
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
@DisplayName("CheckWritePermissionService 단위 테스트")
class CheckWritePermissionServiceTest {

    @Mock
    private CanWriteBoardService canWriteBoardService;

    @InjectMocks
    private CheckWritePermissionService checkWritePermissionService;

    private Board noticeBoard;

    @BeforeEach
    void setUp() {
        noticeBoard = Board.create(BoardCode.NOTICES, "공지사항", "공지사항 게시판", false, false, 1);
    }

    @DisplayName("쓰기 권한이 있는 경우 예외가 발생하지 않음")
    @Test
    void checkWritePermission_WithValidPermission_NoException() {
        // given
        given(canWriteBoardService.canWrite(noticeBoard, UserRole.OPERATOR)).willReturn(true);

        // when & then
        checkWritePermissionService.checkWritePermission(noticeBoard, UserRole.OPERATOR);
    }

    @DisplayName("쓰기 권한 없는 접근 시 BoardWriteDeniedException 발생")
    @Test
    void checkWritePermission_WithoutPermission_ThrowsBoardWriteDeniedException() {
        // given
        given(canWriteBoardService.canWrite(noticeBoard, UserRole.MEMBER)).willReturn(false);

        // getRequiredRoleForWrite 내부에서 호출되는 canWrite를 위한 추가 stubbing
        for (UserRole role : UserRole.values()) {
            if (role != UserRole.MEMBER) {
                given(canWriteBoardService.canWrite(noticeBoard, role)).willReturn(false);
            }
        }

        // when & then
        assertThatThrownBy(() -> checkWritePermissionService.checkWritePermission(noticeBoard, UserRole.MEMBER))
                .isInstanceOf(BoardWriteDeniedException.class);
    }

    @DisplayName("권한 정보가 없는 경우 BoardWriteDeniedException 발생")
    @Test
    void checkWritePermission_WithNoPermissionRecord_ThrowsBoardWriteDeniedException() {
        // given
        given(canWriteBoardService.canWrite(noticeBoard, UserRole.MEMBER)).willReturn(false);

        // getRequiredRoleForWrite 내부에서 호출되는 canWrite를 위한 추가 stubbing
        for (UserRole role : UserRole.values()) {
            if (role != UserRole.MEMBER) {
                given(canWriteBoardService.canWrite(noticeBoard, role)).willReturn(false);
            }
        }

        // when & then
        assertThatThrownBy(() -> checkWritePermissionService.checkWritePermission(noticeBoard, UserRole.MEMBER))
                .isInstanceOf(BoardWriteDeniedException.class);
    }
}
