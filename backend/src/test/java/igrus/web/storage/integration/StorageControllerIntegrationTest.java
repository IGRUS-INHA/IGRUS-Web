package igrus.web.storage.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.storage.dto.ConfirmUploadRequest;
import igrus.web.storage.dto.CreatePresignedUrlRequest;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StorageController MockMvc 통합 테스트.
 *
 * <p>인증/인가, Bean Validation 경계값, HTTP 상태 코드를 검증합니다.</p>
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>TC-001: 최대 허용 크기(10MB) 업로드 URL 요청 성공</li>
 *     <li>TC-002: 최대 허용 크기 초과(10MB+1B) 거부</li>
 *     <li>TC-004: 파일 크기 0바이트 거부</li>
 *     <li>TC-005: 허용된 4종 파일 타입 성공</li>
 *     <li>TC-006: 금지된 파일 타입 거부</li>
 *     <li>TC-013: JWT 없이 업로드 URL 요청 시 401</li>
 *     <li>TC-030: 미인증 업로드 URL 요청 401</li>
 *     <li>TC-032: 미인증 다운로드 URL 요청 401</li>
 *     <li>TC-035: ASSOCIATE/MEMBER 삭제 시 403</li>
 *     <li>TC-037: 파일 크기 음수(-1) 거부</li>
 *     <li>TC-038: 파일 크기 null 거부</li>
 *     <li>TC-039: 소형 파일(1KB) 성공</li>
 *     <li>TC-040: 대형 파일(50MB) 거부</li>
 *     <li>TC-041: 최소 길이 파일명(1자) 성공</li>
 *     <li>TC-042: 최대 길이 파일명(255자) 성공</li>
 *     <li>TC-043: 파일명 256자 이상 거부</li>
 *     <li>TC-044: 빈 문자열 파일명 거부</li>
 *     <li>TC-045: null 파일명 거부</li>
 *     <li>TC-046: 한글 파일명 성공</li>
 *     <li>TC-047: 특수문자 포함 파일명 성공</li>
 *     <li>TC-048: 공백만 파일명 거부</li>
 *     <li>TC-049: null Content-Type 거부</li>
 *     <li>TC-050: 빈 objectKey 완료 알림 거부</li>
 *     <li>TC-029: 다른 사용자 업로드 완료 알림 403</li>
 *     <li>TC-031: 미존재 Object Key 다운로드 404</li>
 * </ul>
 * </p>
 */
@AutoConfigureMockMvc
@DisplayName("StorageController 통합 테스트 (인증/인가 + 경계값)")
class StorageControllerIntegrationTest extends ServiceIntegrationTestBase {

    private static final String PRESIGNED_URL_PATH = "/api/v1/storage/presigned-url";
    private static final String CONFIRM_PATH = "/api/v1/storage/confirm";
    private static final String DOWNLOAD_URL_PATH = "/api/v1/storage/download-url";
    private static final String DELETE_PATH = "/api/v1/storage";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private Clock clock;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User associateUser;
    private User memberUser;
    private User operatorUser;
    private String associateToken;
    private String memberToken;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        setUpBase();

        transactionTemplate.execute(status -> {
            associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
            memberUser = createAndSaveUser("20230002", "member@inha.edu", UserRole.MEMBER);
            operatorUser = createAndSaveUser("20230003", "operator@inha.edu", UserRole.OPERATOR);
            return null;
        });

        associateToken = generateToken(associateUser);
        memberToken = generateToken(memberUser);
        operatorToken = generateToken(operatorUser);

        setupClockMock();
        setupS3PresignerMock();
    }

    private String generateToken(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getStudentId(), user.getRole().name());
    }

    /**
     * Clock Mock 설정. ObjectKeyGenerator가 날짜 경로를 생성할 수 있도록 합니다.
     */
    private void setupClockMock() {
        Instant fixedInstant = Instant.parse("2026-02-26T10:00:00Z");
        Mockito.when(clock.instant()).thenReturn(fixedInstant);
        Mockito.when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    /**
     * S3Presigner Mock 설정. 업로드 URL 생성 시 가짜 HTTPS URL을 반환하도록 설정합니다.
     */
    private void setupS3PresignerMock() {
        Mockito.reset(s3Presigner);
        try {
            PresignedPutObjectRequest mockPresignedRequest = Mockito.mock(PresignedPutObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(new URI("https://test-bucket.s3.amazonaws.com/test-key").toURL());
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedRequest);
        } catch (Exception e) {
            throw new RuntimeException("Mock 설정 실패", e);
        }
    }

    // ==================== 인증/인가 테스트 ====================

    @Nested
    @DisplayName("인증/인가 테스트")
    class AuthenticationAuthorizationTests {

        @Test
        @DisplayName("[TC-013] JWT 없이 업로드 URL 요청 시 401 응답")
        void createUploadUrl_WithoutJwt_Returns401() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "test.png", "image/png", 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[TC-030] 미인증 업로드 URL 요청 시 401 응답")
        void createUploadUrl_Unauthenticated_Returns401() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "test.png", "image/png", 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[TC-032] 미인증 다운로드 URL 요청 시 401 응답")
        void createDownloadUrl_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get(DOWNLOAD_URL_PATH)
                            .param("objectKey", "posts/2026/02/26/test-uuid.png"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("미인증 업로드 완료 확인 요청 시 401 응답")
        void confirmUpload_Unauthenticated_Returns401() throws Exception {
            ConfirmUploadRequest request = new ConfirmUploadRequest("posts/2026/02/26/test-uuid.png");

            mockMvc.perform(post(CONFIRM_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("미인증 파일 삭제 요청 시 401 응답")
        void deleteFile_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(delete(DELETE_PATH)
                            .param("objectKey", "posts/2026/02/26/test-uuid.png"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[TC-035] ASSOCIATE 권한으로 파일 삭제 시 403 응답")
        void deleteFile_AssociateRole_Returns403() throws Exception {
            mockMvc.perform(delete(DELETE_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .param("objectKey", "posts/2026/02/26/test-uuid.png"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[TC-035] MEMBER 권한으로 파일 삭제 시 403 응답")
        void deleteFile_MemberRole_Returns403() throws Exception {
            mockMvc.perform(delete(DELETE_PATH)
                            .header("Authorization", "Bearer " + memberToken)
                            .param("objectKey", "posts/2026/02/26/test-uuid.png"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[TC-029] 다른 사용자의 업로드 완료 알림 시 403 응답")
        void confirmUpload_DifferentUser_Returns403() throws Exception {
            // 사용자 A(associate)가 업로드 URL을 생성하여 FileMetadata를 DB에 저장
            CreatePresignedUrlRequest uploadRequest = new CreatePresignedUrlRequest(
                    "test.png", "image/png", 1024L, "posts"
            );

            String uploadResponse = mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(uploadRequest)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String objectKey = objectMapper.readTree(uploadResponse).get("objectKey").asText();

            // 사용자 B(member)의 토큰으로 완료 알림 요청 -> 403 기대
            ConfirmUploadRequest confirmRequest = new ConfirmUploadRequest(objectKey);

            mockMvc.perform(post(CONFIRM_PATH)
                            .header("Authorization", "Bearer " + memberToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(confirmRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[TC-031] 미존재 Object Key 다운로드 URL 요청 시 404 응답")
        void createDownloadUrl_NonExistentObjectKey_Returns404() throws Exception {
            mockMvc.perform(get(DOWNLOAD_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .param("objectKey", "posts/2026/02/26/00000000-0000-0000-0000-000000000000.png"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== 파일 크기 경계값 테스트 ====================

    @Nested
    @DisplayName("파일 크기 경계값 테스트")
    class FileSizeBoundaryTests {

        @Test
        @DisplayName("[TC-001] 최대 허용 크기(10MB) 업로드 URL 요청 성공")
        void createUploadUrl_MaxSize10MB_Returns200() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "test.png", "image/png", 10485760L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presignedUrl").exists())
                    .andExpect(jsonPath("$.objectKey").exists());
        }

        @Test
        @DisplayName("[TC-003] 최소 유효 크기(1B) 업로드 URL 요청 성공")
        void createUploadUrl_MinSize1Byte_Returns200() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "tiny.png", "image/png", 1L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presignedUrl").exists())
                    .andExpect(jsonPath("$.objectKey").exists());
        }

        @Test
        @DisplayName("[TC-002] 최대 허용 크기 초과(10MB+1B) 거부")
        void createUploadUrl_ExceedsMaxSize_Returns400() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "large.png", "image/png", 10485761L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[TC-004] 파일 크기 0바이트 거부")
        void createUploadUrl_ZeroBytes_Returns400() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "empty.png", "image/png", 0L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[TC-037] 파일 크기 음수(-1) 거부")
        void createUploadUrl_NegativeSize_Returns400() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "test.png", "image/png", -1L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[TC-038] 파일 크기 null 거부")
        void createUploadUrl_NullSize_Returns400() throws Exception {
            // fileSize 필드를 null로 전송하기 위해 수동 JSON 구성
            String json = """
                    {
                        "fileName": "test.png",
                        "contentType": "image/png",
                        "purpose": "posts"
                    }
                    """;

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[TC-039] 소형 파일(1KB) 업로드 URL 요청 성공")
        void createUploadUrl_SmallFile1KB_Returns200() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "small.png", "image/png", 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presignedUrl").exists())
                    .andExpect(jsonPath("$.objectKey").exists());
        }

        @Test
        @DisplayName("[TC-040] 대형 파일(50MB) 거부")
        void createUploadUrl_LargeFile50MB_Returns400() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "huge.png", "image/png", 52428800L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Content-Type 테스트 ====================

    @Nested
    @DisplayName("Content-Type 테스트")
    class ContentTypeTests {

        @ParameterizedTest(name = "[TC-005] 허용된 Content-Type: {0}")
        @ValueSource(strings = {"image/jpeg", "image/png", "image/gif", "image/webp"})
        @DisplayName("[TC-005] 허용된 4종 파일 타입 성공")
        void createUploadUrl_AllowedContentTypes_Returns200(String contentType) throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "test.png", contentType, 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presignedUrl").exists())
                    .andExpect(jsonPath("$.objectKey").exists());
        }

        @ParameterizedTest(name = "[TC-006] 금지된 Content-Type: {0}")
        @ValueSource(strings = {"image/bmp", "image/svg+xml", "image/tiff", "application/pdf", "text/plain"})
        @DisplayName("[TC-006] 금지된 파일 타입 거부")
        void createUploadUrl_ForbiddenContentTypes_Returns400(String contentType) throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "test.bmp", contentType, 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[TC-049] null Content-Type 거부")
        void createUploadUrl_NullContentType_Returns400() throws Exception {
            String json = """
                    {
                        "fileName": "test.png",
                        "fileSize": 1024,
                        "purpose": "posts"
                    }
                    """;

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== 파일명 경계값 테스트 ====================

    @Nested
    @DisplayName("파일명 경계값 테스트")
    class FileNameBoundaryTests {

        @Test
        @DisplayName("[TC-041] 최소 길이 파일명(1자) 성공")
        void createUploadUrl_MinLengthFileName_Returns200() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "a", "image/png", 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presignedUrl").exists())
                    .andExpect(jsonPath("$.objectKey").exists());
        }

        @Test
        @DisplayName("[TC-042] 최대 길이 파일명(255자) 성공")
        void createUploadUrl_MaxLengthFileName255_Returns200() throws Exception {
            String fileName = "a".repeat(251) + ".png"; // 251 + 4 = 255자
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    fileName, "image/png", 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presignedUrl").exists())
                    .andExpect(jsonPath("$.objectKey").exists());
        }

        @Test
        @DisplayName("[TC-043] 파일명 256자 이상 거부")
        void createUploadUrl_FileNameExceeds255_Returns400() throws Exception {
            String fileName = "a".repeat(252) + ".png"; // 252 + 4 = 256자
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    fileName, "image/png", 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[TC-044] 빈 문자열 파일명 거부")
        void createUploadUrl_EmptyFileName_Returns400() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "", "image/png", 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[TC-045] null 파일명 거부")
        void createUploadUrl_NullFileName_Returns400() throws Exception {
            String json = """
                    {
                        "contentType": "image/png",
                        "fileSize": 1024,
                        "purpose": "posts"
                    }
                    """;

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[TC-046] 한글 파일명 성공")
        void createUploadUrl_KoreanFileName_Returns200() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "동아리 사진.png", "image/png", 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presignedUrl").exists())
                    .andExpect(jsonPath("$.objectKey").exists());
        }

        @Test
        @DisplayName("[TC-047] 특수문자 포함 파일명 성공")
        void createUploadUrl_SpecialCharFileName_Returns200() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "photo (1).png", "image/png", 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presignedUrl").exists())
                    .andExpect(jsonPath("$.objectKey").exists());
        }

        @Test
        @DisplayName("[TC-048] 공백만 파일명 거부")
        void createUploadUrl_WhitespaceOnlyFileName_Returns400() throws Exception {
            CreatePresignedUrlRequest request = new CreatePresignedUrlRequest(
                    "   ", "image/png", 1024L, "posts"
            );

            mockMvc.perform(post(PRESIGNED_URL_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== objectKey 입력 검증 테스트 ====================

    @Nested
    @DisplayName("objectKey 입력 검증 테스트")
    class ObjectKeyValidationTests {

        @Test
        @DisplayName("[TC-050] 빈 objectKey 완료 알림 거부")
        void confirmUpload_EmptyObjectKey_Returns400() throws Exception {
            ConfirmUploadRequest request = new ConfirmUploadRequest("");

            mockMvc.perform(post(CONFIRM_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("null objectKey 완료 알림 거부")
        void confirmUpload_NullObjectKey_Returns400() throws Exception {
            String json = """
                    {}
                    """;

            mockMvc.perform(post(CONFIRM_PATH)
                            .header("Authorization", "Bearer " + associateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }
}
