package squote.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import squote.SquoteConstants;
import squote.domain.StockQuote;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class TiingoAPIClient {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    @Value("${tiingoAPIClient.token}") String token;
    
    public final static String BASE_URL = "https://api.tiingo.com/";
    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public TiingoAPIClient() {
        this(BASE_URL);
    }

    public TiingoAPIClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public Future<List<StockQuote>> getPrices(Collection<String> tickers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var tickerList = String.join(",", tickers);
                var url = HttpUrl.parse(baseUrl + "iex")
                        .newBuilder()
                        .addQueryParameter("tickers", tickerList)
                        .addQueryParameter("token", token)
                        .build();

                var request = new Request.Builder().url(url).build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Failed to fetch prices: " + response.message());
                    }
                    var responseBody = response.body().string();
                    var tiingoDataList = objectMapper.readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {});
                    return tiingoDataList.stream()
                            .map(this::mapToStockQuote)
                            .collect(Collectors.toList());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to fetch prices", e);
            }
        });
    }

    private StockQuote mapToStockQuote(Map<String, Object> tiingoData) {
        StockQuote quote = new StockQuote();
        quote.setStockCode((String) tiingoData.get("ticker"));
        quote.setStockName((String) tiingoData.get("ticker"));
        var lastValue = tiingoData.get("last");
        var tngoLastValue = tiingoData.get("tngoLast");
        
        if (lastValue != null) {
            quote.setPrice(formatPrice(lastValue));
        } else if (tngoLastValue != null) {
            quote.setPrice(formatPrice(tngoLastValue));
        }

        if (tiingoData.get("high") != null) quote.setHigh(formatPrice(tiingoData.get("high")));
        if (tiingoData.get("low") != null) quote.setLow(formatPrice(tiingoData.get("low")));
        
        var prevCloseValue = tiingoData.get("prevClose");
        var currentPrice = lastValue != null ? lastValue : tngoLastValue;
        if (prevCloseValue != null && currentPrice != null) {
            try {
                var prev = ((Number) prevCloseValue).doubleValue();
                var current = ((Number) currentPrice).doubleValue();
                var changeAmount = current - prev;
                var changePercent = (changeAmount / prev) * 100;
                quote.setChangeAmount(formatPrice(changeAmount));
                quote.setChange(BigDecimal.valueOf(changePercent).setScale(2, RoundingMode.HALF_UP).toString());
            } catch (Exception e) {
                log.warn("Failed to calculate change for ticker: {}", tiingoData.get("ticker"), e);
            }
        }
        
        var timestampValue = tiingoData.get("timestamp");
        if (timestampValue != null) {
            try {
                var timestamp = timestampValue.toString();
                
                // Parse and convert to UTC
                var inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSSSSS][.SSSSSSSS][.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]XXX");
                var outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                var zonedDateTime = ZonedDateTime.parse(timestamp, inputFormatter);
                var utcDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
                
                quote.setLastUpdate(utcDateTime.format(outputFormatter));
            } catch (Exception e) {
                log.warn("Failed to parse timestamp for ticker: {}", tiingoData.get("ticker"), e);
                quote.setLastUpdate(timestampValue.toString());
            }
        }
        
        return quote;
    }
    
    private String formatPrice(Object priceValue) {
        try {
            double price = ((Number) priceValue).doubleValue();
            return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP).toString();
        } catch (Exception e) {
            return SquoteConstants.NA;
        }
    }
}
