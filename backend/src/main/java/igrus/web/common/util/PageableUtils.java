package igrus.web.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public final class PageableUtils {

    private PageableUtils() {
    }

    /**
     * 개별 page, size, sort 파라미터로부터 Pageable을 생성한다.
     *
     * <p>sort 리스트의 각 항목은 {@code "property,direction"} 형태(예: {@code "createdAt,DESC"})이다.
     * 방향이 생략되면 ASC가 기본값이다.</p>
     *
     * <p>Spring의 {@code @RequestParam(defaultValue = "property,DIR")}이 {@code List<String>}에 적용될 때,
     * 쉼표가 리스트 구분자로 해석되어 {@code ["property", "DIR"]}로 분리될 수 있다.
     * 이 경우 단독 방향 문자열(ASC/DESC)을 직전 속성의 방향으로 병합한다.</p>
     */
    public static Pageable of(Integer page, Integer size, List<String> sort) {
        int pageNum = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 20;

        if (sort == null || sort.isEmpty()) {
            return PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String s : sort) {
            String[] parts = s.split(",");
            String property = parts[0].trim();

            // Spring @RequestParam 기본값에서 쉼표 분리로 인한 단독 방향 문자열 처리
            // 예: defaultValue = "attemptedAt,DESC" -> List = ["attemptedAt", "DESC"]
            if (parts.length == 1 && isDirection(property) && !orders.isEmpty()) {
                Sort.Direction direction = Sort.Direction.fromString(property);
                Sort.Order previous = orders.remove(orders.size() - 1);
                orders.add(new Sort.Order(direction, previous.getProperty()));
                continue;
            }

            Sort.Direction direction = (parts.length > 1)
                    ? Sort.Direction.fromString(parts[1].trim())
                    : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, property));
        }

        return PageRequest.of(pageNum, pageSize, Sort.by(orders));
    }

    private static boolean isDirection(String value) {
        return "ASC".equalsIgnoreCase(value) || "DESC".equalsIgnoreCase(value);
    }
}
