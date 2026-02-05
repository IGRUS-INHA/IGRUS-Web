package igrus.web.community.board.service.permission;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CanWriteBoardService 단위 테스트")
class CanWriteBoardServiceTest {

    @Mock
    private BoardPermissionRepository boardPermissionRepository;

    @InjectMocks
    private CanWriteBoardService canWriteBoardService;

    private Board noticeBoard;

    @BeforeEach
    void setUp() {
        noticeBoard = Board.create(BoardCode.NOTICES, "공지사항", "공지사항 게시판", false, false, 1);
    }

    @DisplayName("정회원이 공지사항 쓰기 권한 확인 - false")
    @Test
    void canWrite_MemberWithNoticeBoard_ReturnsFalse() {
        // given
        BoardPermission permission = BoardPermission.create(noticeBoard, UserRole.MEMBER, true, false);
        given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.MEMBER))
                .willReturn(Optional.of(permission));

        // when
        boolean result = canWriteBoardService.canWrite(noticeBoard, UserRole.MEMBER);

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("OPERATOR가 공지사항 쓰기 권한 확인 - true")
    @Test
    void canWrite_OperatorWithNoticeBoard_ReturnsTrue() {
        // given
        BoardPermission permission = BoardPermission.create(noticeBoard, UserRole.OPERATOR, true, true);
        given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.OPERATOR))
                .willReturn(Optional.of(permission));

        // when
        boolean result = canWriteBoardService.canWrite(noticeBoard, UserRole.OPERATOR);

        // then
        assertThat(result).isTrue();
    }

    @DisplayName("권한 정보가 없는 경우 - false")
    @Test
    void canWrite_WithNoPermission_ReturnsFalse() {
        // given
        given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.MEMBER))
                .willReturn(Optional.empty());

        // when
        boolean result = canWriteBoardService.canWrite(noticeBoard, UserRole.MEMBER);

        // then
        assertThat(result).isFalse();
    }
}
