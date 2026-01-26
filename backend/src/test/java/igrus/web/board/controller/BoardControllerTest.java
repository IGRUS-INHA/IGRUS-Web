package igrus.web.board.controller;

import igrus.web.board.domain.Board;
import igrus.web.board.domain.BoardPermission;
import igrus.web.board.repository.BoardPermissionRepository;
import igrus.web.board.repository.BoardRepository;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.common.exception.ErrorCode;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("BoardController 통합 테스트")
class BoardControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardPermissionRepository boardPermissionRepository;

    private static final String BASE_URL = "/api/v1/boards";

    private User memberUser;
    private User associateUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        setupBoardData();
        memberUser = createAndSaveUser("20200001", "member@inha.edu", UserRole.MEMBER);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
    }

    private void setupBoardData() {
        // 게시판 생성
        Board notices = Board.create("notices", "공지사항", "동아리 공지사항을 확인할 수 있습니다.", false, false, 1);
        Board general = Board.create("general", "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2);
        Board insight = Board.create("insight", "정보공유", "유용한 정보를 공유하는 게시판입니다.", false, false, 3);

        boardRepository.save(notices);
        boardRepository.save(general);
        boardRepository.save(insight);

        // 권한 설정 - notices
        boardPermissionRepository.save(BoardPermission.create(notices, UserRole.ASSOCIATE, true, false));
        boardPermissionRepository.save(BoardPermission.create(notices, UserRole.MEMBER, true, false));
        boardPermissionRepository.save(BoardPermission.create(notices, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(notices, UserRole.ADMIN, true, true));

        // 권한 설정 - general
        boardPermissionRepository.save(BoardPermission.create(general, UserRole.ASSOCIATE, false, false));
        boardPermissionRepository.save(BoardPermission.create(general, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(general, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(general, UserRole.ADMIN, true, true));

        // 권한 설정 - insight
        boardPermissionRepository.save(BoardPermission.create(insight, UserRole.ASSOCIATE, false, false));
        boardPermissionRepository.save(BoardPermission.create(insight, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(insight, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(insight, UserRole.ADMIN, true, true));
    }

    private RequestPostProcessor withAuth(User user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        return authentication(auth);
    }

    @Nested
    @DisplayName("GET /api/v1/boards - 게시판 목록 조회")
    class GetBoardListTest {

        @DisplayName("인증된 사용자가 게시판 목록 조회 성공 (200)")
        @Test
        void getBoardList_WithAuthenticatedUser_ReturnsOk() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].code").value("notices"))
                    .andExpect(jsonPath("$[0].name").value("공지사항"))
                    .andExpect(jsonPath("$[1].code").value("general"))
                    .andExpect(jsonPath("$[1].name").value("자유게시판"))
                    .andExpect(jsonPath("$[2].code").value("insight"))
                    .andExpect(jsonPath("$[2].name").value("정보공유"));
        }

        @DisplayName("준회원이 게시판 목록 조회 시 공지사항만 반환")
        @Test
        void getBoardList_WithAssociateUser_ReturnsOnlyNotices() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].code").value("notices"));
        }

        @DisplayName("비인증 사용자가 접근 시 403 Forbidden")
        @Test
        void getBoardList_WithUnauthenticatedUser_ReturnsForbidden() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/boards/{code} - 게시판 상세 조회")
    class GetBoardByCodeTest {

        @DisplayName("유효한 게시판 코드로 상세 조회 성공 (200)")
        @Test
        void getBoardByCode_WithValidCode_ReturnsOk() throws Exception {
            // given
            String boardCode = "general";

            // when & then
            mockMvc.perform(get(BASE_URL + "/" + boardCode)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("general"))
                    .andExpect(jsonPath("$.name").value("자유게시판"))
                    .andExpect(jsonPath("$.allowsAnonymous").value(true))
                    .andExpect(jsonPath("$.allowsQuestionTag").value(true))
                    .andExpect(jsonPath("$.canRead").value(true))
                    .andExpect(jsonPath("$.canWrite").value(true));
        }

        @DisplayName("존재하지 않는 코드로 조회 시 404 Not Found")
        @Test
        void getBoardByCode_WithInvalidCode_ReturnsNotFound() throws Exception {
            // given
            String invalidCode = "invalid-board";

            // when & then
            mockMvc.perform(get(BASE_URL + "/" + invalidCode)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.BOARD_NOT_FOUND.getCode()));
        }

        @DisplayName("비인증 사용자가 상세 조회 시 403 Forbidden")
        @Test
        void getBoardByCode_WithUnauthenticatedUser_ReturnsForbidden() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL + "/general"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }
}
