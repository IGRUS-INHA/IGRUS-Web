package igrus.web.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public final class PageableUtils {

    private PageableUtils() {
    }

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
            Sort.Direction direction = (parts.length > 1)
                    ? Sort.Direction.fromString(parts[1].trim())
                    : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, property));
        }

        return PageRequest.of(pageNum, pageSize, Sort.by(orders));
    }
}
