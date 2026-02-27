package igrus.web.storage.service;

/**
 * 파일 참조 여부를 확인하는 인터페이스.
 *
 * <p>상위 엔티티(게시글, 프로필, 행사 등)에서 파일을 참조하는지 확인한다.
 * 새로운 참조 대상이 추가될 때 이 인터페이스를 구현하여 Bean으로 등록하면
 * FileDeleteService가 자동으로 참조 무결성을 검사한다.</p>
 */
public interface FileReferenceChecker {

    /**
     * 해당 Object Key의 파일이 참조되고 있는지 확인한다.
     *
     * @param objectKey 확인 대상 S3 Object Key
     * @return 참조 중이면 true, 아니면 false
     */
    boolean isReferenced(String objectKey);
}
