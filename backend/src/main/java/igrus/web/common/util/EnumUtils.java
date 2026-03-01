package igrus.web.common.util;

import igrus.web.common.exception.InvalidEnumValueException;

/**
 * String을 enum으로 안전하게 변환하는 유틸리티.
 *
 * <p>contract-first 방식에서 OpenAPI 생성 코드가 enum 쿼리 파라미터를 String으로 받는 경우,
 * 잘못된 값이 전달되면 {@link IllegalArgumentException} 대신 400 Bad Request를 반환하도록
 * 안전한 변환을 제공합니다.</p>
 */
public final class EnumUtils {

    private EnumUtils() {
    }

    /**
     * nullable String을 enum으로 안전하게 변환한다.
     *
     * <p>값이 null이면 null을 반환한다. 유효하지 않은 값이면 400 Bad Request에 해당하는
     * {@link InvalidEnumValueException}을 던진다.</p>
     *
     * @param enumType 변환 대상 enum 클래스
     * @param value    변환할 문자열 (nullable)
     * @param <T>      enum 타입
     * @return 변환된 enum 값 또는 null
     * @throws InvalidEnumValueException 유효하지 않은 enum 값인 경우
     */
    public static <T extends Enum<T>> T fromStringOrNull(Class<T> enumType, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            throw new InvalidEnumValueException();
        }
    }
}
