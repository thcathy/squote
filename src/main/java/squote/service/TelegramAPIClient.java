package squote.service;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class TelegramAPIClient {
	protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${telegramAPIClient.botToken}") String botToken;
	@Value("${telegramAPIClient.chatIds}") List<String> chatIds;
	private final WebClient webClient;
	public final static String BASE_URL = "https://api.telegram.org/";

	@Autowired
	public TelegramAPIClient(WebClient.Builder webClientBuilder) {
		this(webClientBuilder, BASE_URL);
	}

	// for testing
	public TelegramAPIClient(WebClient.Builder webClientBuilder, String baseUrl) {
		if (StringUtils.isBlank(baseUrl)) baseUrl = BASE_URL;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
	}

	public Flux<String> sendMessage(String message) {
		return Flux.fromIterable(chatIds)
				.flatMap(chatId -> sendMessage(chatId, message));
	}

	public Mono<String> sendMessage(String chatId, String message) {
		String url = String.format("bot%s/sendMessage?chat_id=%s&text=%s", botToken, chatId, message);
		return webClient.get().uri(url).retrieve().bodyToMono(String.class)
				.flatMap(response -> {
					if (isFailedResponse(response)) {
						String errorDescription = extractErrorDescription(response);
						log.error("Error sending message: {}", errorDescription);
						return Mono.error(new RuntimeException(errorDescription));
					}
					return Mono.just(response);
				});
	}

	private static boolean isFailedResponse(String response) {
		return response.contains("\"ok\": false");
	}

	private String extractErrorDescription(String response) {
		try {
			JSONObject json = new JSONObject(response);
			return json.getString("description");
		} catch (Exception e) {
			log.error("Failed to parse error response: {}", response, e);
			return "Unknown error";
		}
	}
}
    
