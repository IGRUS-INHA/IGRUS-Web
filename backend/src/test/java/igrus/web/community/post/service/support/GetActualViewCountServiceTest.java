package igrus.web.community.post.service.support;

import igrus.web.community.post.repository.PostViewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * GetActualViewCountService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetActualViewCountService 단위 테스트")
class GetActualViewCountServiceTest {

    @Mock
    private PostViewRepository postViewRepository;

    @InjectMocks
    private GetActualViewCountService getActualViewCountService;

    @Test
    @DisplayName("게시글의 실제 조회 수 조회 성공")
    void getActualViewCount_ReturnsCorrectCount() {
        // given
        Long postId = 1L;
        long expectedCount = 42L;
        given(postViewRepository.countByPostId(postId)).willReturn(expectedCount);

        // when
        long actualCount = getActualViewCountService.getActualViewCount(postId);

        // then
        assertThat(actualCount).isEqualTo(expectedCount);
    }
}
