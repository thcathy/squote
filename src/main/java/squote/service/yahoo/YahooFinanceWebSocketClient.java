package squote.service.yahoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.service.YahooProtobufParser;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class YahooFinanceWebSocketClient extends WebSocketClient {
    
    private static final Logger log = LoggerFactory.getLogger(YahooFinanceWebSocketClient.class);
    private static final String YAHOO_FINANCE_WEBSOCKET_URL = "wss://streamer.finance.yahoo.com/?version=2";
    private final CountDownLatch connectionLatch;
    private final ObjectMapper objectMapper;
    private final List<Consumer<YahooTicker>> tickerCallbacks;
    
    public YahooFinanceWebSocketClient() {
        super(URI.create(YAHOO_FINANCE_WEBSOCKET_URL));
        this.connectionLatch = new CountDownLatch(1);
        this.objectMapper = new ObjectMapper();
        this.tickerCallbacks = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.info("YahooFinanceWebSocketClient connection opened");
        connectionLatch.countDown();
    }
    
    @Override
    public void onMessage(String message) {
        log.debug("Received text message: {}", message);
        
        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            if (jsonNode.has("type")
                    && "pricing".equals(jsonNode.get("type").asText())
                    && jsonNode.has("message")) {

                var ticker = YahooProtobufParser.parseYahooTicker(jsonNode.get("message").asText());
                if (ticker != null) {
                    log.info("Parsed ticker from JSON message: {}", ticker);
                    onTickerUpdate(ticker);
                }

            } else {
                log.debug("Full message content: {}", message);
            }
        } catch (Exception e) {
            log.error("Error processing JSON message: {}", message, e);
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        log.warn("Unexpected byte message");
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("WebSocket connection closed: {} - {}", code, reason);
    }
    
    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error", ex);
    }
    
    protected void onTickerUpdate(YahooTicker ticker) {
        for (Consumer<YahooTicker> callback : tickerCallbacks) {
            try {
                callback.accept(ticker);
            } catch (Exception e) {
                log.error("Error executing callback for ticker {}", ticker.getId(), e);
            }
        }

//        // Default implementation when no callbacks are registered
//        System.out.printf("Ticker Update: %s = $%.2f (%.2f%%) at %d%n",
//            ticker.getId(), ticker.getPrice(), ticker.getChangePercent(), ticker.getTime());
//
//        // Additional information if available
//        if (ticker.getExchange() != null) {
//            System.out.printf("  Exchange: %s", ticker.getExchange());
//        }
//        if (ticker.getDayHigh() > 0 && ticker.getDayLow() > 0) {
//            System.out.printf("  Day Range: %.2f - %.2f", ticker.getDayLow(), ticker.getDayHigh());
//        }
//        if (ticker.getDayVolume() > 0) {
//            System.out.printf("  Volume: %d", ticker.getDayVolume());
//        }
//        System.out.println();
    }

    /**
     * Add a callback for ticker updates
     */
    public void addTickerCallback(Consumer<YahooTicker> callback) {
        tickerCallbacks.add(callback);
        log.debug("Added ticker callback, total callbacks: {}", tickerCallbacks.size());
    }

    /**
     * Remove a specific callback
     */
    public void removeTickerCallback(Consumer<YahooTicker> callback) {
        tickerCallbacks.remove(callback);
        log.debug("Removed ticker callback, remaining callbacks: {}", tickerCallbacks.size());
    }

    /**
     * Clear all callbacks
     */
    public void clearAllCallbacks() {
        tickerCallbacks.clear();
        log.debug("Cleared all ticker callbacks");
    }

    /**
     * Get the number of registered callbacks
     */
    public int getCallbackCount() {
        return tickerCallbacks.size();
    }

    /**
     * Check if there are any callbacks registered
     */
    public boolean hasCallbacks() {
        return !tickerCallbacks.isEmpty();
    }

    public void subscribeToSymbols(String... symbols) {
        if (!isOpen()) {
            log.warn("WebSocket not connected. Cannot subscribe to symbols.");
            return;
        }
        
        try {
            String subscribeMessage = String.format(
                "{\"subscribe\":[%s]}", 
                String.join(",", java.util.Arrays.stream(symbols)
                    .map(s -> "\"" + s + "\"")
                    .toArray(String[]::new))
            );
            
            send(subscribeMessage);
            log.info("Subscribed to symbols: {}", String.join(", ", symbols));
        } catch (Exception e) {
            log.error("Error subscribing to symbols", e);
        }
    }

    public boolean waitForConnection(long timeout, TimeUnit unit) {
        try {
            return connectionLatch.await(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        YahooFinanceWebSocketClient client = new YahooFinanceWebSocketClient();

        client.addTickerCallback(ticker -> {
            if ("IBIT".equals(ticker.getId())) {
                System.out.println("IBIT Callback: " + ticker.getPrice());
            }
        });
        
        client.addTickerCallback(ticker -> {
            System.out.println("General Callback: " + ticker.getId() + " = " + ticker.getPrice());
        });
        
        client.connect();
        if (client.waitForConnection(10, TimeUnit.SECONDS)) {
            log.info("Connected to Yahoo Finance WebSocket");
            client.subscribeToSymbols("IBIT", "AAPL");
            Thread.sleep(60000);
        }
        client.close();
    }
} 
