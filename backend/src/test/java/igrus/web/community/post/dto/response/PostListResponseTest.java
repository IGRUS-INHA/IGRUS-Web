package igrus.web.community.post.dto.response;

import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static igrus.web.community.fixture.BoardTestFixture.generalBoard;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostListResponse DTO 단위 테스트")
class PostListResponseTest {

    @DisplayName("탈퇴한 사용자(author=null)의 일반 게시글 - authorName='탈퇴한 사용자'")
    @Test
    void from_NullAuthor_NormalPost_ReturnsWithdrawnDisplayName() {
        // given
        Post post = normalPostWithNullAuthor(generalBoard());

        // when
        PostListResponse response = PostListResponse.from(post);

        // then
        assertThat(response.authorName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        assertThat(response.isAnonymous()).isFalse();
    }

    @DisplayName("탈퇴한 사용자(author=null)의 익명 게시글 - authorName='익명'")
    @Test
    void from_NullAuthor_AnonymousPost_ReturnsAnonymousName() {
        // given
        Post post = anonymousPostWithNullAuthor(generalBoard());

        // when
        PostListResponse response = PostListResponse.from(post);

        // then
        assertThat(response.authorName()).isEqualTo("익명");
        assertThat(response.isAnonymous()).isTrue();
    }
}
