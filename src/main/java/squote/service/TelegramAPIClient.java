package squote.service;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class TelegramAPIClient {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	final String CHATID_SEPARATOR = ",";

    @Value("${telegramAPIClient.botToken}") String botToken;
	@Value("${telegramAPIClient.chatIds}") String chatIds;
	public final static String BASE_URL = "https://api.telegram.org/";
	private final OkHttpClient httpClient;
	private final String baseUrl;

	public TelegramAPIClient() {
		this(BASE_URL);
	}

	// for testing
	public TelegramAPIClient(String baseUrl) {
		this.baseUrl = baseUrl;
		this.httpClient = new OkHttpClient();
	}

	public List<String> sendMessage(String message) {
		return Arrays.stream(chatIds.split(CHATID_SEPARATOR))
				.map(chatId -> sendMessage(chatId, message))
				.toList();
	}

	public String sendMessage(String chatId, String message) {

		try {
			String encodedChatId = URLEncoder.encode(chatId, StandardCharsets.UTF_8);
			String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
			HttpUrl url = HttpUrl.parse(String.format("%sbot%s/sendMessage?chat_id=%s&text=%s",
					baseUrl, botToken, encodedChatId, encodedMessage));
			Request request = new Request.Builder().url(url).build();

			try (Response response = httpClient.newCall(request).execute()) {
				var content = response.body().string();
				if (!response.isSuccessful() || isFailedResponse(content)) {
					String errorDescription = extractErrorDescription(content);
					throw new RuntimeException(errorDescription);
				}
				return content;
			}
		} catch (IOException e) {
			throw new RuntimeException("HTTP request failed", e);
		}
	}

	private static boolean isFailedResponse(String response) {
		try {
			JSONObject json = new JSONObject(response);
			return !json.getBoolean("ok");
		} catch (Exception e) {
			return true;
		}
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
    
