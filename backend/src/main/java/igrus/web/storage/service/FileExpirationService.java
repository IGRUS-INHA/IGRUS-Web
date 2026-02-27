package igrus.web.storage.service;

import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.domain.FileUploadStatus;
import igrus.web.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 파일 만료 처리 서비스.
 *
 * <p>REQUESTED 상태에서 24시간이 경과한 파일 메타데이터를 EXPIRED로 전환한다.
 * 스케줄러에서 호출되며, 트랜잭션 내에서 실행된다.</p>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class FileExpirationService {

    private static final long EXPIRATION_HOURS = 24;

    private final FileMetadataRepository fileMetadataRepository;
    private final Clock clock;

    /**
     * REQUESTED 상태에서 24시간 경과된 파일 메타데이터를 EXPIRED로 전환한다.
     *
     * @return 만료 처리된 레코드 수
     */
    public int expireStaleRequests() {
        Instant threshold = Instant.now(clock).minus(EXPIRATION_HOURS, ChronoUnit.HOURS);

        List<FileMetadata> staleFiles = fileMetadataRepository
                .findByStatusAndCreatedAtBefore(FileUploadStatus.REQUESTED, threshold);

        for (FileMetadata file : staleFiles) {
            file.expire();
        }

        return staleFiles.size();
    }
}
