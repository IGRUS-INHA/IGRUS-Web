package igrus.web.event.integration;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventAttachment;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.repository.EventAttachmentRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.repository.FileMetadataRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 행사 이미지 다운로드 URL 엔드포인트 통합 테스트.
 *
 * <p>비인증 사용자가 공개(PUBLISHED) 행사의 이미지 다운로드 URL을
 * 발급받을 수 있는지 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>공개 행사의 유효한 첨부파일 objectKey → 200 + presignedUrl</li>
 *     <li>비공개(UNPUBLISHED) 행사 → 404</li>
 *     <li>존재하지 않는 행사 ID → 404</li>
 *     <li>해당 행사에 속하지 않는 objectKey → 404</li>
 *     <li>비인증 요청으로 접근 가능 (permitAll)</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("행사 이미지 다운로드 URL 통합 테스트")
class EventImageDownloadUrlIntegrationTest extends ServiceIntegrationTestBase {

    private static final String TEST_OBJECT_KEY = "EVENT_IMAGE/2026/03/09/test-uuid.png";
    private static final String PRESIGNED_URL = "https://test-bucket.s3.amazonaws.com/EVENT_IMAGE/2026/03/09/test-uuid.png?signed";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventAttachmentRepository eventAttachmentRepository;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private S3Presigner s3Presigner;

    private User operator;

    @BeforeEach
    void setUp() {
        setUpBase();
        setupS3PresignerMock();
        transactionTemplate.execute(status -> {
            operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
            return null;
        });
    }

    private void setupS3PresignerMock() {
        Mockito.reset(s3Presigner);
        try {
            PresignedGetObjectRequest mockPresignedRequest = Mockito.mock(PresignedGetObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(new URI(PRESIGNED_URL).toURL());
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedRequest);
        } catch (Exception e) {
            throw new RuntimeException("Mock 설정 실패", e);
        }
    }

    private Event createPublishedEvent() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "공개 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.AUTO_APPROVE,
                    null
            );
            event.publish();
            return eventRepository.save(event);
        });
    }

    private Event createUnpublishedEvent() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "비공개 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.AUTO_APPROVE,
                    null
            );
            return eventRepository.save(event);
        });
    }

    private FileMetadata createCompletedFileMetadata(String objectKey) {
        return transactionTemplate.execute(status -> {
            FileMetadata metadata = FileMetadata.create(
                    objectKey,
                    operator.getId(),
                    "test-image.png",
                    "image/png",
                    1024L
            );
            metadata.confirm();
            metadata.complete(Instant.now());
            return fileMetadataRepository.save(metadata);
        });
    }

    private void attachFileToEvent(Event event, FileMetadata fileMetadata) {
        transactionTemplate.execute(status -> {
            EventAttachment attachment = EventAttachment.create(event, fileMetadata);
            eventAttachmentRepository.save(attachment);
            return null;
        });
    }

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("공개 행사의 유효한 첨부파일 objectKey로 요청 시 presigned URL 반환")
        void getEventImageDownloadUrl_WithValidPublishedEvent_ReturnsPresignedUrl() throws Exception {
            // given
            Event event = createPublishedEvent();
            FileMetadata fileMetadata = createCompletedFileMetadata(TEST_OBJECT_KEY);
            attachFileToEvent(event, fileMetadata);

            // when & then
            mockMvc.perform(get("/api/v1/events/{eventId}/images/download-url", event.getId())
                            .param("objectKey", TEST_OBJECT_KEY))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presignedUrl").value(PRESIGNED_URL))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("비인증 요청으로도 공개 행사 이미지 다운로드 URL 발급 가능")
        void getEventImageDownloadUrl_WithoutAuthentication_ReturnsPresignedUrl() throws Exception {
            // given
            Event event = createPublishedEvent();
            FileMetadata fileMetadata = createCompletedFileMetadata(TEST_OBJECT_KEY);
            attachFileToEvent(event, fileMetadata);

            // when & then (Authorization 헤더 없이 요청)
            mockMvc.perform(get("/api/v1/events/{eventId}/images/download-url", event.getId())
                            .param("objectKey", TEST_OBJECT_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presignedUrl").exists());
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCases {

        @Test
        @DisplayName("비공개(UNPUBLISHED) 행사의 이미지 요청 시 404")
        void getEventImageDownloadUrl_WithUnpublishedEvent_Returns404() throws Exception {
            // given
            Event event = createUnpublishedEvent();
            FileMetadata fileMetadata = createCompletedFileMetadata(TEST_OBJECT_KEY);
            attachFileToEvent(event, fileMetadata);

            // when & then
            mockMvc.perform(get("/api/v1/events/{eventId}/images/download-url", event.getId())
                            .param("objectKey", TEST_OBJECT_KEY))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("존재하지 않는 행사 ID로 요청 시 404")
        void getEventImageDownloadUrl_WithNonExistentEvent_Returns404() throws Exception {
            mockMvc.perform(get("/api/v1/events/{eventId}/images/download-url", 999999L)
                            .param("objectKey", TEST_OBJECT_KEY))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("해당 행사에 속하지 않는 objectKey로 요청 시 404")
        void getEventImageDownloadUrl_WithWrongObjectKey_Returns404() throws Exception {
            // given
            Event event = createPublishedEvent();
            FileMetadata fileMetadata = createCompletedFileMetadata(TEST_OBJECT_KEY);
            attachFileToEvent(event, fileMetadata);

            // when & then
            mockMvc.perform(get("/api/v1/events/{eventId}/images/download-url", event.getId())
                            .param("objectKey", "EVENT_IMAGE/2026/03/09/wrong-uuid.png"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("다른 행사의 첨부파일 objectKey로 요청 시 404")
        void getEventImageDownloadUrl_WithOtherEventAttachment_Returns404() throws Exception {
            // given
            Event event1 = createPublishedEvent();
            Event event2 = createPublishedEvent();
            FileMetadata fileMetadata = createCompletedFileMetadata(TEST_OBJECT_KEY);
            attachFileToEvent(event1, fileMetadata);

            // when & then - event2로 event1의 첨부파일 요청
            mockMvc.perform(get("/api/v1/events/{eventId}/images/download-url", event2.getId())
                            .param("objectKey", TEST_OBJECT_KEY))
                    .andExpect(status().isNotFound());
        }
    }
}
