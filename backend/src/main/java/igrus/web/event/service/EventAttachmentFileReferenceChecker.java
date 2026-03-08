package igrus.web.event.service;

import igrus.web.event.repository.EventAttachmentRepository;
import igrus.web.storage.service.FileReferenceChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 행사 첨부파일의 파일 참조 여부를 확인하는 구현체.
 * FileDeleteService가 파일 삭제 전에 참조 무결성을 검사할 때 사용된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventAttachmentFileReferenceChecker implements FileReferenceChecker {

    private final EventAttachmentRepository eventAttachmentRepository;

    @Override
    public boolean isReferenced(String objectKey) {
        boolean referenced = eventAttachmentRepository.existsByFileMetadataObjectKeyAndEventDeletedFalse(objectKey);
        if (referenced) {
            log.warn("파일 삭제 차단 (행사 첨부파일 참조): objectKey={}", objectKey);
        }
        return referenced;
    }
}
