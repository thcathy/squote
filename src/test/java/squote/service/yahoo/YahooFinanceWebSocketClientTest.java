package squote.service.yahoo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class YahooFinanceWebSocketClientTest {

    private YahooFinanceWebSocketClient client;
    private AtomicInteger callbackCount;

    @BeforeEach
    void setUp() {
        client = new YahooFinanceWebSocketClient();
        callbackCount = new AtomicInteger(0);
    }

    @Test
    void testOnTickerUpdateWithCallbacks() {
        var mockTicker = createMockTicker("AAPL", 150.0f, 2.5f);
        client.onTickerUpdate(mockTicker);
        client.addTickerCallback(ticker -> callbackCount.incrementAndGet());
        client.onTickerUpdate(mockTicker);
        client.onTickerUpdate(mockTicker);
        assertEquals(2, callbackCount.get());
    }

    @Test
    void testOnTickerUpdateWithExceptionInCallback() {
        YahooTicker mockTicker = createMockTicker("AAPL", 150.0f, 2.5f);
        client.addTickerCallback(ticker -> {
            callbackCount.incrementAndGet();
            throw new RuntimeException("Test exception");
        });
        assertDoesNotThrow(() -> client.onTickerUpdate(mockTicker));
    }

    @Test
    @Timeout(30)
    void testRealConnectionAndDataReceiving() throws InterruptedException {
        var client = new YahooFinanceWebSocketClient();
        var tickerCount = new AtomicInteger(0);
        var lastTicker = new AtomicReference<YahooTicker>();
        var dataReceivedLatch = new CountDownLatch(1);

        client.addTickerCallback(ticker -> {
            tickerCount.incrementAndGet();
            lastTicker.set(ticker);
            dataReceivedLatch.countDown();
        });

        client.connect();
        var connected = client.waitForConnection(10, TimeUnit.SECONDS);
        assertTrue(connected);
        assertTrue(client.isOpen());

        client.subscribeToSymbols("QQQ", "SPY");
        assertTrue(dataReceivedLatch.await(30, TimeUnit.SECONDS));
        var ticker = lastTicker.get();
        assertNotNull(ticker.getId());
        assertTrue(ticker.getPrice() > 0);
        assertTrue(ticker.getTime() > 0);
    }

    private YahooTicker createMockTicker(String id, float price, float changePercent) {
        YahooTicker ticker = new YahooTicker();
        ticker.setId(id);
        ticker.setPrice(price);
        ticker.setChangePercent(changePercent);
        ticker.setTime(System.currentTimeMillis() / 1000);
        return ticker;
    }
} 
