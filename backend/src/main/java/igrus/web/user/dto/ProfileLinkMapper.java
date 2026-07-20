package igrus.web.user.dto;

import igrus.web.generated.model.ApiProfileLink;
import igrus.web.user.domain.ProfileLink;

import java.util.List;

/**
 * 도메인 {@link ProfileLink} ↔ 생성 모델 {@link ApiProfileLink} 변환.
 * 회원가입/마이페이지/공개 프로필 컨트롤러에서 공용으로 사용한다.
 */
public final class ProfileLinkMapper {

    private ProfileLinkMapper() {}

    public static List<ApiProfileLink> toApi(List<ProfileLink> links) {
        if (links == null) {
            return List.of();
        }
        return links.stream()
                .map(link -> new ApiProfileLink(link.label(), link.url()))
                .toList();
    }

    public static List<ProfileLink> fromApi(List<ApiProfileLink> links) {
        if (links == null) {
            return List.of();
        }
        return links.stream()
                .map(link -> new ProfileLink(link.getLabel(), link.getUrl()))
                .toList();
    }
}
