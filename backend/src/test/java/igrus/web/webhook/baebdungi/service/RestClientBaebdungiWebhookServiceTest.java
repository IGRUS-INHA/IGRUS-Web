package igrus.web.webhook.baebdungi.service;

import igrus.web.common.config.BaebdungiWebhookProperties;
import igrus.web.webhook.baebdungi.dto.BaebdungiSubmissionResponse;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RestClientBaebdungiWebhookService 단위 테스트")
class RestClientBaebdungiWebhookServiceTest {

    @Mock
    private RestClient restClient;

    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private RestClientBaebdungiWebhookService webhookService;

    private User testUser;

    @BeforeEach
    void setUp() {
        BaebdungiWebhookProperties properties = new BaebdungiWebhookProperties(
                "https://test.example.com/webhooks", "test-secret", true, 5000
        );
        webhookService = new RestClientBaebdungiWebhookService(restClient, properties);

        testUser = User.create(
                "20231234", "홍길동", "test@inha.edu",
                "010-1234-5678", "컴퓨터공학과", null,
                List.of(), Gender.MALE, 2,
                List.of(), null, null, null
        );

        given(restClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.retrieve()).willReturn(responseSpec);
    }

    @Nested
    @DisplayName("웹훅 호출 성공")
    class SendSubmissionSuccessTest {

        @Test
        @DisplayName("200 응답 시 정상 처리된다")
        void sendSubmission_Success_CompletesNormally() {
            // given
            BaebdungiSubmissionResponse response = new BaebdungiSubmissionResponse(
                    true, "sub-123", "성공", null
            );
            given(responseSpec.body(BaebdungiSubmissionResponse.class)).willReturn(response);

            // when & then
            assertThatCode(() -> webhookService.sendSubmission(testUser))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("웹훅 호출 실패")
    class SendSubmissionFailureTest {

        @Test
        @DisplayName("4xx 클라이언트 에러 시 예외를 던지지 않는다 (재시도 불가)")
        void sendSubmission_ClientError_DoesNotThrow() {
            // given
            given(responseSpec.body(BaebdungiSubmissionResponse.class))
                    .willThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)));

            // when & then
            assertThatCode(() -> webhookService.sendSubmission(testUser))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("5xx 서버 에러 시 예외가 전파된다 (재시도 대상)")
        void sendSubmission_ServerError_ThrowsException() {
            // given
            given(responseSpec.body(BaebdungiSubmissionResponse.class))
                    .willThrow(new HttpServerErrorException(HttpStatusCode.valueOf(500)));

            // when & then
            assertThatThrownBy(() -> webhookService.sendSubmission(testUser))
                    .isInstanceOf(HttpServerErrorException.class);
        }
    }

    @Nested
    @DisplayName("웹훅 비활성화")
    class DisabledWebhookTest {

        @Test
        @DisplayName("enabled=false일 때 HTTP 호출 없이 반환된다")
        void sendSubmission_Disabled_SkipsHttpCall() {
            // given
            BaebdungiWebhookProperties disabledProperties = new BaebdungiWebhookProperties(
                    "https://test.example.com/webhooks", "test-secret", false, 5000
            );
            RestClientBaebdungiWebhookService disabledService =
                    new RestClientBaebdungiWebhookService(restClient, disabledProperties);

            // when & then
            assertThatCode(() -> disabledService.sendSubmission(testUser))
                    .doesNotThrowAnyException();
            verify(restClient, never()).post();
        }
    }
}
