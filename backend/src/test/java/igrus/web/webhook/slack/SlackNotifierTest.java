package igrus.web.webhook.slack;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("SlackNotifier 단위 테스트")
class SlackNotifierTest {

    private HttpServer server;
    private String url;
    private int responseStatus = 200;
    private final List<String> receivedBodies = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/hook", exchange -> {
            receivedBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();
        url = "http://localhost:" + server.getAddress().getPort() + "/hook";
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("텍스트를 Slack 웹훅 형식(JSON text)으로 POST 한다")
    void send_PostsJsonText() {
        new SlackNotifier(url, Runnable::run).send("새 문의 접수");

        assertThat(receivedBodies).containsExactly("{\"text\":\"새 문의 접수\"}");
    }

    @Test
    @DisplayName("사용자 입력의 멘션/링크 문법을 이스케이프한다")
    void send_EscapesSlackControlCharacters() {
        new SlackNotifier(url, Runnable::run).send("<!channel> a&b");

        assertThat(receivedBodies).containsExactly("{\"text\":\"&lt;!channel&gt; a&amp;b\"}");
    }

    @Test
    @DisplayName("URL 이 비어 있으면 아무것도 보내지 않는다")
    void send_BlankUrl_NoOp() {
        new SlackNotifier("", Runnable::run).send("무시");

        assertThat(receivedBodies).isEmpty();
    }

    @Test
    @DisplayName("Slack 이 에러를 응답해도 예외를 던지지 않는다")
    void send_ServerError_DoesNotThrow() {
        responseStatus = 500;

        assertThatCode(() -> new SlackNotifier(url, Runnable::run).send("실패"))
                .doesNotThrowAnyException();
        assertThat(receivedBodies).hasSize(1);
    }

    @Test
    @DisplayName("연결 실패나 작업 큐 포화에도 예외를 던지지 않는다")
    void send_ConnectionFailureOrRejected_DoesNotThrow() {
        server.stop(0);

        assertThatCode(() -> new SlackNotifier(url, Runnable::run).send("연결 실패"))
                .doesNotThrowAnyException();
        assertThatCode(() -> new SlackNotifier(url, task -> {
            throw new RejectedExecutionException();
        }).send("큐 포화"))
                .doesNotThrowAnyException();
    }
}
