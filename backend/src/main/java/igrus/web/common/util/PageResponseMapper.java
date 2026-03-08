package igrus.web.common.util;

import igrus.web.generated.model.ApiPageableObject;
import igrus.web.generated.model.ApiSortObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Spring Data JPA의 {@link Page} 객체를 openapi-generator가 생성한 모델 DTO로 변환하는 유틸리티.
 *
 * <h3>사용 패턴</h3>
 *
 * <p><b>1. Spring Page 전체 래핑 응답 (Page* 형태)</b></p>
 * <pre>{@code
 * // 서비스에서 Page<Entity> 또는 Page<InternalDto> 반환
 * Page<AccountStatusChangeHistory> page = service.getHistories(pageable);
 *
 * // 컨트롤러에서 변환
 * ApiPageAccountStatusChangeHistoryResponse response = PageResponseMapper.toSpringPageResponse(
 *     page,
 *     entity -> new ApiPageAccountStatusChangeHistoryResponseContentInner()
 *         .changeType(entity.getChangeType().name())
 *         .changedAt(entity.getChangedAt()),
 *     ApiPageAccountStatusChangeHistoryResponse::new,
 *     (r, content, meta) -> r
 *         .content(content)
 *         .totalElements(meta.totalElements())
 *         .totalPages(meta.totalPages())
 *         .number(meta.number())
 *         .size(meta.size())
 *         .numberOfElements(meta.numberOfElements())
 *         .first(meta.first())
 *         .last(meta.last())
 *         .empty(meta.empty())
 *         .pageable(meta.pageable())
 *         .sort(meta.sort())
 * );
 * }</pre>
 *
 * <p><b>2. 커스텀 간소화 응답 (인라인 매핑)</b></p>
 * <pre>{@code
 * ApiPostListPageResponse response = new ApiPostListPageResponse()
 *     .posts(page.getContent().stream()
 *         .map(post -> new ApiPostListResponseItem()
 *             .id(post.getId())
 *             .title(post.getTitle()))
 *         .toList())
 *     .totalElements(page.getTotalElements())
 *     .totalPages(page.getTotalPages())
 *     .currentPage(page.getNumber())
 *     .hasNext(page.hasNext());
 * }</pre>
 */
public final class PageResponseMapper {

    private PageResponseMapper() {
    }

    /**
     * Spring Page의 페이지네이션 메타데이터를 담는 레코드.
     *
     * <p>pageable/sort 필드가 {@code ApiPageableObject} /
     * {@code ApiSortObject} 타입을 사용하는 이유:
     * openapi-generator가 {@code _common.yaml}의 {@code PageableObject}/{@code SortObject} 스키마를
     * 처리할 때, 최초 인라인 참조 위치의 이름으로 클래스를 생성하고 이후 모든 {@code Page*Response}에서
     * 동일한 타입을 재사용한다. 별도로 생성된 {@code ApiPageableObject}/{@code ApiSortObject} 클래스는
     * 실제 응답 모델의 setter와 타입이 호환되지 않으므로 여기서는 사용하지 않는다.</p>
     */
    public record PageMeta(
            long totalElements,
            int totalPages,
            int number,
            int size,
            int numberOfElements,
            boolean first,
            boolean last,
            boolean empty,
            ApiPageableObject pageable,
            ApiSortObject sort
    ) {
    }

    /**
     * Spring {@link Page}에서 생성 모델의 pageable/sort 객체를 포함한 {@link PageMeta}를 추출한다.
     */
    public static PageMeta extractMeta(Page<?> page) {
        Pageable pageable = page.getPageable();
        Sort pageSort = page.getSort();

        ApiSortObject sortObj = new ApiSortObject()
                .empty(pageSort.isEmpty())
                .sorted(!pageSort.isEmpty())
                .unsorted(pageSort.isEmpty());

        ApiPageableObject pageableObj = new ApiPageableObject()
                .pageNumber(pageable.isPaged() ? pageable.getPageNumber() : null)
                .pageSize(pageable.isPaged() ? pageable.getPageSize() : null)
                .offset(pageable.isPaged() ? pageable.getOffset() : null)
                .paged(pageable.isPaged())
                .unpaged(pageable.isUnpaged())
                .sort(sortObj);

        return new PageMeta(
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty(),
                pageableObj,
                sortObj
        );
    }

    /**
     * Spring {@link Page}의 content 요소들을 변환한다.
     *
     * @param page   Spring Page 객체
     * @param mapper 각 요소를 생성 모델 DTO로 변환하는 함수
     * @param <S>    원본 타입
     * @param <T>    대상 타입
     * @return 변환된 리스트
     */
    public static <S, T> List<T> mapContent(Page<S> page, Function<S, T> mapper) {
        return page.getContent().stream().map(mapper).toList();
    }

    /**
     * Spring {@link Page}를 생성 모델의 Page* 응답으로 변환한다.
     *
     * @param page           Spring Page 객체
     * @param contentMapper  각 content 요소를 생성 모델 DTO로 변환하는 함수
     * @param responseFactory 빈 응답 객체를 생성하는 팩토리
     * @param assembler      응답 객체에 content와 메타데이터를 설정하는 함수
     * @param <S>            원본 content 타입
     * @param <T>            대상 content 타입
     * @param <R>            응답 타입
     * @return 완성된 응답 객체
     */
    public static <S, T, R> R toSpringPageResponse(
            Page<S> page,
            Function<S, T> contentMapper,
            Supplier<R> responseFactory,
            ResponseAssembler<R, List<T>, PageMeta> assembler) {
        List<T> content = mapContent(page, contentMapper);
        PageMeta meta = extractMeta(page);
        R response = responseFactory.get();
        return assembler.assemble(response, content, meta);
    }

    /**
     * 응답 객체에 content와 페이지네이션 메타데이터를 설정하여 반환하는 함수형 인터페이스.
     *
     * <p>openapi-generator가 생성한 모델은 fluent setter 패턴을 사용하므로,
     * 전달받은 응답 객체에 값을 설정한 뒤 동일한 객체를 반환하는 mutator 형태로 사용한다.</p>
     *
     * @param <R> 응답 타입 (입력과 반환이 동일)
     * @param <C> content 리스트 타입
     * @param <M> 메타데이터 타입 ({@link PageMeta})
     */
    @FunctionalInterface
    public interface ResponseAssembler<R, C, M> {
        R assemble(R response, C content, M meta);
    }
}
