package igrus.web.security.auth.password.service.support;

import igrus.web.security.auth.password.exception.InvalidPasswordFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 비밀번호 형식 검증 서비스
 */
@Slf4j
@Service
@Transactional
public class ValidatePasswordFormatService {

    /**
     * 비밀번호 복잡도 정규식
     * - 최소 8자 이상
     * - 영문 1개 이상 (대소문자 구분 없음)
     * - 숫자 1개 이상
     * - 특수문자 선택
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d).{8,}$"
    );

    /**
     * 비밀번호 형식을 검증합니다.
     *
     * @param password 검증할 비밀번호
     * @throws InvalidPasswordFormatException 비밀번호 형식이 올바르지 않은 경우
     */
    public void validatePasswordFormat(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            log.warn("비밀번호 형식 검증 실패");
            throw new InvalidPasswordFormatException();
        }
    }
}
