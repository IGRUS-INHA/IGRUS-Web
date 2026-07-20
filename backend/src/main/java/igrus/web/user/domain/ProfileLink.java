package igrus.web.user.domain;

/**
 * 공개 프로필 외부 링크 (github, linkedin, velog 등).
 * users_links JSON 배열의 원소로 저장된다.
 */
public record ProfileLink(String label, String url) {
}
