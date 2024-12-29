package squote.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TelegramAPIClientTest {
    private TelegramAPIClient telegramAPIClient;
    private static MockWebServer mockWebServer;

    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();

        telegramAPIClient = new TelegramAPIClient(WebClient.builder(), baseUrl);
        telegramAPIClient.botToken = "testBotToken";
        telegramAPIClient.chatIds = List.of("chatId1", "chatId2", "chatId3");
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testSendMessageSuccess() {
        String chatId = "12345";
        String message = "Hello, World!";
        String mockResponse = "{\"ok\": true, \"result\": {}}";

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        String response = telegramAPIClient.sendMessage(chatId, message).block();
        assertThat(mockResponse).isEqualTo(response);
    }

    @Test
    public void testSendMessageUnauthorized() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ok\": false, \"error_code\": 401, \"description\": \"Unauthorized\"}")
                .addHeader("Content-Type", "application/json"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> telegramAPIClient.sendMessage("123456", "Hello").block());
        assertThat(thrown).hasMessage("Unauthorized");
    }

    @Test
    public void testSendMessageChatNotFound() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ok\": false, \"error_code\": 400, \"description\": \"Bad Request: chat not found\"}")
                .addHeader("Content-Type", "application/json"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> telegramAPIClient.sendMessage("invalid_chat_id", "Hello").block());
        assertThat(thrown).hasMessage("Bad Request: chat not found");
    }

    @Test
    public void testSendMessageEmptyMessage() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ok\": false, \"error_code\": 400, \"description\": \"Bad Request: message text is empty\"}")
                .addHeader("Content-Type", "application/json"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> telegramAPIClient.sendMessage("123456", "").block());
        assertThat(thrown).hasMessage("Bad Request: message text is empty");
    }

    @Test
    void testSendMessageToAllChatIds() {
        String message = "Hello to all!";
        String mockResponse = "{\"ok\": true, \"result\": {}}";

        for (int i = 0; i < 3; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(mockResponse)
                    .addHeader("Content-Type", "application/json"));
        }

        Flux<String> responseFlux = telegramAPIClient.sendMessage(message);
        List<String> responses = responseFlux.collectList().block();
        assertThat(responses).hasSize(3);
        assertThat(responses).containsExactly(mockResponse, mockResponse, mockResponse);
    }
}
